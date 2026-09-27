package app.fingenmodern.domain

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class ReceiptTransactionMatcherTest {
    @Test fun ranksSameAmountAndDateAsCandidate(){
        val receipt=Receipt("Пятёрочка",null,LocalDateTime.of(2026,9,27,18,30),Money(125_050L,"RUB"),emptyList(),ReceiptQrData("x",null,null,null,null,null,null))
        val transaction=Transaction(42,TransactionType.Expense,Money(125_050L,"RUB"),LocalDate.of(2026,9,27),note="Пятёрочка")
        val result=ReceiptTransactionMatcher.rank(receipt,listOf(transaction))
        assertEquals(1,result.size); assertEquals(42L,result.first().transactionId); assertTrue(result.first().score>=95)
    }
}
