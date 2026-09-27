package app.fingenmodern.data

import android.util.Base64
import app.fingenmodern.domain.GmailReceiptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailReceiptImporter @Inject constructor(
    private val repository: FinanceRepository
) {
    suspend fun sync(accessToken: String): Int = withContext(Dispatchers.IO) {
        val ids = listMessageIds(accessToken)
        var imported = 0
        ids.forEach { id ->
            val message = getMessage(accessToken, id) ?: return@forEach
            val payload = message.optJSONObject("payload") ?: return@forEach
            val headers = payload.optJSONArray("headers")
            var subject: String? = null
            var sender: String? = null
            if (headers != null) {
                for (i in 0 until headers.length()) {
                    val header = headers.getJSONObject(i)
                    when (header.optString("name").lowercase()) {
                        "subject" -> subject = header.optString("value")
                        "from" -> sender = header.optString("value")
                    }
                }
            }
            val body = extractText(payload)
            val candidate = GmailReceiptParser.parse(
                messageId = id,
                subject = subject,
                sender = sender,
                body = body,
                internalDateMillis = message.optString("internalDate").toLongOrNull() ?: return@forEach
            ) ?: return@forEach
            repository.addImportCandidate(candidate)
            imported++
        }
        imported
    }

    private fun listMessageIds(token: String): List<String> {
        val query = URLEncoder.encode(
            """newer_than:90d {чек receipt "кассовый чек" "электронный чек"}""",
            Charsets.UTF_8.name()
        )
        val json = request(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=50&q=" + query,
            token
        ) ?: return emptyList()
        val array = json.optJSONArray("messages") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) add(array.getJSONObject(i).optString("id"))
        }
    }

    private fun getMessage(token: String, id: String): JSONObject? =
        request(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + id + "?format=full",
            token
        )

    private fun request(url: String, token: String): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer " + token)
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        return try {
            if (connection.responseCode !in 200..299) null
            else JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun extractText(part: JSONObject): String {
        val direct = part.optJSONObject("body")?.optString("data").orEmpty()
        val mime = part.optString("mimeType")
        val directText = decodeBase64Url(direct)
        if (directText.isNotBlank() && mime.startsWith("text/")) {
            return if (mime == "text/html") stripHtml(directText) else directText
        }
        val parts = part.optJSONArray("parts") ?: return directText
        return buildString {
            for (i in 0 until parts.length()) {
                val child = extractText(parts.getJSONObject(i))
                if (child.isNotBlank()) {
                    append(child)
                    append('\n')
                }
            }
        }
    }

    private fun decodeBase64Url(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            String(Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun stripHtml(value: String): String =
        value.replace(Regex("""(?is)<br\s*/?>"""), "\n")
            .replace(Regex("""(?is)</p>"""), "\n")
            .replace(Regex("""(?is)<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
