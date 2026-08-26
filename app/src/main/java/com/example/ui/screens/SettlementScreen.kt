package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.SettlementSnapshotEntity
import com.example.domain.model.BalanceStatus
import com.example.domain.model.SettlementTransfer
import com.example.ui.components.BalanceChip
import com.example.ui.components.NumberFormatUtils
import com.example.ui.components.RoleBadge
import com.example.ui.components.VietQrTransferDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    uiState: UiState,
    onFinalizeSettlement: (String) -> Unit,
    onOpenExportReport: () -> Unit
) {
    var selectedTransferForQr by remember { mutableStateOf<SettlementTransfer?>(null) }
    var showFinalizeDialog by remember { mutableStateOf(false) }
    var selectedSnapshotForView by remember { mutableStateOf<SettlementSnapshotEntity?>(null) }
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val isAdmin = uiState.currentMember?.role == "ADMIN"
    val isBalanced = uiState.financialSummary.isBalanced
    val isOffline = uiState.isOfflineMode

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Settlement Algorithm Overview Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountTree, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quyết Toán Tối Ưu", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x33FFFFFF)
                        ) {
                            Text(
                                text = "${uiState.settlementTransfers.size}",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "tối ưu hóa chuyển khoản giữa các thành viên đoàn.",
                        fontSize = 11.sp,
                        color = Color(0xEEFFFFFF)
                    )
                }
            }
        }

        // Finalize Settlement & Export Report Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Khóa Sổ & Xuất Báo Cáo Đoàn", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Xuất báo cáo tiếng Việt (Times New Roman / Excel CSV) và lưu snapshot quyết toán",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onOpenExportReport,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = IndigoSecondaryContainer,
                                    contentColor = IndigoOnSecondaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("export_report_screen_button")
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Báo cáo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showFinalizeDialog = true },
                                enabled = isAdmin && !isOffline && isBalanced,
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("finalize_settlement_button")
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Khóa sổ", fontSize = 11.sp)
                            }
                        }
                    }

                    if (!isAdmin) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Chỉ Trưởng đoàn (Admin) mới có quyền khóa sổ chuyến đi", fontSize = 10.sp, color = Color(0xFFEF4444))
                    }
                    if (isOffline) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Đang ở chế độ Offline. Vui lòng kết nối mạng để khóa sổ", fontSize = 10.sp, color = Color(0xFFD97706))
                    }
                    if (!isBalanced) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Tổng số dư đoàn bị lệch. Không thể khóa sổ", fontSize = 10.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        // Simplified Transfer Instructions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kế Hoạch Chuyển Khoản Chi Tiết",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (copiedNotice != null) {
                    Text(
                        text = copiedNotice!!,
                        fontSize = 11.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (uiState.settlementTransfers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tất cả thành viên đã cân bằng!", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        Text("Không cần chuyển khoản bổ sung thêm", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }
        } else {
            itemsIndexed(uiState.settlementTransfers) { index, transfer ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GIAO DỊCH #${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = NumberFormatUtils.formatVnd(transfer.amount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Transfer Flow: Debtor -> Creditor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Debtor
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Người chuyển (Cần nộp)", fontSize = 10.sp, color = DangerRed)
                                Text(
                                    text = transfer.fromMember.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )

                            // Creditor
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("Người nhận (Được nhận)", fontSize = 10.sp, color = EmeraldPrimary)
                                Text(
                                    text = transfer.toMember.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Bank Details & Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "STK: ${transfer.toMember.bankAccount ?: "Chưa có"} (${transfer.toMember.bankName ?: "Chưa rõ ngân hàng"})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(transfer.transferNote))
                                        copiedNotice = "Đã chép nội dung CK!"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chép ND", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { selectedTransferForQr = transfer },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("VietQR", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Balance Summary Table
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bảng Tổng Hợp Thu Chi Từng Người",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.memberStatuses.forEach { status ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(status.member.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    RoleBadge(role = status.member.role)
                                }
                                Text(
                                    text = "Chi hộ: ${NumberFormatUtils.formatVnd(status.outOfPocketPaid)} | Quỹ: ${NumberFormatUtils.formatVnd(status.fundContributed)} | Chịu chi: ${NumberFormatUtils.formatVnd(status.totalOwed)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            BalanceChip(balance = status.balance, status = status.status)
                        }
                        if (status != uiState.memberStatuses.last()) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        // Snapshots History Section (SRS Section 4 & 2.4)
        if (uiState.snapshots.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lịch Sử Snapshot Quyết Toán (Bất Biến)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(uiState.snapshots) { snap ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(snap.snapshotTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Khóa lúc: ${dateFormat.format(Date(snap.createdAt))}", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("Tổng chi: ${NumberFormatUtils.formatVnd(snap.totalExpenses)}", fontSize = 11.sp, color = EmeraldPrimary)
                        }

                        OutlinedButton(
                            onClick = { selectedSnapshotForView = snap },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Xem chi tiết", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // QR Dialog
    selectedTransferForQr?.let { tr ->
        VietQrTransferDialog(transfer = tr, onDismiss = { selectedTransferForQr = null })
    }

    // Finalize Confirmation Dialog
    if (showFinalizeDialog) {
        var snapshotTitle by remember { mutableStateOf("Quyết toán kết thúc chuyến đi ${uiState.currentTrip?.title}") }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val safeDismiss = {
            focusManager.clearFocus()
            keyboardController?.hide()
            showFinalizeDialog = false
        }

        AlertDialog(
            onDismissRequest = safeDismiss,
            properties = DialogProperties(decorFitsSystemWindows = true),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = IndigoSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xác Nhận Khóa Sổ Quyết Toán", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Sau khi khóa sổ, hệ thống sẽ tạo một bản Snapshot kế toán bất biến. Dữ liệu quyết toán sẽ được bảo toàn chính xác.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = snapshotTitle,
                        onValueChange = { snapshotTitle = it },
                        label = { Text("Tên bản Snapshot") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onFinalizeSettlement(snapshotTitle)
                        showFinalizeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary)
                ) {
                    Text("Lưu Snapshot & Khóa")
                }
            },
            dismissButton = {
                TextButton(onClick = safeDismiss) {
                    Text("Hủy")
                }
            }
        )
    }

    // Snapshot View Dialog
    selectedSnapshotForView?.let { snap ->
        AlertDialog(
            onDismissRequest = { selectedSnapshotForView = null },
            properties = DialogProperties(decorFitsSystemWindows = true),
            title = { Text(snap.snapshotTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        Text(snap.settlementJson, fontSize = 12.sp, color = Color(0xFF1E293B))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSnapshotForView = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}
