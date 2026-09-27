package app.fingenmodern.ui.screens
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fingenmodern.data.FinanceRepository
import app.fingenmodern.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
@HiltViewModel
class DashboardViewModel @Inject constructor(repo:FinanceRepository):ViewModel(){
    val summary=combine(repo.observeAccounts(),repo.observeDebts()){a,d->BalanceEngine().summarize(a,d)}
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),BalanceSummary(Money.zero(),Money.zero(),Money.zero(),Money.zero(),Money.zero(),Money.zero(),Money.zero()))
}
