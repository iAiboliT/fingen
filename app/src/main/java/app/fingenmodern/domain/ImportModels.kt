package app.fingenmodern.domain

import java.time.LocalDateTime

enum class ImportDirection { Income, Expense }

enum class IncomingMoneyReason(val title: String) {
    Salary("Зарплата"),
    DebtRepayment("Возврат долга"),
    ServicePayment("Оплата услуг"),
    Sale("Оплата покупки / продажи"),
    Refund("Возврат денег"),
    Transfer("Перевод между людьми"),
    Gift("Подарок"),
    Other("Другое")
}

enum class ImportCandidateStatus { Pending, Accepted, Ignored }

data class ImportCandidate(
    val id: Long,
    val source: String,
    val sourcePackage: String?,
    val title: String?,
    val text: String,
    val amount: Money,
    val direction: ImportDirection,
    val occurredAt: LocalDateTime,
    val suggestedReason: IncomingMoneyReason? = null,
    val suggestedDebtId: Long? = null,
    val status: ImportCandidateStatus = ImportCandidateStatus.Pending
)

data class NotificationCandidate(
    val sourceKey: String,
    val sourcePackage: String,
    val title: String?,
    val text: String,
    val amount: Money,
    val direction: ImportDirection,
    val occurredAt: LocalDateTime
)

object IncomingMoneyClassifier {
    const val LARGE_INCOMING_THRESHOLD_MINOR = 50_000_00L

    fun suggestReason(text: String, hasExactDebtMatch: Boolean): IncomingMoneyReason {
        val normalized = text.lowercase()
        return when {
            hasExactDebtMatch -> IncomingMoneyReason.DebtRepayment
            normalized.containsAny("зарплат", "аванс", "оклад") -> IncomingMoneyReason.Salary
            normalized.containsAny("возврат", "refund", "отмена") -> IncomingMoneyReason.Refund
            normalized.containsAny("услуг", "работ", "заказ", "клиент", "оплат") -> IncomingMoneyReason.ServicePayment
            normalized.containsAny("перевод", "с карты на карту", "по номеру телефона") -> IncomingMoneyReason.Transfer
            normalized.containsAny("покупк", "продаж") -> IncomingMoneyReason.Sale
            normalized.containsAny("подар") -> IncomingMoneyReason.Gift
            else -> IncomingMoneyReason.Other
        }
    }

    fun isLarge(amount: Money): Boolean = amount.minor >= LARGE_INCOMING_THRESHOLD_MINOR

    private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it) }
}
