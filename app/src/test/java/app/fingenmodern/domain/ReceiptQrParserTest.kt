package app.fingenmodern.domain

import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ReceiptQrParserTest {
    @Test
    fun parsesStandardRussianFiscalQr() {
        val qr = "t=20260927T2045&s=1250.50&fn=9289000100054082&i=16112&fp=399448105&n=1"
        val result = ReceiptQrParser.parse(qr)

        assertNotNull(result)
        assertEquals("9289000100054082", result.fiscalDriveNumber)
        assertEquals("16112", result.fiscalDocumentNumber)
        assertEquals("399448105", result.fiscalSign)
        assertEquals(125050L, result.total?.minor)
        assertEquals(1, result.operationType)
        assertEquals(2026, result.dateTime?.year)
        assertEquals(9, result.dateTime?.monthValue)
        assertEquals(27, result.dateTime?.dayOfMonth)
    }

    @Test
    fun categorizerRecognizesGroceries() {
        assertEquals(ReceiptCategory.Groceries, ReceiptCategorizer.suggest("Молоко 2.5%"))
        assertEquals(ReceiptCategory.Health, ReceiptCategorizer.suggest("Парацетамол"))
        assertEquals(ReceiptCategory.Transport, ReceiptCategorizer.suggest("Бензин АИ-95"))
    }
}
