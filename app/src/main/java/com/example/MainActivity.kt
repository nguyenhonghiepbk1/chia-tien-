package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TripFinanceViewModel

enum class NavigationTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DASHBOARD("Tổng quan", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    EXPENSES("Chi tiêu", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    FUND("Quỹ đoàn", Icons.Filled.Savings, Icons.Outlined.Savings),
    SETTLEMENT("Quyết toán", Icons.Filled.Payments, Icons.Outlined.Payments),
    MEMBERS("Thành viên", Icons.Filled.Group, Icons.Outlined.Group)
}

class MainActivity : ComponentActivity() {

    private val viewModel: TripFinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripFinanceTheme {
                TripFinanceApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripFinanceApp(viewModel: TripFinanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    // Dialog state controllers
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddFundDialog by remember { mutableStateOf(false) }
    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showJoinTripDialog by remember { mutableStateOf(false) }
    var showExportReportDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TripFinance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Export Report Button in TopAppBar
                    IconButton(
                        onClick = { showExportReportDialog = true },
                        modifier = Modifier.testTag("export_report_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = "Xuất Báo Cáo",
                            tint = IndigoSecondary
                        )
                    }

                    // Quick trip selector dropdown if user has multiple trips
                    var tripMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { tripMenuExpanded = true },
                            modifier = Modifier.testTag("switch_trip_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = "Chuyển đoàn",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = tripMenuExpanded,
                            onDismissRequest = { tripMenuExpanded = false }
                        ) {
                            Text(
                                text = "Chọn chuyến đi / đoàn:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            uiState.allTrips.forEach { trip ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = trip.title,
                                            fontWeight = if (trip.id == uiState.currentTrip?.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectTrip(trip.id)
                                        tripMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (trip.id == uiState.currentTrip?.id) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldPrimary)
                                        }
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Tạo đoàn mới...") },
                                onClick = {
                                    showCreateTripDialog = true
                                    tripMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Nhập mã tham gia đoàn...") },
                                onClick = {
                                    showJoinTripDialog = true
                                    tripMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Filled.GroupAdd, contentDescription = null) }
                            )
                        }
                    }

                    // Toggle Offline Simulator Button
                    IconButton(
                        onClick = { viewModel.toggleOfflineMode() },
                        modifier = Modifier.testTag("toggle_offline_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isOfflineMode) Icons.Filled.WifiOff else Icons.Filled.Wifi,
                            contentDescription = "Chế độ Offline",
                            tint = if (uiState.isOfflineMode) AmberTertiary else EmeraldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimaryContainer
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline Banner
            OfflineStatusBanner(
                isOffline = uiState.isOfflineMode,
                pendingCount = uiState.pendingSyncCount,
                onToggleOffline = { viewModel.toggleOfflineMode() }
            )

            // Content per selected tab
            when (currentTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    onNavigateToExpenses = { currentTab = NavigationTab.EXPENSES },
                    onNavigateToFund = { currentTab = NavigationTab.FUND },
                    onNavigateToSettlement = { currentTab = NavigationTab.SETTLEMENT },
                    onOpenAddExpense = { showAddExpenseDialog = true },
                    onOpenAddFund = { showAddFundDialog = true },
                    onOpenExportReport = { showExportReportDialog = true },
                    onOpenCreateTrip = { showCreateTripDialog = true },
                    onOpenJoinTrip = { showJoinTripDialog = true },
                    onSwitchUser = { memberId -> viewModel.switchUserPersona(memberId) }
                )
                NavigationTab.EXPENSES -> ExpensesScreen(
                    uiState = uiState,
                    onOpenAddExpense = { showAddExpenseDialog = true },
                    onDeleteExpense = { exp -> viewModel.deleteExpense(exp) },
                    onCategoryFilterChange = { cat -> viewModel.setCategoryFilter(cat) },
                    onSearchChange = { q -> viewModel.setSearchQuery(q) }
                )
                NavigationTab.FUND -> FundScreen(
                    uiState = uiState,
                    onOpenAddFund = { showAddFundDialog = true }
                )
                NavigationTab.SETTLEMENT -> SettlementScreen(
                    uiState = uiState,
                    onFinalizeSettlement = { title -> viewModel.finalizeSettlement(title) },
                    onOpenExportReport = { showExportReportDialog = true }
                )
                NavigationTab.MEMBERS -> MembersAndSettingsScreen(
                    uiState = uiState,
                    onAddMember = { name, role, bank, acc, holder ->
                        viewModel.addMember(name, role, bank, acc, holder)
                    },
                    onUpdateRole = { member, newRole ->
                        viewModel.updateMemberRole(member, newRole)
                    },
                    onRemoveOrDeactivate = { member ->
                        viewModel.removeOrDeactivateMember(member)
                    },
                    onUpdateRate = { code, rate ->
                        viewModel.updateExchangeRate(code, rate)
                    }
                )
            }
        }
    }

    // Modal Dialogs
    if (showExportReportDialog) {
        ExportReportDialog(
            uiState = uiState,
            onDismiss = { showExportReportDialog = false }
        )
    }

    // Modal Dialogs
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            members = uiState.members,
            exchangeRates = uiState.exchangeRates,
            currentMemberId = uiState.currentMember?.id,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { title, category, payerType, payerMemberId, amount, currency, rate, splitType, splits, note, timestamp ->
                viewModel.addExpense(
                    title, category, payerType, payerMemberId, amount, currency, rate, splitType, splits, note, timestamp
                )
            }
        )
    }

    if (showAddFundDialog) {
        AddFundContributionDialog(
            members = uiState.members,
            onDismiss = { showAddFundDialog = false },
            onSave = { memberId, amount, currency, rate, note ->
                viewModel.addFundContribution(memberId, amount, currency, rate, note)
            }
        )
    }

    if (showCreateTripDialog) {
        CreateTripDialog(
            onDismiss = { showCreateTripDialog = false },
            onConfirm = { title, desc, code, start, end, admin, bName, bAcc, bHolder ->
                viewModel.createTrip(title, desc, code, start, end, admin, bName, bAcc, bHolder)
                showCreateTripDialog = false
            }
        )
    }

    if (showJoinTripDialog) {
        JoinTripDialog(
            onDismiss = { showJoinTripDialog = false },
            onConfirm = { code, userName ->
                viewModel.joinTripByCode(code, userName)
                showJoinTripDialog = false
            }
        )
    }
}
