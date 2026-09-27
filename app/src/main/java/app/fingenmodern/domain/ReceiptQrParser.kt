package app.fingenmodern.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ReceiptQrParser {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")

    fun parse(raw: String): ReceiptQrData? {
        val params = raw.trim()
            .removePrefix("?")
            .split("&")
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
            }
            .toMap()

        val time = params["t"]?.let {
            runCatching { LocalDateTime.parse(it, dateFormatter) }.getOrNull()
        }
        val total = params["s"]?.replace(',', '.')?.let {
            runCatching {
                Money(
                    BigDecimal(it).movePointRight(2).longValueExact(),
                    "RUB"
                )
            }.getOrNull()
        }

        val fn = params["fn"]?.takeIf { it.isNotBlank() }
        val fd = params["i"]?.takeIf { it.isNotBlank() }
        val fp = params["fp"]?.takeIf { it.isNotBlank() }
        if (time == null && total == null && fn == null && fd == null && fp == null) return null

        return ReceiptQrData(
            raw = raw,
            dateTime = time,
            total = total,
            fiscalDriveNumber = fn,
            fiscalDocumentNumber = fd,
            fiscalSign = fp,
            operationType = params["n"]?.toIntOrNull()
        )
    }
}
