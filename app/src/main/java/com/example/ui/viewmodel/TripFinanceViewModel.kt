package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.TripFinanceRepository
import com.example.domain.engine.SettlementEngine
import com.example.domain.engine.SplitCalculator
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class UiState(
    val currentTrip: TripEntity? = null,
    val allTrips: List<TripEntity> = emptyList(),
    val members: List<TripMemberEntity> = emptyList(),
    val currentMember: TripMemberEntity? = null,
    val financialSummary: FinancialSummary = FinancialSummary(),
    val memberStatuses: List<MemberFinancialStatus> = emptyList(),
    val settlementTransfers: List<SettlementTransfer> = emptyList(),
    val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val fundContributions: List<FundContributionEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val snapshots: List<SettlementSnapshotEntity> = emptyList(),
    val isOfflineMode: Boolean = false,
    val pendingSyncCount: Int = 0,
    val selectedCategoryFilter: String? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class TripFinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripFinanceRepository

    private val _currentTripId = MutableStateFlow<String?>("trip_danang_2026")
    private val _currentMemberId = MutableStateFlow<String?>("member_1")
    private val _isOfflineMode = MutableStateFlow(false)
    private val _pendingSyncCount = MutableStateFlow(0)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState>

    private data class FilterState(
        val categoryFilter: String?,
        val query: String,
        val isOffline: Boolean,
        val pendingCount: Int,
        val currentMemberId: String?
    )

    private data class TripCoreData(
        val members: List<TripMemberEntity>,
        val expenses: List<ExpenseEntity>,
        val funds: List<FundContributionEntity>,
        val rates: List<ExchangeRateEntity>
    )

    private data class TripAuxData(
        val logs: List<AuditLogEntity>,
        val snapshots: List<SettlementSnapshotEntity>,
        val financialPair: Pair<FinancialSummary, List<MemberFinancialStatus>>
    )

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TripFinanceRepository(db)

        val filterFlow = combine(
            _selectedCategoryFilter,
            _searchQuery,
            _isOfflineMode,
            _pendingSyncCount,
            _currentMemberId
        ) { cat, q, off, pending, mId ->
            FilterState(cat, q, off, pending, mId)
        }

        uiState = combine(
            repository.allTrips,
            _currentTripId,
            filterFlow,
            _errorMessage,
            _successMessage
        ) { trips, activeTripId, filters, error, success ->
            val trip = trips.find { it.id == activeTripId } ?: trips.firstOrNull()
            Triple(trips, trip, filters) to (error to success)
        }.flatMapLatest { (triple, messagePair) ->
            val (trips, currentTrip, filters) = triple
            val (errorMsg, successMsg) = messagePair

            if (currentTrip == null) {
                flowOf(
                    UiState(
                        allTrips = trips,
                        isOfflineMode = filters.isOffline,
                        pendingSyncCount = filters.pendingCount,
                        errorMessage = errorMsg,
                        successMessage = successMsg
                    )
                )
            } else {
                val tripId = currentTrip.id

                val coreFlow = combine(
                    repository.getMembers(tripId),
                    repository.getExpenses(tripId),
                    repository.getFundContributions(tripId),
                    repository.getExchangeRates(tripId)
                ) { members, expenses, funds, rates ->
                    TripCoreData(members, expenses, funds, rates)
                }

                val auxFlow = combine(
                    repository.getAuditLogs(tripId),
                    repository.getSnapshots(tripId),
                    repository.observeFinancialStatus(tripId)
                ) { logs, snapshots, financialPair ->
                    TripAuxData(logs, snapshots, financialPair)
                }

                combine(coreFlow, auxFlow) { core, aux ->
                    val (summary, statuses) = aux.financialPair
                    val currentMember = core.members.find { it.id == filters.currentMemberId }
                        ?: core.members.firstOrNull()

                    val totalSpent = summary.totalExpenses.coerceAtLeast(1L)
                    val catMap = mapOf(
                        "FOOD" to "Ăn uống",
                        "TRANSPORT" to "Di chuyển",
                        "HOTEL" to "Lưu trú/Khách sạn",
                        "SIGHTSEEING" to "Vé tham quan",
                        "ENTERTAINMENT" to "Vui chơi/Giải trí",
                        "SHOPPING" to "Mua sắm",
                        "OTHER" to "Chi phí khác"
                    )

                    val breakdowns = core.expenses.groupBy { it.category }.map { (cat, list) ->
                        val amount = list.sumOf { it.convertedTotalAmount }
                        CategoryBreakdown(
                            category = cat,
                            labelVi = catMap[cat] ?: cat,
                            iconName = cat,
                            totalAmount = amount,
                            percentage = (amount.toDouble() / totalSpent.toDouble()) * 100.0,
                            count = list.size
                        )
                    }.sortedByDescending { it.totalAmount }

                    val transfers = SettlementEngine.computeSimplifiedTransfers(
                        memberStatuses = statuses,
                        tripJoinCode = currentTrip.joinCode
                    )

                    val filteredExpenses = core.expenses.filter { exp ->
                        val matchesCat = filters.categoryFilter == null || exp.category == filters.categoryFilter
                        val matchesSearch = filters.query.isBlank() ||
                                exp.title.contains(filters.query, ignoreCase = true) ||
                                exp.note.contains(filters.query, ignoreCase = true)
                        matchesCat && matchesSearch
                    }

                    UiState(
                        currentTrip = currentTrip,
                        allTrips = trips,
                        members = core.members,
                        currentMember = currentMember,
                        financialSummary = summary,
                        memberStatuses = statuses,
                        settlementTransfers = transfers,
                        categoryBreakdowns = breakdowns,
                        expenses = filteredExpenses,
                        fundContributions = core.funds,
                        exchangeRates = core.rates,
                        auditLogs = aux.logs,
                        snapshots = aux.snapshots,
                        isOfflineMode = filters.isOffline,
                        pendingSyncCount = filters.pendingCount,
                        selectedCategoryFilter = filters.categoryFilter,
                        searchQuery = filters.query,
                        errorMessage = errorMsg,
                        successMessage = successMsg
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState()
        )
    }

    fun selectTrip(tripId: String) {
        _currentTripId.value = tripId
    }

    fun switchUserPersona(memberId: String) {
        _currentMemberId.value = memberId
    }

    fun toggleOfflineMode() {
        val newStatus = !_isOfflineMode.value
        _isOfflineMode.value = newStatus
        if (!newStatus) {
            _pendingSyncCount.value = 0
            showSuccess("Đã kết nối lại Internet. Toàn bộ dữ liệu đã được đồng bộ thành công!")
        } else {
            showSuccess("Đã chuyển sang chế độ Offline. Dữ liệu sẽ lưu cục bộ trên máy.")
        }
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun showError(msg: String) {
        _errorMessage.value = msg
    }

    fun showSuccess(msg: String) {
        _successMessage.value = msg
    }

    // Trip operations
    fun createTrip(
        title: String,
        description: String,
        joinCode: String,
        startDate: Long,
        endDate: Long,
        adminName: String,
        adminBankName: String?,
        adminBankAccount: String?,
        adminBankHolder: String?
    ) {
        viewModelScope.launch {
            try {
                val newTripId = repository.createTrip(
                    title = title,
                    description = description,
                    joinCode = joinCode,
                    startDate = startDate,
                    endDate = endDate,
                    adminName = adminName,
                    adminBankName = adminBankName,
                    adminBankAccount = adminBankAccount,
                    adminBankHolder = adminBankHolder
                )
                _currentTripId.value = newTripId
                showSuccess("Tạo đoàn '$title' thành công!")
            } catch (e: Exception) {
                showError("Lỗi khi tạo đoàn: ${e.message}")
            }
        }
    }

    fun joinTripByCode(code: String, userName: String) {
        viewModelScope.launch {
            try {
                val tripId = repository.joinTripByCode(code, userName)
                if (tripId != null) {
                    _currentTripId.value = tripId
                    showSuccess("Đã tham gia đoàn thành công với mã $code!")
                } else {
                    showError("Không tìm thấy đoàn nào với mã '$code'!")
                }
            } catch (e: Exception) {
                showError("Lỗi tham gia đoàn: ${e.message}")
            }
        }
    }

    // Expense operations
    fun addExpense(
        title: String,
        category: String,
        payerType: String,
        payerMemberId: String?,
        totalAmount: Long,
        currency: String,
        exchangeRate: Double,
        splitType: String,
        splits: List<Pair<String, Long>>,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val currentTrip = uiState.value.currentTrip ?: return
        val currentMember = uiState.value.currentMember ?: return

        val convertedTotal = (totalAmount.toDouble() * exchangeRate).toLong()
        val totalSplitSum = splits.sumOf { it.second }
        if (totalSplitSum != convertedTotal) {
            showError("Tổng tiền phân bổ ($totalSplitSum VND) không bằng tổng khoản chi ($convertedTotal VND). Vui lòng kiểm tra lại!")
            return
        }

        val expenseId = UUID.randomUUID().toString()
        val expense = ExpenseEntity(
            id = expenseId,
            tripId = currentTrip.id,
            title = title,
            category = category,
            payerType = payerType,
            payerMemberId = if (payerType == "MEMBER") payerMemberId else null,
            totalAmount = totalAmount,
            currency = currency,
            exchangeRate = exchangeRate,
            convertedTotalAmount = convertedTotal,
            splitType = splitType,
            note = note,
            timestamp = timestamp,
            createdMemberId = currentMember.id,
            isSynced = !_isOfflineMode.value
        )

        val splitEntities = splits.map { (memberId, amount) ->
            ExpenseSplitEntity(
                id = UUID.randomUUID().toString(),
                expenseId = expenseId,
                tripId = currentTrip.id,
                memberId = memberId,
                amount = amount
            )
        }

        viewModelScope.launch {
            try {
                repository.addExpenseWithSplits(expense, splitEntities, currentMember)
                if (_isOfflineMode.value) {
                    _pendingSyncCount.value += 1
                    showSuccess("Đã lưu chi tiêu vào bộ nhớ Offline (chờ đồng bộ)!")
                } else {
                    showSuccess("Đã thêm khoản chi '$title' thành công!")
                }
            } catch (e: Exception) {
                showError("Lỗi thêm chi tiêu: ${e.message}")
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        val currentMember = uiState.value.currentMember ?: return

        if (currentMember.role != "ADMIN" && currentMember.role != "TREASURER" && expense.createdMemberId != currentMember.id) {
            showError("Bạn chỉ được phép xóa khoản chi do chính bạn tạo ra!")
            return
        }

        viewModelScope.launch {
            try {
                repository.deleteExpense(expense, currentMember)
                showSuccess("Đã xóa khoản chi '${expense.title}'")
            } catch (e: Exception) {
                showError("Lỗi xóa khoản chi: ${e.message}")
            }
        }
    }

    // Fund operations
    fun addFundContribution(
        memberId: String,
        amount: Long,
        currency: String,
        exchangeRate: Double,
        note: String
    ) {
        val currentTrip = uiState.value.currentTrip ?: return
        val currentMember = uiState.value.currentMember ?: return
        val members = uiState.value.members
        val contributor = members.find { it.id == memberId }

        if (currentMember.role != "ADMIN" && currentMember.role != "TREASURER") {
            showError("Chỉ Trưởng đoàn hoặc Thủ quỹ mới có quyền ghi nhận thu quỹ!")
            return
        }

        val convertedAmount = (amount.toDouble() * exchangeRate).toLong()
        val contribution = FundContributionEntity(
            id = UUID.randomUUID().toString(),
            tripId = currentTrip.id,
            memberId = memberId,
            amount = amount,
            currency = currency,
            exchangeRate = exchangeRate,
            convertedAmount = convertedAmount,
            note = note,
            timestamp = System.currentTimeMillis(),
            recordedByMemberId = currentMember.id
        )

        viewModelScope.launch {
            try {
                repository.addFundContribution(contribution, contributor?.name ?: "Thành viên", currentMember)
                showSuccess("Đã ghi nhận nộp quỹ $convertedAmount VND cho ${contributor?.name}!")
            } catch (e: Exception) {
                showError("Lỗi nộp quỹ: ${e.message}")
            }
        }
    }

    // Members & Roles
    fun addMember(
        name: String,
        role: String,
        bankName: String?,
        bankAccount: String?,
        bankAccountHolder: String?
    ) {
        val currentTrip = uiState.value.currentTrip ?: return
        val currentMember = uiState.value.currentMember ?: return

        if (currentMember.role != "ADMIN" && currentMember.role != "TREASURER") {
            showError("Chỉ Trưởng đoàn hoặc Thủ quỹ mới có quyền thêm thành viên!")
            return
        }

        viewModelScope.launch {
            try {
                repository.addMember(
                    tripId = currentTrip.id,
                    name = name,
                    role = role,
                    bankName = bankName,
                    bankAccount = bankAccount,
                    bankAccountHolder = bankAccountHolder,
                    actor = currentMember
                )
                showSuccess("Đã thêm thành viên '$name' ($role)")
            } catch (e: Exception) {
                showError("Lỗi thêm thành viên: ${e.message}")
            }
        }
    }

    fun updateMemberRole(member: TripMemberEntity, newRole: String) {
        val currentMember = uiState.value.currentMember ?: return
        if (currentMember.role != "ADMIN") {
            showError("Chỉ Trưởng đoàn (Admin) mới có quyền thay đổi vai trò thành viên!")
            return
        }

        viewModelScope.launch {
            try {
                repository.updateMember(member.copy(role = newRole), currentMember)
                showSuccess("Đã đổi vai trò của ${member.name} thành $newRole")
            } catch (e: Exception) {
                showError("Lỗi cập nhật vai trò: ${e.message}")
            }
        }
    }

    fun removeOrDeactivateMember(member: TripMemberEntity) {
        val currentMember = uiState.value.currentMember ?: return
        if (currentMember.role != "ADMIN" && currentMember.role != "TREASURER") {
            showError("Bạn không có quyền quản lý thành viên!")
            return
        }

        viewModelScope.launch {
            try {
                repository.removeOrDeactivateMember(member, currentMember)
                showSuccess("Đã xử lý trạng thái thành viên ${member.name}")
            } catch (e: Exception) {
                showError("Lỗi xóa/vô hiệu hóa thành viên: ${e.message}")
            }
        }
    }

    // Exchange rates
    fun updateExchangeRate(currencyCode: String, rateToBase: Double) {
        val currentTrip = uiState.value.currentTrip ?: return
        val currentMember = uiState.value.currentMember ?: return

        if (currentMember.role != "ADMIN" && currentMember.role != "TREASURER") {
            showError("Chỉ Trưởng đoàn hoặc Thủ quỹ mới có quyền cập nhật tỷ giá!")
            return
        }

        val rateEntity = ExchangeRateEntity(
            id = UUID.randomUUID().toString(),
            tripId = currentTrip.id,
            currencyCode = currencyCode,
            rateToBase = rateToBase,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                repository.updateExchangeRate(rateEntity, currentMember)
                showSuccess("Đã cập nhật tỷ giá 1 $currencyCode = $rateToBase VND")
            } catch (e: Exception) {
                showError("Lỗi cập nhật tỷ giá: ${e.message}")
            }
        }
    }

    // Settlement & Snapshot Finalization
    fun finalizeSettlement(snapshotTitle: String) {
        val currentTrip = uiState.value.currentTrip ?: return
        val currentMember = uiState.value.currentMember ?: return
        val state = uiState.value

        if (currentMember.role != "ADMIN") {
            showError("Chỉ Trưởng đoàn (Admin) mới có quyền khóa sổ và quyết toán chuyến đi!")
            return
        }

        if (state.isOfflineMode) {
            showError("Chế độ Offline đang bật. Vui lòng kết nối mạng để khóa sổ và quyết toán nhằm tránh xung đột!")
            return
        }

        if (!state.financialSummary.isBalanced && kotlin.math.abs(state.financialSummary.balanceDiscrepancy) > 5) {
            showError("Hệ thống phát hiện chênh lệch đối soát (${state.financialSummary.balanceDiscrepancy} VND). Không thể khóa sổ!")
            return
        }

        val settlementJson = buildString {
            append("Chuyến đi: ${currentTrip.title}\n")
            append("Mã: ${currentTrip.joinCode}\n")
            append("Tổng chi: ${state.financialSummary.totalExpenses} VND\n")
            append("Tổng thu quỹ: ${state.financialSummary.totalFundCollected} VND\n")
            append("Quỹ đã chi: ${state.financialSummary.fundPaidExpenses} VND\n")
            append("Quỹ còn lại: ${state.financialSummary.remainingFund} VND\n")
            append("\n--- SỐ DƯ TỪNG THÀNH VIÊN ---\n")
            state.memberStatuses.forEach {
                append("- ${it.member.name}: Paid=${it.totalPaid} | Owed=${it.totalOwed} | Balance=${it.balance} (${it.status})\n")
            }
            append("\n--- KẾ HOẠCH CHUYỂN KHOẢN QUYẾT TOÁN ---\n")
            state.settlementTransfers.forEachIndexed { idx, tr ->
                append("${idx + 1}. ${tr.fromMember.name} -> ${tr.toMember.name}: ${tr.amount} VND (${tr.transferNote})\n")
            }
        }

        viewModelScope.launch {
            try {
                repository.finalizeSettlement(
                    trip = currentTrip,
                    snapshotTitle = snapshotTitle.ifBlank { "Quyết toán ngày ${System.currentTimeMillis()}" },
                    summary = state.financialSummary,
                    settlementJson = settlementJson,
                    actor = currentMember
                )
                showSuccess("Đã khóa sổ và lưu Snapshot quyết toán thành công!")
            } catch (e: Exception) {
                showError("Lỗi khóa sổ quyết toán: ${e.message}")
            }
        }
    }
}
