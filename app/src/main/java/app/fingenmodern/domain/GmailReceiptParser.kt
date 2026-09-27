package app.fingenmodern.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

object GmailReceiptParser {
    private val amountRegex = Regex(
        """(?<!\d)([\d\s]+(?:[.,]\d{1,2})?)\s*(?:₽|руб(?:\.?|лей)?|RUB)\b?""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        messageId: String,
        subject: String?,
        sender: String?,
        body: String,
        internalDateMillis: Long
    ): NotificationCandidate? {
        val combined = listOfNotNull(subject, sender, body).joinToString("\n")
        val lower = combined.lowercase(Locale.ROOT)
        if (!listOf("чек", "кассов", "покупк", "receipt", "оплат").any(lower::contains)) return null

        val preferredLines = body.lines().filter {
            val line = it.lowercase(Locale.ROOT)
            listOf("итого", "к оплате", "сумма", "total", "оплачено").any(line::contains)
        }
        val amount = preferredLines.asSequence()
            .flatMap { amountRegex.findAll(it).asSequence() }
            .mapNotNull { parseMoney(it.groupValues[1]) }
            .firstOrNull()
            ?: amountRegex.findAll(body)
                .mapNotNull { parseMoney(it.groupValues[1]) }
                .maxByOrNull { it.minor }
            ?: return null

        val merchant = extractMerchant(sender, subject)
        val title = merchant ?: subject
        val text = listOfNotNull(title, subject, body.take(4000)).joinToString("\n").trim()

        return NotificationCandidate(
            sourceKey = "gmail:" + messageId,
            source = "GMAIL_RECEIPT",
            sourcePackage = "com.google.android.gm",
            title = title,
            text = text,
            amount = amount,
            direction = ImportDirection.Expense,
            occurredAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(internalDateMillis),
                ZoneId.systemDefault()
            )
        )
    }

    private fun parseMoney(value: String): Money? = runCatching {
        val decimal = BigDecimal(value.replace(" ", "").replace(',', '.'))
        Money(decimal.movePointRight(2).longValueExact(), "RUB")
    }.getOrNull()?.takeUnless { it.isZero() }

    private fun extractMerchant(sender: String?, subject: String?): String? {
        val fromName = sender?.substringBefore('<')?.trim()?.trim('"').orEmpty()
        if (fromName.isNotBlank() && !fromName.contains('@')) return fromName
        return subject?.replace(Regex("(?i)(электронный\s+)?кассовый\s+чек"), "")
            ?.replace(Regex("(?i)чек"), "")
            ?.trim(' ', '-', '—', ':')
            ?.takeIf { it.isNotBlank() }
    }
}
