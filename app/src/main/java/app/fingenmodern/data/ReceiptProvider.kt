package app.fingenmodern.data

import android.util.Log
import app.fingenmodern.domain.Money
import app.fingenmodern.domain.Receipt
import app.fingenmodern.domain.ReceiptItem
import app.fingenmodern.domain.ReceiptQrData
import app.fingenmodern.domain.ReceiptCategorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface ReceiptProvider {
    suspend fun getReceipt(qr: ReceiptQrData): Result<Receipt>
}

@Singleton
class ProverkaChekaReceiptProvider @Inject constructor(
    private val settings: ReceiptSettingsStore
) : ReceiptProvider {

    override suspend fun getReceipt(qr: ReceiptQrData): Result<Receipt> = withContext(Dispatchers.IO) {
        runCatching {
            val token = settings.apiToken().orEmpty()
            require(token.isNotBlank()) {
                "Не настроен API-токен сервиса получения состава чека"
            }

            val connection = URL("https://proverkacheka.com/api/v1/check/get")
                .openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")

            val params = linkedMapOf(
                "fn" to qr.fiscalDriveNumber.orEmpty(),
                "fd" to qr.fiscalDocumentNumber.orEmpty(),
                "fp" to qr.fiscalSign.orEmpty(),
                "t" to qr.dateTime?.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")).orEmpty(),
                "n" to (qr.operationType ?: 1).toString(),
                "s" to qr.total?.formatPlain().orEmpty(),
                "qr" to "1",
                "token" to token
            )
            val body = params.entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            require(response.isNotBlank()) { "Сервис не вернул данные чека (HTTP $responseCode)" }

            val root = JSONObject(response)
            require(root.optInt("code") == 1) {
                root.optString("message").ifBlank { "Не удалось получить состав чека" }
            }

            val json = root.getJSONObject("data").getJSONObject("json")
            val totalKopecks = json.optLong("totalSum", qr.total?.minor ?: 0L)
            val itemsJson = json.optJSONArray("items")
            val items = buildList {
                if (itemsJson != null) {
                    for (i in 0 until itemsJson.length()) {
                        val item = itemsJson.getJSONObject(i)
                        val name = item.optString("name").ifBlank { "Товар" }
                        val quantity = item.optString("quantity").toBigDecimalOrNull() ?: BigDecimal.ONE
                        val sum = item.optLong("sum")
                        val price = item.optLong("price", if (quantity.signum() != 0) (sum / quantity.toDouble()).toLong() else sum)
                        add(
                            ReceiptItem(
                                name = name,
                                quantity = quantity,
                                unitPrice = Money(price, "RUB"),
                                total = Money(sum, "RUB"),
                                suggestedCategory = ReceiptCategorizer.suggest(name)
                            )
                        )
                    }
                }
            }

            Receipt(
                merchantName = json.optString("userInn").takeIf { it.isNotBlank() },
                merchantInn = json.optString("userInn").takeIf { it.isNotBlank() },
                dateTime = json.optString("dateTime").takeIf { it.isNotBlank() }?.let {
                    runCatching { java.time.LocalDateTime.parse(it) }.getOrNull()
                } ?: qr.dateTime,
                total = Money(totalKopecks, "RUB"),
                items = items,
                qr = qr
            )
        }.onFailure { Log.w("FingenNext", "Receipt lookup failed", it) }
    }
}

@Singleton
class ReceiptSettingsStore @Inject constructor(
    private val dataStore: androidx.datastore.preferences.core.PreferenceDataStore
) {
    private val tokenKey = androidx.datastore.preferences.core.stringPreferencesKey("receipt_api_token")

    suspend fun apiToken(): String? = dataStore.data.first()[tokenKey]

    suspend fun saveApiToken(value: String) {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { set(tokenKey, value.trim()) }
        }
    }
}

private fun Money.formatPlain(): String =
    java.math.BigDecimal(minor).movePointLeft(2).toPlainString()
