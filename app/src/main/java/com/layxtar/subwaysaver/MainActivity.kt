package com.layxtar.subwaysaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CryptoAndSyncManager.registerPermissionListener()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UltimateEditorApp(this)
                }
            }
        }
    }

    override fun onDestroy() {
        CryptoAndSyncManager.unregisterPermissionListener()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltimateEditorApp(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    var isCityMode by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("🔄 Готов к работе...") }
    var logs by remember { mutableStateOf("🚀 Старт Subway Ultimate Editor...\n") }

    fun log(msg: String) {
        statusText = msg
        logs = "$msg\n$logs"
    }

    var uiTrigger by remember { mutableIntStateOf(0) }

    // ==================== СОСТОЯНИЕ SUBWAY SURFERS CITY + СТЕК UNDO ====================
    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var activeProfilePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var isCityLoaded by remember { mutableStateOf(false) }
    val cityUndoStack = remember { mutableStateListOf<String>() }

    val currencyStates = remember { mutableStateMapOf<Long, String>() }
    val iapCheckedStates = remember { mutableStateMapOf<String, Boolean>() }
    var iapFilterCategory by remember { mutableStateOf("Все") }

    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("0") }
    var piggyFilled by remember { mutableStateOf(false) }
    var piggyFreeClaim by remember { mutableStateOf(false) }

    var seasonPurchased by remember { mutableStateOf(false) }
    var seasonPoints by remember { mutableStateOf("0") }
    var selectedSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.first().first) }

    var playerName by remember { mutableStateOf("") }
    var playerTag by remember { mutableStateOf("") }
    var playerLevel by remember { mutableStateOf("1") }
    var playerXp by remember { mutableStateOf("0") }
    var boostedMultiplier by remember { mutableStateOf("0") }

    var selectedSurferId by remember { mutableLongStateOf(GameDatabase.SURFERS.first().first) }
    var selectedBoardId by remember { mutableLongStateOf(GameDatabase.BOARDS.first().first) }
    var crewSlot1 by remember { mutableLongStateOf(0L) }
    var crewSlot2 by remember { mutableLongStateOf(0L) }
    var crewSlot3 by remember { mutableLongStateOf(0L) }

    val citySurfersList = remember { mutableStateListOf<CitySurferState>() }
    val cityBoardsList = remember { mutableStateListOf<CityBoardState>() }
    var citySearchQuery by remember { mutableStateOf("") }

    // ==================== СОСТОЯНИЕ CLASSIC SUBWAY SURFERS + СТЕК UNDO ====================
    var classicState by remember { mutableStateOf<ClassicMemoryState?>(null) }
    var isClassicLoaded by remember { mutableStateOf(false) }
    val classicUndoStack = remember { mutableStateListOf<ClassicMemoryState>() }

    val classicCurrencies = remember {
        mutableStateMapOf<String, String>().apply {
            ClassicDatabase.CURRENCIES.forEach { (id, _) -> put(id, "0") }
        }
    }
    var classicDoubleCoinsOnly by remember { mutableStateOf(false) }
    var medalBronze by remember { mutableStateOf("0") }
    var medalSilver by remember { mutableStateOf("0") }
    var medalGold by remember { mutableStateOf("0") }
    var medalDiamond by remember { mutableStateOf("0") }
    var medalChampion by remember { mutableStateOf("0") }

    var classicSearchQuery by remember { mutableStateOf("") }
    var expandedClassicSurferId by remember { mutableStateOf<String?>(null) }
    var expandedClassicBoardId by remember { mutableStateOf<String?>(null) }
    var selectedClassicLootbox by remember { mutableStateOf(ClassicDatabase.LOOTBOX_TYPES.first().first) }
    var classicLootboxCount by remember { mutableStateOf("10") }

    // ==================== ФУНКЦИИ СИНХРОНИЗАЦИИ ПАМЯТИ И UNDO (CITY) ====================

    fun applyUIToCityProfile(prof: JSONObject) {
        val wallet = JSONArray()
        for ((tag, _) in GameDatabase.CURRENCIES) {
            val count = currencyStates[tag]?.toLongOrNull() ?: 0L
            wallet.put(JSONObject().put("dataTag", tag).put("count", count))
        }
        prof.put("wallet", wallet)

        val onlineProf = prof.optJSONObject("onlinePlayerProfile") ?: JSONObject()
        if (playerName.isNotBlank()) onlineProf.put("name", playerName)
        if (playerTag.isNotBlank()) onlineProf.put("tag", playerTag)
        onlineProf.put("renameableAt", "0001-01-01T00:00:00")
        prof.put("onlinePlayerProfile", onlineProf)

        prof.put("level", playerLevel.toIntOrNull() ?: prof.optInt("level", 1))
        prof.put("xp", playerXp.toIntOrNull() ?: prof.optInt("xp", 0))

        val bm = boostedMultiplier.toIntOrNull() ?: 0
        if (bm > 0 || prof.has("boostedRunMultiplier")) {
            prof.put("boostedRunMultiplier", JSONObject().put("runMultiplier", bm))
        }

        prof.put("character", selectedSurferId)
        prof.put("selectedBoard", selectedBoardId)

        if (crewSlot1 != 0L || crewSlot2 != 0L || crewSlot3 != 0L) {
            val crewArr = JSONArray()
            if (crewSlot1 != 0L) crewArr.put(JSONObject().put("SurferId", crewSlot1))
            if (crewSlot2 != 0L) crewArr.put(JSONObject().put("SurferId", crewSlot2))
            if (crewSlot3 != 0L) crewArr.put(JSONObject().put("SurferId", crewSlot3))
            prof.put("crew", crewArr)
        }

        prof.put("seasonPassPurchased", seasonPurchased)
        prof.put("seasonPassPoints", seasonPoints.toIntOrNull() ?: prof.optInt("seasonPassPoints", 0))
        if (selectedSeasonId.isNotBlank()) prof.put("seasonPassId", selectedSeasonId)
        if (seasonPurchased) prof.put("hasSeenPremiumPassActivation", true)

        if (citySurfersList.isNotEmpty()) {
            CryptoAndSyncManager.applyCitySurfers(prof, citySurfersList)
        }
        if (cityBoardsList.isNotEmpty()) {
            CryptoAndSyncManager.applyCityBoards(prof, cityBoardsList)
        }
    }

    fun refreshUIFromCityProfile(prof: JSONObject) {
        val wallet = prof.optJSONArray("wallet") ?: JSONArray()
        for ((tag, _) in GameDatabase.CURRENCIES) currencyStates[tag] = "0"
        for (i in 0 until wallet.length()) {
            val item = wallet.getJSONObject(i)
            currencyStates[item.getLong("dataTag")] = item.getLong("count").toString()
        }

        val purchases = prof.optJSONObject("purchaseHistory") ?: JSONObject()
        for ((pId, _) in GameDatabase.IAP_CATALOG) iapCheckedStates[pId] = purchases.has(pId)

        val online = prof.optJSONObject("onlinePlayerProfile")
        playerName = online?.optString("name", "") ?: ""
        playerTag = online?.optString("tag", "") ?: ""

        playerLevel = prof.optInt("level", 1).toString()
        playerXp = prof.optInt("xp", 0).toString()
        boostedMultiplier = prof.optJSONObject("boostedRunMultiplier")?.optInt("runMultiplier", 0)?.toString() ?: "0"

        selectedSurferId = prof.optLong("character", GameDatabase.SURFERS.first().first)
        selectedBoardId = prof.optLong("selectedBoard", GameDatabase.BOARDS.first().first)

        val crewArr = prof.optJSONArray("crew")
        if (crewArr != null) {
            if (crewArr.length() > 0) crewSlot1 = crewArr.getJSONObject(0).optLong("SurferId", 0L)
            if (crewArr.length() > 1) crewSlot2 = crewArr.getJSONObject(1).optLong("SurferId", 0L)
            if (crewArr.length() > 2) crewSlot3 = crewArr.getJSONObject(2).optLong("SurferId", 0L)
        }

        val piggy = prof.optJSONObject("piggyBank")
        if (piggy != null) {
            val curId = piggy.optString("currentId", GameDatabase.PIGGY_BANKS.first().first)
            selectedPiggyId = curId
            selectedPiggyMission = GameDatabase.PIGGY_BANKS.find { it.first == curId }?.second?.first
                ?: GameDatabase.PIGGY_BANKS.first().second.first
            piggyProgress = piggy.optInt("lastSeenProgress", 0).toString()
            piggyFilled = piggy.optBoolean("isFilled", false)
            piggyFreeClaim = purchases.has(curId)
        }

        seasonPurchased = prof.optBoolean("seasonPassPurchased", false)
        seasonPoints = prof.optInt("seasonPassPoints", 0).toString()
        selectedSeasonId = prof.optString("seasonPassId", GameDatabase.SEASONS.first().first)

        citySurfersList.clear()
        citySurfersList.addAll(CryptoAndSyncManager.parseCitySurfers(prof))

        cityBoardsList.clear()
        cityBoardsList.addAll(CryptoAndSyncManager.parseCityBoards(prof))
        uiTrigger++
    }

    fun pushCityUndo() {
        val prof = profileJson ?: return
        applyUIToCityProfile(prof)
        if (cityUndoStack.size >= 20) cityUndoStack.removeAt(0)
        cityUndoStack.add(prof.toString())
    }

    fun performCityUndo() {
        if (cityUndoStack.isEmpty()) return
        val prevStr = cityUndoStack.removeAt(cityUndoStack.lastIndex)
        val restored = JSONObject(prevStr)
        profileJson = restored
        refreshUIFromCityProfile(restored)
        log("↩️ Отменено последнее изменение в City (осталось шагов: ${cityUndoStack.size})")
    }

    // ==================== ФУНКЦИИ СИНХРОНИЗАЦИИ ПАМЯТИ И UNDO (CLASSIC) ====================

    fun applyUIToClassicState(st: ClassicMemoryState) {
        for ((id, valStr) in classicCurrencies) {
            val amount = valStr.toLongOrNull() ?: 0L
            ClassicCryptoAndSyncManager.setCurrencyInMemory(st.wallet, id, amount)
        }
        val medalsMap = mapOf(
            "103" to (medalBronze.toIntOrNull() ?: 0),
            "104" to (medalSilver.toIntOrNull() ?: 0),
            "105" to (medalGold.toIntOrNull() ?: 0),
            "106" to (medalDiamond.toIntOrNull() ?: 0),
            "107" to (medalChampion.toIntOrNull() ?: 0)
        )
        ClassicCryptoAndSyncManager.setTopRunMedals(st, medalsMap)
    }

    fun refreshUIFromClassicState(st: ClassicMemoryState) {
        val currencies = st.wallet.optJSONObject("currencies")
        for ((id, _) in ClassicDatabase.CURRENCIES) {
            val v = currencies?.optJSONObject(id)?.optLong("value", 0L) ?: 0L
            classicCurrencies[id] = v.toString()
        }
        classicDoubleCoinsOnly = ClassicCryptoAndSyncManager.isDoubleCoinsEnabled(st)
        val medals = ClassicCryptoAndSyncManager.getTopRunMedals(st)
        medalBronze = (medals["103"] ?: 0).toString()
        medalSilver = (medals["104"] ?: 0).toString()
        medalGold = (medals["105"] ?: 0).toString()
        medalDiamond = (medals["106"] ?: 0).toString()
        medalChampion = (medals["107"] ?: 0).toString()
        uiTrigger++
    }

    fun pushClassicUndo() {
        val st = classicState ?: return
        applyUIToClassicState(st)
        if (classicUndoStack.size >= 20) classicUndoStack.removeAt(0)
        classicUndoStack.add(st.deepCopy())
    }

    fun performClassicUndo() {
        if (classicUndoStack.isEmpty()) return
        val prev = classicUndoStack.removeAt(classicUndoStack.lastIndex)
        classicState = prev
        refreshUIFromClassicState(prev)
        log("↩️ Отменено последнее изменение в Classic (осталось шагов: ${classicUndoStack.size})")
    }

    // ==================== ЧТЕНИЕ И ЗАПИСЬ ЧЕРЕЗ SHIZUKU ====================

    fun loadProfileAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, ::log)
            return
        }
        scope.launch {
            isBusy = true
            statusText = "⏳ Чтение твоих файлов сохранения..."
            if (isCityMode) {
                val loaded = withContext(Dispatchers.IO) {
                    CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, false) {}
                    val path = CryptoAndSyncManager.resolveCityProfilePath {}
                    val bytes = CryptoAndSyncManager.readFileViaShizuku(path) {}
                    if (bytes != null) {
                        runCatching {
                            val (cont, prof) = CryptoAndSyncManager.decryptCityProfile(bytes)
                            Triple(path, bytes, cont to prof)
                        }.getOrNull()
                    } else null
                }
                if (loaded != null) {
                    activeProfilePath = loaded.first
                    rawFileBytes = loaded.second
                    containerJson = loaded.third.first
                    profileJson = loaded.third.second
                    cityUndoStack.clear()
                    refreshUIFromCityProfile(loaded.third.second)
                    isCityLoaded = true
                    log("✅ Профиль Subway City загружен (Игрок: $playerName, Ур: $playerLevel)!")
                } else {
                    log("❌ Профиль Subway City не найден.")
                }
            } else {
                val loadedClassic = withContext(Dispatchers.IO) {
                    ClassicCryptoAndSyncManager.loadFullClassicState {}
                }
                classicState = loadedClassic
                classicUndoStack.clear()
                refreshUIFromClassicState(loadedClassic)
                isClassicLoaded = true
                log("✅ Профиль Classic загружен (Монет: ${classicCurrencies["1"]}, Ключей: ${classicCurrencies["2"]})!")
            }
            isBusy = false
        }
    }

    fun saveCityProfileAsync(launchAfterSave: Boolean) {
        val prof = profileJson ?: return
        val cont = containerJson ?: return
        val raw = rawFileBytes ?: return

        applyUIToCityProfile(prof)
        scope.launch {
            isBusy = true
            statusText = "💾 Шифрование и сохранение профиля City..."
            val newBytes = withContext(Dispatchers.IO) {
                CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, true) {}
                val enc = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
                val ok = CryptoAndSyncManager.writeFileViaShizuku(activeProfilePath, enc) {}
                if (ok) enc else null
            }
            if (newBytes != null) {
                rawFileBytes = newBytes
                log("✅ Все изменения Subway City успешно сохранены в игру!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, ::log)
            } else {
                log("❌ Ошибка сохранения профиля City!")
            }
            isBusy = false
        }
    }

    fun saveClassicAllFilesAsync(launchAfterSave: Boolean) {
        val st = classicState ?: return
        applyUIToClassicState(st)
        scope.launch {
            isBusy = true
            statusText = "💾 Шифрование и запись всех файлов Classic..."
            val ok = withContext(Dispatchers.IO) {
                ClassicCryptoAndSyncManager.saveFullClassicState(st) {}
            }
            if (ok) {
                log("✅ Все изменения Classic (кошелёк, персонажи, доски, медали, ивенты) записаны!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, ClassicCryptoAndSyncManager.PKG_CLASSIC, ::log)
            } else {
                log("⚠️ Часть файлов Classic не удалось записать.")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        CryptoAndSyncManager.onPermissionGrantedCallback = { granted ->
            if (granted) loadProfileAsync()
        }
        loadProfileAsync()
    }

    val canUndo = if (isCityMode) cityUndoStack.isNotEmpty() else classicUndoStack.isNotEmpty()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Subway Ultimate", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                enabled = canUndo && !isBusy,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                onClick = { if (isCityMode) performCityUndo() else performClassicUndo() }
                            ) {
                                Text("↩️ Шаг назад", fontSize = 11.sp)
                            }

                            Button(
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                onClick = {
                                    if (!isCityMode) {
                                        classicState?.let { applyUIToClassicState(it) }
                                        isCityMode = true
                                        selectedTab = 0
                                        if (!isCityLoaded) loadProfileAsync()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isCityMode) Color(0xFF1976D2) else Color.DarkGray)
                            ) { Text("🏙️ CITY", fontSize = 11.sp) }

                            Button(
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                onClick = {
                                    if (isCityMode) {
                                        profileJson?.let { applyUIToCityProfile(it) }
                                        isCityMode = false
                                        selectedTab = 0
                                        if (!isClassicLoaded) loadProfileAsync()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (!isCityMode) Color(0xFFE65100) else Color.DarkGray)
                            ) { Text("🛹 CLASSIC", fontSize = 11.sp) }
                        }
                    }
                )
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = if (isCityMode) {
                        listOf(
                            "💰 Валюта",
                            "⚡ Задания & Пропуск",
                            "🏄 Сёрферы (Статы)",
                            "🛹 Доски (Статы)",
                            "🐷 Копилка & Донат",
                            "📊 Профиль & Бэкап"
                        )
                    } else {
                        listOf(
                            "💰 Кошелёк & Бусты",
                            "🏆 Ивенты & Медали",
                            "🏄 Сёрферы (${ClassicDatabase.CLASSIC_SURFERS.size})",
                            "🛹 Доски (${ClassicDatabase.CLASSIC_BOARDS.size})",
                            "🎁 Сундуки & Косметика"
                        )
                    }
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                // Перед переключением вкладки фиксируем введённые числа в память, чтобы ничего не сбрасывалось!
                                if (isCityMode) profileJson?.let { applyUIToCityProfile(it) }
                                else classicState?.let { applyUIToClassicState(it) }
                                selectedTab = idx
                            },
                            text = { Text(title) }
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
                    OutlinedButton(modifier = Modifier.weight(0.9f), enabled = !isBusy, onClick = { loadProfileAsync() }) {
                        Text("🔄 Перечитать", fontSize = 12.sp)
                    }
                    if (isCityMode) {
                        Button(modifier = Modifier.weight(1f), enabled = isCityLoaded && !isBusy, onClick = { saveCityProfileAsync(false) }) {
                            Text("💾 Сохранить", fontSize = 12.sp)
                        }
                        Button(
                            modifier = Modifier.weight(1.2f),
                            enabled = isCityLoaded && !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            onClick = { saveCityProfileAsync(true) }
                        ) { Text("🚀 В City", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    } else {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = isClassicLoaded && !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            onClick = { saveClassicAllFilesAsync(false) }
                        ) { Text("💾 Сохранить всё", fontSize = 12.sp) }
                        Button(
                            modifier = Modifier.weight(1.2f),
                            enabled = isClassicLoaded && !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            onClick = { saveClassicAllFilesAsync(true) }
                        ) { Text("🚀 В Classic", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val _trigger = uiTrigger

            if (isCityMode) {
                // ==================== РЕЖИМ SUBWAY SURFERS CITY ====================
                when (selectedTab) {
                    // ВКЛАДКА 0: ВАЛЮТА CITY
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Твои текущие ресурсы в Subway Surfers City (сохраняются при переходе между вкладками):", fontSize = 12.sp, color = Color.Gray)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    pushCityUndo()
                                    GameDatabase.CURRENCIES.forEach { (tag, _) ->
                                        val c = currencyStates[tag]?.toLongOrNull() ?: 0L
                                        currencyStates[tag] = (c + 100000L).toString()
                                    }
                                    log("➕ Ко всем валютам добавлено +100 000")
                                }) { Text("+100K ко всем") }

                                Button(modifier = Modifier.weight(1f), onClick = {
                                    pushCityUndo()
                                    GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                                    log("💎 Все валюты выставлены на 9 999 999")
                                }) { Text("9.99M MAX") }
                            }
                            GameDatabase.CURRENCIES.forEach { (tag, label) ->
                                OutlinedTextField(
                                    value = currencyStates[tag] ?: "0",
                                    onValueChange = { currencyStates[tag] = it },
                                    label = { Text(label) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 1: ЕЖЕДНЕВНЫЕ/ЕЖЕНЕДЕЛЬНЫЕ ЗАДАНИЯ, СЕЗОННЫЙ ПРОПУСК И МЕГА-ЧИТЫ CITY
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎟️ Сезонный Пропуск (Season Pass) и Задания", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Здесь можно активировать платный Премиум Пропуск, начислить очки сезона для открытия всех ступеней и завершить все ежедневные и еженедельные задания.", fontSize = 12.sp, color = Color.Gray)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = seasonPurchased,
                                            onCheckedChange = {
                                                pushCityUndo()
                                                seasonPurchased = it
                                                profileJson?.let { prof -> CryptoAndSyncManager.toggleSinglePurchase(prof, "premium_pass", it) }
                                            }
                                        )
                                        Text("Разблокировать платный Premium Season Pass", fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedTextField(
                                        value = seasonPoints,
                                        onValueChange = { seasonPoints = it },
                                        label = { Text("Очки сезонного пропуска (15 000 открывает все награды)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(modifier = Modifier.weight(1f), onClick = {
                                            pushCityUndo()
                                            seasonPurchased = true
                                            seasonPoints = "15000"
                                            profileJson?.let { prof ->
                                                CryptoAndSyncManager.toggleSinglePurchase(prof, "premium_pass", true)
                                            }
                                            log("🎟️ Сезонный пропуск активирован + 15 000 очков!")
                                        }) { Text("🎟️ Открыть Весь Пропуск") }

                                        Button(modifier = Modifier.weight(1f), onClick = {
                                            pushCityUndo()
                                            profileJson?.let { prof ->
                                                CryptoAndSyncManager.completeAllDailyAndWeeklyMissions(prof)
                                                refreshUIFromCityProfile(prof)
                                                log("✅ Все Ежедневные и Еженедельные задания выполнены!")
                                            }
                                        }) { Text("✅ Выполнить Задания") }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Text("Активный сезон:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    GameDatabase.SEASONS.forEach { (sId, sName) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selectedSeasonId == sId,
                                                onClick = { pushCityUndo(); selectedSeasonId = sId }
                                            )
                                            Text(sName, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        onClick = {
                                            pushCityUndo()
                                            profileJson?.let {
                                                GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                                                CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                                CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                                                CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                                CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                                CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(it)
                                                CryptoAndSyncManager.hackDailyDealsAndAdRewards(it)
                                                CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(it)
                                                CryptoAndSyncManager.completeAllDailyAndWeeklyMissions(it)
                                                CryptoAndSyncManager.configurePiggyBank(it, "shop.piggybankmlp7.3", -2000200805L, 250000, true, true)
                                                refreshUIFromCityProfile(it)
                                                log("👑 РЕЖИМ БОГА АКТИВИРОВАН (можно отменить кнопкой Шаг назад)!")
                                            }
                                        }
                                    ) { Text("👑 АКТИВИРОВАТЬ РЕЖИМ БОГА (ВСЁ В 1 КЛИК)", color = Color.White, fontWeight = FontWeight.Bold) }
                                    Text("Открывает всех 27 сёрферов и 29 досок на 20 уровне (без накрутки их пробега), все 125 костюмов, 6 районов на 3 звезды, ивентовые триалы, ежедневные/еженедельные задания, сезонный пропуск и бесплатную копилку.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushCityUndo()
                                        profileJson?.let {
                                            CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(it)
                                            refreshUIFromCityProfile(it)
                                            log("🏁 Все ивентовые забеги (Триалы по 3 звезды) разблокированы!")
                                        }
                                    }) { Text("🏁 Открыть все Ивентовые Забеги и Триалы (3 ⭐)") }
                                    Text("Безопасно проходит все миссии ивентовых забегов на 3 звезды, разрешает бегать за любого сёрфера и сбрасывает таймеры ожидания.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushCityUndo()
                                        profileJson?.let {
                                            CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                                            CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(it)
                                            refreshUIFromCityProfile(it)
                                            log("🗺️ Все 6 районов на 3 звезды, Секретные Звёзды и 100% Джекпот активированы!")
                                        }
                                    }) { Text("🗺️ Пройти все 6 Районов (3 ⭐) + 100% Джекпот Сундуков") }
                                    Text("Открывает все районы города, выполняет все 196 этапов на 3 звезды, собирает секретные звёзды и выставляет счётчик гаранта сундуков на 9/10.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 2: ИНДИВИДУАЛЬНЫЙ РЕДАКТОР КАЖДОГО СЁРФЕРА В CITY
                    2 -> {
                        val filteredCitySurfers = citySurfersList.filter {
                            it.name.contains(citySearchQuery, ignoreCase = true)
                        }
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = citySearchQuery,
                                onValueChange = { citySearchQuery = it },
                                label = { Text("🔍 Поиск сёрфера City...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    pushCityUndo()
                                    profileJson?.let {
                                        applyUIToCityProfile(it)
                                        CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                        CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                        refreshUIFromCityProfile(it)
                                        log("🏄 Все 27 сёрферов открыты на 20 ур. (дистанция не тронута)!")
                                    }
                                }) { Text("🏄 Открыть всех (20 Ур.)", fontSize = 12.sp) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                itemsIndexed(filteredCitySurfers, key = { _, item -> item.id }) { _, surfer ->
                                    val realIndex = citySurfersList.indexOfFirst { it.id == surfer.id }
                                    if (realIndex != -1) {
                                        val item = citySurfersList[realIndex]
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        Text(
                                                            text = if (item.isUnlocked) "✅ ОТКРЫТ" else "🔒 ЗАКРЫТ",
                                                            color = if (item.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Switch(
                                                        checked = item.isUnlocked,
                                                        onCheckedChange = { checked ->
                                                            pushCityUndo()
                                                            citySurfersList[realIndex] = item.copy(isUnlocked = checked)
                                                        }
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedTextField(
                                                        value = item.level.toString(),
                                                        onValueChange = { v ->
                                                            val lvl = (v.toIntOrNull() ?: 1).coerceIn(1, 20)
                                                            citySurfersList[realIndex] = item.copy(level = lvl, isUnlocked = true)
                                                        },
                                                        label = { Text("Уровень (1-20)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = item.tierRank.toString(),
                                                        onValueChange = { v ->
                                                            val rk = (v.toIntOrNull() ?: 0).coerceIn(0, 40)
                                                            citySurfersList[realIndex] = item.copy(tierRank = rk)
                                                        },
                                                        label = { Text("Ранг опыта (0-40)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }

                                                OutlinedTextField(
                                                    value = item.highScore.toString(),
                                                    onValueChange = { v ->
                                                        val sc = v.toLongOrNull() ?: 0L
                                                        citySurfersList[realIndex] = item.copy(highScore = sc)
                                                    },
                                                    label = { Text("🏃 Пробег / Рекорд персонажа (highScore)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    OutlinedButton(onClick = {
                                                        pushCityUndo()
                                                        selectedSurferId = item.id
                                                        citySurfersList[realIndex] = item.copy(isUnlocked = true)
                                                        log("🏄 Активный сёрфер: ${item.name}")
                                                    }) {
                                                        Text(if (selectedSurferId == item.id) "⭐ Выбран" else "Взять в забег", fontSize = 11.sp)
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        TextButton(onClick = { pushCityUndo(); crewSlot1 = item.id; log("👥 ${item.name} -> Слот 1") }) { Text("Слот 1", fontSize = 11.sp) }
                                                        TextButton(onClick = { pushCityUndo(); crewSlot2 = item.id; log("👥 ${item.name} -> Слот 2") }) { Text("Слот 2", fontSize = 11.sp) }
                                                        TextButton(onClick = { pushCityUndo(); crewSlot3 = item.id; log("👥 ${item.name} -> Слот 3") }) { Text("Слот 3", fontSize = 11.sp) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ВКЛАДКА 3: ИНДИВИДУАЛЬНЫЙ РЕДАКТОР КАЖДОЙ ДОСКИ В CITY
                    3 -> {
                        val filteredCityBoards = cityBoardsList.filter {
                            it.name.contains(citySearchQuery, ignoreCase = true)
                        }
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = citySearchQuery,
                                onValueChange = { citySearchQuery = it },
                                label = { Text("🔍 Поиск доски City...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushCityUndo()
                                profileJson?.let {
                                    applyUIToCityProfile(it)
                                    CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                                    refreshUIFromCityProfile(it)
                                    log("🛹 Все 29 досок открыты и прокачаны до 20 уровня!")
                                }
                            }) { Text("🛹 Открыть все 29 досок (20 Ур.)") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                itemsIndexed(filteredCityBoards, key = { _, item -> item.id }) { _, board ->
                                    val realIndex = cityBoardsList.indexOfFirst { it.id == board.id }
                                    if (realIndex != -1) {
                                        val item = cityBoardsList[realIndex]
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        Text(
                                                            text = if (item.isUnlocked) "✅ ОТКРЫТА" else "🔒 ЗАКРЫТА",
                                                            color = if (item.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Switch(
                                                        checked = item.isUnlocked,
                                                        onCheckedChange = { checked ->
                                                            pushCityUndo()
                                                            cityBoardsList[realIndex] = item.copy(isUnlocked = checked)
                                                        }
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = item.level.toString(),
                                                        onValueChange = { v ->
                                                            val lvl = (v.toIntOrNull() ?: 1).coerceIn(1, 20)
                                                            cityBoardsList[realIndex] = item.copy(level = lvl, isUnlocked = true)
                                                        },
                                                        label = { Text("Уровень доски (1-20)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Button(onClick = {
                                                        pushCityUndo()
                                                        selectedBoardId = item.id
                                                        cityBoardsList[realIndex] = item.copy(isUnlocked = true)
                                                        log("🛹 Активная доска: ${item.name}")
                                                    }) {
                                                        Text(if (selectedBoardId == item.id) "⭐ Выбрана" else "Экипировать", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ВКЛАДКА 4: СВИНЬЯ-КОПИЛКА И ДОНАТ С ФИЛЬТРАМИ
                    4 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🐷 Единый Центр Управления Свиньёй-Копилкой", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedTextField(
                                        value = piggyProgress,
                                        onValueChange = { piggyProgress = it },
                                        label = { Text("Монет внутри копилки (до 250 000)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = piggyFilled, onCheckedChange = { piggyFilled = it })
                                        Text("Копилка полностью заполнена")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = piggyFreeClaim, onCheckedChange = { piggyFreeClaim = it })
                                        Text("Сделать открытие копилки БЕСПЛАТНЫМ")
                                    }
                                    GameDatabase.PIGGY_BANKS.forEach { (pId, pair) ->
                                        val (mId, desc) = pair
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selectedPiggyId == pId,
                                                onClick = { selectedPiggyId = pId; selectedPiggyMission = mId }
                                            )
                                            Text(desc, fontSize = 13.sp)
                                        }
                                    }
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushCityUndo()
                                        profileJson?.let {
                                            CryptoAndSyncManager.configurePiggyBank(it, selectedPiggyId, selectedPiggyMission, piggyProgress.toIntOrNull() ?: 250000, piggyFilled, piggyFreeClaim)
                                            refreshUIFromCityProfile(it)
                                            log("🐷 Свинья-копилка настроена!")
                                        }
                                    }) { Text("🐷 Применить настройки Свиньи-Копилки") }
                                }
                            }

                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushCityUndo()
                                profileJson?.let {
                                    CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                    refreshUIFromCityProfile(it)
                                    log("💳 Весь платный контент активирован!")
                                }
                            }) { Text("💳 Разблокировать ВЕСЬ Донат в 1 клик") }

                            val categories = listOf("Все", "Пропуска & Реклама", "Наборы героев", "Жетоны сёрферов", "Ресурсы & Бандлы", "Аватары")
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = iapFilterCategory == cat,
                                        onClick = { iapFilterCategory = cat },
                                        label = { Text(cat, fontSize = 12.sp) }
                                    )
                                }
                            }

                            val filteredIap = GameDatabase.IAP_CATALOG.filter { (id, _) ->
                                when (iapFilterCategory) {
                                    "Пропуска & Реклама" -> id.contains("premiumladder") || id.contains("remove.ads") || id.contains("premium_pass") || id.contains("piggybank")
                                    "Наборы героев" -> id.contains("outfit") || id.contains("starterpack") || id.contains("intermediatepack") || id.contains("expertpack") || id.contains("rosalita") || id.contains("classicboard")
                                    "Жетоны сёрферов" -> id.contains("tokenbundle")
                                    "Ресурсы & Бандлы" -> id.contains("megabundle") || id.contains("minibundle") || id.contains("combobundle") || id.contains("ticketbundle") || id.contains("revive_bundle")
                                    "Аватары" -> id.contains("avatars")
                                    else -> true
                                }
                            }

                            filteredIap.forEach { (productId, title) ->
                                val isChecked = iapCheckedStates[productId] == true
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                pushCityUndo()
                                                iapCheckedStates[productId] = checked
                                                profileJson?.let { CryptoAndSyncManager.toggleSinglePurchase(it, productId, checked) }
                                            }
                                        )
                                        Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 5: ПРОФИЛЬ ИГРОКА И БЭКАП (БЕЗ ГЛОБАЛЬНОГО МНОЖИТЕЛЯ И ФЕЙКОВОГО ТУРНИРА)
                    5 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📊 Профиль Игрока (из твоего сохранения)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм игрока") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = playerTag, onValueChange = { playerTag = it }, label = { Text("Сетевой тег профиля (Player Tag)") }, modifier = Modifier.fillMaxWidth())

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень аккаунта (1-50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = playerXp, onValueChange = { playerXp = it }, label = { Text("Опыт аккаунта (XP)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    }
                                    OutlinedTextField(value = boostedMultiplier, onValueChange = { boostedMultiplier = it }, label = { Text("Множитель буста забега") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📂 Резервное копирование", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                        scope.launch {
                                            val ok = withContext(Dispatchers.IO) { CryptoAndSyncManager.restoreBackup(activeProfilePath, ::log) }
                                            if (ok) {
                                                loadProfileAsync()
                                                log("⏪ Резервная копия с диска восстановлена!")
                                            } else {
                                                log("❌ Файл резервной копии не найден.")
                                            }
                                        }
                                    }) { Text("⏪ Откатить файл на диске из .bak_editor") }
                                }
                            }

                            Text("📜 Журнал операций:", fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Color.Black).padding(8.dp)) {
                                Text(text = logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            } else {
                // ==================== РЕЖИМ CLASSIC SUBWAY SURFERS ====================
                val st = classicState
                if (st == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Загрузка файлов Classic Subway Surfers...")
                    }
                } else {
                    when (selectedTab) {
                        // ВКЛАДКА 0: КОШЕЛЁК, ОТДЕЛЬНЫЕ ДВОЙНЫЕ МОНЕТЫ, БИЛЕТЫ РЕКЛАМЫ И ГОНОК
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))

                                // Блок Двойных Монет отдельно от прокачки бустеров!
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("🪙 Только Двойные Монеты (Double Coins x2)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("Включает удвоение монет навсегда БЕЗ прокачки способностей и бустеров.", fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Switch(
                                                checked = classicDoubleCoinsOnly,
                                                onCheckedChange = { enabled ->
                                                    pushClassicUndo()
                                                    classicDoubleCoinsOnly = enabled
                                                    ClassicCryptoAndSyncManager.setDoubleCoinsOnly(st, enabled)
                                                    log(if (enabled) "🪙 Двойные монеты включены (способности не тронуты)!" else "🪙 Двойные монеты отключены.")
                                                }
                                            )
                                        }

                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                                pushClassicUndo()
                                                ClassicCryptoAndSyncManager.maxPowerupLevelsAndScoreMultiplier(st)
                                                log("🚀 Способности (Джетпак, Кроссовки, Магнит, x2) прокачаны до 6 ур.!")
                                            }) { Text("🚀 Прокачать бустеры (6 Ур.)", fontSize = 11.sp) }

                                            OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                                pushClassicUndo()
                                                ClassicCryptoAndSyncManager.addOwnedProductsToWallet(
                                                    st.wallet,
                                                    ClassicDatabase.ONE_TIME_IAP_PRODUCTS.map { it.first }
                                                )
                                                log("🚫 Реклама отключена, весь донат добавлен!")
                                            }) { Text("🚫 Убрать рекламу + Донат", fontSize = 11.sp) }
                                        }
                                    }
                                }

                                Text("Твои ресурсы из `wallet.json` (При изменении Билетов пропуска рекламы или Билетов гонок автоматически прописывается покупка пакета, чтобы игра их сохранила):", fontSize = 12.sp, color = Color.Gray)

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(modifier = Modifier.weight(1f), onClick = {
                                        pushClassicUndo()
                                        ClassicDatabase.CURRENCIES.forEach { (id, _) ->
                                            val c = classicCurrencies[id]?.toLongOrNull() ?: 0L
                                            classicCurrencies[id] = (c + 100000L).toString()
                                        }
                                        applyUIToClassicState(st)
                                        log("➕ Ко всем ресурсам Classic добавлено +100 000")
                                    }) { Text("+100K ко всем") }

                                    Button(modifier = Modifier.weight(1f), onClick = {
                                        pushClassicUndo()
                                        ClassicDatabase.CURRENCIES.forEach { (id, _) -> classicCurrencies[id] = "99999999" }
                                        applyUIToClassicState(st)
                                        log("💎 Все валюты и билеты выставлены на 99 999 999!")
                                    }) { Text("💎 ВСЁ НА 99.9M") }
                                }

                                ClassicDatabase.CURRENCIES.forEach { (id, title) ->
                                    OutlinedTextField(
                                        value = classicCurrencies[id] ?: "0",
                                        onValueChange = { classicCurrencies[id] = it },
                                        label = { Text(title) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }

                        // ВКЛАДКА 1: ПРОХОЖДЕНИЕ ВСЕХ ИВЕНТОВ, АНАЛОГА ПРОПУСКА (SEASON HUNT) И РЕДАКТОР МЕДАЛЕЙ TOP RUN
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("🎟️ Прохождение Ивентов и Сезонного Пропуска (Season Hunt)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Завершает абсолютно все активные ивенты в игре (Trick or Treat, Floor is Lava, Tag Time Attack, Марафон, Subway Showdown, Gauntlet и Сезонную Охоту на 40 тиров), чтобы ты мог зайти и сразу забрать всех ивентовых персонажей, доски и ключи без просмотра рекламы!", fontSize = 12.sp, color = Color.Gray)

                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            onClick = {
                                                pushClassicUndo()
                                                ClassicCryptoAndSyncManager.completeAllEventsAndSeasonPassInMemory(st)
                                                uiTrigger++
                                                log("🏆 Все ивенты, испытания и все 40 ступеней Сезонной Охоты готовы к сбору наград!")
                                            }
                                        ) { Text("🏆 ПРОЙТИ ВСЕ ИВЕНТЫ И ОТКРЫТЬ ВСЕ НАГРАДЫ ПРОПУСКА", color = Color.White, fontWeight = FontWeight.Bold) }
                                    }
                                }

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🏅 Редактор Медалей Top Run (user_stats.json)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Укажи нужное количество медалей каждой лиги, отображаемых в твоём профиле Top Run:", fontSize = 12.sp, color = Color.Gray)

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = medalBronze,
                                                onValueChange = { medalBronze = it },
                                                label = { Text("🥉 Бронза") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = medalSilver,
                                                onValueChange = { medalSilver = it },
                                                label = { Text("🥈 Серебро") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = medalGold,
                                                onValueChange = { medalGold = it },
                                                label = { Text("🥇 Золото") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = medalDiamond,
                                                onValueChange = { medalDiamond = it },
                                                label = { Text("💎 Алмаз") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = medalChampion,
                                            onValueChange = { medalChampion = it },
                                            label = { Text("👑 Чемпион (Champion Medals)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(modifier = Modifier.weight(1f), onClick = {
                                                pushClassicUndo()
                                                applyUIToClassicState(st)
                                                log("🏅 Медали Top Run обновлены в памяти!")
                                            }) { Text("Применить медали") }

                                            OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                                pushClassicUndo()
                                                medalBronze = "100"; medalSilver = "100"; medalGold = "100"; medalDiamond = "100"; medalChampion = "100"
                                                applyUIToClassicState(st)
                                                log("🏅 Все медали Top Run выставлены по 100 шт.!")
                                            }) { Text("По 100 всех") }
                                        }
                                    }
                                }

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            onClick = {
                                                pushClassicUndo()
                                                ClassicDatabase.CURRENCIES.forEach { (id, _) -> classicCurrencies[id] = "99999999" }
                                                applyUIToClassicState(st)
                                                ClassicCryptoAndSyncManager.setDoubleCoinsOnly(st, true)
                                                ClassicCryptoAndSyncManager.maxPowerupLevelsAndScoreMultiplier(st)
                                                ClassicCryptoAndSyncManager.unlockAllClassicSurfersInMemory(st)
                                                ClassicCryptoAndSyncManager.unlockAllClassicBoardsInMemory(st)
                                                ClassicCryptoAndSyncManager.completeAllEventsAndSeasonPassInMemory(st)
                                                ClassicCryptoAndSyncManager.unlockAllPortraitsFramesAndTrophiesInMemory(st)
                                                refreshUIFromClassicState(st)
                                                log("👑 ПОЛНЫЙ GOD-MODE ДЛЯ CLASSIC ПРИМЕНЁН В ПАМЯТЬ!")
                                            }
                                        ) { Text("👑 АКТИВИРОВАТЬ ПОЛНЫЙ GOD-MODE (ВСЁ В 1 КЛИК)", color = Color.White, fontWeight = FontWeight.Bold) }
                                        Text("Открывает всех 275 сёрферов, 297 досок, 114 портретов, 56 рамок, проходит все ивенты и ставит 99.9 млн ресурсов (можно отменить кнопкой Шаг назад).", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }

                        // ВКЛАДКА 2: СЁРФЕРЫ CLASSIC С НАДПИСЬЮ ОТКРЫТО / ЗАКРЫТО И ВЫБОРОМ КОСТЮМОВ
                        2 -> {
                            val filteredSurfers = remember(classicSearchQuery, uiTrigger) {
                                ClassicDatabase.CLASSIC_SURFERS.filter {
                                    it.first.contains(classicSearchQuery, ignoreCase = true) ||
                                    it.second.first.contains(classicSearchQuery, ignoreCase = true)
                                }
                            }
                            val currentSelectedChar = st.chars.optJSONObject("selected")?.optString("character", "jake") ?: "jake"

                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = classicSearchQuery,
                                    onValueChange = { classicSearchQuery = it },
                                    label = { Text("🔍 Поиск сёрфера (${filteredSurfers.size} из ${ClassicDatabase.CLASSIC_SURFERS.size})...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    pushClassicUndo()
                                    ClassicCryptoAndSyncManager.unlockAllClassicSurfersInMemory(st)
                                    uiTrigger++
                                    log("🏄 Все 275 сёрферов и их костюмы отмечены ОТКРЫТЫМИ!")
                                }) { Text("🏄 Открыть абсолютно ВСЕХ 275 сёрферов и костюмы") }
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
                                    items(filteredSurfers, key = { it.first }) { (id, pair) ->
                                        val (name, outfits) = pair
                                        val isUnlocked = ClassicCryptoAndSyncManager.isSurferUnlocked(st, id)
                                        val ownedOutfits = ClassicCryptoAndSyncManager.getOwnedOutfitsForSurfer(st, id)
                                        val isExpanded = expandedClassicSurferId == id

                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(
                                                            text = if (isUnlocked) "✅ ОТКРЫТ (Костюмов: ${ownedOutfits.size}/${outfits.size})" else "🔒 ЗАКРЫТ",
                                                            color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Switch(
                                                            checked = isUnlocked,
                                                            onCheckedChange = { checked ->
                                                                pushClassicUndo()
                                                                ClassicCryptoAndSyncManager.toggleSurferUnlock(st, id, checked, outfits)
                                                                uiTrigger++
                                                            }
                                                        )
                                                        Button(
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            onClick = {
                                                                pushClassicUndo()
                                                                ClassicCryptoAndSyncManager.toggleSurferUnlock(st, id, true, outfits)
                                                                ClassicCryptoAndSyncManager.selectSurfer(st, id, "default")
                                                                uiTrigger++
                                                                log("🏄 Выбран сёрфер: $name")
                                                            }
                                                        ) {
                                                            Text(if (currentSelectedChar == id) "⭐ Взят" else "Выбрать", fontSize = 11.sp)
                                                        }
                                                    }
                                                }

                                                if (outfits.size > 1) {
                                                    TextButton(
                                                        contentPadding = PaddingValues(0.dp),
                                                        onClick = { expandedClassicSurferId = if (isExpanded) null else id }
                                                    ) {
                                                        Text(if (isExpanded) "🔼 Скрыть костюмы" else "🔽 Настроить костюмы (${outfits.size} шт.)", fontSize = 11.sp)
                                                    }
                                                    if (isExpanded) {
                                                        outfits.forEach { outfitId ->
                                                            val hasOutfit = ownedOutfits.contains(outfitId)
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Checkbox(
                                                                    checked = hasOutfit,
                                                                    onCheckedChange = { checked ->
                                                                        pushClassicUndo()
                                                                        ClassicCryptoAndSyncManager.toggleSurferOutfit(st, id, outfitId, checked)
                                                                        uiTrigger++
                                                                    }
                                                                )
                                                                Text("Костюм: $outfitId (${if (hasOutfit) "Открыт" else "Закрыт"})", fontSize = 12.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 3: ДОСКИ CLASSIC С НАДПИСЬЮ ОТКРЫТО / ЗАКРЫТО И СПОСОБНОСТЯМИ
                        3 -> {
                            val filteredBoards = remember(classicSearchQuery, uiTrigger) {
                                ClassicDatabase.CLASSIC_BOARDS.filter {
                                    it.first.contains(classicSearchQuery, ignoreCase = true) ||
                                    it.second.first.contains(classicSearchQuery, ignoreCase = true)
                                }
                            }
                            val currentSelectedBoard = st.boards.optString("selected", "default")

                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = classicSearchQuery,
                                    onValueChange = { classicSearchQuery = it },
                                    label = { Text("🔍 Поиск ховерборда (${filteredBoards.size} из ${ClassicDatabase.CLASSIC_BOARDS.size})...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    pushClassicUndo()
                                    ClassicCryptoAndSyncManager.unlockAllClassicBoardsInMemory(st)
                                    uiTrigger++
                                    log("🛹 Все 297 досок и их способности отмечены ОТКРЫТЫМИ!")
                                }) { Text("🛹 Открыть абсолютно ВСЕ 297 досок и способности") }
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
                                    items(filteredBoards, key = { it.first }) { (id, pair) ->
                                        val (name, upgrades) = pair
                                        val isUnlocked = ClassicCryptoAndSyncManager.isBoardUnlocked(st, id)
                                        val ownedUpgrades = ClassicCryptoAndSyncManager.getOwnedUpgradesForBoard(st, id)
                                        val isExpanded = expandedClassicBoardId == id

                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(
                                                            text = if (isUnlocked) "✅ ОТКРЫТА (Апгрейдов: ${ownedUpgrades.size}/${upgrades.size})" else "🔒 ЗАКРЫТА",
                                                            color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Switch(
                                                            checked = isUnlocked,
                                                            onCheckedChange = { checked ->
                                                                pushClassicUndo()
                                                                ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, checked, upgrades)
                                                                uiTrigger++
                                                            }
                                                        )
                                                        Button(
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            onClick = {
                                                                pushClassicUndo()
                                                                ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, true, upgrades)
                                                                ClassicCryptoAndSyncManager.selectBoard(st, id)
                                                                uiTrigger++
                                                                log("🛹 Выбрана доска: $name")
                                                            }
                                                        ) {
                                                            Text(if (currentSelectedBoard == id) "⭐ Взята" else "Выбрать", fontSize = 11.sp)
                                                        }
                                                    }
                                                }

                                                if (upgrades.isNotEmpty()) {
                                                    TextButton(
                                                        contentPadding = PaddingValues(0.dp),
                                                        onClick = { expandedClassicBoardId = if (isExpanded) null else id }
                                                    ) {
                                                        Text(if (isExpanded) "🔼 Скрыть способности" else "🔽 Настроить способности (${upgrades.size} шт.)", fontSize = 11.sp)
                                                    }
                                                    if (isExpanded) {
                                                        upgrades.forEach { upgId ->
                                                            val hasUpg = ownedUpgrades.contains(upgId)
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Checkbox(
                                                                    checked = hasUpg,
                                                                    onCheckedChange = { checked ->
                                                                        pushClassicUndo()
                                                                        ClassicCryptoAndSyncManager.toggleBoardUpgrade(st, id, upgId, checked)
                                                                        uiTrigger++
                                                                    }
                                                                )
                                                                Text("Способность: $upgId (${if (hasUpg) "Открыта" else "Закрыта"})", fontSize = 12.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 4: СУНДУКИ И КОСМЕТИКА ПРОФИЛЯ CLASSIC
                        4 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🎁 Выдача Сундуков (Lootboxes) в очередь", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        OutlinedTextField(
                                            value = classicLootboxCount,
                                            onValueChange = { classicLootboxCount = it },
                                            label = { Text("Количество коробок для выдачи") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        ClassicDatabase.LOOTBOX_TYPES.forEach { (bId, bTitle) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = selectedClassicLootbox == bId, onClick = { selectedClassicLootbox = bId })
                                                Text(bTitle, fontSize = 13.sp)
                                            }
                                        }
                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            pushClassicUndo()
                                            val cnt = classicLootboxCount.toIntOrNull() ?: 10
                                            ClassicCryptoAndSyncManager.addLootboxesToQueueInMemory(st, selectedClassicLootbox, cnt)
                                            log("🎁 Добавлено $cnt сундуков в очередь (нажми 💾 Сохранить всё)!")
                                        }) { Text("🎁 Добавить сундуки в очередь") }
                                    }
                                }

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("🖼️ Косметика Профиля, Трофеи и Достижения", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            pushClassicUndo()
                                            ClassicCryptoAndSyncManager.unlockAllPortraitsFramesAndTrophiesInMemory(st)
                                            log("🖼️ Все 114 портретов, 56 рамок, трофеи и достижения разблокированы в памяти!")
                                        }) { Text("🖼️ Открыть все 114 Портретов, 56 Рамок, Трофеи и Ачивки") }
                                    }
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
