package app.fingenmodern.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
@Composable fun TransactionsScreen(nav:NavController){
    FingenScaffold(nav,AppDestination.Transactions,floatingActionButton={FloatingActionButton({nav.navigate(AppDestination.AddOperation.route)}){Text("+")}}){p->
        Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Операции",style=MaterialTheme.typography.headlineSmall)
            TextCard("Лента","Операции записываются в Room и формируют ledger. Баланс счета не редактируется вручную.")
            TextCard("Следующий слой","Полная история с paging, фильтрами, категориями и редактированием операций подключается к тому же источнику данных.")
        }
    }
}
