package app.fingenmodern.domain
import java.time.LocalDate

class CreditCardInterestEngine {
    fun projectStatement(
        template: CreditCardRuleTemplate, operations: List<CreditCardOperation>,
        statementDate: LocalDate, assumedPaymentDate: LocalDate, currency: String = "RUB"
    ): CreditCardStatementProjection {
        require(assumedPaymentDate >= statementDate)
        operations.forEach { require(it.amount.currency == currency) }
        val debtAtStatement = operations.filter { !it.date.isAfter(statementDate) }
            .fold(Money.zero(currency)) { acc, op -> if (op.type == CreditOperationType.Refund) acc - op.amount else acc + op.amount }
            .let { Money(it.minor.coerceAtLeast(0), currency) }
        val minimumByPercent = percentOf(debtAtStatement.minor, template.minimumPaymentPercent)
        val floor = template.minimumPaymentFloor.copy(currency = currency)
        val minimumPayment = Money(
            if (debtAtStatement.isZero()) 0 else minOf(debtAtStatement.minor, maxOf(minimumByPercent, floor.minor)), currency
        )
        val dueDate = calculateDueDate(template, statementDate, operations)
        val estimatedInterest = if (assumedPaymentDate <= dueDate) 0L else operations
            .filter { it.date <= statementDate && it.type != CreditOperationType.Refund }
            .sumOf { op ->
                val rate = when (op.type) {
                    CreditOperationType.CashWithdrawal, CreditOperationType.Transfer, CreditOperationType.QuasiCash ->
                        template.annualCashRatePercent ?: template.annualPurchaseRatePercent
                    else -> template.annualPurchaseRatePercent
                }
                val start = when (template.interestStartRule) {
                    InterestStartRule.FromOperationDate -> op.date
                    InterestStartRule.FromGraceEndDate -> dueDate
                    InterestStartRule.FromStatementDate -> statementDate
                }
                dailyInterest(op.amount.minor, rate, daysBetween(start, assumedPaymentDate))
            }
        val explanation = listOf(
            "Долг на дату выписки: " + Money(debtAtStatement.minor, currency).format(),
            "Минимальный платеж: " + minimumPayment.format(),
            "Расчетная дата окончания грейса: " + dueDate,
            if (assumedPaymentDate <= dueDate) "При погашении до этой даты расчетная модель процентов дает 0 ₽."
            else "Дата погашения позже расчетной даты; точные проценты зависят от договора."
        )
        return CreditCardStatementProjection(
            statementDate, dueDate, debtAtStatement, minimumPayment,
            Money(estimatedInterest, currency), explanation,
            if (assumedPaymentDate > dueDate) "Это оценка. Проверьте договор и выписку банка." else null
        )
    }
    private fun calculateDueDate(template: CreditCardRuleTemplate, statementDate: LocalDate, operations: List<CreditCardOperation>) =
        when (template.gracePeriodModel) {
            GracePeriodModel.FixedDaysFromPurchase -> {
                val latest = operations.filter { it.type != CreditOperationType.Refund }.maxOfOrNull { it.date } ?: statementDate
                latest.plusDays(template.graceLengthDays.toLong())
            }
            GracePeriodModel.CalendarMonthPlusPaymentWindow ->
                statementDate.plusMonths(1).plusDays((template.paymentWindowDays ?: 0).toLong())
            GracePeriodModel.StatementBased, GracePeriodModel.Custom ->
                statementDate.plusDays((template.paymentWindowDays ?: template.graceLengthDays).toLong())
        }
}
object RussianCreditCardTemplateSamples {
    val editableSamples = listOf(
        CreditCardRuleTemplate(
            id = "example-sber-custom", bankName = "СберБанк", productName = "Пользовательский шаблон",
            annualPurchaseRatePercent = java.math.BigDecimal("37.8"), gracePeriodModel = GracePeriodModel.Custom,
            graceLengthDays = 120, minimumPaymentPercent = java.math.BigDecimal("10"),
            minimumPaymentFloor = Money.rub(1500), interestStartRule = InterestStartRule.FromGraceEndDate,
            notes = "Пример структуры. Сверить с договором."
        ),
        CreditCardRuleTemplate(
            id = "example-tbank-custom", bankName = "Т-Банк", productName = "Пользовательский шаблон",
            annualPurchaseRatePercent = java.math.BigDecimal("59.9"), gracePeriodModel = GracePeriodModel.Custom,
            graceLengthDays = 55, paymentWindowDays = 25, minimumPaymentPercent = java.math.BigDecimal("14"),
            minimumPaymentFloor = Money.rub(600), interestStartRule = InterestStartRule.FromGraceEndDate,
            notes = "Пример структуры. Сверить с индивидуальными условиями."
        )
    )
}
