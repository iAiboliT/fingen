package app.fingenmodern.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import app.fingenmodern.ui.screens.*
import app.fingenmodern.ui.theme.FingenTheme

@Composable
fun FingenModernAppRoot() {
    val nav = rememberNavController()
    FingenTheme {
        NavHost(nav, AppDestination.Dashboard.route) {
            composable(AppDestination.Dashboard.route) { DashboardScreen(nav) }
            composable(AppDestination.Transactions.route) { TransactionsScreen(nav) }
            composable(AppDestination.Accounts.route) { AccountsScreen(nav) }
            composable(AppDestination.Debts.route) { DebtsScreen(nav) }
            composable(AppDestination.More.route) { MoreScreen(nav) }
            composable(AppDestination.AddOperation.route) { AddOperationScreen(nav) }
            composable(AppDestination.ReceiptScan.route) { ReceiptScanScreen(nav) }
            composable(AppDestination.CreditCards.route) { CreditCardsScreen(nav) }
            composable(AppDestination.Reports.route) { ReportsScreen(nav) }
            composable(AppDestination.Settings.route) { SettingsScreen(nav) }
        }
    }
}
