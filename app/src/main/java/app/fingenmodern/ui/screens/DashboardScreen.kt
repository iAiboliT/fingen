package app.fingenmodern.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
@Composable
fun DashboardScreen(nav:NavController,vm:DashboardViewModel=hiltViewModel()){
    val s by vm.summary.collectAsStateWithLifecycle()
    FingenScaffold(nav,AppDestination.Dashboard,floatingActionButton={FloatingActionButton({nav.navigate(AppDestination.AddOperation.route)}){Text("+")}}){p->
        Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("FingenNext",style=MaterialTheme.typography.headlineMedium)
            MetricCard("Доступно потратить",s.availableCash,"Реальные деньги на ликвидных счетах")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                MetricCard("Чистый капитал",s.netWorth,"Активы − обязательства",Modifier.weight(1f))
                MetricCard("Обязательства",s.liabilities,"Кредиты и долги",Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                AssistChip(onClick={nav.navigate(AppDestination.Debts.route)},label={Text("Мне должны " + s.debtReceivable.format())})
                AssistChip(onClick={nav.navigate(AppDestination.Debts.route)},label={Text("Я должен " + s.debtPayable.format())})
            }
            TextCard("Как читать баланс","Кредит не является доходом. Долг, который вам должны, является активом, но не доступными деньгами. Переводы между своими счетами не меняют капитал.")
        }
    }
}
