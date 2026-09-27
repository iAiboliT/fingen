package app.fingenmodern.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.fingenmodern.data.FinanceRepository
import app.fingenmodern.domain.*
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class Kind(val title: String) { Expense("Расход"), Income("Доход") }

@HiltViewModel
class AddOperationViewModel @Inject constructor(private val repo: FinanceRepository) : ViewModel() {
    val accounts = repo.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(kind: Kind, accountId: Long, minor: Long, note: String, onDone: () -> Unit) = viewModelScope.launch {
        val d = if (kind == Kind.Expense) {
            OperationDraft.Expense(accountId, Money(minor), LocalDate.now(), null, note.ifBlank { null })
        } else {
            OperationDraft.Income(accountId, Money(minor), LocalDate.now(), null, note.ifBlank { null })
        }
        repo.addOperation(d)
        onDone()
    }
}

@Composable
fun AddOperationScreen(nav: NavController, vm: AddOperationViewModel = hiltViewModel()) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(Kind.Expense) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var accountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id) }
    var error by remember { mutableStateOf<String?>(null) }

    FingenScaffold(nav, AppDestination.Transactions) {
        Column(Modifier.padding(it).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Новая операция", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { nav.navigate(AppDestination.ReceiptScan.route) }, modifier = Modifier.fillMaxWidth()) {
                Text("Сканировать чек и распределить расход")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kind.entries.forEach { k -> FilterChip(kind == k, { kind = k }, { Text(k.title) }) }
            }
            Text("Счет")
            accounts.forEach { a -> FilterChip(accountId == a.id, { accountId = a.id }, { Text(a.name) }) }
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Сумма, ₽") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    try {
                        val minor = amount.replace(',', '.').toBigDecimal().movePointRight(2).longValueExact()
                        if (minor <= 0 || accountId == null) error = "Введите сумму и счет"
                        else vm.save(kind, accountId!!, minor, note) { nav.popBackStack() }
                    } catch (_: Exception) {
                        error = "Введите корректную сумму (не более 2 знаков после запятой)"
                    }
                },
                enabled = accountId != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить") }
            TextCard("Подсказка", "Для долгов, переводов и кредиток будут отдельные сценарии. Обычный расход не должен заставлять пользователя выбирать бухгалтерский тип.")
        }
    }
}
