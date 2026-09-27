package app.fingenmodern.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.fingenmodern.data.FinanceRepository
import app.fingenmodern.data.ReceiptProvider
import app.fingenmodern.domain.*
import app.fingenmodern.ui.AppDestination
import app.fingenmodern.ui.components.FingenScaffold
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReceiptScanState {
    data object Ready : ReceiptScanState
    data class Loading(val qr: ReceiptQrData) : ReceiptScanState
    data class Loaded(val receipt: Receipt) : ReceiptScanState
    data class Error(val message: String, val qr: ReceiptQrData? = null) : ReceiptScanState
}

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val provider: ReceiptProvider,
    private val repo: FinanceRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Ready)
    val state = _state

    val accounts = repo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun process(raw: String) {
        val qr = ReceiptQrParser.parse(raw)
        if (qr == null) {
            _state.value = ReceiptScanState.Error("Это не распознанный QR кассового чека.")
            return
        }
        _state.value = ReceiptScanState.Loading(qr)
        viewModelScope.launch {
            provider.getReceipt(qr)
                .onSuccess { _state.value = ReceiptScanState.Loaded(it) }
                .onFailure { _state.value = ReceiptScanState.Error(it.message ?: "Не удалось получить состав чека.", qr) }
        }
    }

    fun saveDistributed(receipt: Receipt, accountId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            receipt.items
                .filter { it.total.minor != 0L }
                .groupBy { it.suggestedCategory }
                .forEach { (category, items) ->
                    val total = items.fold(0L) { acc, item -> Math.addExact(acc, item.total.minor) }
                    val categoryId = repo.categoryId(category.name)
                    repo.addOperation(
                        OperationDraft.Expense(
                            accountId = accountId,
                            amount = Money(total, receipt.total.currency),
                            date = receipt.dateTime?.toLocalDate() ?: java.time.LocalDate.now(),
                            categoryId = categoryId,
                            note = "Чек${receipt.merchantName?.let { " — $it" } ?: ""}: ${category.title}"
                        )
                    )
                }
            onDone()
        }
    }
}

@Composable
fun ReceiptScanScreen(
    nav: NavController,
    vm: ReceiptScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id) }

    val scanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    fun startScan() {
        scanner.startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(vm::process) }
    }

    LaunchedEffect(Unit) { startScan() }

    FingenScaffold(nav, AppDestination.Transactions) { p ->
        LazyColumn(
            Modifier.padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Сканирование чека", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Наведите камеру на QR-код внизу кассового чека. После распознавания FingenNext получит состав покупки и предложит категории.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when (val current = state) {
                ReceiptScanState.Ready -> item {
                    Button(onClick = ::startScan, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сканировать QR")
                    }
                }
                is ReceiptScanState.Loading -> item {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Получаю состав чека…")
                    current.qr.total?.let { Text("Сумма по QR: ${it.format()}") }
                }
                is ReceiptScanState.Error -> {
                    item {
                        Text(current.message, color = MaterialTheme.colorScheme.error)
                        current.qr?.let { Text("QR распознан: ${it.total?.format() ?: "сумма неизвестна"}") }
                        Button(onClick = ::startScan, modifier = Modifier.fillMaxWidth()) { Text("Сканировать ещё") }
                        OutlinedButton(
                            onClick = { nav.navigate(AppDestination.Settings.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Настроить источник чеков") }
                    }
                }
                is ReceiptScanState.Loaded -> {
                    val receipt = current.receipt
                    item {
                        Text(
                            receipt.merchantName ?: "Кассовый чек",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Итого: ${receipt.total.format()}", style = MaterialTheme.typography.headlineSmall)
                        Text("Списать со счёта")
                        accounts.forEach { account ->
                            FilterChip(
                                selected = selectedAccountId == account.id,
                                onClick = { selectedAccountId = account.id },
                                label = { Text(account.name) }
                            )
                        }
                        Text("Автоматическое распределение", style = MaterialTheme.typography.titleMedium)
                    }

                    val grouped = receipt.items.groupBy { it.suggestedCategory }
                    grouped.forEach { (category, items) ->
                        val total = items.fold(0L) { acc, item -> acc + item.total.minor }
                        item {
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${category.title} — ${Money(total, receipt.total.currency).format()}", fontWeight = FontWeight.SemiBold)
                                    items.forEach {
                                        Text("• ${it.name} × ${it.quantity.stripTrailingZeros().toPlainString()} — ${it.total.format()}")
                                    }
                                }
                            }
                        }
                    }

                    val distributed = receipt.items.sumOf { it.total.minor }
                    item {
                        if (distributed != receipt.total.minor) {
                            Text(
                                "Внимание: сумма позиций ${Money(distributed, receipt.total.currency).format()} не совпадает с итогом ${receipt.total.format()}. Перед записью проверьте чек.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            enabled = selectedAccountId != null && distributed == receipt.total.minor && receipt.items.isNotEmpty(),
                            onClick = { vm.saveDistributed(receipt, selectedAccountId!!) { nav.popBackStack() } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Записать распределённый расход") }
                        OutlinedButton(onClick = ::startScan, modifier = Modifier.fillMaxWidth()) {
                            Text("Сканировать другой чек")
                        }
                    }
                }
            }
        }
    }
}
