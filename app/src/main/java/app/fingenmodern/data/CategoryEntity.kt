package app.fingenmodern.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["key"], unique = true), Index("parentId")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val name: String,
    val parentId: Long? = null,
    val archived: Boolean = false
)
