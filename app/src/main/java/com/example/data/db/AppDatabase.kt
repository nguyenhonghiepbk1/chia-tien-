package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        TripEntity::class,
        TripMemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        FundContributionEntity::class,
        ExchangeRateEntity::class,
        SettlementSnapshotEntity::class,
        AuditLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripMemberDao(): TripMemberDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun fundDao(): FundDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun settlementDao(): SettlementDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trip_finance_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Bật ràng buộc khóa ngoại chặt chẽ ở cấp độ SQLite engine
                        db.execSQL("PRAGMA foreign_keys = ON;")
                        createDatabaseCheckConstraints(db)
                    }

                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Kích hoạt khóa ngoại & ràng buộc kiểm tra
                        db.execSQL("PRAGMA foreign_keys = ON;")
                        createDatabaseCheckConstraints(db)

                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { seedDatabase(it) }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun createDatabaseCheckConstraints(db: SupportSQLiteDatabase) {
            // 1. RÀNG BUỘC CƠ SỞ DỮ LIỆU: CHẶN ĐỨNG THAO TÁC XÓA THÀNH VIÊN ĐÃ CÓ PHÁT SINH TÀI CHÍNH / CHI TIÊU
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_prevent_delete_member_with_financials
                BEFORE DELETE ON trip_members
                BEGIN
                    SELECT CASE
                        WHEN (SELECT COUNT(*) FROM expenses WHERE payerMemberId = OLD.id OR createdMemberId = OLD.id) > 0
                            THEN RAISE(ABORT, 'CHECK CONSTRAINT VIOLATION: Không thể xóa thành viên đã có phát sinh chi tiêu (người chi hoặc người tạo khoản chi). Hãy chuyển sang trạng thái Vô hiệu hoá (Deactivate) để bảo toàn chứng từ kế toán.')
                        WHEN (SELECT COUNT(*) FROM expense_splits WHERE memberId = OLD.id) > 0
                            THEN RAISE(ABORT, 'CHECK CONSTRAINT VIOLATION: Không thể xóa thành viên đang có dữ liệu phân bổ chia tiền. Hãy chuyển sang trạng thái Vô hiệu hoá (Deactivate) để bảo toàn chứng từ kế toán.')
                        WHEN (SELECT COUNT(*) FROM fund_contributions WHERE memberId = OLD.id OR recordedByMemberId = OLD.id) > 0
                            THEN RAISE(ABORT, 'CHECK CONSTRAINT VIOLATION: Không thể xóa thành viên đã có phát sinh nộp hoặc thu quỹ chung. Hãy chuyển sang trạng thái Vô hiệu hoá (Deactivate).')
                    END;
                END;
            """.trimIndent())

            // 2. RÀNG BUỘC KIỂM TRA SỐ TIỀN CHI TIÊU KHÔNG ĐƯỢC ÂM VÀ TỶ GIÁ DƯƠNG
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_expense_insert
                BEFORE INSERT ON expenses
                BEGIN
                    SELECT CASE
                        WHEN NEW.totalAmount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: totalAmount cannot be negative')
                        WHEN NEW.convertedTotalAmount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: convertedTotalAmount cannot be negative')
                        WHEN NEW.exchangeRate <= 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: exchangeRate must be positive')
                    END;
                END;
            """.trimIndent())

            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_expense_update
                BEFORE UPDATE ON expenses
                BEGIN
                    SELECT CASE
                        WHEN NEW.totalAmount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: totalAmount cannot be negative')
                        WHEN NEW.convertedTotalAmount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: convertedTotalAmount cannot be negative')
                        WHEN NEW.exchangeRate <= 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: exchangeRate must be positive')
                    END;
                END;
            """.trimIndent())

            // 3. RÀNG BUỘC PHÂN BỔ CHI TIÊU KHÔNG ĐƯỢC ÂM
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_split_insert
                BEFORE INSERT ON expense_splits
                BEGIN
                    SELECT CASE
                        WHEN NEW.amount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: split amount cannot be negative')
                    END;
                END;
            """.trimIndent())

            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_split_update
                BEFORE UPDATE ON expense_splits
                BEGIN
                    SELECT CASE
                        WHEN NEW.amount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: split amount cannot be negative')
                    END;
                END;
            """.trimIndent())

            // 4. RÀNG BUỘC TIỀN ĐÓNG QUÝ KHÔNG ĐƯỢC ÂM
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_fund_insert
                BEFORE INSERT ON fund_contributions
                BEGIN
                    SELECT CASE
                        WHEN NEW.amount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: fund amount cannot be negative')
                        WHEN NEW.convertedAmount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: fund convertedAmount cannot be negative')
                        WHEN NEW.exchangeRate <= 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: fund exchangeRate must be positive')
                    END;
                END;
            """.trimIndent())

            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_fund_update
                BEFORE UPDATE ON fund_contributions
                BEGIN
                    SELECT CASE
                        WHEN NEW.amount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: fund amount cannot be negative')
                        WHEN NEW.convertedAmount < 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: fund convertedAmount cannot be negative')
                        WHEN NEW.exchangeRate <= 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: fund exchangeRate must be positive')
                    END;
                END;
            """.trimIndent())

            // 5. RÀNG BUỘC TỶ GIÁ NGOẠI TỆ PHẢI DƯƠNG
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_rate_insert
                BEFORE INSERT ON exchange_rates
                BEGIN
                    SELECT CASE
                        WHEN NEW.rateToBase <= 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: exchange rate must be positive')
                    END;
                END;
            """.trimIndent())

            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS trg_check_rate_update
                BEFORE UPDATE ON exchange_rates
                BEGIN
                    SELECT CASE
                        WHEN NEW.rateToBase <= 0 THEN RAISE(ABORT, 'CHECK CONSTRAINT FAILED: exchange rate must be positive')
                    END;
                END;
            """.trimIndent())
        }

        private suspend fun seedDatabase(database: AppDatabase) {
            val tripId = "trip_danang_2026"
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            val trip = TripEntity(
                id = tripId,
                title = "Đoàn Công Tác & Team Building Đà Nẵng",
                description = "Chuyến công tác kết hợp team building quý 3/2026 tại Đà Nẵng - Hội An",
                joinCode = "DN2026",
                startDate = now - 2 * dayMillis,
                endDate = now + 1 * dayMillis,
                baseCurrency = "VND",
                isSettled = false,
                settledAt = null,
                createdAt = now - 2 * dayMillis,
                version = 1L
            )
            database.tripDao().insertTrip(trip)

            val members = listOf(
                TripMemberEntity(
                    id = "member_1",
                    tripId = tripId,
                    userId = "user_hiep",
                    name = "Nguyễn Hồng Hiệp (Trưởng đoàn)",
                    role = "ADMIN",
                    isActive = true,
                    bankName = "Vietcombank",
                    bankAccount = "1029384756",
                    bankAccountHolder = "NGUYEN HONG HIEP",
                    joinedAt = now - 2 * dayMillis
                ),
                TripMemberEntity(
                    id = "member_2",
                    tripId = tripId,
                    userId = "user_mai",
                    name = "Trần Tuyết Mai (Thủ quỹ)",
                    role = "TREASURER",
                    isActive = true,
                    bankName = "Techcombank",
                    bankAccount = "190345678901",
                    bankAccountHolder = "TRAN TUYET MAI",
                    joinedAt = now - 2 * dayMillis
                ),
                TripMemberEntity(
                    id = "member_3",
                    tripId = tripId,
                    userId = "user_quang",
                    name = "Lê Nhật Quang",
                    role = "MEMBER",
                    isActive = true,
                    bankName = "MBBank",
                    bankAccount = "0987654321",
                    bankAccountHolder = "LE NHAT QUANG",
                    joinedAt = now - 2 * dayMillis
                ),
                TripMemberEntity(
                    id = "member_4",
                    tripId = tripId,
                    userId = "user_lan",
                    name = "Phạm Hương Lan",
                    role = "MEMBER",
                    isActive = true,
                    bankName = "ACB",
                    bankAccount = "246813579",
                    bankAccountHolder = "PHAM HUONG LAN",
                    joinedAt = now - 2 * dayMillis
                ),
                TripMemberEntity(
                    id = "member_5",
                    tripId = tripId,
                    userId = "user_duc",
                    name = "Hoàng Minh Đức",
                    role = "VIEWER",
                    isActive = true,
                    bankName = "TPBank",
                    bankAccount = "03399887711",
                    bankAccountHolder = "HOANG MINH DUC",
                    joinedAt = now - 2 * dayMillis
                )
            )
            database.tripMemberDao().insertMembers(members)

            // Seed Exchange Rates
            val exchangeRates = listOf(
                ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "USD", 25450.0, now),
                ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "EUR", 27600.0, now),
                ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "JPY", 168.0, now),
                ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "THB", 720.0, now),
                ExchangeRateEntity(UUID.randomUUID().toString(), tripId, "SGD", 19200.0, now)
            )
            database.exchangeRateDao().insertExchangeRates(exchangeRates)

            // Seed Fund Contributions (500,000 VND each from member 1 to 4)
            val fundContributions = listOf(
                FundContributionEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    memberId = "member_1",
                    amount = 500000L,
                    currency = "VND",
                    exchangeRate = 1.0,
                    convertedAmount = 500000L,
                    note = "Đóng quỹ đợt 1",
                    timestamp = now - 2 * dayMillis + 3600000L,
                    recordedByMemberId = "member_2"
                ),
                FundContributionEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    memberId = "member_2",
                    amount = 500000L,
                    currency = "VND",
                    exchangeRate = 1.0,
                    convertedAmount = 500000L,
                    note = "Đóng quỹ đợt 1",
                    timestamp = now - 2 * dayMillis + 3600000L,
                    recordedByMemberId = "member_2"
                ),
                FundContributionEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    memberId = "member_3",
                    amount = 500000L,
                    currency = "VND",
                    exchangeRate = 1.0,
                    convertedAmount = 500000L,
                    note = "Đóng quỹ đợt 1",
                    timestamp = now - 2 * dayMillis + 3600000L,
                    recordedByMemberId = "member_2"
                ),
                FundContributionEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    memberId = "member_4",
                    amount = 500000L,
                    currency = "VND",
                    exchangeRate = 1.0,
                    convertedAmount = 500000L,
                    note = "Đóng quỹ đợt 1",
                    timestamp = now - 2 * dayMillis + 3600000L,
                    recordedByMemberId = "member_2"
                )
            )
            fundContributions.forEach { database.fundDao().insertFundContribution(it) }

            // Expense 1: Seafood Dinner paid by Member 1 (Hiệp out-of-pocket, split equal between 4 active members)
            val exp1Id = UUID.randomUUID().toString()
            val exp1 = ExpenseEntity(
                id = exp1Id,
                tripId = tripId,
                title = "Tiệc tối Hải sản Bé Mặn",
                category = "FOOD",
                payerType = "MEMBER",
                payerMemberId = "member_1",
                totalAmount = 2400000L,
                currency = "VND",
                exchangeRate = 1.0,
                convertedTotalAmount = 2400000L,
                splitType = "EQUAL",
                note = "Bữa tối chào mừng đoàn ngày đầu tiên",
                timestamp = now - 2 * dayMillis + 28800000L,
                createdMemberId = "member_1"
            )
            database.expenseDao().insertExpense(exp1)
            val splits1 = listOf(
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp1Id, tripId, "member_1", 600000L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp1Id, tripId, "member_2", 600000L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp1Id, tripId, "member_3", 600000L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp1Id, tripId, "member_4", 600000L)
            )
            database.expenseDao().insertSplits(splits1)

            // Expense 2: Grab Airport Van paid from FUND (Quỹ đoàn chi)
            val exp2Id = UUID.randomUUID().toString()
            val exp2 = ExpenseEntity(
                id = exp2Id,
                tripId = tripId,
                title = "Xe 16 chỗ đón sân bay Đà Nẵng",
                category = "TRANSPORT",
                payerType = "FUND",
                payerMemberId = null,
                totalAmount = 650000L,
                currency = "VND",
                exchangeRate = 1.0,
                convertedTotalAmount = 650000L,
                splitType = "EQUAL",
                note = "Chi từ Quỹ chung đoàn",
                timestamp = now - 2 * dayMillis + 7200000L,
                createdMemberId = "member_2"
            )
            database.expenseDao().insertExpense(exp2)
            val splits2 = listOf(
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp2Id, tripId, "member_1", 162500L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp2Id, tripId, "member_2", 162500L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp2Id, tripId, "member_3", 162500L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp2Id, tripId, "member_4", 162500L)
            )
            database.expenseDao().insertSplits(splits2)

            // Expense 3: Coffee & Working Space paid by Member 3 (Quang) in USD
            val exp3Id = UUID.randomUUID().toString()
            val exp3 = ExpenseEntity(
                id = exp3Id,
                tripId = tripId,
                title = "Working Cafe & Workshop Hoi An",
                category = "ENTERTAINMENT",
                payerType = "MEMBER",
                payerMemberId = "member_3",
                totalAmount = 40L,
                currency = "USD",
                exchangeRate = 25450.0,
                convertedTotalAmount = 1018000L,
                splitType = "EQUAL",
                note = "Họp review tiến độ dự án tại quán cà phê",
                timestamp = now - 1 * dayMillis + 14400000L,
                createdMemberId = "member_3"
            )
            database.expenseDao().insertExpense(exp3)
            val splits3 = listOf(
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp3Id, tripId, "member_1", 254500L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp3Id, tripId, "member_2", 254500L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp3Id, tripId, "member_3", 254500L),
                ExpenseSplitEntity(UUID.randomUUID().toString(), exp3Id, tripId, "member_4", 254500L)
            )
            database.expenseDao().insertSplits(splits3)

            // Audit Logs
            database.auditLogDao().insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    actorMemberId = "member_1",
                    actorName = "Nguyễn Hồng Hiệp",
                    action = "CREATE_TRIP",
                    description = "Khởi tạo chuyến đi và cấu hình tỷ giá ngoại tệ",
                    timestamp = now - 2 * dayMillis
                )
            )
            database.auditLogDao().insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    actorMemberId = "member_2",
                    actorName = "Trần Tuyết Mai",
                    action = "CONTRIBUTE_FUND",
                    description = "Thu quỹ đợt 1: 500,000 VND x 4 thành viên (Tổng 2,000,000 VND)",
                    timestamp = now - 2 * dayMillis + 3600000L
                )
            )
        }
    }
}
