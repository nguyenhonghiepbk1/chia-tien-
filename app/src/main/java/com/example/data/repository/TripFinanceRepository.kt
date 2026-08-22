package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.entity.*
import com.example.domain.engine.SettlementEngine
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class TripFinanceRepository(private val db: AppDatabase) {

    // Trips
    val allTrips: Flow<List<TripEntity>> = db.tripDao().getAllTrips()

    fun getTrip(tripId: String): Flow<TripEntity?> = db.tripDao().getTripById(tripId)

    suspend fun createTrip(
        title: String,
        description: String,
        joinCode: String,
        startDate: Long,
        endDate: Long,
        adminName: String,
        adminBankName: String?,
        adminBankAccount: String?,
        adminBankHolder: String?
    ): String {
        val tripId = UUID.randomUUID().toString()
        val trip = TripEntity(
            id = tripId,
            title = title,
            description = description,
            joinCode = joinCode.uppercase().trim(),
            startDate = startDate,
            endDate = endDate,
            baseCurrency = "VND",
            isSettled = false,
            createdAt = System.currentTimeMillis()
        )
        db.tripDao().insertTrip(trip)

        // Add creator as Admin
        val adminMember = TripMemberEntity(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            userId = "user_" + UUID.randomUUID().toString().take(6),
            name = adminName,
            role = "ADMIN",
            isActive = true,
            bankName = adminBankName,
            bankAccount = adminBankAccount,
            bankAccountHolder = adminBankHolder,
            joinedAt = System.currentTimeMillis()
        )
        db.tripMemberDao().insertMember(adminMember)

        // Seed default exchange rates
        val rates = listOf(
            ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "USD", 25450.0),
            ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "EUR", 27600.0),
            ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "JPY", 168.0),
            ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "THB", 720.0),
            ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "SGD", 19200.0)
        )
        db.exchangeRateDao().insertExchangeRates(rates)

        logAction(
            tripId = tripId,
            actorMemberId = adminMember.id,
            actorName = adminName,
            action = "CREATE_TRIP",
            description = "Tạo mới đoàn '$title' (Mã: $joinCode)"
        )

        return tripId
    }

    suspend fun joinTripByCode(code: String, userName: String): String? {
        val trip = db.tripDao().getTripByJoinCode(code.uppercase().trim()) ?: return null
        val member = TripMemberEntity(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            userId = "user_" + UUID.randomUUID().toString().take(6),
            name = userName,
            role = "MEMBER",
            isActive = true,
            joinedAt = System.currentTimeMillis()
        )
        db.tripMemberDao().insertMember(member)
        logAction(
            tripId = trip.id,
            actorMemberId = member.id,
            actorName = userName,
            action = "JOIN_TRIP",
            description = "$userName đã tham gia đoàn qua mã $code"
        )
        return trip.id
    }

    suspend fun updateTrip(trip: TripEntity) = db.tripDao().updateTrip(trip)

    // Members
    fun getMembers(tripId: String): Flow<List<TripMemberEntity>> =
        db.tripMemberDao().getMembersByTrip(tripId)

    suspend fun addMember(
        tripId: String,
        name: String,
        role: String,
        bankName: String?,
        bankAccount: String?,
        bankAccountHolder: String?,
        actor: TripMemberEntity
    ): String {
        val memberId = UUID.randomUUID().toString()
        val member = TripMemberEntity(
            id = memberId,
            tripId = tripId,
            userId = "user_" + UUID.randomUUID().toString().take(6),
            name = name,
            role = role,
            isActive = true,
            bankName = bankName,
            bankAccount = bankAccount,
            bankAccountHolder = bankAccountHolder,
            joinedAt = System.currentTimeMillis()
        )
        db.tripMemberDao().insertMember(member)
        logAction(
            tripId = tripId,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "ADD_MEMBER",
            description = "Thêm thành viên $name với vai trò $role"
        )
        return memberId
    }

    suspend fun updateMember(member: TripMemberEntity, actor: TripMemberEntity) {
        db.tripMemberDao().updateMember(member)
        logAction(
            tripId = member.tripId,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "UPDATE_MEMBER",
            description = "Cập nhật thông tin thành viên ${member.name} (${member.role})"
        )
    }

    suspend fun removeOrDeactivateMember(member: TripMemberEntity, actor: TripMemberEntity) {
        val paidExpenseCount = db.expenseDao().countExpensesByPayer(member.id)
        val createdExpenseCount = db.expenseDao().countExpensesCreatedByMember(member.id)
        val splitCount = db.expenseDao().countSplitsByMember(member.id)
        val fundCount = db.fundDao().countFundContributionsByMember(member.id)
        val hasFinancialRecords = (paidExpenseCount + createdExpenseCount + splitCount + fundCount) > 0

        if (hasFinancialRecords) {
            // SRS 3: Thành viên có giao dịch tài chính không được phép "Xóa", chỉ được "Ngừng hoạt động (Deactivate)"
            db.tripMemberDao().setMemberActiveStatus(member.id, false)
            logAction(
                tripId = member.tripId,
                actorMemberId = actor.id,
                actorName = actor.name,
                action = "DEACTIVATE_MEMBER",
                description = "Ngừng hoạt động thành viên ${member.name} (đã phát sinh: $paidExpenseCount chi trả, $splitCount phân bổ, $fundCount đóng quỹ)"
            )
        } else {
            // CSDL sẽ kiểm tra thêm qua trigger trg_prevent_delete_member_with_financials và ForeignKey RESTRICT
            db.tripMemberDao().deleteMember(member.id)
            logAction(
                tripId = member.tripId,
                actorMemberId = actor.id,
                actorName = actor.name,
                action = "DELETE_MEMBER",
                description = "Xóa hoàn toàn thành viên ${member.name} khỏi đoàn (chưa có phát sinh chi tiêu)"
            )
        }
    }

    suspend fun deleteMemberDirect(memberId: String) {
        // Gọi xóa trực tiếp - SQLite Trigger & Foreign Key RESTRICT sẽ chặn đứng nếu có phát sinh tài chính
        db.tripMemberDao().deleteMember(memberId)
    }

    // Expenses
    fun getExpenses(tripId: String): Flow<List<ExpenseEntity>> =
        db.expenseDao().getExpensesByTrip(tripId)

    fun getSplitsForTrip(tripId: String): Flow<List<ExpenseSplitEntity>> =
        db.expenseDao().getAllSplitsByTrip(tripId)

    fun getSplitsByExpense(expenseId: String): Flow<List<ExpenseSplitEntity>> =
        db.expenseDao().getSplitsByExpense(expenseId)

    suspend fun addExpenseWithSplits(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>,
        actor: TripMemberEntity
    ) {
        db.expenseDao().insertExpense(expense)
        db.expenseDao().insertSplits(splits)
        val payerDesc = if (expense.payerType == "FUND") "Quỹ đoàn" else (actor.name)
        logAction(
            tripId = expense.tripId,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "CREATE_EXPENSE",
            description = "Tạo chi tiêu '${expense.title}': ${expense.convertedTotalAmount} VND (Người trả: $payerDesc)"
        )
    }

    suspend fun updateExpenseWithSplits(
        expense: ExpenseEntity,
        splits: List<ExpenseSplitEntity>,
        actor: TripMemberEntity
    ) {
        db.expenseDao().updateExpense(expense)
        db.expenseDao().deleteSplitsByExpense(expense.id)
        db.expenseDao().insertSplits(splits)
        logAction(
            tripId = expense.tripId,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "UPDATE_EXPENSE",
            description = "Cập nhật chi tiêu '${expense.title}': ${expense.convertedTotalAmount} VND"
        )
    }

    suspend fun deleteExpense(expense: ExpenseEntity, actor: TripMemberEntity) {
        db.expenseDao().deleteExpenseById(expense.id)
        db.expenseDao().deleteSplitsByExpense(expense.id)
        logAction(
            tripId = expense.tripId,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "DELETE_EXPENSE",
            description = "Xóa chi tiêu '${expense.title}' (${expense.convertedTotalAmount} VND)"
        )
    }

    // Funds
    fun getFundContributions(tripId: String): Flow<List<FundContributionEntity>> =
        db.fundDao().getFundContributions(tripId)

    suspend fun addFundContribution(
        contribution: FundContributionEntity,
        contributorName: String,
        actor: TripMemberEntity
    ) {
        db.fundDao().insertFundContribution(contribution)
        logAction(
            tripId = contribution.tripId,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "CONTRIBUTE_FUND",
            description = "$contributorName nộp quỹ ${contribution.convertedAmount} VND (Ghi nhận bởi ${actor.name})"
        )
    }

    // Exchange Rates
    fun getExchangeRates(tripId: String): Flow<List<ExchangeRateEntity>> =
        db.exchangeRateDao().getExchangeRates(tripId)

    suspend fun updateExchangeRate(rate: ExchangeRateEntity, actor: TripMemberEntity) {
        db.exchangeRateDao().insertExchangeRate(rate)
        logAction(
            tripId = rate.tripId,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "UPDATE_RATE",
            description = "Cập nhật tỷ giá 1 ${rate.currencyCode} = ${rate.rateToBase} VND"
        )
    }

    // Settlement & Snapshots
    fun getSnapshots(tripId: String): Flow<List<SettlementSnapshotEntity>> =
        db.settlementDao().getSnapshotsByTrip(tripId)

    suspend fun finalizeSettlement(
        trip: TripEntity,
        snapshotTitle: String,
        summary: FinancialSummary,
        settlementJson: String,
        actor: TripMemberEntity
    ) {
        val snapshot = SettlementSnapshotEntity(
            id = UUID.randomUUID().toString(),
            tripId = trip.id,
            snapshotTitle = snapshotTitle,
            createdAt = System.currentTimeMillis(),
            totalExpenses = summary.totalExpenses,
            totalFundCollected = summary.totalFundCollected,
            totalFundSpent = summary.fundPaidExpenses,
            remainingFund = summary.remainingFund,
            settlementJson = settlementJson
        )
        db.settlementDao().insertSnapshot(snapshot)
        db.tripDao().updateTrip(trip.copy(isSettled = true, settledAt = System.currentTimeMillis()))

        logAction(
            tripId = trip.id,
            actorMemberId = actor.id,
            actorName = actor.name,
            action = "SETTLE_TRIP",
            description = "Khóa sổ và quyết toán chuyến đi. Snapshot: $snapshotTitle"
        )
    }

    // Audit logs
    fun getAuditLogs(tripId: String): Flow<List<AuditLogEntity>> =
        db.auditLogDao().getAuditLogsByTrip(tripId)

    suspend fun logAction(
        tripId: String,
        actorMemberId: String,
        actorName: String,
        action: String,
        description: String,
        before: String? = null,
        after: String? = null
    ) {
        db.auditLogDao().insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                actorMemberId = actorMemberId,
                actorName = actorName,
                action = action,
                description = description,
                detailBefore = before,
                detailAfter = after,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Nguồn chân lý tính toán đối soát (SRS Section 2)
     * Paid(A): Chi hộ của cá nhân + Tiền nộp quỹ
     * Owed(A): Tổng tiền chịu chi trong các expenseSplits
     * Balance(A) = Paid(A) - Owed(A)
     */
    fun observeFinancialStatus(tripId: String): Flow<Pair<FinancialSummary, List<MemberFinancialStatus>>> {
        return combine(
            getMembers(tripId),
            getExpenses(tripId),
            getSplitsForTrip(tripId),
            getFundContributions(tripId)
        ) { members, expenses, splits, fundContributions ->
            val totalExpenseSum = expenses.sumOf { it.convertedTotalAmount }
            val fundPaidExpensesSum = expenses
                .filter { it.payerType == "FUND" }
                .sumOf { it.convertedTotalAmount }
            val personalPaidExpensesSum = expenses
                .filter { it.payerType == "MEMBER" }
                .sumOf { it.convertedTotalAmount }
            val totalFundCollected = fundContributions.sumOf { it.convertedAmount }
            val remainingFund = totalFundCollected - fundPaidExpensesSum

            // Tính toán tài chính cho từng thành viên
            val memberStatuses = members.map { member ->
                // 1. Chi hộ thực tế từ túi thành viên (expenses where payerMemberId == member.id and payerType == MEMBER)
                val outOfPocket = expenses
                    .filter { it.payerType == "MEMBER" && it.payerMemberId == member.id }
                    .sumOf { it.convertedTotalAmount }

                // 2. Tiền đã đóng góp vào Quỹ chung
                val fundContributed = fundContributions
                    .filter { it.memberId == member.id }
                    .sumOf { it.convertedAmount }

                // Tổng tiền thành viên đã thực tế chi/nộp cho đoàn
                val totalPaid = outOfPocket + fundContributed

                // 3. Tiền phải chịu chi (Owed) từ các bảng phân bổ
                val totalOwed = splits
                    .filter { it.memberId == member.id }
                    .sumOf { it.amount }

                // 4. Số dư ròng (Balance) = Paid - Owed
                val balance = totalPaid - totalOwed

                val status = when {
                    balance > 0 -> BalanceStatus.RECEIVE
                    balance < 0 -> BalanceStatus.PAY
                    else -> BalanceStatus.BALANCED
                }

                MemberFinancialStatus(
                    member = member,
                    totalPaid = totalPaid,
                    outOfPocketPaid = outOfPocket,
                    fundContributed = fundContributed,
                    totalOwed = totalOwed,
                    balance = balance,
                    status = status
                )
            }

            // Đối soát liên tục: Tổng Balance của toàn bộ thành viên đoàn
            // Chú ý: Quỹ đoàn còn dư (nếu có) phản ánh phần tiền chưa chi tiêu trong số tiền đã nộp
            val totalBalance = memberStatuses.sumOf { it.balance }
            val isBalanced = kotlin.math.abs(totalBalance - remainingFund) == 0L || kotlin.math.abs(totalBalance) == 0L

            val summary = FinancialSummary(
                totalExpenses = totalExpenseSum,
                personalPaidExpenses = personalPaidExpensesSum,
                fundPaidExpenses = fundPaidExpensesSum,
                totalFundCollected = totalFundCollected,
                remainingFund = remainingFund,
                isBalanced = isBalanced,
                balanceDiscrepancy = totalBalance,
                memberCount = members.size,
                expenseCount = expenses.size
            )

            summary to memberStatuses
        }
    }
}
