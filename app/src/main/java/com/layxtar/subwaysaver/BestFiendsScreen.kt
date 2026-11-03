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
    var autoLockReadOnly by remember { mutableStateOf(true) }
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
                        "🐛 Донат-Букашки & Скины",
                        "🗑️ Списание (Слив валют)",
                        "💎 Накрутка & Геймплей",
                        "🛡️ Metadata & Защита 444"
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
                            if (isReadOnlyLocked) "🔓 Снять Read-Only" else "🔒 Заморозить (444)",
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1.2f),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = {
                            CryptoAndSyncManager.launchGame(activity, BestFiendsDatabase.PKG_BEST_FIENDS, log)
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

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hotSwapMode, onCheckedChange = { hotSwapMode = it })
                            Text("🔥 Хот-Свап (не закрывать игру при записи конфигов)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = autoLockReadOnly, onCheckedChange = { autoLockReadOnly = it })
                            Text("🔒 Авто-заморозка (chmod 444 / chattr +i) после записи", fontSize = 12.sp)
                        }
                    }
                }

                when (selectedTab) {
                    // ВКЛАДКА 0: ДОНАТНЫЕ ПЕРСОНАЖИ И ВСЕ СКИНЫ
                    0 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🐛 Разблокировка Донатных Букашек за 1 Золотой Слиток", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Прописывает всех 19 эксклюзивных донатных персонажей в магазин Ключей, Энергии и VIP за 1 золото (`shops.json`):\n" +
                                    "• Пак 1 (SmallKeyPack): Green4, Green6, Green8, Green12\n" +
                                    "• Пак 2 (MediumKeyPack): Yellow7, Yellow8, Yellow12\n" +
                                    "• Пак 3 (LargeKeyPack): Blue4, Blue8, Blue11, Blue12\n" +
                                    "• Пак 4 (ExtraLargeKeyPack): Purple7, Purple8, Purple9, Purple12\n" +
                                    "• Пак 5 (GachaKeyPack): Red5, Red6, Red8, Red12",
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

                    // ВКЛАДКА 1: СПИСАНИЕ ЛИШНИХ ВАЛЮТ
                    1 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🗑️ Списание лишних Метеоров, Ключей и Золота в 1 клик", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Укажи точные суммы, которые нужно списать с баланса. Приложение пропишет их как стоимость покупки наборов в Магазине Ключей и Энергии:",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

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

                    // ВКЛАДКА 2: НАКРУТКА И ГЕЙМПЛЕЙНЫЕ МОДЫ
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
                                Text("🚀 Геймплейные Моды в 1 клик", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
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
                                ) { Text("⚔️ Авто-победа (+50 ходов, -95% ХП слизней) + Без ворот", color = Color.White) }

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.installWheelBattlePassAndLevelJackpots(hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("🎡 Джекпот Колеса Фортуны + Баттл-Пасс за 1 очко", color = Color.White) }

                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.disableAllAdsInGame(hotSwapMode, autoLockReadOnly, log)
                                                isReadOnlyLocked = BestFiendsManager.isSettingsLocked()
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("🚫 Отключить всю рекламу (ads.json)") }
                            }
                        }
                    }

                    // ВКЛАДКА 3: METADATA ПАТЧЕР И БЭКАПЫ
                    3 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛡️ Патчер и Сканер global-metadata.dat (IL2CPP)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Блокирует внутри бинарника `global-metadata.dat` ссылки синхронизации конфигов (`https://` -> `http://0`), чтобы сервер не мог сбросить твои моды в `GameSettings` даже при открытых правах!",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                BestFiendsManager.patchGlobalMetadataKillConfigSync(log)
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) {
                                    Text("🛡️ ЗАБЛОКИРОВАТЬ СЕРВЕРНЫЙ СБРОС В METADATA.DAT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

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
                                ) { Text("⏪ Восстановить оригиналы .json и metadata из бэкапов") }
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
