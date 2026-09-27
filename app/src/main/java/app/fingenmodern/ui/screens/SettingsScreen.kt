package app.fingenmodern.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
@Composable fun SettingsScreen(nav:NavController){
    FingenScaffold(nav,AppDestination.More){p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Настройки",style=MaterialTheme.typography.headlineSmall)
        TextCard("Данные","База хранится локально. Автоматический backup базы отключен, чтобы финансовые данные не уходили в системное облачное резервное копирование без явного сценария приложения.")
        TextCard("Шаблоны кредиток","Условия можно версионировать и менять без пересчета прошлой истории.")
        TextCard("Версия","FingenNext 0.1.0 — production foundation")
    }}
}
