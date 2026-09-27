package app.fingenmodern.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

data class AccountBalanceRow(
    val id: Long, val name: String, val type: String, val openingBalanceMinor: Long, val currency: String,
    val openingDate: String, val includeInNetWorth: Boolean, val archived: Boolean, val balanceMinor: Long
)

@Dao
interface FinanceDao {
    @Query("""SELECT a.id,a.name,a.type,a.openingBalanceMinor,a.currency,a.openingDate,a.includeInNetWorth,a.archived,
        a.openingBalanceMinor + COALESCE((SELECT SUM(le.deltaMinor) FROM ledger_entries le WHERE le.accountId=a.id),0) AS balanceMinor
        FROM accounts a WHERE a.archived=0 ORDER BY a.name""")
    fun observeAccounts(): Flow<List<AccountBalanceRow>>

    @Query("SELECT * FROM transactions ORDER BY date DESC,id DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int = 100): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM accounts") suspend fun countAccounts(): Int
    @Query("SELECT COUNT(*) FROM credit_card_templates") suspend fun countCreditCardTemplates(): Int
    @Query("SELECT COUNT(*) FROM categories") suspend fun countCategories(): Int
    @Query("SELECT COUNT(*) FROM import_candidates WHERE status='Pending'") fun observePendingImportCount(): Flow<Int>

    @Query("SELECT * FROM debts WHERE status!='Closed' ORDER BY dueDate IS NULL,dueDate")
    fun observeOpenDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id=:debtId LIMIT 1")
    suspend fun getDebt(debtId: Long): DebtEntity?

    @Query("""SELECT * FROM debts
        WHERE status!='Closed' AND remainingMinor=:amountMinor AND currency=:currency
        ORDER BY id LIMIT 2""")
    suspend fun findExactOpenDebts(amountMinor: Long, currency: String): List<DebtEntity>

    @Query("SELECT * FROM credit_card_templates ORDER BY bankName,productName")
    fun observeCreditCardTemplates(): Flow<List<CreditCardTemplateEntity>>

    @Query("SELECT * FROM categories WHERE archived=0 ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT id FROM categories WHERE key=:key AND archived=0 LIMIT 1")
    suspend fun getCategoryId(key: String): Long?

    @Query("SELECT * FROM import_candidates WHERE status='Pending' ORDER BY occurredAt DESC,id DESC")
    fun observePendingImportCandidates(): Flow<List<ImportCandidateEntity>>

    @Query("SELECT * FROM import_candidates WHERE id=:candidateId LIMIT 1")
    suspend fun getImportCandidate(candidateId: Long): ImportCandidateEntity?

    @Insert suspend fun insertAccount(account: AccountEntity): Long
    @Insert suspend fun insertDebt(debt: DebtEntity): Long
    @Update suspend fun updateDebt(debt: DebtEntity)
    @Insert suspend fun insertCreditCardTemplate(template: CreditCardTemplateEntity)
    @Insert suspend fun insertCategory(category: CategoryEntity): Long
    @Insert suspend fun insertTransaction(transaction: TransactionEntity): Long
    @Insert suspend fun insertLedgerEntries(entries: List<LedgerEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImportCandidate(candidate: ImportCandidateEntity): Long
    @Update suspend fun updateImportCandidate(candidate: ImportCandidateEntity)

    @Transaction
    suspend fun recordOperation(
        transaction: TransactionEntity,
        entries: List<LedgerEntryEntity>,
        newDebt: DebtEntity? = null,
        debtToReduceId: Long? = null,
        debtReductionMinor: Long = 0,
        acceptedImportCandidateId: Long? = null
    ): Long {
        val id = insertTransaction(transaction)
        insertLedgerEntries(entries.map { it.copy(transactionId = id) })
        newDebt?.let { insertDebt(it) }
        debtToReduceId?.let { debtId ->
            val debt = requireNotNull(getDebt(debtId)) { "Debt not found: $debtId" }
            val remaining = debt.remainingMinor - debtReductionMinor
            require(remaining >= 0) { "Debt repayment exceeds remaining amount" }
            updateDebt(debt.copy(remainingMinor = remaining, status = if (remaining == 0L) "Closed" else debt.status))
        }
        acceptedImportCandidateId?.let { candidateId ->
            getImportCandidate(candidateId)?.let {
                updateImportCandidate(it.copy(status = "Accepted"))
            }
        }
        return id
    }
}
