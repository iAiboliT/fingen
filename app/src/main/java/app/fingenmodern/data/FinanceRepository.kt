package app.fingenmodern.data

import app.fingenmodern.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface FinanceRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeDebts(): Flow<List<DebtPosition>>
    fun observeCreditCardTemplates(): Flow<List<CreditCardRuleTemplate>>
    fun observeCategories(): Flow<List<Category>>
    suspend fun categoryId(key: String): Long?
    suspend fun addOperation(draft: OperationDraft)
}

@Singleton
class RoomFinanceRepository @Inject constructor(private val dao: FinanceDao) : FinanceRepository {
    override fun observeAccounts() = dao.observeAccounts().map { rows ->
        rows.map {
            Account(it.id, it.name, AccountType.valueOf(it.type), Money(it.balanceMinor, it.currency), it.includeInNetWorth, it.archived)
        }
    }

    override fun observeDebts() = dao.observeOpenDebts().map { it.map { e -> e.toDomain() } }

    override fun observeCreditCardTemplates() =
        dao.observeCreditCardTemplates().map { it.map { e -> e.toDomain() } }

    override fun observeCategories() =
        dao.observeCategories().map { it.map { e -> Category(e.id, e.key, e.name, e.parentId, e.archived) } }

    override suspend fun categoryId(key: String): Long? = dao.getCategoryId(key)

    override suspend fun addOperation(d: OperationDraft) {
        val tx = when (d) {
            is OperationDraft.Income -> TransactionEntity(TransactionType.Income.name, d.amount.minor, d.amount.currency, d.date.toString(), null, d.accountId, d.categoryId, d.note)
            is OperationDraft.Expense -> TransactionEntity(TransactionType.Expense.name, d.amount.minor, d.amount.currency, d.date.toString(), d.accountId, null, d.categoryId, d.note)
            is OperationDraft.Transfer -> TransactionEntity(TransactionType.Transfer.name, d.amount.minor, d.amount.currency, d.date.toString(), d.fromAccountId, d.toAccountId, null, d.note)
            is OperationDraft.LendToPerson -> TransactionEntity(TransactionType.DebtIssue.name, d.amount.minor, d.amount.currency, d.date.toString(), d.fromAccountId, null, null, d.note ?: ("Долг: " + d.personName))
            is OperationDraft.BorrowFromPerson -> TransactionEntity(TransactionType.BorrowFromPerson.name, d.amount.minor, d.amount.currency, d.date.toString(), null, d.toAccountId, null, d.note ?: ("Долг перед: " + d.personName))
            is OperationDraft.ReceiveDebtBack -> TransactionEntity(TransactionType.DebtRepayment.name, d.amount.minor, d.amount.currency, d.date.toString(), null, d.toAccountId, null, d.note ?: ("Возврат долга #" + d.debtId))
            is OperationDraft.RepayBorrowedMoney -> TransactionEntity(TransactionType.RepayPerson.name, d.amount.minor, d.amount.currency, d.date.toString(), d.fromAccountId, null, null, d.note ?: ("Погашение долга #" + d.debtId))
            is OperationDraft.CreditCardPurchase -> TransactionEntity(TransactionType.CreditDrawdown.name, d.amount.minor, d.amount.currency, d.date.toString(), d.creditCardAccountId, null, d.categoryId, d.note)
            is OperationDraft.CreditCardPayment -> TransactionEntity(TransactionType.CreditPayment.name, d.amount.minor, d.amount.currency, d.date.toString(), d.fromAccountId, d.creditCardAccountId, null, d.note)
        }

        val entries = when (d) {
            is OperationDraft.Income -> listOf(entry(d.accountId, d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.Expense -> listOf(entry(d.accountId, -d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.Transfer -> listOf(entry(d.fromAccountId, -d.amount.minor, d.amount.currency, d.date), entry(d.toAccountId, d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.LendToPerson -> listOf(entry(d.fromAccountId, -d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.BorrowFromPerson -> listOf(entry(d.toAccountId, d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.ReceiveDebtBack -> listOf(entry(d.toAccountId, d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.RepayBorrowedMoney -> listOf(entry(d.fromAccountId, -d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.CreditCardPurchase -> listOf(entry(d.creditCardAccountId, -d.amount.minor, d.amount.currency, d.date))
            is OperationDraft.CreditCardPayment -> listOf(entry(d.fromAccountId, -d.amount.minor, d.amount.currency, d.date), entry(d.creditCardAccountId, d.amount.minor, d.amount.currency, d.date))
        }

        val newDebt = when (d) {
            is OperationDraft.LendToPerson -> DebtEntity(title = "Долг " + d.personName, counterparty = d.personName, originalMinor = d.amount.minor, remainingMinor = d.amount.minor, currency = d.amount.currency, kind = DebtKind.TheyOweMe.name, openedAt = d.date.toString(), dueDate = null, monthlyPaymentMinor = null, status = DebtStatus.Active.name, note = d.note)
            is OperationDraft.BorrowFromPerson -> DebtEntity(title = "Долг перед " + d.personName, counterparty = d.personName, originalMinor = d.amount.minor, remainingMinor = d.amount.minor, currency = d.amount.currency, kind = DebtKind.IOwePerson.name, openedAt = d.date.toString(), dueDate = null, monthlyPaymentMinor = null, status = DebtStatus.Active.name, note = d.note)
            else -> null
        }
        val debtId = when (d) {
            is OperationDraft.ReceiveDebtBack -> d.debtId
            is OperationDraft.RepayBorrowedMoney -> d.debtId
            else -> null
        }
        dao.recordOperation(tx, entries, newDebt, debtId, if (debtId != null) d.amount.minor else 0L)
    }

    private fun entry(id: Long, delta: Long, currency: String, date: LocalDate) =
        LedgerEntryEntity(accountId = id, deltaMinor = delta, currency = currency, date = date.toString(), transactionId = 0)

    private fun DebtEntity.toDomain() = DebtPosition(id, title, counterparty, Money(originalMinor, currency), Money(remainingMinor, currency), DebtKind.valueOf(kind), LocalDate.parse(openedAt), dueDate?.let(LocalDate::parse), monthlyPaymentMinor?.let { Money(it, currency) }, DebtStatus.valueOf(status), note)

    private fun CreditCardTemplateEntity.toDomain() = CreditCardRuleTemplate(id, version, bankName, productName, BigDecimal(annualPurchaseRatePercent), annualCashRatePercent?.let(::BigDecimal), GracePeriodModel.valueOf(gracePeriodModel), graceLengthDays, statementDayOfMonth, paymentWindowDays, BigDecimal(minimumPaymentPercent), Money(minimumPaymentFloorMinor, currency), InterestStartRule.valueOf(interestStartRule), cashOperationsBreakGrace, notes)
}
