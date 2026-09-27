package app.fingenmodern.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.fingenmodern.data.ReceiptSettingsStore
import app.fingenmodern.ui.*
import app.fingenmodern.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val receiptSettings: ReceiptSettingsStore) : ViewModel() {
    val receiptToken = MutableStateFlow("")
    init {
        viewModelScope.launch { receiptToken.value = receiptSettings.apiToken().orEmpty() }
    }
    fun saveReceiptToken(value: String) {
        viewModelScope.launch { receiptSettings.saveApiToken(value) }
    }
}

@Composable
fun SettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val token by vm.receiptToken.collectAsStateWithLifecycle()
    FingenScaffold(nav, AppDestination.More) { p ->
        Column(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Получение чеков", style = MaterialTheme.typography.titleMedium)
                    Text("QR-код считывается на телефоне. Для получения состава позиций используется настраиваемый API-поставщик. Токен хранится локально в DataStore.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = token, onValueChange = { vm.receiptToken.value = it }, label = { Text("API-токен сервиса чеков") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { vm.saveReceiptToken(token) }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить токен") }
                }
            }
            TextCard("Данные", "База хранится локально. Автоматический backup базы отключен, чтобы финансовые данные не уходили в системное облачное резервное копирование без явного сценария приложения.")
            TextCard("Шаблоны кредиток", "Условия можно версионировать и менять без пересчета прошлой истории.")
            TextCard("Версия", "FingenNext 0.2.0 — QR-чек и автоматическое распределение")
        }
    }
}
