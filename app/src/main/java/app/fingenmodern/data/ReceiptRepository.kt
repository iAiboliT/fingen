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
        val receiptEntity = ReceiptEntity(fiscalKey = fiscalKey, rawQr = receipt.qr.raw, merchantName = receipt.merchantName, merchantInn = receipt.merchantInn, occurredAt = receipt.dateTime?.toString(), totalMinor = receipt.total.minor, currency = receipt.total.currency)
        val itemEntities = receipt.items.mapIndexed { index, item ->
            ReceiptItemEntity(
                receiptId = 0L,
                position = index,
                name = item.name,
                quantity = item.quantity.stripTrailingZeros().toPlainString(),
                unitPriceMinor = item.unitPrice.minor,
                totalMinor = item.total.minor,
                currency = item.total.currency,
                suggestedCategory = item.suggestedCategory.name
            )
        }
        val date = (receipt.dateTime?.toLocalDate() ?: LocalDate.now()).toString()
        val operations = receipt.items.filter { it.total.minor != 0L }.groupBy { it.suggestedCategory }.map { (category, items) ->
            val total = items.fold(0L) { acc, item -> Math.addExact(acc, item.total.minor) }
            ReceiptOperationWrite(
                TransactionEntity(
                    type = TransactionType.Expense.name,
                    amountMinor = total,
                    currency = receipt.total.currency,
                    date = date,
                    fromAccountId = accountId,
                    toAccountId = null,
                    categoryId = categoryIds[category],
                    note = "Чек" + (receipt.merchantName?.let { " — " + it } ?: "") + ": " + category.title
                ),
                accountId,
                -total
            )
        }
        require(operations.sumOf { it.transaction.amountMinor } == receipt.total.minor) { "Сумма позиций чека не совпадает с итогом" }
        return dao.saveReceiptAndExpenses(receiptEntity, itemEntities, operations)
    }

    suspend fun linkExistingTransaction(receipt: Receipt, transactionId: Long): Long {
        val fiscalKey = fiscalKey(receipt.qr)
        val receiptEntity = ReceiptEntity(
            fiscalKey = fiscalKey,
            rawQr = receipt.qr.raw,
            merchantName = receipt.merchantName,
            merchantInn = receipt.merchantInn,
            occurredAt = receipt.dateTime?.toString(),
            totalMinor = receipt.total.minor,
            currency = receipt.total.currency
        )
        val itemEntities = receipt.items.mapIndexed { index, item ->
            ReceiptItemEntity(
                receiptId = 0L,
                position = index,
                name = item.name,
                quantity = item.quantity.stripTrailingZeros().toPlainString(),
                unitPriceMinor = item.unitPrice.minor,
                totalMinor = item.total.minor,
                currency = item.total.currency,
                suggestedCategory = item.suggestedCategory.name
            )
        }
        return dao.saveReceiptAndLink(receiptEntity, itemEntities, transactionId)
    }

    private fun fiscalKey(qr: ReceiptQrData): String =
        listOf(qr.fiscalDriveNumber, qr.fiscalDocumentNumber, qr.fiscalSign).takeIf { it.all { value -> !value.isNullOrBlank() } }?.joinToString(":") ?: sha256(qr.raw)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        amount = Money(amountMinor, currency),
        date = LocalDate.parse(date),
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        note = note
    )
}
