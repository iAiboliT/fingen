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
    fun observeImportCandidates(): Flow<List<ImportCandidate>>
    fun observePendingImportCount(): Flow<Int>
    suspend fun categoryId(key: String): Long?
    suspend fun addOperation(draft: OperationDraft)
    suspend fun addImportCandidate(candidate: NotificationCandidate)
    suspend fun acceptImportCandidate(
        candidateId: Long,
        accountId: Long,
        reason: IncomingMoneyReason?,
        categoryId: Long?,
        debtId: Long?
    )
    suspend fun ignoreImportCandidate(candidateId: Long)
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

    override fun observeImportCandidates() =
        dao.observePendingImportCandidates().map { list ->
            list.map {
                ImportCandidate(
                    id = it.id,
                    source = it.source,
                    sourcePackage = it.sourcePackage,
                    title = it.title,
                    text = it.text,
                    amount = Money(it.amountMinor, it.currency),
                    direction = ImportDirection.valueOf(it.direction),
                    occurredAt = java.time.LocalDateTime.parse(it.occurredAt),
                    suggestedReason = it.suggestedReason?.let(IncomingMoneyReason::valueOf),
                    suggestedDebtId = it.suggestedDebtId,
                    status = ImportCandidateStatus.valueOf(it.status)
                )
            }
        }

    override fun observePendingImportCount() = dao.observePendingImportCount()

    override suspend fun categoryId(key: String): Long? = dao.getCategoryId(key)

    override suspend fun addOperation(d: OperationDraft) {
        val tx = when (d) {
            is OperationDraft.Income -> TransactionEntity(type = TransactionType.Income.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = null, toAccountId = d.accountId, categoryId = d.categoryId, note = d.note)
            is OperationDraft.Expense -> TransactionEntity(type = TransactionType.Expense.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = d.accountId, toAccountId = null, categoryId = d.categoryId, note = d.note)
            is OperationDraft.Transfer -> TransactionEntity(type = TransactionType.Transfer.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = d.fromAccountId, toAccountId = d.toAccountId, categoryId = null, note = d.note)
            is OperationDraft.LendToPerson -> TransactionEntity(type = TransactionType.DebtIssue.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = d.fromAccountId, toAccountId = null, categoryId = null, note = d.note ?: ("Долг: " + d.personName))
            is OperationDraft.BorrowFromPerson -> TransactionEntity(type = TransactionType.BorrowFromPerson.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = null, toAccountId = d.toAccountId, categoryId = null, note = d.note ?: ("Долг перед: " + d.personName))
            is OperationDraft.ReceiveDebtBack -> TransactionEntity(type = TransactionType.DebtRepayment.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = null, toAccountId = d.toAccountId, categoryId = null, note = d.note ?: ("Возврат долга #" + d.debtId))
            is OperationDraft.RepayBorrowedMoney -> TransactionEntity(type = TransactionType.RepayPerson.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = d.fromAccountId, toAccountId = null, categoryId = null, note = d.note ?: ("Погашение долга #" + d.debtId))
            is OperationDraft.CreditCardPurchase -> TransactionEntity(type = TransactionType.CreditDrawdown.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = d.creditCardAccountId, toAccountId = null, categoryId = d.categoryId, note = d.note)
            is OperationDraft.CreditCardPayment -> TransactionEntity(type = TransactionType.CreditPayment.name, amountMinor = d.amount.minor, currency = d.amount.currency, date = d.date.toString(), fromAccountId = d.fromAccountId, toAccountId = d.creditCardAccountId, categoryId = null, note = d.note)
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

    override suspend fun addImportCandidate(candidate: NotificationCandidate) {
        val exactDebts = if (candidate.direction == ImportDirection.Income) {
            dao.findExactOpenDebts(candidate.amount.minor, candidate.amount.currency)
        } else {
            emptyList()
        }
        val suggestedDebtId = exactDebts.singleOrNull()?.id
        val suggestedReason = if (candidate.direction == ImportDirection.Income) {
            IncomingMoneyClassifier.suggestReason(candidate.text, suggestedDebtId != null)
        } else null

        dao.insertImportCandidate(
            ImportCandidateEntity(
                sourceKey = candidate.sourceKey,
                source = "BANK_NOTIFICATION",
                sourcePackage = candidate.sourcePackage,
                title = candidate.title,
                text = candidate.text,
                amountMinor = candidate.amount.minor,
                currency = candidate.amount.currency,
                direction = candidate.direction.name,
                occurredAt = candidate.occurredAt.toString(),
                suggestedReason = suggestedReason?.name,
                suggestedDebtId = suggestedDebtId
            )
        )
    }

    override suspend fun acceptImportCandidate(
        candidateId: Long,
        accountId: Long,
        reason: IncomingMoneyReason?,
        categoryId: Long?,
        debtId: Long?
    ) {
        val candidate = requireNotNull(dao.getImportCandidate(candidateId)) { "Import candidate not found: " + candidateId }
        require(candidate.status == ImportCandidateStatus.Pending.name) { "Import candidate is already processed" }

        val date = LocalDate.parse(candidate.occurredAt.substringBefore('T'))
        val amount = Money(candidate.amountMinor, candidate.currency)
        val draft = when {
            candidate.direction == ImportDirection.Income.name && reason == IncomingMoneyReason.DebtRepayment -> {
                val selectedDebt = requireNotNull(debtId) { "Для возврата долга нужно выбрать долг" }
                val debt = requireNotNull(dao.getDebt(selectedDebt)) { "Debt not found: " + selectedDebt }
                require(debt.kind == DebtKind.TheyOweMe.name) { "Выбран не долг, который должны вам" }
                require(debt.currency == candidate.currency) { "Валюта долга и поступления различается" }
                require(candidate.amountMinor <= debt.remainingMinor) { "Сумма возврата больше остатка долга" }
                OperationDraft.ReceiveDebtBack(
                    toAccountId = accountId,
                    debtId = selectedDebt,
                    amount = amount,
                    date = date,
                    note = "Автоимпорт: " + candidate.text
                )
            }
            candidate.direction == ImportDirection.Income.name -> {
                OperationDraft.Income(
                    accountId = accountId,
                    amount = amount,
                    date = date,
                    categoryId = categoryId,
                    note = "Автоимпорт: " + candidate.text
                )
            }
            else -> {
                OperationDraft.Expense(
                    accountId = accountId,
                    amount = amount,
                    date = date,
                    categoryId = categoryId,
                    note = "Автоимпорт: " + candidate.text
                )
            }
        }

        val tx = when (draft) {
            is OperationDraft.Income -> TransactionEntity(
                type = TransactionType.Income.name,
                amountMinor = draft.amount.minor,
                currency = draft.amount.currency,
                date = draft.date.toString(),
                fromAccountId = null,
                toAccountId = draft.accountId,
                categoryId = draft.categoryId,
                note = draft.note
            )
            is OperationDraft.Expense -> TransactionEntity(
                type = TransactionType.Expense.name,
                amountMinor = draft.amount.minor,
                currency = draft.amount.currency,
                date = draft.date.toString(),
                fromAccountId = draft.accountId,
                toAccountId = null,
                categoryId = draft.categoryId,
                note = draft.note
            )
            is OperationDraft.ReceiveDebtBack -> TransactionEntity(
                type = TransactionType.DebtRepayment.name,
                amountMinor = draft.amount.minor,
                currency = draft.amount.currency,
                date = draft.date.toString(),
                fromAccountId = null,
                toAccountId = draft.toAccountId,
                categoryId = null,
                note = draft.note
            )
            else -> error("Unsupported import operation")
        }
        val entries = when (draft) {
            is OperationDraft.Income -> listOf(entry(draft.accountId, draft.amount.minor, draft.amount.currency, draft.date))
            is OperationDraft.Expense -> listOf(entry(draft.accountId, -draft.amount.minor, draft.amount.currency, draft.date))
            is OperationDraft.ReceiveDebtBack -> listOf(entry(draft.toAccountId, draft.amount.minor, draft.amount.currency, draft.date))
            else -> error("Unsupported import operation")
        }
        val debtIdToReduce = (draft as? OperationDraft.ReceiveDebtBack)?.debtId
        dao.recordOperation(
            transaction = tx,
            entries = entries,
            debtToReduceId = debtIdToReduce,
            debtReductionMinor = if (debtIdToReduce != null) amount.minor else 0L,
            acceptedImportCandidateId = candidateId
        )
    }

    override suspend fun ignoreImportCandidate(candidateId: Long) {
        val candidate = requireNotNull(dao.getImportCandidate(candidateId)) { "Import candidate not found: " + candidateId }
        dao.updateImportCandidate(candidate.copy(status = ImportCandidateStatus.Ignored.name))
    }

    private fun entry(id: Long, delta: Long, currency: String, date: LocalDate) =
        LedgerEntryEntity(accountId = id, deltaMinor = delta, currency = currency, date = date.toString(), transactionId = 0)

    private fun DebtEntity.toDomain() = DebtPosition(id, title, counterparty, Money(originalMinor, currency), Money(remainingMinor, currency), DebtKind.valueOf(kind), LocalDate.parse(openedAt), dueDate?.let(LocalDate::parse), monthlyPaymentMinor?.let { Money(it, currency) }, DebtStatus.valueOf(status), note)

    private fun CreditCardTemplateEntity.toDomain() = CreditCardRuleTemplate(id, version, bankName, productName, BigDecimal(annualPurchaseRatePercent), annualCashRatePercent?.let(::BigDecimal), GracePeriodModel.valueOf(gracePeriodModel), graceLengthDays, statementDayOfMonth, paymentWindowDays, BigDecimal(minimumPaymentPercent), Money(minimumPaymentFloorMinor, currency), InterestStartRule.valueOf(interestStartRule), cashOperationsBreakGrace, notes)
}
