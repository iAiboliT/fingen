package app.fingenmodern.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class ReceiptQrData(
    val raw: String,
    val dateTime: LocalDateTime?,
    val total: Money?,
    val fiscalDriveNumber: String?,
    val fiscalDocumentNumber: String?,
    val fiscalSign: String?,
    val operationType: Int?
)

data class ReceiptItem(
    val name: String,
    val quantity: BigDecimal,
    val unitPrice: Money,
    val total: Money,
    val suggestedCategory: ReceiptCategory
)

data class Receipt(
    val merchantName: String?,
    val merchantInn: String?,
    val dateTime: LocalDateTime?,
    val total: Money,
    val items: List<ReceiptItem>,
    val qr: ReceiptQrData
)

enum class ReceiptCategory(val title: String) {
    Groceries("Продукты"),
    Restaurants("Кафе и рестораны"),
    Transport("Транспорт"),
    Health("Аптека и здоровье"),
    Home("Дом"),
    Clothing("Одежда"),
    Electronics("Электроника"),
    Services("Услуги"),
    Other("Другое")
}
