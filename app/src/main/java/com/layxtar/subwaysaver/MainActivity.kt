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

private enum class GameMode { CITY, CLASSIC, PVZ2 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltimateEditorApp(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    var gameMode by remember { mutableStateOf(GameMode.CITY) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("🔄 Готов к работе...") }
    var logs by remember { mutableStateOf("🚀 Старт Ultimate Multi-Game Editor...\n") }

    fun log(msg: String) {
        statusText = msg
        logs = "$msg\n$logs"
    }

    var uiTrigger by remember { mutableIntStateOf(0) }

    // ==================== 1. СОСТОЯНИЯ SUBWAY SURFERS CITY ====================
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

    // ==================== 2. СОСТОЯНИЯ CLASSIC SUBWAY SURFERS ====================
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

    // ==================== 3. СОСТОЯНИЯ PLANTS VS. ZOMBIES 2 ====================
    var pvzActiveCdnDir by remember {
        mutableStateOf("/storage/emulated/0/Android/data/com.ea.game.pvz2_row/files/No_Backup/CDN.13.5")
    }
    var pvzDetectedCdnList by remember { mutableStateOf<List<String>>(emptyList()) }
    var pvzPpDatJson by remember { mutableStateOf<JSONObject?>(null) }
    val pvzCurrencies = remember {
        mutableStateMapOf<String, String>().apply {
            Pvz2Database.CURRENCIES.forEach { (k, _) -> put(k, "0") }
        }
    }
    var pvzPlantSearch by remember { mutableStateOf("") }
    var pvzSelectedPlantId by remember { mutableStateOf(Pvz2Database.PLANTS.first().first) }
    var pvzSelectedSeedAmount by remember { mutableIntStateOf(1000000) }
    var pvzCustomSeedInput by remember { mutableStateOf("1000000") }
    var pvzUnlockAllPlantsInShop by remember { mutableStateOf(true) }
    var pvzTargetPlantLevel by remember { mutableStateOf("20") }
    var pvzTargetMastery by remember { mutableStateOf("200") }
    var pvzPinataMultiplier by remember { mutableStateOf("100") }
    var pvzPinataMinSeeds by remember { mutableStateOf("1000") }

    // ==================== СИНХРОНИЗАЦИЯ ПАМЯТИ CITY ====================

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

        if (citySurfersList.isNotEmpty()) CryptoAndSyncManager.applyCitySurfers(prof, citySurfersList)
        if (cityBoardsList.isNotEmpty()) CryptoAndSyncManager.applyCityBoards(prof, cityBoardsList)
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
        log("↩️ Отменено последнее изменение в City")
    }

    // ==================== СИНХРОНИЗАЦИЯ ПАМЯТИ CLASSIC ====================

