package app.fingenmodern.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.fingenmodern.data.GmailReceiptImporter
import app.fingenmodern.data.ReceiptSettingsStore
import app.fingenmodern.ui.AppDestination
import app.fingenmodern.ui.components.*
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val receiptSettings: ReceiptSettingsStore,
    private val gmailImporter: GmailReceiptImporter
) : ViewModel() {
    val receiptToken = MutableStateFlow("")
    val gmailStatus = MutableStateFlow("")

    init {
        viewModelScope.launch { receiptToken.value = receiptSettings.apiToken().orEmpty() }
    }

    fun saveReceiptToken(value: String) {
        viewModelScope.launch { receiptSettings.saveApiToken(value) }
    }

    fun syncGmail(accessToken: String) {
        viewModelScope.launch {
            gmailStatus.value = "Синхронизация..."
            runCatching { gmailImporter.sync(accessToken) }
                .onSuccess { gmailStatus.value = "Найдено новых чеков: " + it }
                .onFailure { gmailStatus.value = "Ошибка Gmail: " + (it.message ?: "неизвестная ошибка") }
        }
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
    val gmailStatus by vm.gmailStatus.collectAsStateWithLifecycle()
    var notificationEnabled by remember { mutableStateOf(notificationAccessEnabled(context)) }

    val gmailLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        runCatching {
            val authorization = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data)
            authorization.accessToken?.let(vm::syncGmail)
        }.onFailure {
            vm.gmailStatus.value = "Авторизация Gmail отменена или не завершена"
        }
    }

    fun connectGmail() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/gmail.readonly")))
            .build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    authorization.pendingIntent?.let {
                        gmailLauncher.launch(
                            IntentSenderRequest.Builder(it.intentSender).build()
                        )
                    }
                } else {
                    authorization.accessToken?.let(vm::syncGmail)
                }
            }
            .addOnFailureListener {
                vm.gmailStatus.value = "Не удалось открыть авторизацию Gmail: " + (it.message ?: "ошибка")
            }
    }

    LaunchedEffect(Unit) {
        notificationEnabled = notificationAccessEnabled(context)
    }

    FingenScaffold(nav, AppDestination.More) { p ->
        Column(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Банковские уведомления", style = MaterialTheme.typography.titleMedium)
                    Text("FingenNext получает только уведомления после вашего системного разрешения. Извлеченная операция сначала попадает на проверку.", style = MaterialTheme.typography.bodyMedium)
                    Text(if (notificationEnabled) "Доступ включен" else "Доступ не включен", color = if (notificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (notificationEnabled) "Настроить доступ" else "Разрешить чтение уведомлений")
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gmail — электронные чеки", style = MaterialTheme.typography.titleMedium)
                    Text("Подключение использует официальный Google OAuth с доступом только для чтения Gmail. FingenNext ищет письма с электронными чеками и помещает найденные расходы на проверку.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = ::connectGmail, modifier = Modifier.fillMaxWidth()) { Text("Подключить Gmail и найти чеки") }
                    if (gmailStatus.isNotBlank()) Text(gmailStatus, style = MaterialTheme.typography.bodySmall)
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Получение чеков по QR", style = MaterialTheme.typography.titleMedium)
                    Text("QR считывается на телефоне. Для получения состава позиций используется настраиваемый API-поставщик.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = token, onValueChange = { vm.receiptToken.value = it }, label = { Text("API-токен сервиса чеков") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { vm.saveReceiptToken(token) }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить токен") }
                }
            }

            TextCard("ФНС", "Официальное API проверки чеков ФНС требует регистрации внешнего пользователя и мастер-токена. Поэтому в приложении оставлен отдельный ReceiptProvider, без неофициального доступа.")
            TextCard("Данные", "Банк, Gmail и чеки сначала создают кандидатов. Только подтвержденные пользователем данные попадают в Financial Engine.")
            TextCard("Версия", "FingenNext 0.3.0 — банковские уведомления, входящие платежи и импорт электронных чеков Gmail")
        }
    }
}
