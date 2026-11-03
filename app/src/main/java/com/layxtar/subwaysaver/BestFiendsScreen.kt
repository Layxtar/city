package com.layxtar.subwaysaver

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BestFiendsScreen(
    activity: ComponentActivity,
    statusText: String,
    logs: String,
    log: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }

    var hotSwapMode by remember { mutableStateOf(true) }
    // По умолчанию false, чтобы права 444 не мешали инициализации при холодном старте игры
    var autoLockReadOnly by remember { mutableStateOf(false) }
    var isReadOnlyLocked by remember { mutableStateOf(false) }

    var drainYellowInput by remember { mutableStateOf("240000000") }
    var drainBlueInput by remember { mutableStateOf("24000000") }
    var drainKeysInput by remember { mutableStateOf("199000") }
    var drainGoldInput by remember { mutableStateOf("50000") }

    var gainGoldInput by remember { mutableStateOf("50000") }
    var gainKeysInput by remember { mutableStateOf("500") }
    var gainYellowInput by remember { mutableStateOf("500000") }
    var gainBlueInput by remember { mutableStateOf("500000") }

    var maxStyleIdInput by remember { mutableStateOf("9") }
    var scannedFiendsText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (CryptoAndSyncManager.hasPermission()) {
            withContext(Dispatchers.IO) {
                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "🐛 Букашки & Починка",
                        "🗑️ Списание (Слив валют)",
                        "💎 Накрутка & Геймплей",
                        "🔒 Бэкап & Сканер"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp)) {
                Text(text = statusText, fontSize = 12.sp, color = Color(0xFF81C784), maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isBusy,
                        onClick = {
                            scope.launch {
                                isBusy = true
                                val newLock = !isReadOnlyLocked
                                withContext(Dispatchers.IO) {
                                    BestFiendsManager.lockSettingsReadOnly(newLock, log)
                                    isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                }
                                isBusy = false
                            }
                        }
                    ) {
                        Text(
                            if (isReadOnlyLocked) "🔓 Снять Read-Only" else "🔒 Защитить моды",
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1.2f),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    CryptoAndSyncManager.launchGame(activity, BestFiendsDatabase.PKG_BEST_FIENDS, log)
                                }
                            }
                        }
                    ) {
                        Text("🚀 Запустить Best Fiends", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Кнопка экстренной починки зависания на экране загрузки
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🚑 Зависает загрузка Best Fiends?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFFB74D))
                        Text(
                            "Снимает права chmod 555/444 с папки GameSettings, восстанавливает оригинальные ссылки в global-metadata.dat и удаляет конфликтующие конфиги.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    withContext(Dispatchers.IO) {
                                        BestFiendsManager.fixInfiniteLoadingFreeze(log)
                                        isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                    }
                                    isBusy = false
                                }
                            }
                        ) {
                            Text("🚑 ПОЧИНИТЬ ЗАВИСАНИЕ ЗАГРУЗКИ ИГРЫ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hotSwapMode, onCheckedChange = { hotSwapMode = it })
                            Text("🔥 Хот-Свап (применять моды на лету при запущенной игре)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = autoLockReadOnly, onCheckedChange = { autoLockReadOnly = it })
                            Text("🔒 Ставить Read-Only (444) на файлы модов после записи", fontSize = 12.sp)
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🐛 Разблокировка Донатных Букашек за 1 Золотой Слиток", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Прописывает всех 19 эксклюзивных донатных персонажей в магазин Ключей, Энергии и VIP за 1 золото (`shops.json`). Совет: применяй в режиме Хот-Свапа, когда игра уже загрузилась на карту мира!",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.installDonateFiendsShopFor1Gold(hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) {
                                    Text("🐛 ВШИТЬ ДОНАТНЫХ БУКАШЕК В МАГАЗИН ЗА 1🪙", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🎁 Все Персонажи + Все 600+ Скинов в Сундуках за Ключи", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Генерирует `gachaLevelsFiends.json` строго по одному предмету на блок со 100% вероятностью (`probability: 1.0`, `fromLevel: 1`).",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                OutlinedTextField(
                                    value = maxStyleIdInput,
                                    onValueChange = { maxStyleIdInput = it },
                                    label = { Text("Макс. номер скина styleId (рекомендуется 9)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            val maxSt = (maxStyleIdInput.toIntOrNull() ?: 9).coerceIn(1, 15)
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.installAllFiendsAndSkinsGacha(maxSt, hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) {
                                    Text("🎁 ЗАПИСАТЬ ВСЕХ БУКАШЕК И ВСЕ СКИНЫ В СУНДУКИ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    1 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🗑️ Списание лишних Метеоров, Ключей и Золота в 1 клик", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedTextField(
                                    value = drainYellowInput,
                                    onValueChange = { drainYellowInput = it },
                                    label = { Text("🟡 Списать Жёлтых метеоров (1-й пак в Ключах)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = drainBlueInput,
                                    onValueChange = { drainBlueInput = it },
                                    label = { Text("🔵 Списать Синих метеоров (2-й пак в Ключах)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = drainKeysInput,
                                    onValueChange = { drainKeysInput = it },
                                    label = { Text("🔑 Списать Ключей (3-й пак в Ключах)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = drainGoldInput,
                                    onValueChange = { drainGoldInput = it },
                                    label = { Text("🪙 Списать Золота (4-й пак в Ключах)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            val y = drainYellowInput.toIntOrNull() ?: 240000000
                                            val b = drainBlueInput.toIntOrNull() ?: 24000000
                                            val k = drainKeysInput.toIntOrNull() ?: 199000
                                            val g = drainGoldInput.toIntOrNull() ?: 50000
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.installCurrencyDrainerShop(k, y, b, g, hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) {
                                    Text("🗑️ СОЗДАТЬ МАГАЗИН СПИСАНИЯ ЭТИХ СУММ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    2 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💎 Настройка Магазина Накрутки Валют (за 1 золото)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = gainGoldInput,
                                        onValueChange = { gainGoldInput = it },
                                        label = { Text("🪙 Выдать Золота") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gainKeysInput,
                                        onValueChange = { gainKeysInput = it },
                                        label = { Text("🔑 Выдать Ключей") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = gainYellowInput,
                                        onValueChange = { gainYellowInput = it },
                                        label = { Text("🟡 Выдать Жёлтых") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gainBlueInput,
                                        onValueChange = { gainBlueInput = it },
                                        label = { Text("🔵 Выдать Синих") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            val g = gainGoldInput.toIntOrNull() ?: 50000
                                            val k = gainKeysInput.toIntOrNull() ?: 500
                                            val y = gainYellowInput.toIntOrNull() ?: 500000
                                            val b = gainBlueInput.toIntOrNull() ?: 500000
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.installMegaResourcesShop(g, k, y, b, hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("💎 Установить Магазин Накрутки") }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🚀 Проверенные Геймплейные Моды", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.installFreeFiendexAndBoosters(hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("🐞 Покупка в Fiendex за 0🪙 + Бустеры по 999 шт.") }

                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.installEasyWinAndRemoveGates(hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("⚔️ Убрать ворота на карте + 50 ходов за 0🪙") }
                            }
                        }
                    }

                    3 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔍 Сканер global-metadata.dat и Откат", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            val list = withContext(Dispatchers.IO) {
                                                BestFiendsManager.scanGlobalMetadataForFiendIds(log)
                                            }
                                            scannedFiendsText = list.joinToString(", ")
                                            isBusy = false
                                        }
                                    }
                                ) { Text("🔍 Просканировать metadata.dat на все ID букашек") }

                                if (scannedFiendsText.isNotEmpty()) {
                                    Text("Найдено: $scannedFiendsText", fontSize = 11.sp, color = Color(0xFF81C784))
                                }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.restoreAllBackups(log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("⏪ Полный откат всех конфигов Best Fiends к оригиналам") }
                            }
                        }

                        Text("📜 Журнал операций:", fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.Black)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = logs,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}
