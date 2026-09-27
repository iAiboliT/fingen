package app.fingenmodern.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
@Composable fun ReportsScreen(nav:NavController){
    FingenScaffold(nav,AppDestination.More){p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Отчеты",style=MaterialTheme.typography.headlineSmall)
        TextCard("Финансовый отчет","Доходы, расходы, структура категорий, обязательства и динамика капитала должны строиться из ledger, а не из ручных итогов.")
    }}
}
