package app.fingenmodern.domain

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test

class GmailReceiptParserTest {
    @Test
    fun parsesElectronicReceiptTotal() {
        val result = GmailReceiptParser.parse(
            messageId = "abc",
            subject = "Электронный чек Пятёрочка",
            sender = "Пятёрочка <receipt@example.ru>",
            body = "Молоко 129 ₽\nХлеб 79 ₽\nИтого: 208,00 ₽",
            internalDateMillis = 0L
        )

        assertNotNull(result)
        assertEquals(ImportDirection.Expense, result.direction)
        assertEquals(20_800L, result.amount.minor)
        assertEquals("gmail:abc", result.sourceKey)
    }
}
