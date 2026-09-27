package app.fingenmodern.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

private fun notificationAccessEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
    return enabled.split(':').any { it.contains(context.packageName, ignoreCase = true) }
}

@Composable
fun SettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val token by vm.receiptToken.collectAsStateWithLifecycle()
    var notificationEnabled by remember { mutableStateOf(notificationAccessEnabled(context)) }

    LaunchedEffect(Unit) {
        notificationEnabled = notificationAccessEnabled(context)
    }

    FingenScaffold(nav, AppDestination.More) { p ->
        Column(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Банковские уведомления", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "FingenNext может получать только те уведомления, к которым вы сами дадите системный доступ. Приложение извлекает сумму и направление операции и сначала спрашивает вас, что это было.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (notificationEnabled) "Доступ включен" else "Доступ не включен",
                        color = if (notificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (notificationEnabled) "Настроить доступ" else "Разрешить чтение уведомлений")
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Получение чеков", style = MaterialTheme.typography.titleMedium)
                    Text("QR-код считывается на телефоне. Для получения состава позиций используется настраиваемый API-поставщик. Токен хранится локально в DataStore.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = token, onValueChange = { vm.receiptToken.value = it }, label = { Text("API-токен сервиса чеков") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { vm.saveReceiptToken(token) }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить токен") }
                }
            }

            TextCard("ФНС", "Официальное API проверки чеков ФНС подключается только после регистрации внешнего пользователя и выдачи ФНС мастер-токена. В приложении оставлен отдельный слой ReceiptProvider, чтобы не зашивать неофициальный доступ к сервису.")
            TextCard("Gmail", "Подключение Gmail будет работать через официальный OAuth и только после явного согласия пользователя. Доступ к почте не смешивается с банковским импортом.")
            TextCard("Данные", "База хранится локально. Автоматический backup базы отключен, чтобы финансовые данные не уходили в системное облачное резервное копирование без явного сценария приложения.")
            TextCard("Версия", "FingenNext 0.3.0 — автоимпорт банковских уведомлений и проверка входящих денег")
        }
    }
}
