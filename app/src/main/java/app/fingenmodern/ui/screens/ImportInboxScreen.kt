package app.fingenmodern.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.fingenmodern.ui.AppDestination
import app.fingenmodern.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.format.DateTimeFormatter

@HiltViewModel
class ImportInboxViewModel @Inject constructor(
    private val repo: FinanceRepository
) : ViewModel() {
    val candidates = repo.observeImportCandidates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts = repo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val debts = repo.observeDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun accept(
        candidate: ImportCandidate,
        accountId: Long,
        reason: IncomingMoneyReason?,
        categoryId: Long?,
        debtId: Long?
    ) = viewModelScope.launch {
        repo.acceptImportCandidate(candidate.id, accountId, reason, categoryId, debtId)
    }

    fun ignore(candidate: ImportCandidate) = viewModelScope.launch {
        repo.ignoreImportCandidate(candidate.id)
    }
}

@Composable
fun ImportInboxScreen(
    nav: NavController,
    vm: ImportInboxViewModel = hiltViewModel()
) {
    val candidates by vm.candidates.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val debts by vm.debts.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    FingenScaffold(nav, AppDestination.Transactions) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Операции на проверке", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "FingenNext не проводит импорт автоматически. Для каждого поступления можно выбрать: зарплата, возврат долга, оплата услуг, продажа, возврат или другое.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (candidates.isEmpty()) {
                item { TextCard("Пока пусто", "Когда банк или Mir Pay пришлет подходящее уведомление, оно появится здесь.") }
            }
            items(candidates, key = { it.id }) { candidate ->
                ImportCandidateCard(candidate, accounts, debts, categories, vm)
            }
        }
    }
}

@Composable
private fun ImportCandidateCard(
    candidate: ImportCandidate,
    accounts: List<Account>,
    debts: List<DebtPosition>,
    categories: List<Category>,
    vm: ImportInboxViewModel
) {
    var accountId by remember(candidate.id, accounts) { mutableStateOf(accounts.firstOrNull()?.id) }
    var reason by remember(candidate.id) {
        mutableStateOf(candidate.suggestedReason ?: if (candidate.direction == ImportDirection.Income) IncomingMoneyReason.Other else null)
    }
    var categoryId by remember(candidate.id) { mutableStateOf<Long?>(null) }
    var debtId by remember(candidate.id) { mutableStateOf(candidate.suggestedDebtId) }

    val matchingDebts = debts.filter {
        it.kind == DebtKind.TheyOweMe &&
            it.remaining.currency == candidate.amount.currency &&
            it.remaining.minor >= candidate.amount.minor
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (candidate.direction == ImportDirection.Income) "Поступление" else "Расход",
                style = MaterialTheme.typography.titleMedium
            )
            Text(candidate.amount.format(), style = MaterialTheme.typography.headlineSmall)
            Text(candidate.text, style = MaterialTheme.typography.bodyMedium)
            Text(candidate.occurredAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))

            Text("Счёт")
            accounts.forEach { account ->
                FilterChip(
                    selected = accountId == account.id,
                    onClick = { accountId = account.id },
                    label = { Text(account.name) }
                )
            }

            if (candidate.direction == ImportDirection.Income) {
                Text("Что это за поступление?", style = MaterialTheme.typography.titleSmall)
                IncomingMoneyReason.entries.forEach { item ->
                    FilterChip(
                        selected = reason == item,
                        onClick = { reason = item },
                        label = { Text(item.title) }
                    )
                }

                if (matchingDebts.isNotEmpty()) {
                    Text("Есть долги, которые могут совпадать с этим возвратом")
                    matchingDebts.forEach { debt ->
                        FilterChip(
                            selected = debtId == debt.id,
                            onClick = { debtId = debt.id; reason = IncomingMoneyReason.DebtRepayment },
                            label = { Text(debt.counterparty + " · " + debt.remaining.format()) }
                        )
                    }
                }

                if (reason == IncomingMoneyReason.DebtRepayment && debtId == null) {
                    Text("Выберите долг, который погашается этим поступлением.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text("Категория расхода", style = MaterialTheme.typography.titleSmall)
                categories.forEach { category ->
                    FilterChip(
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id },
                        label = { Text(category.name) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = accountId != null &&
                        (candidate.direction == ImportDirection.Income || categoryId != null) &&
                        (candidate.direction != ImportDirection.Income ||
                            (reason != IncomingMoneyReason.DebtRepayment || debtId != null)),
                    onClick = {
                        vm.accept(candidate, accountId!!, reason, categoryId, debtId)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Провести") }
                OutlinedButton(
                    onClick = { vm.ignore(candidate) },
                    modifier = Modifier.weight(1f)
                ) { Text("Не учитывать") }
            }
        }
    }
}
