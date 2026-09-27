package app.fingenmodern.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "import_candidates",
    indices = [
        Index(value = ["sourceKey"], unique = true),
        Index("status"),
        Index("occurredAt")
    ]
)
data class ImportCandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceKey: String,
    val source: String,
    val sourcePackage: String?,
    val title: String?,
    val text: String,
    val amountMinor: Long,
    val currency: String,
    val direction: String,
    val occurredAt: String,
    val suggestedReason: String?,
    val suggestedDebtId: Long?,
    val status: String = "Pending"
)
