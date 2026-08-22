package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.TripMemberEntity
import com.example.ui.components.CategoryIcon
import com.example.ui.components.NumberFormatUtils
import com.example.ui.theme.*
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    uiState: UiState,
    onOpenAddExpense: () -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onSearchChange: (String) -> Unit
) {
    var selectedExpenseForDetail by remember { mutableStateOf<ExpenseEntity?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddExpense,
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("fab_add_expense")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm khoản chi")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Tìm kiếm khoản chi, ghi chú...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B))
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onSearchChange("")
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            val categories = listOf(
                null to "Tất cả",
                "FOOD" to "Ăn uống",
                "TRANSPORT" to "Di chuyển",
                "HOTEL" to "Khách sạn",
                "SIGHTSEEING" to "Vé tham quan",
                "ENTERTAINMENT" to "Giải trí",
                "SHOPPING" to "Mua sắm",
                "OTHER" to "Khác"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(categories) { (catKey, catLabel) ->
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == catKey,
                        onClick = { onCategoryFilterChange(catKey) },
                        label = { Text(catLabel, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expense List
            if (uiState.expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa có khoản chi nào",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Nhấn nút '+' bên dưới để tạo khoản chi mới",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(uiState.expenses) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            members = uiState.members,
                            currentMember = uiState.currentMember,
                            onClick = { selectedExpenseForDetail = expense },
                            onDelete = { onDeleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }

    // Expense Detail Dialog
    selectedExpenseForDetail?.let { exp ->
        ExpenseDetailDialog(
            expense = exp,
            members = uiState.members,
            onDismiss = { selectedExpenseForDetail = null }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    members: List<TripMemberEntity>,
    currentMember: TripMemberEntity?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val payerName = if (expense.payerType == "FUND") {
        "Quỹ chung đoàn"
    } else {
        members.find { it.id == expense.payerMemberId }?.name ?: "Thành viên"
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(expense.timestamp))

    // Can delete if ADMIN, TREASURER, or creator
    val canDelete = currentMember?.role == "ADMIN" ||
            currentMember?.role == "TREASURER" ||
            expense.createdMemberId == currentMember?.id

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(category = expense.category, size = 22)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (expense.payerType == "FUND") Color(0xFFFEF3C7) else Color(0xFFE0E7FF)
                    ) {
                        Text(
                            text = if (expense.payerType == "FUND") "Quỹ chi" else "Chi hộ: $payerName",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (expense.payerType == "FUND") Color(0xFF92400E) else Color(0xFF3730A3),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = NumberFormatUtils.formatVnd(expense.convertedTotalAmount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )

                if (expense.currency != "VND") {
                    Text(
                        text = "${expense.totalAmount} ${expense.currency}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Xóa",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseDetailDialog(
    expense: ExpenseEntity,
    members: List<TripMemberEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val payerMember = members.find { it.id == expense.payerMemberId }
    val payerName = if (expense.payerType == "FUND") {
        "Quỹ chung của đoàn (Trừ thẳng vào quỹ)"
    } else {
        "${payerMember?.name ?: "Thành viên"} (${when (payerMember?.role) {
            "ADMIN" -> "Trưởng đoàn"
            "TREASURER" -> "Thủ quỹ"
            else -> "Thành viên"
        }})"
    }

    val categoryLabel = when (expense.category) {
        "FOOD" -> "Ăn uống"
        "TRANSPORT" -> "Di chuyển"
        "HOTEL" -> "Khách sạn / Lưu trú"
        "SIGHTSEEING" -> "Vé tham quan"
        "ENTERTAINMENT" -> "Giải trí"
        "SHOPPING" -> "Mua sắm"
        else -> "Chi phí khác"
    }

    val splitTypeLabel = when (expense.splitType) {
        "EQUAL" -> "Chia đều cho tất cả thành viên"
        "CUSTOM_PARTICIPANT" -> "Chọn danh sách người tham gia"
        "RATIO" -> "Chia theo tỷ lệ %"
        "CUSTOM_AMOUNT" -> "Tùy nhập số tiền từng người"
        else -> expense.splitType
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(category = expense.category, size = 26)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(expense.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Thời gian: ${dateFormat.format(Date(expense.timestamp))}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tổng số tiền quy đổi (VND)", fontSize = 11.sp, color = EmeraldOnPrimaryContainer)
                        Text(
                            text = NumberFormatUtils.formatVnd(expense.convertedTotalAmount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldOnPrimaryContainer
                        )
                        if (expense.currency != "VND") {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tiền gốc: ${expense.totalAmount} ${expense.currency} • Tỷ giá: ${expense.exchangeRate}",
                                fontSize = 12.sp,
                                color = EmeraldOnPrimaryContainer
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "• Danh mục: $categoryLabel",
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "• Người xuất tiền: $payerName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "• Phân bổ: $splitTypeLabel",
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                    if (expense.note.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 2.dp))
                        Text(
                            text = "• Chi tiết / Ghi chú: ${expense.note}",
                            fontSize = 13.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Đóng")
            }
        }
    )
}
