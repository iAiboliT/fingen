package app.fingenmodern.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.fingenmodern.data.FinanceRepository
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(repo: FinanceRepository) : ViewModel() {
    val pendingImports = repo.observePendingImportCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@Composable
fun TransactionsScreen(
    nav: NavController,
    vm: TransactionsViewModel = hiltViewModel()
) {
    val pendingImports = vm.pendingImports.collectAsState().value
    FingenScaffold(nav, AppDestination.Transactions, floatingActionButton = {
        FloatingActionButton({ nav.navigate(AppDestination.AddOperation.route) }) { Text("+") }
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Операции", style = MaterialTheme.typography.headlineSmall)
            if (pendingImports > 0) {
                ElevatedCard(onClick = { nav.navigate(AppDestination.ImportInbox.route) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Есть операции на проверке", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Найдено: " + pendingImports + ". Банк прислал данные, но FingenNext не проведет их без вашего подтверждения.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Button(onClick = { nav.navigate(AppDestination.ReceiptScan.route) }, modifier = Modifier.fillMaxWidth()) {
                Text("Сканировать чек")
            }
            TextCard("Автоучет", "Банковские уведомления и чеки сначала попадают на проверку. После подтверждения они становятся обычными операциями Financial Engine.")
            TextCard("Чек → расход", "QR-код позволяет получить состав чека. FingenNext группирует позиции по категориям и создаёт отдельные расходы после вашего подтверждения.")
        }
    }
}
