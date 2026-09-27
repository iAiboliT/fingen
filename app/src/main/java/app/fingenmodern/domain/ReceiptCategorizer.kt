package app.fingenmodern.domain

object ReceiptCategorizer {
    fun suggest(name: String): ReceiptCategory {
        val value = name.lowercase()
        return when {
            listOf("молоко", "хлеб", "сыр", "мясо", "рыба", "овощ", "фрукт", "круп", "макарон", "продукт", "колбас", "напит", "вода", "сок").any(value::contains) ->
                ReceiptCategory.Groceries
            listOf("кафе", "ресторан", "пицц", "бургер", "суши", "кофе", "столов", "бар ").any(value::contains) ->
                ReceiptCategory.Restaurants
            listOf("такси", "метро", "автобус", "билет", "жд ", "ржд", "заправ", "бензин", "топлив").any(value::contains) ->
                ReceiptCategory.Transport
            listOf("аптек", "лекар", "таблет", "витамин", "медицин", "стомат").any(value::contains) ->
                ReceiptCategory.Health
            listOf("ламп", "мебел", "посуда", "моющ", "уборк", "хозтовар", "домаш").any(value::contains) ->
                ReceiptCategory.Home
            listOf("обув", "куртк", "футбол", "джинс", "плать", "одежд").any(value::contains) ->
                ReceiptCategory.Clothing
            listOf("телефон", "смартфон", "ноутбук", "монитор", "кабель", "наушник", "электроник").any(value::contains) ->
                ReceiptCategory.Electronics
            listOf("ремонт", "доставка", "парикмах", "мойк", "сервис", "услуг").any(value::contains) ->
                ReceiptCategory.Services
            else -> ReceiptCategory.Other
        }
    }
}
