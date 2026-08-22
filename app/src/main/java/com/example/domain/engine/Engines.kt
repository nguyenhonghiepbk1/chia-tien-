package com.example.domain.engine

import com.example.data.entity.TripMemberEntity
import com.example.domain.model.MemberFinancialStatus
import com.example.domain.model.SettlementTransfer
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

object SplitCalculator {

    /**
     * Chia đều tất cả thành viên (Equal Split)
     * Quy tắc làm tròn: Làm tròn nguyên phần chia cơ sở (totalAmount / count),
     * nếu phép chia có phần dư (remainder), cộng toàn bộ phần chênh lệch cho người đầu tiên trong danh sách (hoặc primaryMemberId).
     */
    fun calculateEqualSplit(
        totalAmount: Long,
        memberIds: List<String>,
        primaryMemberId: String? = null
    ): List<Pair<String, Long>> {
        if (memberIds.isEmpty() || totalAmount <= 0) return emptyList()
        val count = memberIds.size
        val baseShare = totalAmount / count
        val remainder = (totalAmount % count).toInt()

        val priorityIndex = if (primaryMemberId != null && memberIds.contains(primaryMemberId)) {
            memberIds.indexOf(primaryMemberId)
        } else {
            0 // Mặc định người đầu tiên trong danh sách
        }

        return memberIds.mapIndexed { index, memberId ->
            val extra = if (index == priorityIndex) remainder.toLong() else 0L
            memberId to (baseShare + extra)
        }
    }

    /**
     * Chia theo tỷ lệ phần trăm (Ratio Split)
     * Ràng buộc: Tổng % = 100%. Phần dư làm tròn bù vào người đầu tiên/người tạo.
     */
    fun calculateRatioSplit(
        totalAmount: Long,
        memberRatios: List<Pair<String, Double>>,
        primaryMemberId: String? = null
    ): Pair<List<Pair<String, Long>>, Boolean> {
        if (memberRatios.isEmpty()) return emptyList<Pair<String, Long>>() to false
        val totalRatio = memberRatios.sumOf { it.second }
        val isRatioValid = abs(totalRatio - 100.0) < 0.001

        var currentSum = 0L
        val splits = memberRatios.map { (memberId, ratio) ->
            val share = ((totalAmount * ratio) / 100.0).roundToLong()
            currentSum += share
            memberId to share
        }.toMutableList()

        // Bù chênh lệch làm tròn để SUM(splits) == totalAmount
        val diff = totalAmount - currentSum
        if (diff != 0L && splits.isNotEmpty()) {
            val priorityIndex = if (primaryMemberId != null) {
                splits.indexOfFirst { it.first == primaryMemberId }.takeIf { it >= 0 } ?: 0
            } else 0

            val current = splits[priorityIndex]
            splits[priorityIndex] = current.first to (current.second + diff)
        }

        return splits to isRatioValid
    }

    /**
     * Chia theo nhóm người được chọn (Custom Participant Split)
     */
    fun calculateSelectedParticipantsSplit(
        totalAmount: Long,
        selectedMemberIds: List<String>,
        primaryMemberId: String? = null
    ): List<Pair<String, Long>> {
        return calculateEqualSplit(totalAmount, selectedMemberIds, primaryMemberId)
    }

    /**
     * Kiểm tra Toàn vẹn Dữ liệu (Integrity Check):
     * Hệ thống đảm bảo tổng các phần phân bổ bắt buộc phải bằng đúng tổng số tiền của khoản chi gốc.
     */
    fun validateSplits(totalAmount: Long, splits: List<Pair<String, Long>>): Boolean {
        if (splits.isEmpty() && totalAmount == 0L) return true
        return splits.sumOf { it.second } == totalAmount
    }
}

object SettlementEngine {

    // Ngưỡng sai số so sánh dập tắt hoàn toàn rủi ro sai số số học khiến vòng lặp chạy vô tận
    const val EPSILON = 0.001

    /**
     * Thuật toán Tham Lam (Greedy Algorithm) Tối Ưu Hóa Dòng Tiền:
     * - Ý tưởng cốt lõi: Luôn ưu tiên lấy người nợ nhiều nhất (Max Debtor) trả cho người đang được hệ thống nợ nhiều nhất (Max Creditor).
     * - Bằng cách triệt tiêu các khoản lớn trước, rút gọn đáng kể số lượng giao dịch chuyển khoản qua lại.
     * - Tránh lỗi vô cực: Dùng EPSILON (0.001) làm mốc so sánh triệt tiêu hoàn toàn rủi ro sai số thập phân.
     * - Khóa chặt đầu ra: Trả về danh sách SettlementTransfer tinh gọn (Người gửi, Người nhận, Số tiền, Nội dung chuyển khoản).
     */
    fun computeSimplifiedTransfers(
        memberStatuses: List<MemberFinancialStatus>,
        tripJoinCode: String
    ): List<SettlementTransfer> {
        // Kiểm tra đối soát: Tổng balance toàn đoàn phải xấp xỉ 0 (trong giới hạn sai số cho phép)
        val totalBalance = memberStatuses.sumOf { it.balance.toDouble() }
        if (abs(totalBalance) > 5.0) {
            // Có chênh lệch đối soát chưa cân bằng
            return emptyList()
        }

        data class BalanceEntry(val member: TripMemberEntity, var balance: Double)

        // 1. Phân loại và sắp xếp giảm dần:
        // Creditors: Người có balance > EPSILON (được nhận lại tiền)
        val creditors = memberStatuses
            .filter { it.balance.toDouble() > EPSILON }
            .map { BalanceEntry(it.member, it.balance.toDouble()) }
            .sortedByDescending { it.balance }
            .toMutableList()

        // Debtors: Người có balance < -EPSILON (phải trả thêm tiền)
        val debtors = memberStatuses
            .filter { it.balance.toDouble() < -EPSILON }
            .map { BalanceEntry(it.member, -it.balance.toDouble()) }
            .sortedByDescending { it.balance }
            .toMutableList()

        val transfers = mutableListOf<SettlementTransfer>()
        var i = 0 // con trỏ debtors
        var j = 0 // con trỏ creditors

        // Vòng lặp tham lam giải quyết nợ từng cặp lớn nhất
        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]
            val creditor = creditors[j]

            // Bỏ qua nếu số dư đã tiệm cận 0 (nhỏ hơn EPSILON = 0.001) để tránh vòng lặp vô cực
            if (debtor.balance <= EPSILON) {
                i++
                continue
            }
            if (creditor.balance <= EPSILON) {
                j++
                continue
            }

            // Triệt tiêu số tiền nhỏ hơn giữa 2 bên
            val settleDouble = minOf(debtor.balance, creditor.balance)
            val settleAmount = settleDouble.roundToLong()

            if (settleAmount > 0) {
                val transferNote = "[$tripJoinCode] ${debtor.member.name} quyet toan cho ${creditor.member.name}"

                transfers.add(
                    SettlementTransfer(
                        id = UUID.randomUUID().toString(),
                        fromMember = debtor.member,
                        toMember = creditor.member,
                        amount = settleAmount,
                        transferNote = transferNote
                    )
                )
            }

            debtor.balance -= settleDouble
            creditor.balance -= settleDouble

            if (debtor.balance <= EPSILON) i++
            if (creditor.balance <= EPSILON) j++
        }

        return transfers
    }
}
