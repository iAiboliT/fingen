package app.fingenmodern.domain
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CreditOperationType { Purchase, CashWithdrawal, Transfer, QuasiCash, Fee, Refund }
enum class GracePeriodModel { StatementBased, CalendarMonthPlusPaymentWindow, FixedDaysFromPurchase, Custom }
enum class InterestStartRule { FromOperationDate, FromGraceEndDate, FromStatementDate }

data class CreditCardRuleTemplate(
    val id: String, val version: Int = 1, val bankName: String, val productName: String,
    val annualPurchaseRatePercent: BigDecimal, val annualCashRatePercent: BigDecimal? = null,
    val gracePeriodModel: GracePeriodModel, val graceLengthDays: Int,
    val statementDayOfMonth: Int? = null, val paymentWindowDays: Int? = null,
    val minimumPaymentPercent: BigDecimal, val minimumPaymentFloor: Money,
    val interestStartRule: InterestStartRule, val cashOperationsBreakGrace: Boolean = true,
    val notes: String = ""
) {
    init {
        require(graceLengthDays >= 0)
        require(annualPurchaseRatePercent >= BigDecimal.ZERO)
        require(annualCashRatePercent == null || annualCashRatePercent >= BigDecimal.ZERO)
        require(minimumPaymentPercent >= BigDecimal.ZERO)
        require(statementDayOfMonth == null || statementDayOfMonth in 1..31)
        require(paymentWindowDays == null || paymentWindowDays >= 0)
    }
}
data class CreditCardOperation(
    val id: Long = 0, val date: LocalDate, val amount: Money,
    val type: CreditOperationType, val description: String? = null
)
data class CreditCardStatementProjection(
    val statementDate: LocalDate, val paymentDueDate: LocalDate, val debtAtStatement: Money,
    val minimumPayment: Money, val estimatedInterestIfPaidOn: Money,
    val explanation: List<String>, val warning: String? = null
)
internal fun dailyInterest(amountMinor: Long, annualRatePercent: BigDecimal, days: Long): Long {
    if (amountMinor <= 0 || annualRatePercent.signum() == 0 || days <= 0) return 0
    return BigDecimal.valueOf(amountMinor).multiply(annualRatePercent)
        .divide(BigDecimal("100"), 16, RoundingMode.HALF_UP).divide(BigDecimal("365"), 16, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(days)).setScale(0, RoundingMode.HALF_UP).longValueExact()
}
fun daysBetween(start: LocalDate, endExclusive: LocalDate): Long =
    ChronoUnit.DAYS.between(start, endExclusive).coerceAtLeast(0)
