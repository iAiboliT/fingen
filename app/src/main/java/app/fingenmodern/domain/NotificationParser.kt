package app.fingenmodern.domain

import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

object NotificationParser {
    private val amountRegex = Regex(
        """(?<!\d)([+−-]?\s*[\d\s]+(?:[.,]\d{1,2})?)\s*(?:₽|руб(?:\.?|лей)?|RUB)\b?""",
        RegexOption.IGNORE_CASE
    )

    private val incomeWords = listOf(
        "зачислен", "зачисление", "поступил", "поступление", "получен перевод",
        "перевод вам", "пополнение", "возврат", "возвращено"
    )
    private val expenseWords = listOf(
        "покупка", "оплата", "списание", "перевод", "платеж", "платёж",
        "снятие", "перечисление"
    )
    private val financialPackageWords = listOf(
        "sber", "tinkoff", "tbank", "vtb", "alfa", "raiffeisen", "gazprombank",
        "psb", "promsvyaz", "sovcom", "ozonbank", "mtsbank", "mirpay", "mir.pay",
        "yoomoney", "qiwi"
    )

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        postedAtMillis: Long,
        sourceKey: String
    ): NotificationCandidate? {
        val body = listOfNotNull(title, text).joinToString(" ").trim()
        if (body.isBlank() || !isFinancial(packageName, body)) return null

        val match = amountRegex.find(body) ?: return null
        val normalizedAmount = match.groupValues[1]
            .replace(" ", "")
            .replace("−", "-")
            .replace(',', '.')
            .trimStart('+')
        val amount = runCatching {
            val decimal = BigDecimal(normalizedAmount).abs()
            Money(decimal.movePointRight(2).longValueExact(), "RUB")
        }.getOrNull() ?: return null
        if (amount.isZero()) return null

        val lower = body.lowercase(Locale.ROOT)
        val direction = when {
            incomeWords.any(lower::contains) -> ImportDirection.Income
            expenseWords.any(lower::contains) -> ImportDirection.Expense
            match.groupValues[1].trim().startsWith("-") || match.groupValues[1].trim().startsWith("−") ->
                ImportDirection.Expense
            match.groupValues[1].trim().startsWith("+") -> ImportDirection.Income
            else -> return null
        }

        return NotificationCandidate(
            sourceKey = sourceKey,
            sourcePackage = packageName,
            title = title,
            text = body,
            amount = amount,
            direction = direction,
            occurredAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(postedAtMillis),
                ZoneId.systemDefault()
            )
        )
    }

    private fun isFinancial(packageName: String, body: String): Boolean {
        val packageLooksFinancial = financialPackageWords.any { packageName.lowercase(Locale.ROOT).contains(it) }
        val textLooksFinancial = listOf(
            "банк", "карта", "₽", "руб", "покупка", "оплата", "зачисление",
            "поступление", "списание", "перевод", "платёж", "платеж"
        ).any(body.lowercase(Locale.ROOT)::contains)
        return packageLooksFinancial || textLooksFinancial
    }

    fun stableHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
