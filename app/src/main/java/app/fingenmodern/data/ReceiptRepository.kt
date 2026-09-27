package app.fingenmodern.data

import app.fingenmodern.domain.*
import java.security.MessageDigest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(private val dao: ReceiptDao) {
    suspend fun findMatches(receipt: Receipt): List<ReceiptTransactionMatch> {
        val date = receipt.dateTime?.toLocalDate() ?: return emptyList()
        val rows = dao.findExpenseCandidates(receipt.total.minor, receipt.total.currency, date.minusDays(2).toString(), date.plusDays(2).toString())
        return ReceiptTransactionMatcher.rank(receipt, rows.map { it.toDomain() })
    }

    suspend fun saveDistributed(receipt: Receipt, accountId: Long, categoryIds: Map<ReceiptCategory, Long?>): Long {
        val fiscalKey = fiscalKey(receipt.qr)
        require(dao.findReceipt(fiscalKey) == null) { "Этот чек уже сохранён в FingenNext" }
        val receiptEntity = ReceiptEntity(fiscalKey, receipt.qr.raw, receipt.merchantName, receipt.merchantInn, receipt.dateTime?.toString(), receipt.total.minor, receipt.total.currency)
        val itemEntities = receipt.items.mapIndexed { index, item ->
            ReceiptItemEntity(0L, 0L, index, item.name, item.quantity.stripTrailingZeros().toPlainString(), item.unitPrice.minor, item.total.minor, item.total.currency, item.suggestedCategory.name)
        }
        val date = (receipt.dateTime?.toLocalDate() ?: LocalDate.now()).toString()
        val operations = receipt.items.filter { it.total.minor != 0L }.groupBy { it.suggestedCategory }.map { (category, items) ->
            val total = items.fold(0L) { acc, item -> Math.addExact(acc, item.total.minor) }
            ReceiptOperationWrite(
                TransactionEntity(0L, TransactionType.Expense.name, total, receipt.total.currency, date, accountId, null, categoryIds[category], "Чек" + (receipt.merchantName?.let { " — " + it } ?: "") + ": " + category.title),
                accountId, -total
            )
        }
        require(operations.sumOf { it.transaction.amountMinor } == receipt.total.minor) { "Сумма позиций чека не совпадает с итогом" }
        return dao.saveReceiptAndExpenses(receiptEntity, itemEntities, operations)
    }

    suspend fun linkExistingTransaction(receipt: Receipt, transactionId: Long): Long {
        val fiscalKey = fiscalKey(receipt.qr)
        val existing = dao.findReceipt(fiscalKey)
        val receiptId = existing?.id ?: dao.insertReceipt(
            ReceiptEntity(fiscalKey, receipt.qr.raw, receipt.merchantName, receipt.merchantInn, receipt.dateTime?.toString(), receipt.total.minor, receipt.total.currency)
        ).also { id ->
            require(id > 0L) { "Не удалось сохранить чек" }
            dao.insertItems(receipt.items.mapIndexed { index, item ->
                ReceiptItemEntity(id, id, index, item.name, item.quantity.stripTrailingZeros().toPlainString(), item.unitPrice.minor, item.total.minor, item.total.currency, item.suggestedCategory.name)
            })
        }
        dao.linkTransaction(ReceiptTransactionLinkEntity(receiptId, transactionId))
        return receiptId
    }

    private fun fiscalKey(qr: ReceiptQrData): String =
        listOf(qr.fiscalDriveNumber, qr.fiscalDocumentNumber, qr.fiscalSign).takeIf { it.all { value -> !value.isNullOrBlank() } }?.joinToString(":") ?: sha256(qr.raw)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun TransactionEntity.toDomain() = Transaction(id, TransactionType.valueOf(type), Money(amountMinor, currency), LocalDate.parse(date), fromAccountId, toAccountId, categoryId, note)
}
