package app.fingenmodern.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.FingenScaffold
@Composable fun MoreScreen(nav:NavController){
    FingenScaffold(nav,AppDestination.More){p->
        Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Еще",style=MaterialTheme.typography.headlineSmall)
            Button(onClick={nav.navigate(AppDestination.CreditCards.route)},modifier=Modifier.fillMaxWidth()){Text("Кредитные карты")}
            OutlinedButton(onClick={nav.navigate(AppDestination.Reports.route)},modifier=Modifier.fillMaxWidth()){Text("Отчеты")}
            OutlinedButton(onClick={nav.navigate(AppDestination.Settings.route)},modifier=Modifier.fillMaxWidth()){Text("Настройки")}
        }
    }
}
