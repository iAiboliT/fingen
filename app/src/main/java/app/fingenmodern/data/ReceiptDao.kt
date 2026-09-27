package app.fingenmodern.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

data class ReceiptOperationWrite(val transaction:TransactionEntity,val accountId:Long,val deltaMinor:Long)

@Dao
interface ReceiptDao {
    @Insert suspend fun insertReceipt(receipt:ReceiptEntity):Long
    @Insert suspend fun insertItems(items:List<ReceiptItemEntity>)
    @Insert suspend fun insertTransaction(transaction:TransactionEntity):Long
    @Insert suspend fun insertLedgerEntries(entries:List<LedgerEntryEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun linkTransaction(link:ReceiptTransactionLinkEntity):Long
    @Query("SELECT * FROM receipts WHERE fiscalKey=:fiscalKey LIMIT 1") suspend fun findReceipt(fiscalKey:String):ReceiptEntity?
    @Query("SELECT * FROM transactions WHERE type='Expense' AND amountMinor=:amountMinor AND currency=:currency AND date BETWEEN :fromDate AND :toDate ORDER BY date DESC,id DESC LIMIT 20")
    suspend fun findExpenseCandidates(amountMinor:Long,currency:String,fromDate:String,toDate:String):List<TransactionEntity>

    @Transaction
    suspend fun saveReceiptAndExpenses(receipt:ReceiptEntity,items:List<ReceiptItemEntity>,operations:List<ReceiptOperationWrite>):Long {
        val receiptId=insertReceipt(receipt)
        require(receiptId>0L){"Чек с таким фискальным идентификатором уже сохранён"}
        if(items.isNotEmpty()) insertItems(items.map { it.copy(receiptId=receiptId) })
        operations.forEach { write ->
            val transactionId=insertTransaction(write.transaction)
            insertLedgerEntries(listOf(LedgerEntryEntity(transactionId=transactionId,accountId=write.accountId,deltaMinor=write.deltaMinor,currency=write.transaction.currency,date=write.transaction.date)))
            linkTransaction(ReceiptTransactionLinkEntity(receiptId,transactionId))
        }
        return receiptId
    }
}
