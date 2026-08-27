package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.locale.AppLanguage
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.theme.*

enum class GuideCategory(
    val titleVi: String,
    val titleEn: String,
    val icon: ImageVector
) {
    ALL("Tất cả", "All", Icons.Filled.Apps),
    GETTING_STARTED("Bắt đầu nhanh", "Quick Start", Icons.Filled.RocketLaunch),
    TRIP_MGMT("Quản lý đoàn", "Trip Management", Icons.Filled.FlightTakeoff),
    EXPENSES("Ghi nhận chi tiêu", "Expenses & Splitting", Icons.Filled.ReceiptLong),
    FUND("Quản lý Quỹ", "Group Fund", Icons.Filled.Savings),
    SETTLEMENT("Quyết toán & QR", "Settlement & QR", Icons.Filled.Payments),
    EXCHANGE_RATE("Ngoại tệ & Tỷ giá", "Multi-Currency", Icons.Filled.CurrencyExchange),
    EXPORT("Xuất Báo Cáo", "Export Reports", Icons.Filled.FileDownload),
    FAQ("Câu hỏi thường gặp", "FAQ", Icons.Filled.HelpOutline)
}

data class GuideItem(
    val id: String,
    val category: GuideCategory,
    val titleVi: String,
    val titleEn: String,
    val summaryVi: String,
    val summaryEn: String,
    val stepsVi: List<String>,
    val stepsEn: List<String>,
    val tipsVi: String? = null,
    val tipsEn: String? = null,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(
    onBack: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var selectedCategory by remember { mutableStateOf(GuideCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedItemIds by remember { mutableStateOf(setOf<String>("quick_start_flow", "expense_add_guide")) }

    val guideItems = remember {
        listOf(
            GuideItem(
                id = "quick_start_flow",
                category = GuideCategory.GETTING_STARTED,
                titleVi = "1. Quy trình 4 bước quản lý tài chính đoàn",
                titleEn = "1. 4-Step Group Finance Management Workflow",
                summaryVi = "Toàn bộ chu trình từ khi bắt đầu chuyến đi đến khi thanh toán dứt điểm công nợ.",
                summaryEn = "Complete lifecycle from trip kickoff to final debt settlement.",
                stepsVi = listOf(
                    "Bước 1: Tạo đoàn công tác mới (hoặc nhập Mã tham gia nếu đã có người tạo trước).",
                    "Bước 2: Cài đặt danh sách thành viên, phân vai trò (Trưởng đoàn, Thủ quỹ, Thành viên) và thiết lập tài khoản nhận tiền.",
                    "Bước 3: Thu quỹ đoàn đầu chuyến (nếu có) và liên tục ghi lại mọi khoản chi tiêu phát sinh trong chuyến đi.",
                    "Bước 4: Vào tab 'Quyết toán' vào cuối chuyến, hệ thống tự tính bù trừ tối ưu min-transfers và quét mã VietQR chuyển khoản là xong!"
                ),
                stepsEn = listOf(
                    "Step 1: Create a new trip (or enter Join Code if already created).",
                    "Step 2: Setup member roster, assign roles (Leader, Treasurer, Member), and configure bank details.",
                    "Step 3: Collect group fund up-front (if applicable) and log all trip expenses as they occur.",
                    "Step 4: Navigate to 'Settlement' tab at the end of the trip to view optimal min-transfers & scan VietQR to clear all debts!"
                ),
                tipsVi = "Mẹo: Mọi khoản chi tiêu đều có thể nhập ngoại tệ (USD, EUR, JPY...), ứng dụng tự động quy đổi VND theo tỷ giá đã cấu hình.",
                tipsEn = "Tip: You can log expenses in foreign currencies (USD, EUR, JPY...); the app converts to VND automatically.",
                icon = Icons.Filled.AutoAwesome
            ),
            GuideItem(
                id = "trip_create_join",
                category = GuideCategory.TRIP_MGMT,
                titleVi = "2. Tạo đoàn mới và Mời thành viên tham gia",
                titleEn = "2. Create a Trip & Invite Members",
                summaryVi = "Cách khởi tạo chuyến đi, lấy mã Join Code và phân bổ người tham gia.",
                summaryEn = "How to initialize trips, generate Join Codes, and distribute to members.",
                stepsVi = listOf(
                    "Tại màn hình Tổng quan (Dashboard), bấm vào khung chọn đoàn xổ xuống và chọn '+ Tạo đoàn công tác mới'.",
                    "Điền Tên đoàn (VD: 'Công tác Đà Nẵng 2026'), Mô tả, Ngày đi - Ngày về và Mã tham gia (VD: 'DN2026').",
                    "Nhập Tên Trưởng đoàn/Thủ quỹ kèm thông tin Ngân hàng + Số tài khoản nhận tiền quỹ.",
                    "Gửi Mã tham gia cho các thành viên trong đoàn để họ chọn 'Nhập mã tham gia đoàn' trên máy của họ."
                ),
                stepsEn = listOf(
                    "On Dashboard, tap the trip dropdown selector and pick '+ Create New Trip'.",
                    "Fill in Trip Title (e.g. 'Da Nang Business Trip 2026'), Description, Dates, and Join Code (e.g. 'DN2026').",
                    "Enter Admin/Treasurer name with Bank name + Account number for fund receipt.",
                    "Share the Join Code with other team members so they can join via 'Join Trip with Code'."
                ),
                tipsVi = "Mã tham gia không phân biệt hoa thường và giúp mọi người vào đúng dữ liệu của đoàn một cách nhanh chóng.",
                tipsEn = "Join codes are case-insensitive and allow fast team onboarding.",
                icon = Icons.Filled.FlightTakeoff
            ),
            GuideItem(
                id = "switch_trip_guide",
                category = GuideCategory.TRIP_MGMT,
                titleVi = "3. Chuyển đổi giữa nhiều đoàn công tác",
                titleEn = "3. Switching Between Multiple Trips",
                summaryVi = "Quản lý song song nhiều chuyến đi hoặc các đoàn công tác trong quá khứ.",
                summaryEn = "Manage parallel or past trips seamlessly.",
                stepsVi = listOf(
                    "Ngay đầu màn hình Tổng quan (Dashboard), bạn sẽ thấy nút chọn 'ĐOÀN CÔNG TÁC ĐANG CHỌN' có mũi tên xổ xuống.",
                    "Chạm vào thanh này để mở danh sách toàn bộ các đoàn công tác bạn đã tạo hoặc đã tham gia.",
                    "Chạm vào đoàn bạn muốn chuyển sang: toàn bộ số liệu thu chi, quỹ, số dư thành viên sẽ lập tức chuyển đổi tức thì.",
                    "Bạn cũng có thể sửa thông tin hoặc xóa đoàn cũ ở phần danh sách đoàn phía dưới."
                ),
                stepsEn = listOf(
                    "At the top of the Dashboard, tap the 'SELECTED TRIP' card with the dropdown arrow.",
                    "The dropdown lists all trips you have created or joined.",
                    "Tap on any trip to instantly switch all dashboards, expenses, fund, and balances.",
                    "You can also edit or delete past trips from the trip management list below."
                ),
                tipsVi = "Dữ liệu của từng đoàn được lưu trữ độc lập và an toàn, không bị lẫn lộn.",
                tipsEn = "Each trip's data is isolated and safely preserved.",
                icon = Icons.Filled.SwapHoriz
            ),
            GuideItem(
                id = "expense_add_guide",
                category = GuideCategory.EXPENSES,
                titleVi = "4. Ghi nhận Khoản Chi & Các cách Chia Tiền",
                titleEn = "4. Recording Expenses & Split Methods",
                summaryVi = "Hỗ trợ chi từ Quỹ hoặc Thành viên chi hộ, chia đều hoặc chia theo suất tùy chỉnh.",
                summaryEn = "Supports Fund-paid vs Member-paid, split equally or custom shares.",
                stepsVi = listOf(
                    "Bấm nút '+ Thêm Chi' (hoặc nút + nổi tròn tại tab Chi Tiêu).",
                    "Nhập tên khoản chi (VD: 'Ăn tối hải sản', 'Taxi ra sân bay') và chọn Danh mục phù hợp.",
                    "Chọn Người trả tiền: 'Trích từ Quỹ Đoàn' (nếu lấy từ quỹ chung) HOẶC chọn Tên thành viên đứng ra chi trả hộ.",
                    "Nhập Số tiền và Loại tiền tệ (VND, USD, EUR, JPY...).",
                    "Chọn Cách phân bổ chi phí: 'Chia đều cho tất cả', 'Chia đều cho người được chọn', hoặc 'Tùy chỉnh số tiền/suất' cho từng người cụ thể.",
                    "Bấm 'Lưu Khoản Chi'. Hệ thống tự động ghi nhận và cập nhật lại số dư công nợ của mọi người!"
                ),
                stepsEn = listOf(
                    "Tap '+ Add Expense' (or the floating action button in Expenses tab).",
                    "Enter expense title (e.g. 'Team Dinner', 'Airport Taxi') and pick a category.",
                    "Choose Payer: 'Paid from Group Fund' OR select the specific member who paid on behalf.",
                    "Enter Amount and Currency (VND, USD, EUR, JPY...).",
                    "Choose Split Method: 'Equal to all', 'Selected participants', or 'Custom shares/amounts'.",
                    "Tap 'Save Expense'. Balances update automatically in real time!"
                ),
                tipsVi = "Khi chọn 'Chia cho người được chọn', bạn có thể tích/bỏ tích tên ai tham gia bữa ăn/chuyến đi đó để chia công bằng tuyệt đối.",
                tipsEn = "With custom selection, uncheck anyone who didn't participate so they are not charged.",
                icon = Icons.Filled.ReceiptLong
            ),
            GuideItem(
                id = "fund_mgmt_guide",
                category = GuideCategory.FUND,
                titleVi = "5. Thu & Quản Lý Quỹ Đoàn",
                titleEn = "5. Collecting & Managing Group Fund",
                summaryVi = "Theo dõi tiền quỹ nộp vào, tiến độ thu quỹ và các khoản chi trực tiếp từ quỹ.",
                summaryEn = "Track contributions, collection progress, and fund withdrawals.",
                stepsVi = listOf(
                    "Vào tab 'Quỹ đoàn' hoặc bấm 'Nộp Quỹ' tại Dashboard.",
                    "Bấm '+ Ghi nhận nộp quỹ' khi có thành viên chuyển tiền hoặc đưa tiền mặt cho thủ quỹ.",
                    "Chọn Tên thành viên nộp, Số tiền nộp (VND/Ngoại tệ), phương thức và ghi chú.",
                    "Màn hình Quỹ hiển thị rõ ràng: Tổng tiền đã thu, Tổng đã chi từ quỹ, và Số dư quỹ khả dụng còn lại.",
                    "Khi thêm chi tiêu, chỉ cần chọn người chi là 'Quỹ đoàn' thì tiền sẽ tự động trừ vào quỹ này."
                ),
                stepsEn = listOf(
                    "Go to 'Group Fund' tab or tap 'Add Fund' from Dashboard.",
                    "Tap '+ Record Contribution' when a member pays cash or transfers fund money to treasurer.",
                    "Select member name, contribution amount (VND/Foreign), method, and notes.",
                    "The Fund screen displays: Total Collected, Total Disbursed, and Current Available Balance.",
                    "When adding an expense, selecting 'Group Fund' automatically deducts from this pool."
                ),
                tipsVi = "Tiền quỹ giúp đoàn chi tiêu các khoản chung nhanh chóng mà không cần từng người phải ứng tiền túi nhiều lần.",
                tipsEn = "A group fund minimizes individual out-of-pocket payments during group activities.",
                icon = Icons.Filled.Savings
            ),
            GuideItem(
                id = "settlement_algorithm_guide",
                category = GuideCategory.SETTLEMENT,
                titleVi = "6. Quyết Toán Tối Ưu & Quét Mã VietQR",
                titleEn = "6. Optimal Settlement & VietQR Scan",
                summaryVi = "Thuật toán rút gọn số giao dịch (Min-Transfers) kèm mã QR chuyển khoản tích tắc.",
                summaryEn = "Debt simplification algorithm with instant VietQR payment codes.",
                stepsVi = listOf(
                    "Khi kết thúc chuyến đi hoặc hết đợt công tác, mở tab 'Quyết toán'.",
                    "Hệ thống tự động phân tích: Ai chi nhiều hơn mức phải chịu sẽ 'Cần nhận lại', ai chi ít hơn sẽ 'Cần đóng thêm'.",
                    "Thuật toán Tối Ưu Hóa Giao Dịch tự động tính toán số lệnh chuyển khoản ít nhất (Ví dụ 10 người chỉ cần 3-4 lệnh chuyển là xong nợ).",
                    "Chạm vào từng lệnh chuyển tiền để xem chi tiết hoặc mở Mã QR Ngân Hàng (VietQR) tự điền đúng STK, Ngân hàng và Số tiền chính xác.",
                    "Người nộp chỉ cần quét mã QR bằng App Ngân hàng bất kỳ (Vietcombank, MBBank, Techcombank, BIDV, VPBank...) để chuyển tiền trong 3 giây!",
                    "Sau khi tất cả đã chuyển xong, bấm 'Chốt quyết toán đoàn' để lưu trữ lịch sử."
                ),
                stepsEn = listOf(
                    "At the end of the trip, switch to 'Settlement' tab.",
                    "The app calculates net balances: who paid more receives refund, who paid less settles the difference.",
                    "The Min-Transfers Algorithm condenses complex group debts into the minimal number of direct bank transfers.",
                    "Tap any transfer item to view bank details and open the VietQR payment code pre-filled with exact amount and account.",
                    "Members simply scan the QR code using any banking app to transfer in 3 seconds!",
                    "Tap 'Finalize Settlement' to archive the completed settlement cycle."
                ),
                tipsVi = "Mã VietQR tự động sinh theo chuẩn quốc gia NAPAS 247, chuyển khoản tức thì không sợ gõ nhầm số tài khoản hay số tiền.",
                tipsEn = "VietQR is generated following NAPAS 247 standards, avoiding typos in bank account numbers or amounts.",
                icon = Icons.Filled.QrCode2
            ),
            GuideItem(
                id = "currency_rates_guide",
                category = GuideCategory.EXCHANGE_RATE,
                titleVi = "7. Tỷ Giá Ngoại Tệ (USD, EUR, JPY, KRW...)",
                titleEn = "7. Multi-Currency & Custom Exchange Rates",
                summaryVi = "Hỗ trợ công tác nước ngoài thuận tiện, tự cập nhật và quy đổi tỷ giá.",
                summaryEn = "Ideal for overseas business trips with custom exchange rates.",
                stepsVi = listOf(
                    "Vào tab 'Thành viên' (Settings), cuộn xuống phần 'Bảng Tỷ Giá Quy Đổi Ngoại Tệ'.",
                    "Ứng dụng đã cài đặt sẵn tỷ giá tham khảo cho USD, EUR, JPY, KRW, THB, SGD, CNY...",
                    "Bạn có thể nhấn nút 'Sửa' bên cạnh bất kỳ loại tiền nào để điều chỉnh tỷ giá thực tế theo lúc đổi tiền hoặc quẹt thẻ tín dụng.",
                    "Mỗi khi nhập khoản chi bằng ngoại tệ, ứng dụng tự động hiển thị số tiền quy đổi ra VND tương ứng."
                ),
                stepsEn = listOf(
                    "Go to 'Members & Settings' tab and scroll to 'Currency Exchange Rates'.",
                    "Default rates for USD, EUR, JPY, KRW, THB, SGD, CNY are preloaded.",
                    "Tap 'Edit' beside any currency to adjust the rate according to your actual exchange receipt or credit card rate.",
                    "Whenever an expense is logged in foreign currency, VND converted equivalent is displayed."
                ),
                tipsVi = "Bạn có thể gõ đè tỷ giá trực tiếp ngay trong bảng nhập chi tiêu nếu một giao dịch có tỷ giá riêng.",
                tipsEn = "You can also override the exchange rate directly on individual expense entries.",
                icon = Icons.Filled.CurrencyExchange
            ),
            GuideItem(
                id = "export_report_guide",
                category = GuideCategory.EXPORT,
                titleVi = "8. Xuất Báo Cáo Tài Chính & Bảng Kê Excel (CSV)",
                titleEn = "8. Financial Report Export & Excel (CSV)",
                summaryVi = "Xuất dữ liệu chi tiết phục vụ thanh quyết toán công tác phí với cơ quan/doanh nghiệp.",
                summaryEn = "Export detailed breakdown for company expense reimbursement.",
                stepsVi = listOf(
                    "Tại Dashboard hoặc tab Quyết toán, bấm nút 'Báo Cáo' / biểu tượng Tài liệu trên thanh tiêu đề.",
                    "Chọn định dạng xuất mong muốn: 'Báo cáo văn bản tóm tắt' hoặc 'Bảng kê chi tiết Excel (.csv)'.",
                    "Xem trước nội dung bảng tổng hợp chi phí, phân loại danh mục, đối soát từng người.",
                    "Bấm 'Sao chép' hoặc 'Chia sẻ' để gửi trực tiếp qua Zalo, Messenger, Email hoặc lưu về máy tính mở bằng Microsoft Excel."
                ),
                stepsEn = listOf(
                    "From Dashboard or Settlement tab, tap 'Report' / Document icon on the top bar.",
                    "Choose format: 'Summary Text Report' or 'Detailed Excel CSV Table'.",
                    "Preview the consolidated financial statement, category breakdown, and member ledger.",
                    "Tap 'Copy' or 'Share' to send via Zalo, Email, or open directly in Microsoft Excel."
                ),
                tipsVi = "File CSV được mã hóa chuẩn UTF-8 kèm BOM, hiển thị tiếng Việt hoàn hảo trên mọi phiên bản Excel máy tính và điện thoại.",
                tipsEn = "CSV exports include UTF-8 BOM encoding, ensuring Vietnamese characters display properly in Excel.",
                icon = Icons.Filled.Description
            ),
            GuideItem(
                id = "offline_sync_faq",
                category = GuideCategory.FAQ,
                titleVi = "9. Đi máy bay / Không có mạng có dùng được không?",
                titleEn = "9. Can I use the app Offline without Internet?",
                summaryVi = "Ứng dụng hoạt động 100% ngoại tuyến (Offline-First) với cơ sở dữ liệu nội bộ SQLite.",
                summaryEn = "100% Offline-First functionality backed by local SQLite Room database.",
                stepsVi = listOf(
                    "Hoàn toàn ĐƯỢC! Ứng dụng được thiết kế theo kiến trúc Offline-First.",
                    "Khi bạn ở trên máy bay, vùng không có sóng hoặc công tác nước ngoài chưa bật Roaming, bạn vẫn thêm chi tiêu, nộp quỹ và xem quyết toán bình thường.",
                    "Tất cả dữ liệu được lưu an toàn tuyệt đối trong bộ nhớ máy của bạn.",
                    "Bạn có thể thử bật/tắt nút 'Chế độ Ngoại Tuyến' (biểu tượng Wifi trên góc phải) để trải nghiệm cơ chế lưu trữ."
                ),
                stepsEn = listOf(
                    "YES, absolutely! The app is designed with an Offline-First architecture.",
                    "On airplanes, remote areas, or overseas trips without roaming, you can still add expenses, funds, and view settlements normally.",
                    "All data is securely persisted on your local device storage.",
                    "You can toggle the 'Offline Mode' icon on top bar anytime."
                ),
                tipsVi = "Không bao giờ lo mất dữ liệu hay bị gián đoạn ghi chép chi phí trong suốt chuyến công tác dài ngày.",
                tipsEn = "Never worry about lost data or interrupted records during business trips.",
                icon = Icons.Filled.WifiOff
            )
        )
    }

    val filteredItems = remember(guideItems, selectedCategory, searchQuery) {
        guideItems.filter { item ->
            val matchCategory = selectedCategory == GuideCategory.ALL || item.category == selectedCategory
            val matchSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                item.titleVi.lowercase().contains(q) ||
                        item.titleEn.lowercase().contains(q) ||
                        item.summaryVi.lowercase().contains(q) ||
                        item.summaryEn.lowercase().contains(q) ||
                        item.stepsVi.any { it.lowercase().contains(q) } ||
                        item.stepsEn.any { it.lowercase().contains(q) }
            }
            matchCategory && matchSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (lang == AppLanguage.VI) "Hướng Dẫn Sử Dụng" else "User Guide & Help",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == AppLanguage.VI) "Cẩm nang quản lý tài chính đoàn" else "Trip finance handbook",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("user_guide_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (lang == AppLanguage.VI) "Quay lại" else "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.VI) "Chào mừng đến với TripFinance" else "Welcome to TripFinance",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (lang == AppLanguage.VI)
                                        "Giải pháp quản lý quỹ, chi tiêu minh bạch & quyết toán bù trừ tối ưu cho đoàn công tác"
                                    else
                                        "Transparent group expense tracking & optimal debt settlement for business trips",
                                    fontSize = 12.sp,
                                    color = Color(0xEEFFFFFF)
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_guide_search_input"),
                    placeholder = {
                        Text(
                            if (lang == AppLanguage.VI) "Tìm kiếm chủ đề, tính năng, từ khóa..." else "Search guide topics, keywords...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GuideCategory.values()) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.VI) cat.titleVi else cat.titleEn,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) EmeraldPrimary else Color(0xFF64748B)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimaryContainer,
                                selectedLabelColor = EmeraldOnPrimaryContainer
                            )
                        )
                    }
                }
            }

            // List of Guide Accordion Cards
            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == AppLanguage.VI) "Không tìm thấy nội dung phù hợp" else "No matching guide found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (lang == AppLanguage.VI) "Thử tìm với từ khóa khác như: 'quỹ', 'chia tiền', 'qr', 'tỷ giá'" else "Try searching with 'fund', 'split', 'qr', 'rate'",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredItems) { item ->
                    val isExpanded = expandedItemIds.contains(item.id)
                    GuideAccordionCard(
                        item = item,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedItemIds = if (isExpanded) {
                                expandedItemIds - item.id
                            } else {
                                expandedItemIds + item.id
                            }
                        }
                    )
                }
            }

            // Bottom Quick Help Footer
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.SupportAgent,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (lang == AppLanguage.VI) "Cần hỗ trợ thêm?" else "Need More Help?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == AppLanguage.VI)
                                    "Ứng dụng tự động lưu trữ và bảo vệ dữ liệu nội bộ an toàn trên thiết bị của bạn."
                                else
                                    "All trip financial data is safely persisted locally on your device.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideAccordionCard(
    item: GuideItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val title = if (lang == AppLanguage.VI) item.titleVi else item.titleEn
    val summary = if (lang == AppLanguage.VI) item.summaryVi else item.summaryEn
    val steps = if (lang == AppLanguage.VI) item.stepsVi else item.stepsEn
    val tips = if (lang == AppLanguage.VI) item.tipsVi else item.tipsEn

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("guide_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isExpanded) EmeraldPrimaryContainer else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isExpanded) EmeraldPrimary else Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isExpanded) {
                            Text(
                                text = summary,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                        tint = if (isExpanded) EmeraldPrimary else Color(0xFF64748B)
                    )
                }
            }

            // Expanded Details Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = summary,
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step by step list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldOnPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Tips / Notes Box
                    if (!tips.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tips,
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
