package app.fingenmodern.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.fingenmodern.ui.*

@Composable
fun FingenScaffold(
    navController: NavController,
    selected: AppDestination,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = floatingActionButton,
        bottomBar = {
            NavigationBar {
                mainDestinations.forEach { d ->
                    NavigationBarItem(
                        selected = selected.route == d.route,
                        onClick = {
                            if (selected.route != d.route) navController.navigate(d.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(AppDestination.Dashboard.route) { saveState = true }
                            }
                        },
                        icon = { Icon(iconFor(d), d.title) },
                        label = { Text(d.title, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        alwaysShowLabel = true
                    )
                }
            }
        },
        content = content
    )
}

private fun iconFor(d: AppDestination) = when(d) {
    AppDestination.Dashboard -> Icons.Default.Home
    AppDestination.Transactions -> Icons.Default.Payments
    AppDestination.Accounts -> Icons.Default.AccountBalanceWallet
    AppDestination.Debts -> Icons.Default.AccountCircle
    else -> Icons.Default.MoreHoriz
}
