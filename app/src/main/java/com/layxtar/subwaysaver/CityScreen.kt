package com.layxtar.subwaysaver

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var uiTrigger by remember { mutableIntStateOf(0) }

    var activePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var innerProfileJson by remember { mutableStateOf<JSONObject?>(null) }

    val undoStack = remember { mutableStateListOf<String>() }

    // Валюты City
    val cityCurrencies = remember {
        mutableStateMapOf<Long, String>().apply {
            GameDatabase.CURRENCIES.forEach { (tag, _) -> put(tag, "0") }
        }
    }
    var playerXpLevel by remember { mutableStateOf("1") }
    var seasonPassPoints by remember { mutableStateOf("0") }
    var activeSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.last().first) }

    // Состояния сёрферов, скинов и досок (используем mutableStateListOf вместо mutableStateSetOf для 100% совместимости!)
    val surferStates = remember { mutableStateListOf<CitySurferState>() }
    val unlockedSkinTags = remember { mutableStateListOf<Long>() }
    val boardStates = remember { mutableStateListOf<CityBoardState>() }

    var searchQuery by remember { mutableStateOf("") }
    var surferFilter by remember { mutableStateOf("Все") }
    var boardFilter by remember { mutableStateOf("Все") }
    var expandedSurferId by remember { mutableStateOf<Long?>(null) }

    // Копилка и магазин
    var selectedPiggyIndex by remember { mutableIntStateOf(2) }
    var piggyProgressInput by remember { mutableStateOf("250000") }
    var piggyFilled by remember { mutableStateOf(true) }
    var dailyDealAmountInput by remember { mutableStateOf("9999") }

    fun syncStateToProfileJson(profile: JSONObject) {
        val walletObj = profile.optJSONObject("wallet") ?: JSONObject()
        val currArr = walletObj.optJSONArray("currencies") ?: JSONArray()
        val mapExisting = mutableMapOf<Long, JSONObject>()
        for (i in 0 until currArr.length()) {
            val c = currArr.getJSONObject(i)
            mapExisting[c.getLong("id")] = c
        }
        for ((tag, strVal) in cityCurrencies) {
            val amount = strVal.toLongOrNull() ?: 0L
            val cObj = mapExisting[tag] ?: JSONObject().put("id", tag)
            cObj.put("amount", amount)
            mapExisting[tag] = cObj
        }
        val newCurrArr = JSONArray()
        mapExisting.values.forEach { newCurrArr.put(it) }
        walletObj.put("currencies", newCurrArr)
        profile.put("wallet", walletObj)

        profile.put("xpLevel", (playerXpLevel.toIntOrNull() ?: 1).coerceAtLeast(1))
        profile.put("seasonPassPoints", (seasonPassPoints.toIntOrNull() ?: 0).coerceAtLeast(0))
        profile.put("activeSeasonId", activeSeasonId)

        if (surferStates.isNotEmpty()) {
            CryptoAndSyncManager.applyCitySurfers(profile, surferStates)
        }
        CryptoAndSyncManager.applyUnlockedCitySkins(profile, unlockedSkinTags.toSet())
        if (boardStates.isNotEmpty()) {
            CryptoAndSyncManager.applyCityBoards(profile, boardStates)
        }
    }

    fun refreshUIFromProfileJson(profile: JSONObject) {
        val walletObj = profile.optJSONObject("wallet")
        val currArr = walletObj?.optJSONArray("currencies")
        val mapCurr = mutableMapOf<Long, Long>()
        if (currArr != null) {
            for (i in 0 until currArr.length()) {
                val c = currArr.getJSONObject(i)
                mapCurr[c.getLong("id")] = c.optLong("amount", 0L)
            }
        }
        GameDatabase.CURRENCIES.forEach { (tag, _) ->
            cityCurrencies[tag] = (mapCurr[tag] ?: 0L).toString()
        }

        playerXpLevel = profile.optInt("xpLevel", 1).toString()
        seasonPassPoints = profile.optInt("seasonPassPoints", 0).toString()
        val curSeason = profile.optString("activeSeasonId", "")
        if (curSeason.isNotEmpty()) activeSeasonId = curSeason

        surferStates.clear()
        surferStates.addAll(CryptoAndSyncManager.parseCitySurfers(profile))

        val parsedSkins = CryptoAndSyncManager.parseUnlockedCitySkins(profile).toMutableSet()
        GameDatabase.SURFER_SKINS_MAP.values.forEach { list ->
            list.firstOrNull()?.let { parsedSkins.add(it.dataTag) }
        }
        unlockedSkinTags.clear()
        unlockedSkinTags.addAll(parsedSkins)

        boardStates.clear()
        boardStates.addAll(CryptoAndSyncManager.parseCityBoards(profile))

        uiTrigger++
    }

    fun pushUndo() {
        val prof = innerProfileJson ?: return
        syncStateToProfileJson(prof)
        if (undoStack.size >= 20) undoStack.removeAt(0)
        undoStack.add(prof.toString())
    }

    fun performUndo() {
        if (undoStack.isEmpty()) return
        val prevStr = undoStack.removeAt(undoStack.lastIndex)
        val restored = JSONObject(prevStr)
        innerProfileJson = restored
        refreshUIFromProfileJson(restored)
        log("↩️ Шаг назад выполнен в City")
    }

    fun loadCityProfileAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Поиск и расшифровка профиля Subway Surfers City...")
            val result = withContext(Dispatchers.IO) {
                CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, false, log)
                val resolved = CryptoAndSyncManager.resolveCityProfilePath(log)
                val bytes = CryptoAndSyncManager.readFileViaShizuku(resolved, log)
                if (bytes != null) {
                    val (cont, inner) = CryptoAndSyncManager.decryptCityProfile(bytes)
                    Triple(resolved, bytes, cont to inner)
                } else null
            }
            if (result != null) {
                activePath = result.first
                rawFileBytes = result.second
                containerJson = result.third.first
                innerProfileJson = result.third.second
                undoStack.clear()
                refreshUIFromProfileJson(result.third.second)
                log("✅ Профиль City успешно загружен! (Монет: ${cityCurrencies[-633437229L]}, Ключей: ${cityCurrencies[-2008679869L]})")
            } else {
                log("❌ Не удалось прочитать профиль City ($activePath)")
            }
            isBusy = false
        }
    }

    fun saveCityProfileAsync(launchAfter: Boolean) {
        val raw = rawFileBytes ?: return
        val cont = containerJson ?: return
        val prof = innerProfileJson ?: return
        syncStateToProfileJson(prof)

        scope.launch {
            isBusy = true
            log("💾 Шифрование AES-CTR и запись профиля City...")
            val ok = withContext(Dispatchers.IO) {
                CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, true, log)
                val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
                CryptoAndSyncManager.writeFileViaShizuku(activePath, encryptedBytes, log)
            }
            if (ok) {
                log("✅ Профиль City успешно сохранён!")
                if (launchAfter) {
                    CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, log)
                }
            } else {
                log("❌ Ошибка записи профиля City!")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadCityProfileAsync()
    }

    val innerProfile = innerProfileJson
    val _trig = uiTrigger

    val totalSurfers = GameDatabase.SURFERS.size
    val unlockedSurfers = surferStates.count { it.isUnlocked }
    val totalSkins = GameDatabase.ALL_SKINS.size
    val unlockedSkins = unlockedSkinTags.distinct().size
    val totalBoards = GameDatabase.BOARDS.size
    val unlockedBoards = boardStates.count { it.isUnlocked }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Валюты & Сезон",
                        "🏙️ Трассы & Триалы",
                        "🏄 Сёрферы ($unlockedSurfers/$totalSurfers | 👕$unlockedSkins/$totalSkins)",
                        "🛹 Доски ($unlockedBoards/$totalBoards)",
                        "🐷 Копилка & Донат",
                        "⚙️ Бэкап & Логи"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                innerProfileJson?.let { syncStateToProfileJson(it) }
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
                        modifier = Modifier.weight(0.8f),
                        enabled = undoStack.isNotEmpty() && !isBusy,
                        onClick = { performUndo() }
                    ) { Text("↩️ Назад", fontSize = 11.sp) }

                    OutlinedButton(
                        modifier = Modifier.weight(0.9f),
                        enabled = !isBusy,
                        onClick = { loadCityProfileAsync() }
                    ) { Text("🔄 Чтение", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = innerProfile != null && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        onClick = { saveCityProfileAsync(false) }
                    ) { Text("💾 Сохранить", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1.1f),
                        enabled = innerProfile != null && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = { saveCityProfileAsync(true) }
                    ) { Text("🚀 В City", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (innerProfile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка профиля Subway Surfers City...")
                }
            } else {
                when (selectedTab) {
                    // ==================== ВКЛАДКА 0: ВАЛЮТЫ, УРОВЕНЬ И СЕЗОН ====================
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚡ Быстрые пресеты профиля City", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pushUndo()
                                                GameDatabase.CURRENCIES.forEach { (tag, _) ->
                                                    cityCurrencies[tag] = "9999999"
                                                }
                                                syncStateToProfileJson(innerProfile)
                                                log("💎 Все валюты выставлены на 9 999 999!")
                                            }
                                        ) { Text("💎 Все валюты 9.9M", fontSize = 11.sp) }

                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pushUndo()
                                                CryptoAndSyncManager.completeAllDailyAndWeeklyMissions(innerProfile)
                                                refreshUIFromProfileJson(innerProfile)
                                                log("📜 Все ежедневные и еженедельные задания выполнены!")
                                            }
                                        ) { Text("📜 Задания + Pass", fontSize = 11.sp) }
                                    }
                                }
                            }

                            GameDatabase.CURRENCIES.forEach { (tag, label) ->
                                OutlinedTextField(
                                    value = cityCurrencies[tag] ?: "0",
                                    onValueChange = { cityCurrencies[tag] = it },
                                    label = { Text(label) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎟️ Уровень Аккаунта и Сезонный Пропуск", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = playerXpLevel,
                                            onValueChange = { playerXpLevel = it },
                                            label = { Text("Уровень XP профиля") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = seasonPassPoints,
                                            onValueChange = { seasonPassPoints = it },
                                            label = { Text("Очки Season Pass") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Text("Активный сезон:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    GameDatabase.SEASONS.forEach { (sId, sTitle) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = activeSeasonId == sId,
                                                onClick = { activeSeasonId = sId }
                                            )
                                            Text("$sTitle ($sId)", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ==================== ВКЛАДКА 1: ТРАССЫ (РАЙОНЫ 1..6), ТУРЫ И ТРИАЛЫ ПО ВЫБОРУ ====================
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏙️ Управление Трассами / Районами (Districts 1–6)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Ты можешь открыть все 6 районов на 3★ или ЗАКРЫТЬ дополнительные трассы (2–6), чтобы в забегах выпадала только стартовая локация:",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            onClick = {
                                                pushUndo()
                                                CryptoAndSyncManager.completeAllDistrictsAndStages(innerProfile)
                                                uiTrigger++
                                                log("🏙️ Все 6 районов и все их этапы открыты на 3★!")
                                            }
                                        ) { Text("🔓 Открыть все 6", fontSize = 11.sp, color = Color.White) }

                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pushUndo()
                                                CryptoAndSyncManager.lockAllExtraDistricts(innerProfile)
                                                uiTrigger++
                                                log("🔒 Дополнительные трассы (2–6) закрыты! Оставлен только Район 1.")
                                            }
                                        ) { Text("🔒 Закрыть доп. трассы (2–6)", fontSize = 11.sp) }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                                    val districtNames = listOf(
                                        1 to "Район 1: Стартовый (Delorean Park / Downtown)",
                                        2 to "Район 2: Доп. трасса (Subway Docks)",
                                        3 to "Район 3: Доп. трасса (Neon Boulevard)",
                                        4 to "Район 4: Доп. трасса (Industrial Yard)",
                                        5 to "Район 5: Доп. трасса (Midtown Heights)",
                                        6 to "Район 6: Доп. трасса (Grand Central)"
                                    )

                                    districtNames.forEach { (dNum, dTitle) ->
                                        val isUnlocked = remember(uiTrigger) {
                                            CryptoAndSyncManager.isCityDistrictUnlocked(innerProfile, dNum)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(dTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text(
                                                    text = if (isUnlocked) "✅ Открыта (все этапы 3★)" else "🔒 Закрыта",
                                                    fontSize = 11.sp,
                                                    color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373)
                                                )
                                            }
                                            Switch(
                                                checked = isUnlocked,
                                                enabled = dNum != 1,
                                                onCheckedChange = { checked ->
                                                    pushUndo()
                                                    CryptoAndSyncManager.setCityDistrictUnlocked(innerProfile, dNum, checked)
                                                    uiTrigger++
                                                    log(if (checked) "🔓 Открыта трасса: $dTitle" else "🔒 Закрыта трасса: $dTitle")
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏁 Открытие Триалов и Ивентовых Туров по выбору", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Отмечай галочками нужные испытания (Триалы и Кампании), чтобы пройти их на 3 звезды и открыть доступ к их наградам:",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    val trialsCatalog = listOf(
                                        Triple("daily_trial", "⏱️ Ежедневный Триал (Daily Trial — 5 этапов)", 5),
                                        Triple("event_trial", "🔥 Ивентовый Триал (Event Trial — 5 этапов)", 5),
                                        Triple("weekly_trial", "📅 Еженедельный Триал (Weekly Trial — 5 этапов)", 5),
                                        Triple("trial", "🏆 Основной Триал (Standard Trial — 5 этапов)", 5),
                                        Triple("scheduled_campaign_01", "🎪 Ивентовый Тур / Кампания 1 (7 этапов)", 7),
                                        Triple("scheduled_campaign_02", "🎪 Ивентовый Тур / Кампания 2 (7 этапов)", 7),
                                        Triple("scheduled_campaign_03", "🎪 Ивентовый Тур / Кампания 3 (7 этапов)", 7)
                                    )

                                    val currentActiveCamp = innerProfile.optJSONObject("activeScheduledCampaignProfile")
                                        ?.optString("activeScheduledCampaignId", "scheduled_campaign_01") ?: "scheduled_campaign_01"

                                    trialsCatalog.forEach { (prefixId, title, maxStg) ->
                                        val isDone = remember(uiTrigger) {
                                            CryptoAndSyncManager.isCityTrialOrCampaignCompleted(innerProfile, prefixId)
                                        }
                                        val isCampaign = prefixId.startsWith("scheduled_campaign")

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Checkbox(
                                                    checked = isDone,
                                                    onCheckedChange = { checked ->
                                                        pushUndo()
                                                        CryptoAndSyncManager.setCityTrialOrCampaignCompleted(
                                                            profile = innerProfile,
                                                            prefixId = prefixId,
                                                            maxStages = maxStg,
                                                            completed = checked,
                                                            setAsActiveCampaign = isCampaign && checked
                                                        )
                                                        uiTrigger++
                                                        log(if (checked) "✅ Пройден на 3★: $title" else "⏪ Сброшен прогресс: $title")
                                                    }
                                                )
                                                Text(title, fontSize = 13.sp)
                                            }

                                            if (isCampaign) {
                                                OutlinedButton(
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    onClick = {
                                                        pushUndo()
                                                        CryptoAndSyncManager.setCityTrialOrCampaignCompleted(
                                                            profile = innerProfile,
                                                            prefixId = prefixId,
                                                            maxStages = maxStg,
                                                            completed = true,
                                                            setAsActiveCampaign = true
                                                        )
                                                        uiTrigger++
                                                        log("🎪 Активным туром назначен: $prefixId")
                                                    }
                                                ) {
                                                    Text(if (currentActiveCamp == prefixId) "⭐ Активен" else "Активировать", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pushUndo()
                                                CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(innerProfile)
                                                uiTrigger++
                                                log("🏁 Все Триалы и Ивентовые Туры пройдены на 3★, таймеры сброшены!")
                                            }
                                        ) { Text("🏁 Пройти все Триалы", fontSize = 11.sp) }

                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pushUndo()
                                                CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(innerProfile)
                                                uiTrigger++
                                                log("⭐ Секретные звёзды на трассах, ачивки и гарант джекпотов выданы!")
                                            }
                                        ) { Text("⭐ Секретные Звёзды", fontSize = 11.sp) }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ==================== ВКЛАДКА 2: СЁРФЕРЫ И ПОДСКИНЫ ====================
                    2 -> {
                        val filteredSurfers = remember(searchQuery, surferFilter, uiTrigger) {
                            surferStates.filter { s ->
                                val skinsForSurfer = GameDatabase.SURFER_SKINS_MAP[s.id] ?: emptyList()
                                val matchesSearch = searchQuery.isBlank() ||
                                        s.name.contains(searchQuery, ignoreCase = true) ||
                                        s.id.toString().contains(searchQuery) ||
                                        skinsForSurfer.any { it.title.contains(searchQuery, ignoreCase = true) || it.key.contains(searchQuery, ignoreCase = true) }

                                val matchesFilter = when (surferFilter) {
                                    "Открытые" -> s.isUnlocked
                                    "Закрытые" -> !s.isUnlocked
                                    else -> true
                                }
                                matchesSearch && matchesFilter
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📊 Сёрферов: $unlockedSurfers / $totalSurfers | Костюмов (Скинов): $unlockedSkins / $totalSkins",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("🔍 Поиск сёрфера или костюма (напр. Rey Romeo, Oni One, Superstar)...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Все", "Открытые", "Закрытые").forEach { mode ->
                                    FilterChip(
                                        selected = surferFilter == mode,
                                        onClick = { surferFilter = mode },
                                        label = { Text(mode, fontSize = 12.sp) }
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        pushUndo()
                                        surferStates.forEach { s ->
                                            s.isUnlocked = true
                                            s.level = 20
                                        }
                                        GameDatabase.ALL_SKINS.forEach { tag ->
                                            if (!unlockedSkinTags.contains(tag)) unlockedSkinTags.add(tag)
                                        }
                                        CryptoAndSyncManager.unlockAllSkinsAndAvatars(innerProfile)
                                        syncStateToProfileJson(innerProfile)
                                        uiTrigger++
                                        log("🏄 Все 27 сёрферов (20 ур.), 125 костюмов и 34 аватарки открыты (без изменения рекордов)!")
                                    }
                                ) { Text("👑 Открыть всех + Скины", fontSize = 11.sp) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredSurfers, key = { it.id }) { s ->
                                    val skinsList = GameDatabase.SURFER_SKINS_MAP[s.id] ?: emptyList()
                                    val ownedSkinsCount = skinsList.count { unlockedSkinTags.contains(it.dataTag) }
                                    val isSkinSearchMatch = searchQuery.isNotBlank() && skinsList.any {
                                        it.title.contains(searchQuery, ignoreCase = true) || it.key.contains(searchQuery, ignoreCase = true)
                                    }
                                    val isExpanded = expandedSurferId == s.id || isSkinSearchMatch

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text(
                                                        text = if (s.isUnlocked) "✅ Открыт • Ур. ${s.level}/20 • Скинов: $ownedSkinsCount/${skinsList.size}"
                                                        else "🔒 Закрыт • Скинов: $ownedSkinsCount/${skinsList.size}",
                                                        fontSize = 12.sp,
                                                        color = if (s.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373)
                                                    )
                                                }
                                                Switch(
                                                    checked = s.isUnlocked,
                                                    onCheckedChange = { checked ->
                                                        pushUndo()
                                                        s.isUnlocked = checked
                                                        uiTrigger++
                                                    }
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = s.level.toString(),
                                                    onValueChange = { v ->
                                                        s.level = (v.toIntOrNull() ?: 1).coerceIn(1, 20)
                                                        uiTrigger++
                                                    },
                                                    label = { Text("Уровень (1–20)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = s.highScore.toString(),
                                                    onValueChange = { v ->
                                                        s.highScore = (v.toLongOrNull() ?: 0L).coerceAtLeast(0L)
                                                        uiTrigger++
                                                    },
                                                    label = { Text("Личный рекорд") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1.2f)
                                                )
                                                OutlinedButton(
                                                    onClick = {
                                                        pushUndo()
                                                        s.isUnlocked = true
                                                        s.level = 20
                                                        skinsList.forEach { skin ->
                                                            if (!unlockedSkinTags.contains(skin.dataTag)) {
                                                                unlockedSkinTags.add(skin.dataTag)
                                                            }
                                                        }
                                                        uiTrigger++
                                                    }
                                                ) { Text("MAX", fontSize = 11.sp) }
                                            }

                                            if (skinsList.isNotEmpty()) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedSurferId = if (isExpanded) null else s.id }
                                                ) {
                                                    Text(
                                                        if (isExpanded) "🔼 Скрыть костюмы (${skinsList.size})"
                                                        else "🔽 Костюмы сёрфера ($ownedSkinsCount/${skinsList.size})",
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                if (isExpanded) {
                                                    skinsList.forEachIndexed { skinIdx, skinInfo ->
                                                        val isSkinOwned = unlockedSkinTags.contains(skinInfo.dataTag) || skinIdx == 0
                                                        val isEquipped = s.selectedSkin == skinIdx
                                                        val isHighlighted = searchQuery.isNotBlank() &&
                                                                (skinInfo.title.contains(searchQuery, ignoreCase = true) || skinInfo.key.contains(searchQuery, ignoreCase = true))

                                                        Row(
                                                            modifier = (if (isHighlighted) Modifier.background(Color(0xFF33691E)) else Modifier).fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Checkbox(
                                                                    checked = isSkinOwned,
                                                                    enabled = skinIdx != 0,
                                                                    onCheckedChange = { checked ->
                                                                        pushUndo()
                                                                        if (checked) {
                                                                            if (!unlockedSkinTags.contains(skinInfo.dataTag)) {
                                                                                unlockedSkinTags.add(skinInfo.dataTag)
                                                                            }
                                                                        } else {
                                                                            unlockedSkinTags.remove(skinInfo.dataTag)
                                                                        }
                                                                        uiTrigger++
                                                                    }
                                                                )
                                                                Text(skinInfo.title, fontSize = 12.sp)
                                                            }
                                                            OutlinedButton(
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                onClick = {
                                                                    pushUndo()
                                                                    s.isUnlocked = true
                                                                    if (!unlockedSkinTags.contains(skinInfo.dataTag)) {
                                                                        unlockedSkinTags.add(skinInfo.dataTag)
                                                                    }
                                                                    s.selectedSkin = skinIdx
                                                                    uiTrigger++
                                                                    log("👕 Выбран костюм '${skinInfo.title}' для ${s.name}")
                                                                }
                                                            ) {
                                                                Text(if (isEquipped) "⭐ Надет" else "Надеть", fontSize = 10.sp)
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
                    }

                    // ==================== ВКЛАДКА 3: ДОСКИ CITY ====================
                    3 -> {
                        val filteredBoards = remember(searchQuery, boardFilter, uiTrigger) {
                            boardStates.filter { b ->
                                val matchesSearch = searchQuery.isBlank() ||
                                        b.name.contains(searchQuery, ignoreCase = true) ||
                                        b.id.toString().contains(searchQuery)
                                val matchesFilter = when (boardFilter) {
                                    "Открытые" -> b.isUnlocked
                                    "Закрытые" -> !b.isUnlocked
                                    else -> true
                                }
                                matchesSearch && matchesFilter
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📊 Досок открыто: $unlockedBoards / $totalBoards",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("🔍 Поиск доски...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Все", "Открытые", "Закрытые").forEach { mode ->
                                    FilterChip(
                                        selected = boardFilter == mode,
                                        onClick = { boardFilter = mode },
                                        label = { Text(mode, fontSize = 12.sp) }
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        pushUndo()
                                        boardStates.forEach { b ->
                                            b.isUnlocked = true
                                            b.level = 20
                                        }
                                        syncStateToProfileJson(innerProfile)
                                        uiTrigger++
                                        log("🛹 Все 29 досок открыты и прокачаны до 20 уровня!")
                                    }
                                ) { Text("🛹 Все 29 досок MAX", fontSize = 11.sp) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredBoards, key = { it.id }) { b ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(b.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    text = if (b.isUnlocked) "✅ Открыта • Ур. ${b.level}/20" else "🔒 Закрыта",
                                                    fontSize = 12.sp,
                                                    color = if (b.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373)
                                                )
                                            }
                                            OutlinedTextField(
                                                value = b.level.toString(),
                                                onValueChange = { v ->
                                                    b.level = (v.toIntOrNull() ?: 1).coerceIn(1, 20)
                                                    uiTrigger++
                                                },
                                                label = { Text("Ур.") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.width(75.dp).padding(horizontal = 6.dp)
                                            )
                                            Switch(
                                                checked = b.isUnlocked,
                                                onCheckedChange = { checked ->
                                                    pushUndo()
                                                    b.isUnlocked = checked
                                                    uiTrigger++
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== ВКЛАДКА 4: КОПИЛКА, ДОНАТ-ПРОПУСКА И МАГАЗИН ====================
                    4 -> {
                        val purchaseHistory = innerProfile.optJSONObject("purchaseHistory") ?: JSONObject()

                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🐷 Настройка Копилки (Piggy Bank)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    GameDatabase.PIGGY_BANKS.forEachIndexed { idx, (pId, pair) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selectedPiggyIndex == idx,
                                                onClick = { selectedPiggyIndex = idx }
                                            )
                                            Text("${pair.second} ($pId)", fontSize = 13.sp)
                                        }
                                    }
                                    OutlinedTextField(
                                        value = piggyProgressInput,
                                        onValueChange = { piggyProgressInput = it },
                                        label = { Text("Накоплено в копилке") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = piggyFilled, onCheckedChange = { piggyFilled = it })
                                        Text("Копилка полностью заполнена и бесплатна к сбору", fontSize = 12.sp)
                                    }
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            pushUndo()
                                            val (pId, pair) = GameDatabase.PIGGY_BANKS[selectedPiggyIndex]
                                            val amt = piggyProgressInput.toIntOrNull() ?: 250000
                                            CryptoAndSyncManager.configurePiggyBank(
                                                profile = innerProfile,
                                                piggyId = pId,
                                                missionId = pair.first,
                                                progressAmount = amt,
                                                isFilled = piggyFilled,
                                                makeFreeToClaim = true
                                            )
                                            uiTrigger++
                                            log("🐷 Копилка '$pId' настроена на $amt!")
                                        }
                                    ) { Text("🐷 Применить Копилку") }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎁 Daily Deals & Награды за Рекламу", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedTextField(
                                        value = dailyDealAmountInput,
                                        onValueChange = { dailyDealAmountInput = it },
                                        label = { Text("Количество наград в слотах (напр. 9999)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            pushUndo()
                                            val amt = dailyDealAmountInput.toIntOrNull() ?: 9999
                                            CryptoAndSyncManager.hackDailyDealsAndAdRewards(innerProfile, amt, null)
                                            uiTrigger++
                                            log("🎁 Все слоты Daily Deals и рекламных наград умножены на $amt!")
                                        }
                                    ) { Text("🎁 Увеличить Daily Deals и Ad Rewards") }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("💳 Платные Пропуска и Донат-Пакеты (IAP)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        onClick = {
                                            pushUndo()
                                            CryptoAndSyncManager.injectAllPaidPassesAndDonations(innerProfile)
                                            uiTrigger++
                                            log("💳 Все платные пропуска (Season Pass, Remove Ads и все наборы) активированы!")
                                        }
                                    ) { Text("💳 АКТИВИРОВАТЬ ВЕСЬ ДОНАТ И SEASON PASS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    GameDatabase.IAP_CATALOG.forEach { (prodId, prodTitle) ->
                                        val isOwned = remember(uiTrigger) { purchaseHistory.has(prodId) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isOwned,
                                                onCheckedChange = { checked ->
                                                    pushUndo()
                                                    CryptoAndSyncManager.toggleSinglePurchase(innerProfile, prodId, checked)
                                                    uiTrigger++
                                                }
                                            )
                                            Text(prodTitle, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ==================== ВКЛАДКА 5: БЭКАП И ЛОГИ ====================
                    5 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📂 Путь к профилю City и Бэкап", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedTextField(
                                        value = activePath,
                                        onValueChange = { activePath = it },
                                        label = { Text("Путь к файлу profile") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                val ok = withContext(Dispatchers.IO) {
                                                    CryptoAndSyncManager.restoreBackup(activePath, log)
                                                }
                                                if (ok) {
                                                    log("⏪ Бэкап .bak_editor успешно восстановлен!")
                                                    loadCityProfileAsync()
                                                } else {
                                                    log("❌ Файл бэкапа .bak_editor не найден!")
                                                }
                                                isBusy = false
                                            }
                                        }
                                    ) { Text("⏪ Восстановить профиль из бэкапа (.bak_editor)") }
                                }
                            }

                            Text("📜 Журнал операций:", fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
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
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            }
        }
    }
}
