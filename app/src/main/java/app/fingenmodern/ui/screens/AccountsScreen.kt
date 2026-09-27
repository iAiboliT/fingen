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
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
@HiltViewModel class AccountsViewModel @Inject constructor(repo:FinanceRepository):ViewModel(){
    val accounts=repo.observeAccounts().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
}
@Composable fun AccountsScreen(nav:NavController,vm:AccountsViewModel=hiltViewModel()){
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    FingenScaffold(nav,AppDestination.Accounts){p->LazyColumn(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Счета")}
        items(accounts,key={it.id}){a->MetricCard(a.name,a.balance,if(a.type.name in listOf("CreditCard","Loan"))"Обязательство" else "Баланс")}
    }}
}
