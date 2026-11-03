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
import org.json.JSONObject

@Composable
fun BrimScreen(
    activity: ComponentActivity,
    statusText: String,
    logs: String,
    log: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var uiTrigger by remember { mutableIntStateOf(0) }

    var profilePath by remember { mutableStateOf(BrimDatabase.PROFILE_CANDIDATE_PATHS.first()) }
    var profileJsonState by remember { mutableStateOf<JSONObject?>(null) }
    var catalogState by remember {
        mutableStateOf(
            BrimScannedCatalog(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )
    }

    val currencies = remember {
        mutableStateMapOf<String, String>().apply {
            BrimDatabase.CURRENCIES.forEach { (id, _) -> put(id, "0") }
        }
    }
    var highscoreInput by remember { mutableStateOf("5965") }
    var levelInput by remember { mutableStateOf("50") }
    var selectedWeaponInput by remember { mutableStateOf("32") }
    var selectedCharInput by remember { mutableStateOf("5000") }
    var gearUpgradeLevelInput by remember { mutableStateOf("10") }
    var customGearIdInput by remember { mutableStateOf("") }
    var customPetIdInput by remember { mutableStateOf("") }

    fun refreshUiFromProfile(json: JSONObject) {
        val curMap = BrimManager.readCurrencies(json)
        for ((k, v) in curMap) {
            currencies[k] = v.toString()
        }
        highscoreInput = json.optLong("highscore", 0L).toString()
        levelInput = json.optInt("level", 1).toString()
        selectedWeaponInput = json.optString("selectedWeapon", "32")
        selectedCharInput = json.optString("selectedCharacter", "5000")
        uiTrigger++
    }

    fun applyUiToProfile(json: JSONObject) {
        val curMap = currencies.mapValues { it.value.toLongOrNull() ?: 0L }
        val hs = highscoreInput.toLongOrNull() ?: 0L
        val lvl = levelInput.toIntOrNull() ?: 1
        BrimManager.writeCurrenciesAndStats(json, curMap, hs, lvl)
        if (selectedWeaponInput.isNotBlank()) json.put("selectedWeapon", selectedWeaponInput.trim())
        if (selectedCharInput.isNotBlank()) json.put("selectedCharacter", selectedCharInput.trim())
    }

    fun loadBrimAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Чтение профиля Blades of Brim и сканирование tower/gamedata...")
            val loaded = withContext(Dispatchers.IO) {
                val pair = BrimManager.loadUserProfile(log)
                val cat = BrimManager.scanCatalogFromDevice(pair?.second)
                pair to cat
            }
            val pair = loaded.first
            catalogState = loaded.second
            if (pair != null) {
                profilePath = pair.first
                profileJsonState = pair.second
                refreshUiFromProfile(pair.second)
                log("⚔️ Brim загружен! Золота: ${currencies["1"]}, Эссенции: ${currencies["2"]}")
            } else {
                log("⚠️ Файл user_profile не найден! Запусти игру хотя бы раз.")
            }
            isBusy = false
        }
    }

    fun saveBrimAsync(launchAfter: Boolean) {
        val json = profileJsonState ?: return
        applyUiToProfile(json)
        scope.launch {
            isBusy = true
            val ok = withContext(Dispatchers.IO) {
                BrimManager.saveUserProfile(profilePath, json, log)
            }
            if (ok && launchAfter) {
                withContext(Dispatchers.IO) {
                    CryptoAndSyncManager.launchGame(activity, BrimDatabase.PKG_BRIM, log)
                }
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadBrimAsync()
    }

    val pJson = profileJsonState
    val _trig = uiTrigger

    val ownedCharsCount = pJson?.optJSONObject("characterInventory")?.length() ?: 0
    val ownedGearCount = pJson?.optJSONObject("gearInventory")?.length() ?: 0
    val ownedPetsCount = pJson?.optJSONObject("petInventory")?.length() ?: 0

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Валюты & Уровень",
                        "⚔️ Герои ($ownedCharsCount) & Броня ($ownedGearCount)",
                        "🐺 Питомцы ($ownedPetsCount) & Сундуки",
                        "⚡ Моды tower/gamedata",
                        "📦 Бэкап & Инфо"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                profileJsonState?.let { applyUiToProfile(it) }
                                selectedTab = idx
                            },
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
                        modifier = Modifier.weight(0.9f),
                        enabled = !isBusy,
                        onClick = { loadBrimAsync() }
                    ) { Text("🔄 Чтение", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = pJson != null && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        onClick = { saveBrimAsync(false) }
                    ) { Text("💾 Сохранить", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1.1f),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = {
                            if (pJson != null) saveBrimAsync(true)
                            else scope.launch {
                                withContext(Dispatchers.IO) {
                                    CryptoAndSyncManager.launchGame(activity, BrimDatabase.PKG_BRIM, log)
                                }
                            }
                        }
                    ) { Text("🚀 В Brim", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (pJson == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ожидание чтения user_profile Blades of Brim...", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { loadBrimAsync() }) { Text("🔄 Прочитать профиль") }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    when (selectedTab) {
                        // ВКЛАДКА 0: ВАЛЮТЫ, РЕКОРД, УРОВЕНЬ И БУСТЫ
                        0 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        currencies["1"] = "99999999"
                                        currencies["2"] = "9999999"
                                        currencies["3"] = "999999"
                                        currencies["4"] = "999999"
                                        log("💎 Золото и Магическая Эссенция выставлены на максимум!")
                                    }
                                ) { Text("💎 Все валюты MAX", fontSize = 11.sp) }

                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        BrimDatabase.CURRENCIES.forEach { (id, _) -> currencies[id] = "0" }
                                        log("🗑️ Валюты сброшены в 0")
                                    }
                                ) { Text("🗑️ Сбросить в 0", fontSize = 11.sp) }
                            }

                            BrimDatabase.CURRENCIES.forEach { (id, title) ->
                                OutlinedTextField(
                                    value = currencies[id] ?: "0",
                                    onValueChange = { currencies[id] = it },
                                    label = { Text(title) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏆 Уровень Игрока, Рекорд и Бусты (straightUpgrades)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = levelInput,
                                            onValueChange = { levelInput = it },
                                            label = { Text("Уровень игрока (Level)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = highscoreInput,
                                            onValueChange = { highscoreInput = it },
                                            label = { Text("Рекорд (Highscore)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            BrimManager.maxAllStraightUpgrades(pJson, catalogState, 10)
                                            uiTrigger++
                                            log("🚀 Все постоянные улучшения (extra_score_bonus и бусты) прокачаны на 10 ур.!")
                                        }
                                    ) { Text("🚀 Прокачать все постоянные Бусты на максимум") }
                                }
                            }
                        }

                        // ВКЛАДКА 1: ГЕРОИ, ОРУЖИЕ И БРОНЯ
                        1 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🦸 Герои (characterInventory)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Открыто героев в профиле: $ownedCharsCount (найдено ID в базе/gamedata: ${catalogState.characterIds.size})",
                                        fontSize = 12.sp,
                                        color = Color(0xFF81C784)
                                    )
                                    OutlinedTextField(
                                        value = selectedCharInput,
                                        onValueChange = { selectedCharInput = it },
                                        label = { Text("ID выбранного героя (напр. 5000 = Zuma)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        onClick = {
                                            BrimManager.unlockAllCharacters(pJson, catalogState)
                                            uiTrigger++
                                            log("🦸 Все герои (${catalogState.characterIds.size} шт.) разблокированы в characterInventory!")
                                        }
                                    ) { Text("🦸 ОТКРЫТЬ ВСЕХ ГЕРОЕВ", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🗡️ Оружие и Комплекты Брони (gearInventory)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Открыто предметов экипировки: $ownedGearCount (в каталоге: ${catalogState.gearIds.size})",
                                        fontSize = 12.sp,
                                        color = Color(0xFF81C784)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = selectedWeaponInput,
                                            onValueChange = { selectedWeaponInput = it },
                                            label = { Text("ID активного оружия") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = gearUpgradeLevelInput,
                                            onValueChange = { gearUpgradeLevelInput = it },
                                            label = { Text("Уровень прокачки") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            val upg = gearUpgradeLevelInput.toIntOrNull() ?: 10
                                            BrimManager.unlockAndMaxAllGear(pJson, catalogState, upg)
                                            uiTrigger++
                                            log("🗡️ Всё оружие и броня открыты с уровнем прокачки $upg!")
                                        }
                                    ) { Text("🗡️ Открыть и прокачать всё оружие и броню") }

                                    Divider()
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = customGearIdInput,
                                            onValueChange = { customGearIdInput = it },
                                            label = { Text("Добавить конкретный ID оружия/брони") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(onClick = {
                                            val id = customGearIdInput.trim()
                                            if (id.isNotEmpty()) {
                                                val inv = pJson.optJSONObject("gearInventory") ?: JSONObject()
                                                val upg = gearUpgradeLevelInput.toIntOrNull() ?: 10
                                                inv.put(id, JSONObject().put("upgradeLevel", upg))
                                                pJson.put("gearInventory", inv)
                                                pJson.put("gearCount", inv.length())
                                                uiTrigger++
                                                log("🗡️ Предмет ID '$id' (ур. $upg) добавлен в gearInventory!")
                                            }
                                        }) { Text("+ Добавить") }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 2: ПИТОМЦЫ, СУНДУКИ И ДОСТИЖЕНИЯ
                        2 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🐺 Ездовые Питомцы (petInventory)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Открыто питомцев: $ownedPetsCount", fontSize = 12.sp, color = Color(0xFF81C784))
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                        onClick = {
                                            val upg = gearUpgradeLevelInput.toIntOrNull() ?: 10
                                            BrimManager.unlockAndMaxAllPets(pJson, catalogState, upg)
                                            uiTrigger++
                                            log("🐺 Все питомцы (${catalogState.petIds.size} шт.) открыты и прокачаны!")
                                        }
                                    ) { Text("🐺 ОТКРЫТЬ ВСЕХ ПИТОМЦЕВ (ВОЛКИ / КОНИ / ДРАКОНЫ)", color = Color.White, fontSize = 11.sp) }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = customPetIdInput,
                                            onValueChange = { customPetIdInput = it },
                                            label = { Text("Добавить ID питомца вручную") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(onClick = {
                                            val id = customPetIdInput.trim()
                                            if (id.isNotEmpty()) {
                                                val inv = pJson.optJSONObject("petInventory") ?: JSONObject()
                                                inv.put(id, JSONObject().put("upgradeLevel", 10))
                                                pJson.put("petInventory", inv)
                                                pJson.put("petsCount", inv.length())
                                                uiTrigger++
                                                log("🐺 Питомец ID '$id' добавлен!")
                                            }
                                        }) { Text("+ Питомец") }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎁 Сундуки (gachaBoxInventory) и Достижения (3⭐)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            BrimManager.addGachaBoxesToInventory(pJson, catalogState, 50)
                                            uiTrigger++
                                            log("🎁 В инвентарь добавлено по 50 сундуков каждого вида!")
                                        }
                                    ) { Text("🎁 Начислить по 50 Сундуков каждого типа") }

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        onClick = {
                                            BrimManager.completeAllAchievementsAndFeatures(pJson, catalogState)
                                            uiTrigger++
                                            log("⭐ Все достижения закрыты на 3 звезды, цели глав выполнены, обучение пропущено!")
                                        }
                                    ) { Text("⭐ Выполнить все Достижения (3 звезды) и Миссии", color = Color.White) }
                                }
                            }
                        }

                        // ВКЛАДКА 3: ПАТЧЕР TOWER/GAMEDATA
                        3 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚡ Патчинг игровых конфигов tower/gamedata", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Обнуляет стоимость покупок и улучшений в `pricelist.json`, `economy.json`, `straightupgrades.json` и делает возрождения бесплатными в `revive.json`.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                withContext(Dispatchers.IO) {
                                                    BrimManager.patchGameDataFreePricesAndRevive(log)
                                                }
                                                isBusy = false
                                            }
                                        }
                                    ) {
                                        Text("⚡ СДЕЛАТЬ ВСЕ ЦЕНЫ И ВОЗРОЖДЕНИЯ БЕСПЛАТНЫМИ (0🪙)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 4: БЭКАПЫ И ИНФОРМАЦИЯ
                        4 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📦 Бэкап и Путь к профилю Blades of Brim", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Активный путь: $profilePath", fontSize = 11.sp, color = Color.Gray)
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                withContext(Dispatchers.IO) {
                                                    CryptoAndSyncManager.exportBackupToDownload(profilePath, "Brim", log)
                                                }
                                                isBusy = false
                                            }
                                        }
                                    ) { Text("📦 Экспортировать user_profile в /Download") }

                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                val ok = withContext(Dispatchers.IO) {
                                                    CryptoAndSyncManager.restoreBackup(profilePath, log)
                                                }
                                                if (ok) loadBrimAsync()
                                                isBusy = false
                                            }
                                        }
                                    ) { Text("⏪ Восстановить оригинальный user_profile из бэкапа") }
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
}
