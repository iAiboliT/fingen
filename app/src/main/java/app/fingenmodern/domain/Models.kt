package app.fingenmodern.domain
import java.time.LocalDate

enum class AccountType { Cash, DebitCard, CreditCard, Loan, DebtReceivable, DebtPayable, Savings, Investment }
enum class TransactionType { Income, Expense, Transfer, DebtIssue, DebtRepayment, BorrowFromPerson, RepayPerson, CreditDrawdown, CreditPayment, Adjustment }

data class Account(
    val id: Long, val name: String, val type: AccountType, val balance: Money,
    val includeInNetWorth: Boolean = true, val archived: Boolean = false
)
data class Transaction(
    val id: Long = 0, val type: TransactionType, val amount: Money, val date: LocalDate,
    val fromAccountId: Long? = null, val toAccountId: Long? = null,
    val categoryId: Long? = null, val note: String? = null
)
enum class DebtKind { TheyOweMe, IOwePerson, BankLoan, CreditCard }
enum class DebtStatus { Active, Closed, Overdue }
data class DebtPosition(
    val id: Long, val title: String, val counterparty: String, val originalAmount: Money,
    val remaining: Money, val kind: DebtKind, val openedAt: LocalDate,
    val dueDate: LocalDate? = null, val monthlyPayment: Money? = null,
    val status: DebtStatus = DebtStatus.Active, val note: String? = null
)
data class BalanceSummary(
    val assets: Money, val liabilities: Money, val netWorth: Money, val availableCash: Money,
    val debtReceivable: Money, val debtPayable: Money, val bankDebt: Money
)
