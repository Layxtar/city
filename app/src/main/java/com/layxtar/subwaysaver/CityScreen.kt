package com.layxtar.subwaysaver

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun CityScreen(
    activity: ComponentActivity,
    statusText: String,
    logs: String,
    log: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }

    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var activeProfilePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var isLoaded by remember { mutableStateOf(false) }
    val undoStack = remember { mutableStateListOf<String>() }

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

    val surfersList = remember { mutableStateListOf<CitySurferState>() }
    val boardsList = remember { mutableStateListOf<CityBoardState>() }
    var searchQuery by remember { mutableStateOf("") }

    fun applyUIToProfile(prof: JSONObject) {
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

        if (surfersList.isNotEmpty()) CryptoAndSyncManager.applyCitySurfers(prof, surfersList)
        if (boardsList.isNotEmpty()) CryptoAndSyncManager.applyCityBoards(prof, boardsList)
    }

    fun refreshUIFromProfile(prof: JSONObject) {
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

        surfersList.clear()
        surfersList.addAll(CryptoAndSyncManager.parseCitySurfers(prof))
        boardsList.clear()
        boardsList.addAll(CryptoAndSyncManager.parseCityBoards(prof))
    }

    fun pushUndo() {
        val prof = profileJson ?: return
        applyUIToProfile(prof)
        if (undoStack.size >= 20) undoStack.removeAt(0)
        undoStack.add(prof.toString())
    }

    fun performUndo() {
        if (undoStack.isEmpty()) return
        val prevStr = undoStack.removeAt(undoStack.lastIndex)
        val restored = JSONObject(prevStr)
        profileJson = restored
        refreshUIFromProfile(restored)
        log("↩️ Шаг назад выполнен (осталось в истории: ${undoStack.size})")
    }

    fun loadCityAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Чтение профиля Subway Surfers City...")
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
                undoStack.clear()
                refreshUIFromProfile(loaded.third.second)
                isLoaded = true
                log("✅ Профиль City загружен (Игрок: $playerName, Ур: $playerLevel)!")
            } else {
                log("❌ Профиль Subway Surfers City не найден.")
            }
            isBusy = false
        }
    }

    fun saveCityAsync(launchAfterSave: Boolean) {
        val prof = profileJson ?: return
        val cont = containerJson ?: return
        val raw = rawFileBytes ?: return

        applyUIToProfile(prof)
        scope.launch {
            isBusy = true
            log("💾 Шифрование и сохранение профиля City...")
            val newBytes = withContext(Dispatchers.IO) {
                CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, true) {}
                val enc = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
                val ok = CryptoAndSyncManager.writeFileViaShizuku(activeProfilePath, enc) {}
                if (ok) enc else null
            }
            if (newBytes != null) {
                rawFileBytes = newBytes
                log("✅ Профиль Subway City успешно сохранён!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, log)
            } else {
                log("❌ Ошибка сохранения профиля City!")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadCityAsync()
    }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Валюта",
                        "⚡ Задания & Пропуск",
                        "🏄 Сёрферы (${surfersList.size})",
                        "🛹 Доски (${boardsList.size})",
                        "🐷 Копилка & Донат",
                        "📊 Профиль & Бэкап"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                profileJson?.let { applyUIToProfile(it) }
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
                    OutlinedButton(
                        modifier = Modifier.weight(0.8f),
                        enabled = undoStack.isNotEmpty() && !isBusy,
                        onClick = { performUndo() }
                    ) { Text("↩️ Назад", fontSize = 11.sp) }

                    OutlinedButton(
                        modifier = Modifier.weight(0.9f),
                        enabled = !isBusy,
                        onClick = { loadCityAsync() }
                    ) { Text("🔄 Чтение", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = isLoaded && !isBusy,
                        onClick = { saveCityAsync(false) }
                    ) { Text("💾 Сохранить", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1.1f),
                        enabled = isLoaded && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = { saveCityAsync(true) }
                    ) { Text("🚀 В City", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                // ВКЛАДКА 0: ВАЛЮТА
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(modifier = Modifier.weight(1f), onClick = {
                                pushUndo()
                                GameDatabase.CURRENCIES.forEach { (tag, _) ->
                                    val c = currencyStates[tag]?.toLongOrNull() ?: 0L
                                    currencyStates[tag] = (c + 100000L).toString()
                                }
                                log("➕ Ко всем валютам City добавлено +100 000")
                            }) { Text("+100K ко всем") }

                            Button(modifier = Modifier.weight(1f), onClick = {
                                pushUndo()
                                GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                                log("💎 Все валюты City выставлены на 9 999 999")
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

                // ВКЛАДКА 1: СЕЗОННЫЙ ПРОПУСК, ЗАДАНИЯ И МЕГА-ЧИТЫ
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
                                            pushUndo()
                                            seasonPurchased = it
                                            profileJson?.let { prof -> CryptoAndSyncManager.toggleSinglePurchase(prof, "premium_pass", it) }
                                        }
                                    )
                                    Text("Разблокировать платный Premium Season Pass", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedTextField(
                                    value = seasonPoints,
                                    onValueChange = { seasonPoints = it },
                                    label = { Text("Очки сезона (15 000 открывает все ступени наград)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(modifier = Modifier.weight(1f), onClick = {
                                        pushUndo()
                                        seasonPurchased = true
                                        seasonPoints = "15000"
                                        profileJson?.let { prof -> CryptoAndSyncManager.toggleSinglePurchase(prof, "premium_pass", true) }
                                        log("🎟️ Сезонный пропуск открыт + 15 000 очков!")
                                    }) { Text("🎟️ Открыть Пропуск") }

                                    Button(modifier = Modifier.weight(1f), onClick = {
                                        pushUndo()
                                        profileJson?.let { prof ->
                                            CryptoAndSyncManager.completeAllDailyAndWeeklyMissions(prof)
                                            refreshUIFromProfile(prof)
                                            log("✅ Все Ежедневные и Еженедельные задания выполнены!")
                                        }
                                    }) { Text("✅ Выполнить Задания") }
                                }
                                Divider()
                                GameDatabase.SEASONS.forEach { (sId, sName) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = selectedSeasonId == sId, onClick = { pushUndo(); selectedSeasonId = sId })
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
                                        pushUndo()
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
                                            refreshUIFromProfile(it)
                                            log("👑 РЕЖИМ БОГА АКТИВИРОВАН!")
                                        }
                                    }
                                ) { Text("👑 АКТИВИРОВАТЬ РЕЖИМ БОГА (ВСЁ В 1 КЛИК)", color = Color.White, fontWeight = FontWeight.Bold) }
                                Text("Открывает всех 27 сёрферов, 29 досок, 125 костюмов, 6 районов на 3 звезды, триалы, задания, сезонный пропуск и бесплатную копилку (без накрутки твоих рекордов пробега).", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    pushUndo()
                                    profileJson?.let {
                                        CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(it)
                                        refreshUIFromProfile(it)
                                        log("🏁 Все ивентовые забеги (Триалы по 3 звезды) открыты!")
                                    }
                                }) { Text("🏁 Открыть все Ивентовые Забеги и Триалы (3 ⭐)") }
                                Text("Проходит миссии ивентовых забегов на 3 звезды и обнуляет таймеры ожидания.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 2: ИНДИВИДУАЛЬНЫЙ РЕДАКТОР СЁРФЕРОВ (БЕЗ РАНГА ОПЫТА)
                2 -> {
                    val filteredSurfers = surfersList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("🔍 Поиск сёрфера City...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            pushUndo()
                            profileJson?.let {
                                applyUIToProfile(it)
                                CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                refreshUIFromProfile(it)
                                log("🏄 Все 27 сёрферов открыты на 20 ур. (дистанция сохранена)!")
                            }
                        }) { Text("🏄 Открыть всех 27 сёрферов (20 Ур.) + Костюмы") }
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            itemsIndexed(filteredSurfers, key = { _, item -> item.id }) { _, surfer ->
                                val realIdx = surfersList.indexOfFirst { it.id == surfer.id }
                                if (realIdx != -1) {
                                    val item = surfersList[realIdx]
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
                                                        pushUndo()
                                                        surfersList[realIdx] = item.copy(isUnlocked = checked)
                                                    }
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = item.level.toString(),
                                                    onValueChange = { v ->
                                                        val lvl = (v.toIntOrNull() ?: 1).coerceIn(1, 20)
                                                        surfersList[realIdx] = item.copy(level = lvl, isUnlocked = true)
                                                    },
                                                    label = { Text("Уровень (1-20)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(0.8f)
                                                )
                                                OutlinedTextField(
                                                    value = item.highScore.toString(),
                                                    onValueChange = { v ->
                                                        val sc = v.toLongOrNull() ?: 0L
                                                        surfersList[realIdx] = item.copy(highScore = sc)
                                                    },
                                                    label = { Text("🏃 Дистанция / Рекорд") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1.2f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                OutlinedButton(onClick = {
                                                    pushUndo()
                                                    selectedSurferId = item.id
                                                    surfersList[realIdx] = item.copy(isUnlocked = true)
                                                    log("🏄 Выбран: ${item.name}")
                                                }) {
                                                    Text(if (selectedSurferId == item.id) "⭐ Активен" else "Взять в забег", fontSize = 11.sp)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    TextButton(onClick = { pushUndo(); crewSlot1 = item.id; log("👥 ${item.name} -> Слот 1") }) { Text("Слот 1", fontSize = 11.sp) }
                                                    TextButton(onClick = { pushUndo(); crewSlot2 = item.id; log("👥 ${item.name} -> Слот 2") }) { Text("Слот 2", fontSize = 11.sp) }
                                                    TextButton(onClick = { pushUndo(); crewSlot3 = item.id; log("👥 ${item.name} -> Слот 3") }) { Text("Слот 3", fontSize = 11.sp) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ВКЛАДКА 3: ИНДИВИДУАЛЬНЫЙ РЕДАКТОР ДОСОК
                3 -> {
                    val filteredBoards = boardsList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("🔍 Поиск доски City...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            pushUndo()
                            profileJson?.let {
                                applyUIToProfile(it)
                                CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                                refreshUIFromProfile(it)
                                log("🛹 Все 29 досок открыты и прокачаны до 20 уровня!")
                            }
                        }) { Text("🛹 Открыть все 29 досок (20 Ур.)") }
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            itemsIndexed(filteredBoards, key = { _, item -> item.id }) { _, board ->
                                val realIdx = boardsList.indexOfFirst { it.id == board.id }
                                if (realIdx != -1) {
                                    val item = boardsList[realIdx]
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
                                                        pushUndo()
                                                        boardsList[realIdx] = item.copy(isUnlocked = checked)
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
                                                        boardsList[realIdx] = item.copy(level = lvl, isUnlocked = true)
                                                    },
                                                    label = { Text("Уровень доски (1-20)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Button(onClick = {
                                                    pushUndo()
                                                    selectedBoardId = item.id
                                                    boardsList[realIdx] = item.copy(isUnlocked = true)
                                                    log("🛹 Выбрана доска: ${item.name}")
                                                }) {
                                                    Text(if (selectedBoardId == item.id) "⭐ Активна" else "Экипировать", fontSize = 11.sp)
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
                                Text("🐷 Единый Центр Свиньи-Копилки", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                                        RadioButton(selected = selectedPiggyId == pId, onClick = { selectedPiggyId = pId; selectedPiggyMission = mId })
                                        Text(desc, fontSize = 13.sp)
                                    }
                                }
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    pushUndo()
                                    profileJson?.let {
                                        CryptoAndSyncManager.configurePiggyBank(it, selectedPiggyId, selectedPiggyMission, piggyProgress.toIntOrNull() ?: 250000, piggyFilled, piggyFreeClaim)
                                        refreshUIFromProfile(it)
                                        log("🐷 Свинья-копилка настроена!")
                                    }
                                }) { Text("🐷 Применить настройки Свиньи-Копилки") }
                            }
                        }

                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            pushUndo()
                            profileJson?.let {
                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                refreshUIFromProfile(it)
                                log("💳 Весь донат City активирован!")
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
                                            pushUndo()
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

                // ВКЛАДКА 5: ПРОФИЛЬ И БЭКАП
                5 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📊 Профиль Игрока (из сохранения)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм игрока") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = playerTag, onValueChange = { playerTag = it }, label = { Text("Сетевой тег профиля (Player Tag)") }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень (1-50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = playerXp, onValueChange = { playerXp = it }, label = { Text("Опыт (XP)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = boostedMultiplier, onValueChange = { boostedMultiplier = it }, label = { Text("Множитель буста забега") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("📂 Восстановление из резервной копии", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) { CryptoAndSyncManager.restoreBackup(activeProfilePath, log) }
                                        if (ok) {
                                            loadCityAsync()
                                            log("⏪ Резервная копия успешно восстановлена!")
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
        }
    }
}
