package app.fingenmodern.domain

data class Category(
    val id: Long,
    val key: String,
    val name: String,
    val parentId: Long? = null,
    val archived: Boolean = false
)
