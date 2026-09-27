package app.fingenmodern.domain
import java.time.LocalDate

sealed interface OperationDraft {
    val amount: Money
    val date: LocalDate
    val note: String?
    data class Income(val accountId: Long, override val amount: Money, override val date: LocalDate, val categoryId: Long?, override val note: String? = null): OperationDraft
    data class Expense(val accountId: Long, override val amount: Money, override val date: LocalDate, val categoryId: Long?, override val note: String? = null): OperationDraft
    data class Transfer(val fromAccountId: Long, val toAccountId: Long, override val amount: Money, override val date: LocalDate, override val note: String? = null): OperationDraft
    data class LendToPerson(val fromAccountId: Long, val personName: String, override val amount: Money, override val date: LocalDate, override val note: String? = null): OperationDraft
    data class BorrowFromPerson(val toAccountId: Long, val personName: String, override val amount: Money, override val date: LocalDate, override val note: String? = null): OperationDraft
    data class ReceiveDebtBack(val toAccountId: Long, val debtId: Long, override val amount: Money, override val date: LocalDate, override val note: String? = null): OperationDraft
    data class RepayBorrowedMoney(val fromAccountId: Long, val debtId: Long, override val amount: Money, override val date: LocalDate, override val note: String? = null): OperationDraft
    data class CreditCardPurchase(val creditCardAccountId: Long, override val amount: Money, override val date: LocalDate, val categoryId: Long?, override val note: String? = null): OperationDraft
    data class CreditCardPayment(val fromAccountId: Long, val creditCardAccountId: Long, override val amount: Money, override val date: LocalDate, override val note: String? = null): OperationDraft
}
data class OperationPreview(val title: String, val transaction: Transaction, val walletEffect: Money, val netWorthEffect: Money, val explanation: String)
class OperationFactory {
    fun preview(d: OperationDraft) = when (d) {
        is OperationDraft.Income -> OperationPreview("Доход", Transaction(type=TransactionType.Income, amount=d.amount, date=d.date, toAccountId=d.accountId, categoryId=d.categoryId, note=d.note), d.amount, d.amount, "Деньги пришли на счет и увеличили капитал.")
        is OperationDraft.Expense -> OperationPreview("Расход", Transaction(type=TransactionType.Expense, amount=d.amount, date=d.date, fromAccountId=d.accountId, categoryId=d.categoryId, note=d.note), -d.amount, -d.amount, "Деньги ушли со счета и уменьшили капитал.")
        is OperationDraft.Transfer -> OperationPreview("Перевод между своими счетами", Transaction(type=TransactionType.Transfer, amount=d.amount, date=d.date, fromAccountId=d.fromAccountId, toAccountId=d.toAccountId, note=d.note), Money.zero(d.amount.currency), Money.zero(d.amount.currency), "Деньги переместились между вашими счетами.")
        is OperationDraft.LendToPerson -> OperationPreview("Дал в долг", Transaction(type=TransactionType.DebtIssue, amount=d.amount, date=d.date, fromAccountId=d.fromAccountId, note=d.note ?: ("Долг: " + d.personName)), -d.amount, Money.zero(d.amount.currency), "Появился актив 'мне должны'.")
        is OperationDraft.BorrowFromPerson -> OperationPreview("Взял в долг", Transaction(type=TransactionType.BorrowFromPerson, amount=d.amount, date=d.date, toAccountId=d.toAccountId, note=d.note ?: ("Долг перед: " + d.personName)), d.amount, Money.zero(d.amount.currency), "Появилось обязательство.")
        is OperationDraft.ReceiveDebtBack -> OperationPreview("Мне вернули долг", Transaction(type=TransactionType.DebtRepayment, amount=d.amount, date=d.date, toAccountId=d.toAccountId, note=d.note ?: ("Возврат долга #" + d.debtId)), d.amount, Money.zero(d.amount.currency), "Актив по долгу уменьшается.")
        is OperationDraft.RepayBorrowedMoney -> OperationPreview("Я вернул долг", Transaction(type=TransactionType.RepayPerson, amount=d.amount, date=d.date, fromAccountId=d.fromAccountId, note=d.note ?: ("Погашение долга #" + d.debtId)), -d.amount, Money.zero(d.amount.currency), "Обязательство уменьшается.")
        is OperationDraft.CreditCardPurchase -> OperationPreview("Покупка по кредитке", Transaction(type=TransactionType.CreditDrawdown, amount=d.amount, date=d.date, fromAccountId=d.creditCardAccountId, categoryId=d.categoryId, note=d.note), Money.zero(d.amount.currency), -d.amount, "Собственные деньги сейчас не ушли, но возникло обязательство.")
        is OperationDraft.CreditCardPayment -> OperationPreview("Платеж по кредитке", Transaction(type=TransactionType.CreditPayment, amount=d.amount, date=d.date, fromAccountId=d.fromAccountId, toAccountId=d.creditCardAccountId, note=d.note), -d.amount, Money.zero(d.amount.currency), "Деньги ушли с дебетового счета, долг уменьшился.")
    }
}
