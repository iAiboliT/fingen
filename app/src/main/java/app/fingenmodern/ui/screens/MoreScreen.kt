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
    FingenScaffold(nav,AppDestination.More){p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Еще",style=MaterialTheme.typography.headlineSmall)
        Button(Modifier.fillMaxWidth(),{nav.navigate(AppDestination.CreditCards.route)}){Text("Кредитные карты")}
        OutlinedButton(Modifier.fillMaxWidth(),{nav.navigate(AppDestination.Reports.route)}){Text("Отчеты")}
        OutlinedButton(Modifier.fillMaxWidth(),{nav.navigate(AppDestination.Settings.route)}){Text("Настройки")}
    }}
}
