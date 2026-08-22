package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.ExchangeRateEntity
import com.example.data.entity.TripMemberEntity
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersAndSettingsScreen(
    uiState: UiState,
    onAddMember: (name: String, role: String, bankName: String?, bankAccount: String?, holder: String?) -> Unit,
    onUpdateRole: (TripMemberEntity, String) -> Unit,
    onRemoveOrDeactivate: (TripMemberEntity) -> Unit,
    onUpdateRate: (currencyCode: String, rate: Double) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var copiedCodeNotice by remember { mutableStateOf<String?>(null) }

    val currentMember = uiState.currentMember
    val isAdmin = currentMember?.role == "ADMIN"
    val isTreasurer = currentMember?.role == "TREASURER"
    val canManageMembers = isAdmin || isTreasurer
    val canViewAuditLogs = isAdmin || isTreasurer // SRS 5: Member KHÔNG được xem Audit Log kỹ thuật

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Trip Details & 6-Char Join Code
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MÃ THAM GIA ĐOÀN (6 KÝ TỰ)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldPrimaryContainer
                        ) {
                            Text(
                                text = uiState.currentTrip?.joinCode ?: "---",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldOnPrimaryContainer,
                                letterSpacing = 3.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val code = uiState.currentTrip?.joinCode ?: ""
                                clipboardManager.setText(AnnotatedString(code))
                                copiedCodeNotice = "Đã chép mã $code!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sao chép mã")
                        }
                    }

                    if (copiedCodeNotice != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(copiedCodeNotice!!, fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Thành viên mới chỉ cần nhập mã 6 ký tự trên để tham gia đoàn ngay lập tức mà không cần thủ tục phức tạp.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // Quick Management Utilities (Exchange Rates & Audit Logs)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showRateDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CurrencyExchange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tỷ giá (${uiState.exchangeRates.size})", fontSize = 12.sp)
                }

                Button(
                    onClick = { showAuditLogsDialog = true },
                    enabled = canViewAuditLogs,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audit Log", fontSize = 12.sp)
                }
            }
        }

        // Members Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Danh Sách Thành Viên (${uiState.members.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (canManageMembers) {
                    FilledTonalButton(
                        onClick = { showAddMemberDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Member Cards List
        items(uiState.members) { member ->
            var expandedRoleMenu by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (member.isActive) MaterialTheme.colorScheme.surface else Color(0xFFF8FAFC)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (member.isActive) EmeraldPrimaryContainer else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (member.isActive) EmeraldOnPrimaryContainer else Color(0xFF64748B),
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = member.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!member.isActive) {
                                    Text("• Đã ngừng hoạt động", fontSize = 10.sp, color = DangerRed)
                                }
                            }
                        }

                        // Role Tag / Role Selector
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(enabled = isAdmin) { expandedRoleMenu = true }
                            ) {
                                RoleBadge(role = member.role)
                                if (isAdmin) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = expandedRoleMenu,
                                onDismissRequest = { expandedRoleMenu = false }
                            ) {
                                listOf(
                                    "ADMIN" to "Trưởng đoàn (Admin)",
                                    "TREASURER" to "Thủ quỹ (Treasurer)",
                                    "MEMBER" to "Thành viên (Member)",
                                    "VIEWER" to "Người xem (Viewer)"
                                ).forEach { (rKey, rLabel) ->
                                    DropdownMenuItem(
                                        text = { Text(rLabel) },
                                        onClick = {
                                            onUpdateRole(member, rKey)
                                            expandedRoleMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bank Account details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STK: ${member.bankAccount ?: "Chưa cập nhật"} (${member.bankName ?: "Chưa có"})",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                            if (!member.bankAccountHolder.isNullOrBlank()) {
                                Text(
                                    text = "Chủ TK: ${member.bankAccountHolder}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        if (canManageMembers && member.id != currentMember?.id) {
                            TextButton(
                                onClick = { onRemoveOrDeactivate(member) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (member.isActive) "Xóa/Khóa" else "Đã khóa",
                                    fontSize = 11.sp,
                                    color = DangerRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onSave = { name, role, bankName, bankAccount, holder ->
                onAddMember(name, role, bankName, bankAccount, holder)
                showAddMemberDialog = false
            }
        )
    }

    // Exchange Rates Dialog
    if (showRateDialog) {
        ExchangeRatesDialog(
            rates = uiState.exchangeRates,
            canEdit = canManageMembers,
            onDismiss = { showRateDialog = false },
            onSave = { code, rate ->
                onUpdateRate(code, rate)
            }
        )
    }

    // Audit Logs Dialog
    if (showAuditLogsDialog) {
        AuditLogsDialog(
            logs = uiState.auditLogs,
            onDismiss = { showAuditLogsDialog = false }
        )
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, bankName: String?, bankAccount: String?, holder: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }
    var bankName by remember { mutableStateOf("Vietcombank") }
    var bankAccount by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val safeDismiss = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        title = { Text("Thêm Thành Viên Vào Đoàn", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên thành viên *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Vai trò:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("MEMBER" to "Thành viên", "TREASURER" to "Thủ quỹ", "VIEWER" to "Xem").forEach { (rKey, rLabel) ->
                        FilterChip(
                            selected = role == rKey,
                            onClick = { role = rKey },
                            label = { Text(rLabel, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Tên Ngân hàng (VD: MBBank, VCB...)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bankAccount,
                    onValueChange = { bankAccount = it },
                    label = { Text("Số tài khoản nhận tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it },
                    label = { Text("Chủ tài khoản (Không dấu)") },
                    singleLine = true,
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
                    onSave(name, role, bankName.takeIf { it.isNotBlank() }, bankAccount.takeIf { it.isNotBlank() }, holder.takeIf { it.isNotBlank() })
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun ExchangeRatesDialog(
    rates: List<ExchangeRateEntity>,
    canEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var selectedCode by remember { mutableStateOf("USD") }
    var rateValueText by remember { mutableStateOf("25450") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val safeDismiss = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CurrencyExchange, contentDescription = null, tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quản Lý Tỷ Giá Ngoại Tệ", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tỷ giá quy đổi cố định theo đoàn (Base: VND):", fontSize = 12.sp, color = Color(0xFF64748B))

                rates.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1 ${r.currencyCode}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("= ${r.rateToBase} VND", fontSize = 14.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (canEdit) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Cập nhật tỷ giá mới:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("USD", "EUR", "JPY", "THB", "SGD", "CNY").forEach { c ->
                            FilterChip(
                                selected = selectedCode == c,
                                onClick = { selectedCode = c },
                                label = { Text(c, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = rateValueText,
                        onValueChange = { rateValueText = it },
                        label = { Text("Tỷ giá 1 $selectedCode = ? VND") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (canEdit) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        val parsed = rateValueText.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            onSave(selectedCode, parsed)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Lưu tỷ giá")
                }
            } else {
                TextButton(onClick = safeDismiss) { Text("Đóng") }
            }
        },
        dismissButton = {
            if (canEdit) TextButton(onClick = safeDismiss) { Text("Đóng") }
        }
    )
}

@Composable
fun AuditLogsDialog(
    logs: List<AuditLogEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.HistoryEdu, contentDescription = null, tint = IndigoSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nhật Ký Kiểm Toán (Audit Logs)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (logs.isEmpty()) {
                Text("Chưa có bản ghi nhật ký nào.", fontSize = 13.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = NeutralLightSurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoSecondary)
                                    Text(dateFormat.format(Date(log.timestamp)), fontSize = 9.sp, color = Color(0xFF94A3B8))
                                }
                                Text(log.description, fontSize = 12.sp, color = Color(0xFF1E293B))
                                Text("Thực hiện bởi: ${log.actorName}", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
