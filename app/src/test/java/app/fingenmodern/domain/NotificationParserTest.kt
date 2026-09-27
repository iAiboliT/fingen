package app.fingenmodern.domain

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test

class NotificationParserTest {
    @Test
    fun parsesIncomingBankNotification() {
        val result = NotificationParser.parse(
            packageName = "ru.sberbankmobile",
            title = "СберБанк",
            text = "Зачисление 150 000,00 ₽ Перевод от Иван",
            postedAtMillis = 0L,
            sourceKey = "test-1"
        )

        assertNotNull(result)
        assertEquals(ImportDirection.Income, result.direction)
        assertEquals(15_000_000L, result.amount.minor)
    }

    @Test
    fun parsesExpenseBankNotification() {
        val result = NotificationParser.parse(
            packageName = "com.tinkoff.mobile",
            title = "Т-Банк",
            text = "Покупка 1 250,50 ₽ Пятёрочка",
            postedAtMillis = 0L,
            sourceKey = "test-2"
        )

        assertNotNull(result)
        assertEquals(ImportDirection.Expense, result.direction)
        assertEquals(125_050L, result.amount.minor)
    }

    @Test
    fun debtMatchHasPriorityOverGenericIncomingReason() {
        assertEquals(
            IncomingMoneyReason.DebtRepayment,
            IncomingMoneyClassifier.suggestReason("Зачисление 100 000 ₽", hasExactDebtMatch = true)
        )
        assertEquals(
            IncomingMoneyReason.Salary,
            IncomingMoneyClassifier.suggestReason("Зачисление зарплаты 100 000 ₽", hasExactDebtMatch = false)
        )
    }
}
