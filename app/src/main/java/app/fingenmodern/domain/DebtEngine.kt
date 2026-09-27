package app.fingenmodern.domain
import java.time.LocalDate

sealed interface DebtAction {
    data class GiveMoney(val accountId: Long, val counterparty: String, val amount: Money, val date: LocalDate, val note: String? = null): DebtAction
    data class ReceiveBack(val accountId: Long, val debtId: Long, val amount: Money, val date: LocalDate, val note: String? = null): DebtAction
    data class BorrowMoney(val accountId: Long, val counterparty: String, val amount: Money, val date: LocalDate, val note: String? = null): DebtAction
    data class RepayBorrowed(val accountId: Long, val debtId: Long, val amount: Money, val date: LocalDate, val note: String? = null): DebtAction
}
data class DebtActionPreview(val title: String, val walletEffect: Money, val netWorthEffect: Money, val transactionType: TransactionType, val explanation: String)
class DebtEngine {
    fun preview(action: DebtAction) = when (action) {
        is DebtAction.GiveMoney -> DebtActionPreview("Я дал деньги в долг", -action.amount, Money.zero(action.amount.currency), TransactionType.DebtIssue, "Деньги ушли из кошелька, но появился актив: человек должен вам.")
        is DebtAction.ReceiveBack -> DebtActionPreview("Мне вернули долг", action.amount, Money.zero(action.amount.currency), TransactionType.DebtRepayment, "Деньги вернулись, актив по долгу уменьшился.")
        is DebtAction.BorrowMoney -> DebtActionPreview("Я взял деньги в долг", action.amount, Money.zero(action.amount.currency), TransactionType.BorrowFromPerson, "Деньги появились, но одновременно возникло обязательство.")
        is DebtAction.RepayBorrowed -> DebtActionPreview("Я вернул долг", -action.amount, Money.zero(action.amount.currency), TransactionType.RepayPerson, "Деньги ушли, обязательство уменьшилось.")
    }
}