    fun applyUIToClassicState(st: ClassicMemoryState) {
        for ((id, valStr) in classicCurrencies) {
            val amount = valStr.toLongOrNull() ?: 0L
            ClassicCryptoAndSyncManager.setCurrencyInMemory(st.wallet, id, amount)
        }
        // Исправленные ключи медалей по твоему скриншоту: 105=Бронза, 106=Серебро, 107=Золото, 108=Алмаз, 102=Чемпион
        val medalsMap = mapOf(
            "105" to (medalBronze.toIntOrNull() ?: 0),
            "106" to (medalSilver.toIntOrNull() ?: 0),
            "107" to (medalGold.toIntOrNull() ?: 0),
            "108" to (medalDiamond.toIntOrNull() ?: 0),
            "102" to (medalChampion.toIntOrNull() ?: 0)
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
        medalBronze = (medals["105"] ?: 0).toString()
        medalSilver = (medals["106"] ?: 0).toString()
        medalGold = (medals["107"] ?: 0).toString()
        medalDiamond = (medals["108"] ?: 0).toString()
        medalChampion = (medals["102"] ?: 0).toString()
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
        log("↩️ Отменено последнее изменение в Classic")
    }

    // ==================== АВТОЗАГРУЗКА ДЛЯ ВЫБРАННОЙ ИГРЫ ====================

    fun loadActiveGameAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, ::log)
            return
        }
        scope.launch {
            isBusy = true
            when (gameMode) {
                GameMode.CITY -> {
                    statusText = "⏳ Чтение профиля Subway City..."
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
                        log("✅ Subway City загружен ($playerName, Ур: $playerLevel)!")
                    } else {
                        log("❌ Профиль Subway City не найден.")
                    }
                }

                GameMode.CLASSIC -> {
                    statusText = "⏳ Чтение файлов Classic Subway Surfers..."
                    val loadedClassic = withContext(Dispatchers.IO) {
                        ClassicCryptoAndSyncManager.loadFullClassicState {}
                    }
                    classicState = loadedClassic
                    classicUndoStack.clear()
                    refreshUIFromClassicState(loadedClassic)
                    isClassicLoaded = true
                    log("✅ Classic загружен (Монет: ${classicCurrencies["1"]}, Ключей: ${classicCurrencies["2"]})!")
                }

                GameMode.PVZ2 -> {
                    statusText = "⏳ Поиск актуальной папки CDN.* и чтение pp.dat..."
                    val (cdn, allCdns, pp) = withContext(Dispatchers.IO) {
                        val active = Pvz2Manager.resolveActiveCdnDir {}
                        val list = Pvz2Manager.listAllCdnDirs()
                        val ppObj = Pvz2Manager.loadPlayerSavePpDat {}
                        Triple(active, list, ppObj)
                    }
                    pvzActiveCdnDir = cdn
                    pvzDetectedCdnList = allCdns
                    pvzPpDatJson = pp
                    if (pp != null) {
                        val curMap = Pvz2Manager.readPpDatCurrencies(pp)
                        for ((k, v) in curMap) pvzCurrencies[k] = v.toString()
                        log("🌻 PvZ 2 готов! Активный кэш: ${cdn.substringAfterLast("/")}")
                    } else {
                        log("🌻 Кэш PvZ 2 найден: ${cdn.substringAfterLast("/")} (готов к патчингу RTON!)")
                    }
                }
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
                log("✅ Все изменения Subway City сохранены!")
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
            statusText = "💾 Шифрование и запись файлов Classic..."
            val ok = withContext(Dispatchers.IO) {
                ClassicCryptoAndSyncManager.saveFullClassicState(st) {}
            }
            if (ok) {
                log("✅ Все изменения Classic успешно записаны!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, ClassicCryptoAndSyncManager.PKG_CLASSIC, ::log)
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        CryptoAndSyncManager.onPermissionGrantedCallback = { granted ->
            if (granted) loadActiveGameAsync()
        }
        loadActiveGameAsync()
    }

    val canUndo = when (gameMode) {
        GameMode.CITY -> cityUndoStack.isNotEmpty()
        GameMode.CLASSIC -> classicUndoStack.isNotEmpty()
        GameMode.PVZ2 -> false
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Ultimate Game Editor", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (gameMode != GameMode.PVZ2) {
                                OutlinedButton(
                                    enabled = canUndo && !isBusy,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    onClick = { if (gameMode == GameMode.CITY) performCityUndo() else performClassicUndo() }
                                ) { Text("↩️ Назад", fontSize = 11.sp) }
                            }

                            Button(
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                onClick = {
                                    gameMode = GameMode.CITY; selectedTab = 0
                                    if (!isCityLoaded) loadActiveGameAsync()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (gameMode == GameMode.CITY) Color(0xFF1976D2) else Color.DarkGray)
                            ) { Text("🏙️ CITY", fontSize = 11.sp) }

                            Button(
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                onClick = {
                                    gameMode = GameMode.CLASSIC; selectedTab = 0
                                    if (!isClassicLoaded) loadActiveGameAsync()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (gameMode == GameMode.CLASSIC) Color(0xFFE65100) else Color.DarkGray)
                            ) { Text("🛹 CLASSIC", fontSize = 11.sp) }

                            Button(
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                onClick = {
                                    gameMode = GameMode.PVZ2; selectedTab = 0
                                    loadActiveGameAsync()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (gameMode == GameMode.PVZ2) Color(0xFF2E7D32) else Color.DarkGray)
                            ) { Text("🌻 PVZ 2", fontSize = 11.sp) }
                        }
                    }
                )
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = when (gameMode) {
                        GameMode.CITY -> listOf("💰 Валюта", "⚡ Задания & Пропуск", "🏄 Сёрферы", "🛹 Доски", "🐷 Копилка & Донат", "📊 Профиль")
                        GameMode.CLASSIC -> listOf("💰 Кошелёк & Билеты", "🏆 Ивенты & Медали", "🏄 Сёрферы", "🛹 Доски", "🎁 Сундуки")
                        GameMode.PVZ2 -> listOf("🛒 Магазин за 1🪙 & Семена", "🪅 Пиньяты & Моды RTON", "💰 Профиль pp.dat", "📂 Путь CDN & Откат")
                    }
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                if (gameMode == GameMode.CITY) profileJson?.let { applyUIToCityProfile(it) }
                                if (gameMode == GameMode.CLASSIC) classicState?.let { applyUIToClassicState(it) }
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
                    OutlinedButton(modifier = Modifier.weight(0.9f), enabled = !isBusy, onClick = { loadActiveGameAsync() }) {
                        Text("🔄 Обновить", fontSize = 12.sp)
                    }
                    when (gameMode) {
                        GameMode.CITY -> {
                            Button(modifier = Modifier.weight(1f), enabled = isCityLoaded && !isBusy, onClick = { saveCityProfileAsync(false) }) {
                                Text("💾 Сохранить", fontSize = 12.sp)
                            }
                            Button(
                                modifier = Modifier.weight(1.2f),
                                enabled = isCityLoaded && !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                onClick = { saveCityProfileAsync(true) }
                            ) { Text("🚀 В City", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                        GameMode.CLASSIC -> {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = isClassicLoaded && !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                onClick = { saveClassicAllFilesAsync(false) }
                            ) { Text("💾 Сохранить", fontSize = 12.sp) }
                            Button(
                                modifier = Modifier.weight(1.2f),
                                enabled = isClassicLoaded && !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                onClick = { saveClassicAllFilesAsync(true) }
                            ) { Text("🚀 В Classic", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                        GameMode.PVZ2 -> {
                            Button(
                                modifier = Modifier.weight(1.4f),
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                onClick = {
                                    val (pkg, _) = Pvz2Manager.resolveNoBackupDir {}
                                    CryptoAndSyncManager.launchGame(activity, pkg, ::log)
                                }
                            ) { Text("🚀 Запустить PvZ 2", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val _trigger = uiTrigger

            when (gameMode) {
                // ==================== 1. РЕЖИМ SUBWAY SURFERS CITY ====================
                GameMode.CITY -> {
                    when (selectedTab) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
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

                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🎟️ Сезонный Пропуск и Задания", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = seasonPurchased,
                                                onCheckedChange = {
                                                    pushCityUndo()
                                                    seasonPurchased = it
                                                    profileJson?.let { prof -> CryptoAndSyncManager.toggleSinglePurchase(prof, "premium_pass", it) }
                                                }
                                            )
                                            Text("Разблокировать платный Premium Season Pass")
                                        }
                                        OutlinedTextField(
                                            value = seasonPoints,
                                            onValueChange = { seasonPoints = it },
                                            label = { Text("Очки сезонного пропуска (15 000 = все тиры)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(modifier = Modifier.weight(1f), onClick = {
                                                pushCityUndo()
                                                seasonPurchased = true
                                                seasonPoints = "15000"
                                                profileJson?.let { prof -> CryptoAndSyncManager.toggleSinglePurchase(prof, "premium_pass", true) }
                                                log("🎟️ Сезонный пропуск активирован + 15 000 очков!")
                                            }) { Text("🎟️ Открыть Пропуск") }

                                            Button(modifier = Modifier.weight(1f), onClick = {
                                                pushCityUndo()
                                                profileJson?.let { prof ->
                                                    CryptoAndSyncManager.completeAllDailyAndWeeklyMissions(prof)
                                                    refreshUIFromCityProfile(prof)
                                                    log("✅ Все Ежедневные и Еженедельные задания выполнены!")
                                                }
                                            }) { Text("✅ Выполнить Задания") }
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
                                                    log("👑 РЕЖИМ БОГА АКТИВИРОВАН!")
                                                }
                                            }
                                        ) { Text("👑 АКТИВИРОВАТЬ РЕЖИМ БОГА (ВСЁ В 1 КЛИК)", color = Color.White, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }

                        // ВКЛАДКА 2: СЁРФЕРЫ CITY (БЕЗ РАНГА ОПЫТА, ТОЛЬКО УРОВЕНЬ И ПРОБЕГ)
                        2 -> {
                            val filteredCitySurfers = citySurfersList.filter { it.name.contains(citySearchQuery, ignoreCase = true) }
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = citySearchQuery,
                                    onValueChange = { citySearchQuery = it },
                                    label = { Text("🔍 Поиск сёрфера City...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    pushCityUndo()
                                    profileJson?.let {
                                        applyUIToCityProfile(it)
                                        CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                        CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                        refreshUIFromCityProfile(it)
                                        log("🏄 Все 27 сёрферов открыты на 20 ур.!")
                                    }
                                }) { Text("🏄 Открыть всех 27 сёрферов (20 Ур.)") }
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
                                                            modifier = Modifier.weight(0.9f)
                                                        )
                                                        OutlinedTextField(
                                                            value = item.highScore.toString(),
                                                            onValueChange = { v ->
                                                                val sc = v.toLongOrNull() ?: 0L
                                                                citySurfersList[realIndex] = item.copy(highScore = sc)
                                                            },
                                                            label = { Text("🏃 Рекорд / Пробег") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.weight(1.3f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            val filteredCityBoards = cityBoardsList.filter { it.name.contains(citySearchQuery, ignoreCase = true) }
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = citySearchQuery,
                                    onValueChange = { citySearchQuery = it },
                                    label = { Text("🔍 Поиск доски City...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                                    OutlinedTextField(
                                                        value = item.level.toString(),
                                                        onValueChange = { v ->
                                                            val lvl = (v.toIntOrNull() ?: 1).coerceIn(1, 20)
                                                            cityBoardsList[realIndex] = item.copy(level = lvl, isUnlocked = true)
                                                        },
                                                        label = { Text("Уровень доски (1-20)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🐷 Свинья-Копилка", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        OutlinedTextField(value = piggyProgress, onValueChange = { piggyProgress = it }, label = { Text("Монет внутри копилки") }, modifier = Modifier.fillMaxWidth())
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = piggyFreeClaim, onCheckedChange = { piggyFreeClaim = it })
                                            Text("Сделать открытие копилки БЕСПЛАТНЫМ")
                                        }
                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            pushCityUndo()
                                            profileJson?.let {
                                                CryptoAndSyncManager.configurePiggyBank(it, selectedPiggyId, selectedPiggyMission, piggyProgress.toIntOrNull() ?: 250000, true, piggyFreeClaim)
                                                refreshUIFromCityProfile(it)
                                                log("🐷 Свинья-копилка настроена!")
                                            }
                                        }) { Text("🐷 Применить Свинью-Копилку") }
                                    }
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }

                        5 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм игрока") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень аккаунта (1-50)") }, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }
                    }
                }

                // ==================== 2. РЕЖИМ CLASSIC SUBWAY SURFERS ====================
                GameMode.CLASSIC -> {
                    val st = classicState
                    if (st != null) {
                        when (selectedTab) {
                            0 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("🪙 Только Двойные Монеты (Double Coins)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("Включает удвоение монет БЕЗ прокачки бустеров.", fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Switch(
                                                    checked = classicDoubleCoinsOnly,
                                                    onCheckedChange = { enabled ->
                                                        pushClassicUndo()
                                                        classicDoubleCoinsOnly = enabled
                                                        ClassicCryptoAndSyncManager.setDoubleCoinsOnly(st, enabled)
                                                        log("🪙 Двойные монеты: $enabled")
                                                    }
                                                )
                                            }
                                            Divider()
                                            Text("Пакетная выдача билетов (с автоматической активацией донатного пакета):", fontSize = 12.sp, color = Color.Gray)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(modifier = Modifier.weight(1f), onClick = {
                                                    pushClassicUndo()
                                                    classicCurrencies["11"] = "250"
                                                    ClassicCryptoAndSyncManager.setCurrencyInMemory(st.wallet, "11", 250L)
                                                    log("🎟️ Выдано 250 Билетов пропуска рекламы + пакет SkipAds!")
                                                }) { Text("🎟️ +250 SkipAd Билетов", fontSize = 11.sp) }

                                                Button(modifier = Modifier.weight(1f), onClick = {
                                                    pushClassicUndo()
                                                    classicCurrencies["12"] = "500"
                                                    ClassicCryptoAndSyncManager.setCurrencyInMemory(st.wallet, "12", 500L)
                                                    log("🏁 Выдано 500 Билетов забегов / Токенов!")
                                                }) { Text("🏁 +500 PvP Билетов", fontSize = 11.sp) }
                                            }
                                        }
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

                            1 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("🎟️ Безопасное открытие Сезонной Охоты и Ивентов", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Открывает все 40 наград Season Hunt и текущие этапы ивентов без зависания вкладки Событий!", fontSize = 12.sp, color = Color.Gray)
                                            Button(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                onClick = {
                                                    pushClassicUndo()
                                                    ClassicCryptoAndSyncManager.completeAllEventsAndSeasonPassInMemory(st)
                                                    log("🏆 Сезонная охота и активные ивенты открыты для сбора наград!")
                                                }
                                            ) { Text("🏆 Открыть все награды Сезонной Охоты и Ивентов") }
                                        }
                                    }

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("🏅 Точный Редактор Медалей Top Run", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Исправлены ID всех 5 медалей (включая Медаль Чемпиона, где раньше стояло 2147483647):", fontSize = 12.sp, color = Color.Gray)

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(value = medalBronze, onValueChange = { medalBronze = it }, label = { Text("🥉 Бронза") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                                OutlinedTextField(value = medalSilver, onValueChange = { medalSilver = it }, label = { Text("🥈 Серебро") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(value = medalGold, onValueChange = { medalGold = it }, label = { Text("🥇 Золото") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                                OutlinedTextField(value = medalDiamond, onValueChange = { medalDiamond = it }, label = { Text("💎 Алмаз") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                            }
                                            OutlinedTextField(
                                                value = medalChampion,
                                                onValueChange = { medalChampion = it },
                                                label = { Text("👑 Чемпион (впиши адекватное число, например 50)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                                pushClassicUndo()
                                                applyUIToClassicState(st)
                                                log("🏅 Медали Top Run (включая Чемпиона = $medalChampion) обновлены!")
                                            }) { Text("🏅 Применить медали Top Run") }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(90.dp))
                                }
                            }

                            2 -> {
                                val filteredSurfers = remember(classicSearchQuery, uiTrigger) {
                                    ClassicDatabase.CLASSIC_SURFERS.filter {
                                        it.first.contains(classicSearchQuery, ignoreCase = true) ||
                                        it.second.first.contains(classicSearchQuery, ignoreCase = true)
                                    }
                                }
                                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(value = classicSearchQuery, onValueChange = { classicSearchQuery = it }, label = { Text("🔍 Поиск сёрфера...") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushClassicUndo()
                                        ClassicCryptoAndSyncManager.unlockAllClassicSurfersInMemory(st)
                                        uiTrigger++
                                        log("🏄 Все 275 сёрферов открыты!")
                                    }) { Text("🏄 Открыть всех 275 сёрферов") }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                                        items(filteredSurfers, key = { it.first }) { (id, pair) ->
                                            val (name, outfits) = pair
                                            val isUnlocked = ClassicCryptoAndSyncManager.isSurferUnlocked(st, id)
                                            Card(modifier = Modifier.fillMaxWidth()) {
                                                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(
                                                            text = if (isUnlocked) "✅ ОТКРЫТ" else "🔒 ЗАКРЫТ",
                                                            color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Switch(
                                                        checked = isUnlocked,
                                                        onCheckedChange = { checked ->
                                                            pushClassicUndo()
                                                            ClassicCryptoAndSyncManager.toggleSurferUnlock(st, id, checked, outfits)
                                                            uiTrigger++
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                val filteredBoards = remember(classicSearchQuery, uiTrigger) {
                                    ClassicDatabase.CLASSIC_BOARDS.filter {
                                        it.first.contains(classicSearchQuery, ignoreCase = true) ||
                                        it.second.first.contains(classicSearchQuery, ignoreCase = true)
                                    }
                                }
                                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(value = classicSearchQuery, onValueChange = { classicSearchQuery = it }, label = { Text("🔍 Поиск доски...") }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushClassicUndo()
                                        ClassicCryptoAndSyncManager.unlockAllClassicBoardsInMemory(st)
                                        uiTrigger++
                                        log("🛹 Все 297 досок открыты!")
                                    }) { Text("🛹 Открыть все 297 досок") }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                                        items(filteredBoards, key = { it.first }) { (id, pair) ->
                                            val (name, upgrades) = pair
                                            val isUnlocked = ClassicCryptoAndSyncManager.isBoardUnlocked(st, id)
                                            Card(modifier = Modifier.fillMaxWidth()) {
                                                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(
                                                            text = if (isUnlocked) "✅ ОТКРЫТА" else "🔒 ЗАКРЫТА",
                                                            color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Switch(
                                                        checked = isUnlocked,
                                                        onCheckedChange = { checked ->
                                                            pushClassicUndo()
                                                            ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, checked, upgrades)
                                                            uiTrigger++
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("🎁 Выдача Сундуков и Косметики", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            OutlinedTextField(value = classicLootboxCount, onValueChange = { classicLootboxCount = it }, label = { Text("Количество боксов") }, modifier = Modifier.fillMaxWidth())
                                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                                pushClassicUndo()
                                                val cnt = classicLootboxCount.toIntOrNull() ?: 10
                                                ClassicCryptoAndSyncManager.addLootboxesToQueueInMemory(st, selectedClassicLootbox, cnt)
                                                log("🎁 Добавлено $cnt сундуков в очередь!")
                                            }) { Text("🎁 Выдать Super Mystery Box в очередь") }

                                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                                pushClassicUndo()
                                                ClassicCryptoAndSyncManager.unlockAllPortraitsFramesAndTrophiesInMemory(st)
                                                log("🖼️ Все 114 портретов, 56 рамок и трофеи открыты!")
                                            }) { Text("🖼️ Открыть все 114 Портретов, 56 Рамок и Трофеи") }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(90.dp))
                                }
                            }
                        }
                    }
                }

                // ==================== 3. РЕЖИМ PLANTS VS. ZOMBIES 2 (RTON & PP.DAT) ====================
                GameMode.PVZ2 -> {
                    when (selectedTab) {
                        // ВКЛАДКА 0: МАГАЗИН «ВСЁ ЗА 1 МОНЕТУ» И ГЕНЕРАТОР СЕМЯН ОТ 1 ДО 1 000 000
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🛒 Патчер Магазина PvZ 2: Всё по 1 Монете + Свои Семена", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Декодирует `products.rton`, `market_schedule.rton` и `market_layout.rton` из папки `${pvzActiveCdnDir.substringAfterLast("/")}`, ставит всем растениям и скинам цену в 1 монету и добавляет пакеты семян в выбранном количестве (от 1 до 1 000 000 шт.).", fontSize = 12.sp, color = Color.Gray)

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = pvzUnlockAllPlantsInShop, onCheckedChange = { pvzUnlockAllPlantsInShop = it })
                                            Text("Добавить в магазин разблокировку всех растений и скинов за 1 монету")
                                        }

                                        Text("Выбери количество семян в пакете за 1 монету:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Pvz2Database.SEED_PACKET_TIERS.forEach { (amount, desc) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = pvzSelectedSeedAmount == amount,
                                                    onClick = {
                                                        pvzSelectedSeedAmount = amount
                                                        pvzCustomSeedInput = amount.toString()
                                                    }
                                                )
                                                Text(desc, fontSize = 13.sp)
                                            }
                                        }

                                        OutlinedTextField(
                                            value = pvzCustomSeedInput,
                                            onValueChange = { pvzCustomSeedInput = it },
                                            label = { Text("Или введи своё точное количество семян (1 .. 1 000 000)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            onClick = {
                                                scope.launch {
                                                    isBusy = true
                                                    val seeds = (pvzCustomSeedInput.toIntOrNull() ?: pvzSelectedSeedAmount).coerceIn(1, 1000000)
                                                    withContext(Dispatchers.IO) {
                                                        Pvz2Manager.patchShopAllFor1CoinAndInjectSeeds(
                                                            cdnDir = pvzActiveCdnDir,
                                                            selectedPlantId = pvzSelectedPlantId,
                                                            seedAmount = seeds,
                                                            unlockAllPlantsAndSkinsInShop = pvzUnlockAllPlantsInShop,
                                                            injectCustomGodBundles = true,
                                                            log = ::log
                                                        )
                                                    }
                                                    isBusy = false
                                                }
                                            }
                                        ) { Text("🛒 ПРОПАТЧИТЬ МАГАЗИН (ВСЁ ПО 1 МОНЕТЕ + СЕМЕНА)", color = Color.White, fontWeight = FontWeight.Bold) }

                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        OutlinedTextField(
                                            value = pvzPlantSearch,
                                            onValueChange = { pvzPlantSearch = it },
                                            label = { Text("🔍 Выбери растение для целевого пакета семян...") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        val filteredPlants = Pvz2Database.PLANTS.filter {
                                            it.first.contains(pvzPlantSearch, ignoreCase = true) ||
                                            it.second.contains(pvzPlantSearch, ignoreCase = true)
                                        }.take(25)

                                        filteredPlants.forEach { (pId, pName) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = pvzSelectedPlantId == pId, onClick = { pvzSelectedPlantId = pId })
                                                Text(pName, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }

                        // ВКЛАДКА 1: ВЗЛОМ ЛУТА ПИНЬЯТ И ГЕЙМПЛЕЙНЫЕ МОДЫ RTON
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🪅 Корректор Лута Пиньят (pinatacoreloot.rton)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Увеличивает количество выпадающих семян и монет из любой пиньяты в игре.", fontSize = 12.sp, color = Color.Gray)

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = pvzPinataMultiplier,
                                                onValueChange = { pvzPinataMultiplier = it },
                                                label = { Text("Множитель лута (x100)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = pvzPinataMinSeeds,
                                                onValueChange = { pvzPinataMinSeeds = it },
                                                label = { Text("Мин. семян в слоте") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            scope.launch {
                                                isBusy = true
                                                val mult = pvzPinataMultiplier.toIntOrNull() ?: 100
                                                val minS = pvzPinataMinSeeds.toIntOrNull() ?: 1000
                                                withContext(Dispatchers.IO) {
                                                    Pvz2Manager.patchPinataCoreLoot(pvzActiveCdnDir, mult, minS, ::log)
                                                }
                                                isBusy = false
                                            }
                                        }) { Text("🪅 Пропатчить Лут Пиньят") }
                                    }
                                }

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("🌞 Мод: Все Растения за 0 Солнца + 0 сек Перезарядки", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Патчит `PlantProperties.rton` и `PlantLevels.rton`: посадка любого растения на газон не тратит солнце и не имеет отката!", fontSize = 12.sp, color = Color.Gray)
                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            scope.launch {
                                                isBusy = true
                                                withContext(Dispatchers.IO) {
                                                    Pvz2Manager.patchZeroSunAndInstantCooldown(pvzActiveCdnDir, ::log)
                                                }
                                                isBusy = false
                                            }
                                        }) { Text("🌞 Включить 0 Солнца + Мгновенную Перезарядку") }
                                    }
                                }

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("🏆 Мега-Награды Арены (Joust) и Календаря", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Умножает все награды Арены и ежедневного календаря в 100 раз и снижает порог корон для главного приза сезона до 1 победы!", fontSize = 12.sp, color = Color.Gray)
                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            scope.launch {
                                                isBusy = true
                                                withContext(Dispatchers.IO) {
                                                    Pvz2Manager.patchArenaAndCalendarMegaRewards(pvzActiveCdnDir, ::log)
                                                }
                                                isBusy = false
                                            }
                                        }) { Text("🏆 Включить Мега-Награды Арены и Календаря (x100)") }
                                    }
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }

                        // ВКЛАДКА 2: ПРЯМОЙ РЕДАКТОР ПРОФИЛЯ ИГРОКА (pp.dat)
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Редактор сохранения игрока `No_Backup/pp.dat` (формат RTON):", fontSize = 12.sp, color = Color.Gray)

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(modifier = Modifier.weight(1f), onClick = {
                                        Pvz2Database.CURRENCIES.forEach { (k, _) -> pvzCurrencies[k] = "99999999" }
                                        log("💎 Все валюты PvZ 2 выставлены на 99 999 999")
                                    }) { Text("💎 ВСЁ НА 99.9M") }
                                }

                                Pvz2Database.CURRENCIES.forEach { (k, label) ->
                                    OutlinedTextField(
                                        value = pvzCurrencies[k] ?: "0",
                                        onValueChange = { pvzCurrencies[k] = it },
                                        label = { Text(label) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🌿 Прокачка растений напрямую в pp.dat", fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = pvzTargetPlantLevel,
                                                onValueChange = { pvzTargetPlantLevel = it },
                                                label = { Text("Уровень (1-20)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = pvzTargetMastery,
                                                onValueChange = { pvzTargetMastery = it },
                                                label = { Text("Mastery (0-200)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                            val pp = pvzPpDatJson
                                            if (pp == null) {
                                                log("❌ Файл pp.dat не загружен!")
                                                return@Button
                                            }
                                            scope.launch {
                                                isBusy = true
                                                val map = pvzCurrencies.mapValues { it.value.toLongOrNull() ?: 0L }
                                                val lvl = pvzTargetPlantLevel.toIntOrNull() ?: 20
                                                val mst = pvzTargetMastery.toIntOrNull() ?: 200
                                                withContext(Dispatchers.IO) {
                                                    Pvz2Manager.updatePpDatAndUnlockPlants(pp, map, true, lvl, mst, ::log)
                                                }
                                                isBusy = false
                                            }
                                        }) { Text("💾 Сохранить Валюты и Открыть все растения в pp.dat") }
                                    }
                                }
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }

                        // ВКЛАДКА 3: АВТО-ПОИСК ПАПКИ CDN.* И ВОССТАНОВЛЕНИЕ ОРИГИНАЛЬНЫХ RTON
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("📂 Автоматический выбор папки CDN (учитывает обновления игры)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Когда PvZ 2 обновляется (например, с CDN.13.5 на CDN.13.6), редактор сам находит самую свежую папку. Также ты можешь выбрать любую найденную папку ниже или вписать путь вручную:", fontSize = 12.sp, color = Color.Gray)

                                        OutlinedTextField(
                                            value = pvzActiveCdnDir,
                                            onValueChange = { pvzActiveCdnDir = it },
                                            label = { Text("Активный путь к папке CDN.*") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (pvzDetectedCdnList.isNotEmpty()) {
                                            Text("Найденные папки CDN на устройстве:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            pvzDetectedCdnList.forEach { dirPath ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = pvzActiveCdnDir == dirPath, onClick = { pvzActiveCdnDir = dirPath })
                                                    Text(dirPath.substringAfterLast("/"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                            scope.launch {
                                                isBusy = true
                                                withContext(Dispatchers.IO) {
                                                    Pvz2Manager.restoreAllCdnBackups(pvzActiveCdnDir, ::log)
                                                }
                                                isBusy = false
                                            }
                                        }) { Text("⏪ Откатить все .rton файлы в этой папке к оригиналу (.bak_editor)") }
                                    }
                                }

                                Text("📜 Журнал операций:", fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black).padding(8.dp)) {
                                    Text(text = logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
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
