package app.fingenmodern.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.fingenmodern.data.FinanceRepository
import app.fingenmodern.domain.DebtKind
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
@HiltViewModel class DebtsViewModel @Inject constructor(repo:FinanceRepository):ViewModel(){
    val debts=repo.observeDebts().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
}
@Composable fun DebtsScreen(nav:NavController,vm:DebtsViewModel=hiltViewModel()){
    val debts by vm.debts.collectAsStateWithLifecycle()
    FingenScaffold(nav,AppDestination.Debts){p->LazyColumn(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Долги")}
        item{TextCard("Без бухгалтерии","«Мне должны» — актив, «я должен» — обязательство. Они не маскируются под доход или расход.")}
        items(debts,key={it.id}){d->
            val due = d.dueDate?.let { "Срок: " + it } ?: "Срок не задан"
            TextCard(d.title,label(d.kind) + "\n" + d.counterparty + "\nОстаток: " + d.remaining.format() + "\n" + due)
        }
    }}
}
private fun label(k:DebtKind)=when(k){DebtKind.TheyOweMe->"Мне должны";DebtKind.IOwePerson->"Я должен";DebtKind.BankLoan->"Банковский кредит";DebtKind.CreditCard->"Кредитная карта"}
