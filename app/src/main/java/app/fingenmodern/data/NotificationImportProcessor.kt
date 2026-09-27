package app.fingenmodern.data

import app.fingenmodern.domain.NotificationParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationImportProcessor @Inject constructor(
    private val repository: FinanceRepository
) {
    suspend fun process(
        packageName: String,
        title: String?,
        text: String?,
        postedAtMillis: Long,
        notificationKey: String
    ) {
        val parsed = NotificationParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            postedAtMillis = postedAtMillis,
            sourceKey = "notification:" + NotificationParser.stableHash(notificationKey)
        ) ?: return
        repository.addImportCandidate(parsed)
    }
}
