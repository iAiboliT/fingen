package app.fingenmodern.data
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName="accounts", indices=[Index("currency"), Index("archived")])
data class AccountEntity(
    @PrimaryKey(autoGenerate=true) val id:Long=0, val name:String, val type:String,
    val openingBalanceMinor:Long, val currency:String="RUB", val openingDate:String,
    val includeInNetWorth:Boolean=true, val archived:Boolean=false
)
@Entity(tableName="transactions", indices=[Index("date"), Index("type")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate=true) val id:Long=0, val type:String, val amountMinor:Long,
    val currency:String="RUB", val date:String, val fromAccountId:Long?, val toAccountId:Long?,
    val categoryId:Long?, val note:String?
)
@Entity(tableName="ledger_entries", indices=[Index("accountId"), Index("transactionId"), Index(value=["accountId","date"])])
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate=true) val id:Long=0, val transactionId:Long, val accountId:Long,
    val deltaMinor:Long, val currency:String, val date:String
)
@Entity(tableName="debts", indices=[Index("kind"),Index("status"),Index("currency")])
data class DebtEntity(
    @PrimaryKey(autoGenerate=true) val id:Long=0, val title:String, val counterparty:String,
    val originalMinor:Long, val remainingMinor:Long, val currency:String, val kind:String,
    val openedAt:String, val dueDate:String?, val monthlyPaymentMinor:Long?, val status:String, val note:String?
)
@Entity(tableName="credit_card_templates", indices=[Index("bankName")])
data class CreditCardTemplateEntity(
    @PrimaryKey val id:String, val version:Int, val bankName:String, val productName:String,
    val annualPurchaseRatePercent:String, val annualCashRatePercent:String?, val gracePeriodModel:String,
    val graceLengthDays:Int, val statementDayOfMonth:Int?, val paymentWindowDays:Int?,
    val minimumPaymentPercent:String, val minimumPaymentFloorMinor:Long, val currency:String,
    val interestStartRule:String, val cashOperationsBreakGrace:Boolean, val notes:String
)
