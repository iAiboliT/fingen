package app.fingenmodern.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*

@Composable
fun TransactionsScreen(nav: NavController) {
    FingenScaffold(nav, AppDestination.Transactions, floatingActionButton = {
        FloatingActionButton({ nav.navigate(AppDestination.AddOperation.route) }) { Text("+") }
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Операции", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { nav.navigate(AppDestination.ReceiptScan.route) }, modifier = Modifier.fillMaxWidth()) {
                Text("Сканировать чек")
            }
            TextCard("Лента", "Операции записываются в Room и формируют ledger. Баланс счета не редактируется вручную.")
            TextCard("Чек → расход", "QR-код позволяет получить состав чека. FingenNext группирует позиции по категориям и создаёт отдельные расходы после вашего подтверждения.")
        }
    }
}
