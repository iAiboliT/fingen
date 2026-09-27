package app.fingenmodern.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities=[AccountEntity::class,TransactionEntity::class,LedgerEntryEntity::class,DebtEntity::class,CreditCardTemplateEntity::class,CategoryEntity::class,ImportCandidateEntity::class,ReceiptEntity::class,ReceiptItemEntity::class,ReceiptTransactionLinkEntity::class],version=6,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){
    abstract fun financeDao():FinanceDao
    abstract fun receiptDao():ReceiptDao
}
