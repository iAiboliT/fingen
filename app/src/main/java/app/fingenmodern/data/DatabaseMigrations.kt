package app.fingenmodern.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override suspend fun migrate(c: SQLiteConnection) {
            c.execSQL("""CREATE TABLE accounts_new(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,type TEXT NOT NULL,
                openingBalanceMinor INTEGER NOT NULL,currency TEXT NOT NULL DEFAULT 'RUB',openingDate TEXT NOT NULL DEFAULT '1970-01-01',
                includeInNetWorth INTEGER NOT NULL DEFAULT 1,archived INTEGER NOT NULL DEFAULT 0)""")
            c.execSQL("""INSERT INTO accounts_new(id,name,type,openingBalanceMinor,currency,openingDate,includeInNetWorth,archived)
                SELECT id,name,type,balanceMinor,currency,'1970-01-01',includeInNetWorth,0 FROM accounts""")
            c.execSQL("DROP TABLE accounts")
            c.execSQL("ALTER TABLE accounts_new RENAME TO accounts")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_currency ON accounts(currency)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_archived ON accounts(archived)")
            c.execSQL("""CREATE TABLE IF NOT EXISTS ledger_entries(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,transactionId INTEGER NOT NULL,
                accountId INTEGER NOT NULL,deltaMinor INTEGER NOT NULL,currency TEXT NOT NULL,date TEXT NOT NULL)""")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_entries_accountId ON ledger_entries(accountId)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_entries_transactionId ON ledger_entries(transactionId)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_entries_accountId_date ON ledger_entries(accountId,date)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override suspend fun migrate(c: SQLiteConnection) {
            c.execSQL("""CREATE TABLE IF NOT EXISTS debts(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,title TEXT NOT NULL,counterparty TEXT NOT NULL,
                originalMinor INTEGER NOT NULL,remainingMinor INTEGER NOT NULL,currency TEXT NOT NULL,kind TEXT NOT NULL,openedAt TEXT NOT NULL,
                dueDate TEXT,monthlyPaymentMinor INTEGER,status TEXT NOT NULL,note TEXT)""")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_debts_kind ON debts(kind)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_debts_status ON debts(status)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_debts_currency ON debts(currency)")
            c.execSQL("""CREATE TABLE IF NOT EXISTS credit_card_templates(id TEXT PRIMARY KEY NOT NULL,version INTEGER NOT NULL,bankName TEXT NOT NULL,
                productName TEXT NOT NULL,annualPurchaseRatePercent TEXT NOT NULL,annualCashRatePercent TEXT,gracePeriodModel TEXT NOT NULL,
                graceLengthDays INTEGER NOT NULL,statementDayOfMonth INTEGER,paymentWindowDays INTEGER,minimumPaymentPercent TEXT NOT NULL,
                minimumPaymentFloorMinor INTEGER NOT NULL,currency TEXT NOT NULL,interestStartRule TEXT NOT NULL,cashOperationsBreakGrace INTEGER NOT NULL,notes TEXT NOT NULL)""")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_templates_bankName ON credit_card_templates(bankName)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override suspend fun migrate(c: SQLiteConnection) {
            c.execSQL("""CREATE TABLE IF NOT EXISTS categories(
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                key TEXT NOT NULL,
                name TEXT NOT NULL,
                parentId INTEGER,
                archived INTEGER NOT NULL DEFAULT 0
            )""")
            c.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_key ON categories(key)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_categories_parentId ON categories(parentId)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override suspend fun migrate(c: SQLiteConnection) {
            c.execSQL("""CREATE TABLE IF NOT EXISTS import_candidates(
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sourceKey TEXT NOT NULL,
                source TEXT NOT NULL,
                sourcePackage TEXT,
                title TEXT,
                text TEXT NOT NULL,
                amountMinor INTEGER NOT NULL,
                currency TEXT NOT NULL,
                direction TEXT NOT NULL,
                occurredAt TEXT NOT NULL,
                suggestedReason TEXT,
                suggestedDebtId INTEGER,
                status TEXT NOT NULL DEFAULT 'Pending'
            )""")
            c.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_import_candidates_sourceKey ON import_candidates(sourceKey)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_import_candidates_status ON import_candidates(status)")
            c.execSQL("CREATE INDEX IF NOT EXISTS index_import_candidates_occurredAt ON import_candidates(occurredAt)")
        }
    }
}
