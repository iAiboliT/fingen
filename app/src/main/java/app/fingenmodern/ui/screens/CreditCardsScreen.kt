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
import app.fingenmodern.domain.*
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject
@HiltViewModel class CreditCardsViewModel @Inject constructor(repo:FinanceRepository):ViewModel(){
    val templates=repo.observeCreditCardTemplates().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
}
@Composable fun CreditCardsScreen(nav:NavController,vm:CreditCardsViewModel=hiltViewModel()){
    val templates by vm.templates.collectAsStateWithLifecycle()
    FingenScaffold(nav,AppDestination.More){p->LazyColumn(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Кредитные карты",style=MaterialTheme.typography.headlineSmall)}
        item{TextCard("Важно","Шаблон — версия условий пользователя, а не гарантия банка. История должна ссылаться на конкретную версию.")}
        items(templates,key={it.id}){t->
            val pr=CreditCardInterestEngine().projectStatement(t,listOf(CreditCardOperation(date=LocalDate.now().minusDays(20),amount=Money.rub(42_000),type=CreditOperationType.Purchase)),LocalDate.now(),LocalDate.now().plusDays((t.paymentWindowDays?:t.graceLengthDays).toLong()))
            val title=t.bankName + " · " + t.productName
            val body="Ставка: " + t.annualPurchaseRatePercent + "%\nГрейс: " + t.graceLengthDays + " дней\nМинимальный платеж: " + pr.minimumPayment.format() + "\n" + pr.explanation.joinToString("\n") + "\n" + (pr.warning ?: "")
            TextCard(title,body)
        }
    }}
}
