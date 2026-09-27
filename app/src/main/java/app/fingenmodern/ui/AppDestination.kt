package app.fingenmodern.ui

sealed class AppDestination(val route: String, val title: String) {
    data object Dashboard : AppDestination("dashboard", "Главная")
    data object Transactions : AppDestination("transactions", "Операции")
    data object Accounts : AppDestination("accounts", "Счета")
    data object Debts : AppDestination("debts", "Долги")
    data object More : AppDestination("more", "Еще")
    data object AddOperation : AppDestination("add_operation", "Новая операция")
    data object ReceiptScan : AppDestination("receipt_scan", "Сканировать чек")
    data object CreditCards : AppDestination("credit_cards", "Кредитки")
    data object Reports : AppDestination("reports", "Отчеты")
    data object Settings : AppDestination("settings", "Настройки")
}

val mainDestinations = listOf(AppDestination.Dashboard, AppDestination.Transactions, AppDestination.Accounts, AppDestination.Debts, AppDestination.More)
