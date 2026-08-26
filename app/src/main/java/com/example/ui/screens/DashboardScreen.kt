package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TripEntity
import com.example.domain.model.BalanceStatus
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: UiState,
    onNavigateToExpenses: () -> Unit,
    onNavigateToFund: () -> Unit,
    onNavigateToSettlement: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddFund: () -> Unit,
    onOpenExportReport: () -> Unit,
    onOpenCreateTrip: () -> Unit,
    onOpenJoinTrip: () -> Unit,
    onSwitchUser: (String) -> Unit,
    onSelectTrip: (String) -> Unit = {},
    onEditTrip: (trip: TripEntity, title: String, description: String, startDate: Long, endDate: Long) -> Unit = { _, _, _, _, _ -> },
    onDeleteTrip: (TripEntity) -> Unit = {}
) {
    var showUserPicker by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<TripEntity?>(null) }
    var tripToDelete by remember { mutableStateOf<TripEntity?>(null) }
    val isAdmin = uiState.currentMember?.role == "ADMIN"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Trip Banner & Persona Switcher
        item {
            TripHeaderCard(
                uiState = uiState,
                onOpenCreateTrip = onOpenCreateTrip,
                onOpenJoinTrip = onOpenJoinTrip,
                onOpenUserPicker = { showUserPicker = true }
            )
        }

        // Financial Overview Cards
        item {
            Text(
                text = "Tổng Quan Tài Chính",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Card 1: Total Expenses & Sources
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .background(EmeraldPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = EmeraldOnPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "TỔNG CHI TIÊU ĐOÀN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = NumberFormatUtils.formatVnd(uiState.financialSummary.totalExpenses),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onNavigateToExpenses,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Chi tiết", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Breakdown of expense source: Member paid vs Fund paid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Người trả (Paid)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = NumberFormatUtils.formatVnd(uiState.financialSummary.personalPaidExpenses),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Chi từ Quỹ chung",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = NumberFormatUtils.formatVnd(uiState.financialSummary.fundPaidExpenses),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberTertiary
                                )
                            }
                        }
                    }
                }

                // Card 2: Group Fund (Quỹ đoàn)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .background(AmberTertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Savings,
                                        contentDescription = null,
                                        tint = AmberOnTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SỐ DƯ QUỸ CHUNG",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = NumberFormatUtils.formatVnd(uiState.financialSummary.remainingFund),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberTertiary
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onNavigateToFund,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Quỹ đoàn", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Đã thu quỹ: ${NumberFormatUtils.formatVnd(uiState.financialSummary.totalFundCollected)}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Đã chi quỹ: ${NumberFormatUtils.formatVnd(uiState.financialSummary.fundPaidExpenses)}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Card 3: Single Source of Truth Reconciliation Badge (SRS Section 2.4)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.financialSummary.isBalanced) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (uiState.financialSummary.isBalanced) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (uiState.financialSummary.isBalanced) Color(0xFF059669) else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (uiState.financialSummary.isBalanced) {
                                        "Đối soát hợp lệ: Tổng Balance = 0đ"
                                    } else {
                                        "Cảnh báo: Chênh lệch ${NumberFormatUtils.formatVnd(uiState.financialSummary.balanceDiscrepancy)}"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.financialSummary.isBalanced) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                                Text(
                                    text = "Đã đối soát",
                                    fontSize = 10.sp,
                                    color = if (uiState.financialSummary.isBalanced) Color(0xFF047857) else Color(0xFFB91C1C)
                                )
                            }
                        }

                        TextButton(
                            onClick = onNavigateToSettlement,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                "Quyết toán",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.financialSummary.isBalanced) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenAddExpense,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("quick_add_expense_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Thêm Chi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onOpenAddFund,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AmberTertiaryContainer,
                        contentColor = AmberOnTertiaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("quick_add_fund_button")
                ) {
                    Icon(Icons.Filled.Savings, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Nộp Quỹ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onOpenExportReport,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = IndigoSecondaryContainer,
                        contentColor = IndigoOnSecondaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("quick_export_report_button")
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Báo Cáo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Danh Sách Các Đoàn Đã Tạo (Created Trips List)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Danh Sách Các Đoàn Đã Tạo (${uiState.allTrips.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = onOpenCreateTrip,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tạo đoàn mới", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                uiState.allTrips.forEach { trip ->
                    val isCurrent = trip.id == uiState.currentTrip?.id
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrip(trip.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Filled.Luggage,
                                        contentDescription = null,
                                        tint = if (isCurrent) EmeraldPrimary else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trip.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCurrent) EmeraldPrimaryContainer else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "MÃ: ${trip.joinCode}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) EmeraldOnPrimaryContainer else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (trip.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = trip.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lịch trình: ${dateFormat.format(Date(trip.startDate))} - ${dateFormat.format(Date(trip.endDate))}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAdmin) {
                                        IconButton(
                                            onClick = { tripToEdit = trip },
                                            modifier = Modifier.size(28.dp).testTag("edit_trip_${trip.id}")
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Sửa đoàn",
                                                tint = IndigoSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (uiState.allTrips.size > 1) {
                                            IconButton(
                                                onClick = { tripToDelete = trip },
                                                modifier = Modifier.size(28.dp).testTag("delete_trip_${trip.id}")
                                            ) {
                                                Icon(
                                                    Icons.Filled.DeleteOutline,
                                                    contentDescription = "Xóa đoàn",
                                                    tint = DangerRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!isCurrent) {
                                        FilledTonalButton(
                                            onClick = { onSelectTrip(trip.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Chọn đoàn", fontSize = 10.sp)
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldPrimary
                                        ) {
                                            Text(
                                                text = "Đang chọn",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Breakdown
        if (uiState.categoryBreakdowns.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phân Loại Chi Tiêu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.expenses.size} khoản chi",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.categoryBreakdowns.forEach { cat ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIcon(category = cat.category, size = 16)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat.labelVi,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = " (${cat.count})",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Text(
                                        text = "${NumberFormatUtils.formatVnd(cat.totalAmount)} (${cat.percentage.toInt()}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (cat.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = EmeraldPrimary,
                                    trackColor = Color(0xFFF1F5F9)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Member Balances Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bảng Số Dư Thành Viên",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToSettlement) {
                    Text("Xem quyết toán", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.memberStatuses.forEach { status ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = status.member.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    RoleBadge(role = status.member.role)
                                }
                                Text(
                                    text = "Đã chi: ${NumberFormatUtils.formatVnd(status.totalPaid)} • Phải trả: ${NumberFormatUtils.formatVnd(status.totalOwed)}",
                                    fontSize = 11.sp,
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
    }

    // Role / Persona Switcher Dialog
    if (showUserPicker) {
        AlertDialog(
            onDismissRequest = { showUserPicker = false },
            title = {
                Text(
                    text = "Chuyển Đổi Người Dùng / Vai Trò",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Chọn phân quyền (Admin, Thủ quỹ, Thành viên, Người xem):",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    uiState.members.forEach { m ->
                        val isSelected = m.id == uiState.currentMember?.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimaryContainer else NeutralLightSurfaceCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSwitchUser(m.id)
                                    showUserPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = m.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) EmeraldOnPrimaryContainer else Color(0xFF0F172A)
                                    )
                                    RoleBadge(role = m.role)
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserPicker = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Edit Trip Dialog
    tripToEdit?.let { tr ->
        EditTripDialog(
            trip = tr,
            onDismiss = { tripToEdit = null },
            onConfirm = { title, desc, start, end ->
                onEditTrip(tr, title, desc, start, end)
                tripToEdit = null
            }
        )
    }

    // Delete Trip Dialog
    tripToDelete?.let { tr ->
        DeleteTripDialog(
            tripTitle = tr.title,
            onDismiss = { tripToDelete = null },
            onConfirm = {
                onDeleteTrip(tr)
                tripToDelete = null
            }
        )
    }
}

@Composable
fun TripHeaderCard(
    uiState: UiState,
    onOpenCreateTrip: () -> Unit,
    onOpenJoinTrip: () -> Unit,
    onOpenUserPicker: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x33FFFFFF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MÃ: ${uiState.currentTrip?.joinCode ?: "---"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = onOpenJoinTrip,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0x33FFFFFF),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = "Nhập mã tham gia", modifier = Modifier.size(16.dp))
                    }
                    FilledTonalIconButton(
                        onClick = onOpenCreateTrip,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0x33FFFFFF),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tạo đoàn mới", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trip Title
            Text(
                text = uiState.currentTrip?.title ?: "Chưa có đoàn nào",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = uiState.currentTrip?.description ?: "Quản lý tài chính minh bạch cho đoàn",
                fontSize = 12.sp,
                color = Color(0xCCFFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x33FFFFFF))
            Spacer(modifier = Modifier.height(8.dp))

            // User Persona switcher bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenUserPicker() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bạn: ${uiState.currentMember?.name ?: "Khách"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Đổi vai trò",
                        fontSize = 11.sp,
                        color = Color(0xFFCCFBF1),
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFFCCFBF1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
