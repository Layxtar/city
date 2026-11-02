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

private data class CityCurrencyEntry(
    val keyOrTag: String,
    val label: String,
    val isArrayItem: Boolean,
    val tagLong: Long = 0L
)

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

    var profilePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var originalRawBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var innerProfileJson by remember { mutableStateOf<JSONObject?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    val undoStack = remember { mutableStateListOf<String>() }

    // Состояния валют и очков пасса
    val currencyDefinitions = remember { mutableStateListOf<CityCurrencyEntry>() }
    val currencyValues = remember { mutableStateMapOf<String, String>() }
    var seasonPassPointsInput by remember { mutableStateOf("0") }
    var dealRewardAmountInput by remember { mutableStateOf("9999") }

    // Состояния Сёрферов, Скинов и Досок
    var surferStates by remember { mutableStateOf<List<CitySurferState>>(emptyList()) }
    var unlockedSkinIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var boardStates by remember { mutableStateOf<List<CityBoardState>>(emptyList()) }

    var surferSearchQuery by remember { mutableStateOf("") }
    var surferFilterMode by remember { mutableStateOf("Все") }
    var expandedSurferId by remember { mutableStateOf<Long?>(null) }

    var boardSearchQuery by remember { mutableStateOf("") }
    var boardFilterMode by remember { mutableStateOf("Все") }

    // Состояния Туров и Копилки
    var customTourIdInput by remember { mutableStateOf("scheduled_campaign_01") }
    var piggyProgressInput by remember { mutableStateOf("5000") }
    var piggyFilledToggle by remember { mutableStateOf(true) }

    val knownCurrencyLabels = remember {
        mapOf(
            "coins" to "🪙 Монеты (coins)",
            "gems" to "💎 Алмазы / Кристаллы (gems)",
            "keys" to "🔑 Ключи (keys)",
            "energy" to "⚡ Энергия (energy)",
            "xp" to "⭐ Опыт профиля (xp)",
            "sprayCans" to "🎨 Баллончики краски (sprayCans)",
            "-1878560402" to "🪙 Монеты / Ресурс (-1878560402)",
            "-1509662453" to "💎 Кристаллы / Ресурс (-1509662453)",
            "-1085419483" to "🎨 Жетоны / Баллончики (-1085419483)",
            "-635142441" to "🔑 Ключи / Ресурс (-635142441)",
            "-2008679869" to "⚡ Энергия / Бустер (-2008679869)",
            "326719861" to "🎁 Сундуки / Награда (326719861)"
        )
    }

    fun parseCurrenciesFromProfile(profile: JSONObject) {
        currencyDefinitions.clear()
        currencyValues.clear()

        // 1. Проверяем прямые числовые поля валют в корне профиля
        val rootCurrencyKeys = listOf("coins", "gems", "keys", "energy", "xp", "sprayCans")
        for (key in rootCurrencyKeys) {
            if (profile.has(key) && profile.opt(key) is Number) {
                val label = knownCurrencyLabels[key] ?: "💰 Валюта ($key)"
                currencyDefinitions.add(CityCurrencyEntry(key, label, isArrayItem = false))
                currencyValues[key] = profile.optLong(key, 0L).toString()
            }
        }

        // 2. Проверяем объект wallet / currencies
        val currenciesNode = profile.opt("currencies") ?: profile.opt("wallet")
        if (currenciesNode is JSONObject) {
            val keys = currenciesNode.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = currenciesNode.opt(k)
                if (v is Number) {
                    val mapKey = "obj_$k"
                    val label = knownCurrencyLabels[k] ?: "💰 Ресурс ($k)"
                    currencyDefinitions.add(CityCurrencyEntry(mapKey, label, isArrayItem = false))
                    currencyValues[mapKey] = v.toLong().toString()
                } else if (v is JSONObject && (v.has("amount") || v.has("value") || v.has("count"))) {
                    val mapKey = "nested_$k"
                    val label = knownCurrencyLabels[k] ?: "💰 Ресурс ($k)"
                    val amt = v.optLong("amount", v.optLong("value", v.optLong("count", 0L)))
                    currencyDefinitions.add(CityCurrencyEntry(mapKey, label, isArrayItem = false))
                    currencyValues[mapKey] = amt.toString()
                }
            }
        } else if (currenciesNode is JSONArray) {
            for (i in 0 until currenciesNode.length()) {
                val item = currenciesNode.optJSONObject(i) ?: continue
                val idLong = item.optLong("id", item.optLong("dataTag", 0L))
                if (idLong != 0L) {
                    val idStr = idLong.toString()
                    val mapKey = "arr_$idStr"
                    val label = knownCurrencyLabels[idStr] ?: "💰 Ресурс DataTag ($idStr)"
                    val amt = item.optLong("amount", item.optLong("value", item.optLong("count", 0L)))
                    currencyDefinitions.add(CityCurrencyEntry(mapKey, label, isArrayItem = true, tagLong = idLong))
                    currencyValues[mapKey] = amt.toString()
                }
            }
        }

        seasonPassPointsInput = profile.optInt("seasonPassPoints", 0).toString()
    }

    fun applyUIToProfile(profile: JSONObject) {
        // Сохраняем валюты обратно в JSON
        val currenciesNode = profile.opt("currencies") ?: profile.opt("wallet")
        for (def in currencyDefinitions) {
            val newVal = currencyValues[def.keyOrTag]?.toLongOrNull() ?: continue
            when {
                def.isArrayItem && currenciesNode is JSONArray -> {
                    for (i in 0 until currenciesNode.length()) {
                        val item = currenciesNode.optJSONObject(i) ?: continue
                        val idLong = item.optLong("id", item.optLong("dataTag", 0L))
                        if (idLong == def.tagLong) {
                            if (item.has("amount")) item.put("amount", newVal)
                            else if (item.has("value")) item.put("value", newVal)
                            else if (item.has("count")) item.put("count", newVal)
                            else item.put("amount", newVal)
                        }
                    }
                }
                def.keyOrTag.startsWith("obj_") && currenciesNode is JSONObject -> {
                    val realKey = def.keyOrTag.removePrefix("obj_")
                    currenciesNode.put(realKey, newVal)
                }
                def.keyOrTag.startsWith("nested_") && currenciesNode is JSONObject -> {
                    val realKey = def.keyOrTag.removePrefix("nested_")
                    val sub = currenciesNode.optJSONObject(realKey)
                    if (sub != null) {
                        if (sub.has("amount")) sub.put("amount", newVal)
                        else if (sub.has("value")) sub.put("value", newVal)
                        else if (sub.has("count")) sub.put("count", newVal)
                        else sub.put("amount", newVal)
                    }
                }
                else -> {
                    profile.put(def.keyOrTag, newVal)
                }
            }
        }

        profile.put("seasonPassPoints", seasonPassPointsInput.toIntOrNull() ?: 0)
        CryptoAndSyncManager.applyCitySurfers(profile, surferStates)
        CryptoAndSyncManager.applyUnlockedCitySkins(profile, unlockedSkinIds)
        CryptoAndSyncManager.applyCityBoards(profile, boardStates)
    }

    fun refreshUIFromProfile(profile: JSONObject) {
        parseCurrenciesFromProfile(profile)
        surferStates = CryptoAndSyncManager.parseCitySurfers(profile)
        unlockedSkinIds = CryptoAndSyncManager.parseUnlockedCitySkins(profile)
        boardStates = CryptoAndSyncManager.parseCityBoards(profile)
        customTourIdInput = CryptoAndSyncManager.getActiveCityTourId(profile)
        uiTrigger++
    }

    fun pushUndo() {
        val prof = innerProfileJson ?: return
        applyUIToProfile(prof)
        if (undoStack.size >= 20) undoStack.removeAt(0)
        undoStack.add(prof.toString())
    }

    fun performUndo() {
        if (undoStack.isEmpty()) return
        val prevStr = undoStack.removeAt(undoStack.lastIndex)
        val restored = JSONObject(prevStr)
        innerProfileJson = restored
        refreshUIFromProfile(restored)
        log("↩️ Шаг назад выполнен в Subway Surfers City")
    }

    fun loadCityAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Поиск и расшифровка профиля Subway Surfers City...")
            val loadedTriple = withContext(Dispatchers.IO) {
                CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, false, log)
                val resolvedPath = CryptoAndSyncManager.resolveCityProfilePath(log)
                val raw = CryptoAndSyncManager.readFileViaShizuku(resolvedPath, log)
                if (raw != null) {
                    runCatching {
                        val (cont, inner) = CryptoAndSyncManager.decryptCityProfile(raw)
                        Triple(resolvedPath, raw, cont to inner)
                    }.onFailure { e ->
                        log("❌ Ошибка расшифровки профиля City: ${e.message}")
                    }.getOrNull()
                } else null
            }

            if (loadedTriple != null) {
                profilePath = loadedTriple.first
                originalRawBytes = loadedTriple.second
                containerJson = loadedTriple.third.first
                val inner = loadedTriple.third.second
                innerProfileJson = inner
                undoStack.clear()
                refreshUIFromProfile(inner)
                isLoaded = true
                log("✅ Профиль City успешно загружен! Сёрферов в базе: ${surferStates.size}, Досок: ${boardStates.size}")
            } else {
                log("⚠️ Не удалось прочитать профиль City по пути: $profilePath")
            }
            isBusy = false
        }
    }

    fun saveCityAsync(launchAfterSave: Boolean) {
        val raw = originalRawBytes ?: return
        val cont = containerJson ?: return
        val inner = innerProfileJson ?: return
        applyUIToProfile(inner)

        scope.launch {
            isBusy = true
            log("💾 Шифрование и запись профиля Subway Surfers City...")
            val ok = withContext(Dispatchers.IO) {
                CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, true, log)
                val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, inner)
                CryptoAndSyncManager.writeFileViaShizuku(profilePath, encryptedBytes, log)
            }
            if (ok) {
                log("✅ Профиль Subway Surfers City успешно сохранён!")
                if (launchAfterSave) {
                    CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, log)
                }
            } else {
                log("❌ Ошибка сохранения профиля City!")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadCityAsync()
    }

    val innerProfile = innerProfileJson
    val _trig = uiTrigger

    val unlockedSurfersCount = remember(surferStates, uiTrigger) { surferStates.count { it.isUnlocked } }
    val unlockedBoardsCount = remember(boardStates, uiTrigger) { boardStates.count { it.isUnlocked } }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "🗺️ Туры, Трассы & Триалы",
                        "💰 Валюты & Награды",
                        "🏄 Сёрферы ($unlockedSurfersCount/${surferStates.size})",
                        "🛹 Доски ($unlockedBoardsCount/${boardStates.size})",
                        "🐷 Копилка & Донат",
                        "📂 Бэкап & Логи"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                innerProfileJson?.let { applyUIToProfile(it) }
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
                        onClick = { loadCityAsync() }
                    ) { Text("🔄 Чтение", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = isLoaded && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
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
            if (innerProfile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Профиль Subway Surfers City не загружен")
                        Button(onClick = { loadCityAsync() }) { Text("🔄 Загрузить профиль через Shizuku") }
                    }
                }
            } else {
                when (selectedTab) {
                    // ==================== ВКЛАДКА 0: ОТКРЫТИЕ И ВЫБОР ТУРОВ, ДОП. ТРАСС (1..6) И ТРИАЛОВ ====================
                    0 -> {
                        val availableTours = remember(uiTrigger) { CryptoAndSyncManager.getAllAvailableCityTours(innerProfile) }
                        val currentActiveTour = remember(uiTrigger) { CryptoAndSyncManager.getActiveCityTourId(innerProfile) }
                        val selectedDistrictNum = remember(uiTrigger) { CryptoAndSyncManager.getSelectedCityDistrict(innerProfile) }

                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            // БЛОК 1: ОТКРЫТИЕ И ВЫБОР ТУРОВ (CITY TOURS & SCHEDULED CAMPAIGNS)
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🗺️ Открытие и Выбор Туров (City Tours & Campaigns)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Выбери активный Тур, открой в нём все этапы на 3★ и разблокируй всех персонажей для участия в забегах:",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    availableTours.forEach { (tourId, tourTitle) ->
                                        val isCompleted = remember(uiTrigger) { CryptoAndSyncManager.isCityTrialOrCampaignCompleted(innerProfile, tourId) }
                                        val isSelected = currentActiveTour == tourId

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Checkbox(
                                                    checked = isCompleted,
                                                    onCheckedChange = { checked ->
                                                        pushUndo()
                                                        CryptoAndSyncManager.setCityTrialOrCampaignCompleted(
                                                            profile = innerProfile,
                                                            prefixId = tourId,
                                                            maxStages = 10,
                                                            completed = checked,
                                                            setAsActiveCampaign = checked
                                                        )
                                                        uiTrigger++
                                                        log(if (checked) "✅ Открыт Тур на 3★: $tourTitle" else "🔒 Закрыт Тур: $tourTitle")
                                                    }
                                                )
                                                Column {
                                                    Text(tourTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("ID: $tourId", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                            Button(
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                                ),
                                                onClick = {
                                                    pushUndo()
                                                    CryptoAndSyncManager.selectActiveCityTour(innerProfile, tourId, true)
                                                    customTourIdInput = tourId
                                                    uiTrigger++
                                                    log("🗺️ Выбран активный Тур: $tourTitle ($tourId)")
                                                }
                                            ) {
                                                Text(if (isSelected) "⭐ Активен" else "Выбрать", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    OutlinedTextField(
                                        value = customTourIdInput,
                                        onValueChange = { customTourIdInput = it },
                                        label = { Text("Свой ID Тура (напр. scheduled_campaign_04)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            if (customTourIdInput.isNotBlank()) {
                                                pushUndo()
                                                CryptoAndSyncManager.selectActiveCityTour(innerProfile, customTourIdInput.trim(), true)
                                                uiTrigger++
                                                log("🗺️ Активирован и открыт на 3★ Тур: $customTourIdInput")
                                            }
                                        }
                                    ) { Text("🚀 Активировать и открыть указанный Тур на 3★") }
                                }
                            }

                            // БЛОК 2: ОТКРЫТИЕ, ВЫБОР И ЗАКРЫТИЕ ДОПОЛНИТЕЛЬНЫХ ТРАСС (РАЙОНОВ 1..6)
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏙️ Управление Трассами / Районами (Districts 1–6)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Открывай все 6 трасс, закрывай дополнительные трассы (2–6) обратно или выбирай нужные трассы вручную:",
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
                                                log("🏙️ Все 6 трасс (районов) и их этапы открыты на 3★!")
                                            }
                                        ) { Text("🔓 Открыть все 6", fontSize = 11.sp, color = Color.White) }

                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pushUndo()
                                                CryptoAndSyncManager.lockAllExtraDistricts(innerProfile)
                                                uiTrigger++
                                                log("🔒 Дополнительные трассы (2–6) закрыты! Оставлен только стартовый Район 1.")
                                            }
                                        ) { Text("🔒 Закрыть доп. (2–6)", fontSize = 11.sp) }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                                    val districtNames = listOf(
                                        1 to "Район 1: Стартовый (Downtown / Delorean Park)",
                                        2 to "Район 2: Доп. трасса (Subway Docks)",
                                        3 to "Район 3: Доп. трасса (Neon Boulevard)",
                                        4 to "Район 4: Доп. трасса (Industrial Yard)",
                                        5 to "Район 5: Доп. трасса (Midtown Heights)",
                                        6 to "Район 6: Доп. трасса (Grand Central)"
                                    )

                                    districtNames.forEach { (dNum, dTitle) ->
                                        val isUnlocked = remember(uiTrigger) { CryptoAndSyncManager.isCityDistrictUnlocked(innerProfile, dNum) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Switch(
                                                    checked = isUnlocked,
                                                    onCheckedChange = { checked ->
                                                        pushUndo()
                                                        CryptoAndSyncManager.setCityDistrictUnlocked(innerProfile, dNum, checked)
                                                        uiTrigger++
                                                        log(if (checked) "🔓 Открыта трасса: $dTitle" else "🔒 Закрыта трасса: $dTitle")
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(dTitle, fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                onClick = {
                                                    pushUndo()
                                                    CryptoAndSyncManager.selectCityDistrict(innerProfile, dNum)
                                                    uiTrigger++
                                                    log("🏙️ Выбрана активная трасса: $dTitle")
                                                }
                                            ) {
                                                Text(if (selectedDistrictNum == dNum) "⭐ Взята" else "Выбрать", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // БЛОК 3: ОТКРЫТИЕ ТРИАЛОВ ПО ВЫБОРУ
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏁 Открытие Триалов и Испытаний по выбору", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Включай или отключай прохождение на 3★ для каждого типа триалов:", fontSize = 12.sp, color = Color.Gray)

                                    val trialsCatalog = listOf(
                                        Triple("daily_trial", "⏱️ Ежедневный Триал (Daily Trial)", 5),
                                        Triple("event_trial", "🔥 Ивентовый Триал (Event Trial)", 5),
                                        Triple("weekly_trial", "📅 Еженедельный Триал (Weekly Trial)", 5),
                                        Triple("trial", "🏆 Основной Триал (Standard Trial)", 5)
                                    )

                                    trialsCatalog.forEach { (prefixId, title, maxStg) ->
                                        val isDone = remember(uiTrigger) { CryptoAndSyncManager.isCityTrialOrCampaignCompleted(innerProfile, prefixId) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isDone,
                                                onCheckedChange = { checked ->
                                                    pushUndo()
                                                    CryptoAndSyncManager.setCityTrialOrCampaignCompleted(
                                                        profile = innerProfile,
                                                        prefixId = prefixId,
                                                        maxStages = maxStg,
                                                        completed = checked,
                                                        setAsActiveCampaign = false
                                                    )
                                                    uiTrigger++
                                                    log(if (checked) "✅ Пройден на 3★: $title" else "⏪ Сброшен: $title")
                                                }
                                            )
                                            Text(title, fontSize = 13.sp)
                                        }
                                    }

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            pushUndo()
                                            CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(innerProfile)
                                            uiTrigger++
                                            log("🏁 Все Триалы и Туры пройдены на 3★, таймеры попыток сброшены!")
                                        }
                                    ) { Text("🏁 Пройти все Триалы и Туры разом + Сбросить таймеры") }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ==================== ВКЛАДКА 1: ВАЛЮТЫ, МИССИИ, РЕКЛАМНЫЕ НАГРАДЫ И СЕКРЕТНЫЕ ЗВЁЗДЫ ====================
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚡ Быстрая Прокачка Профиля City", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            onClick = {
                                                pushUndo()
                                                currencyDefinitions.forEach { def ->
                                                    currencyValues[def.keyOrTag] = "9999999"
                                                }
                                                seasonPassPointsInput = "99999"
                                                applyUIToProfile(innerProfile)
                                                log("💎 Все валюты выставлены на 9 999 999, очки Сезонного Пасса = 99 999!")
                                            }
                                        ) { Text("💎 Все валюты 9.9M", fontSize = 11.sp, color = Color.White) }

                                        Button(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                pushUndo()
                                                CryptoAndSyncManager.completeAllDailyAndWeeklyMissions(innerProfile)
                                                refreshUIFromProfile(innerProfile)
                                                log("📜 Все ежедневные и сезонные миссии выполнены и готовы к сбору!")
                                            }
                                        ) { Text("📜 Выполнить миссии", fontSize = 11.sp) }
                                    }

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            pushUndo()
                                            CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(innerProfile)
                                            uiTrigger++
                                            log("⭐ Все Секретные Звёзды на трассах собраны, достижения открыты, гарант сундуков = 9/10!")
                                        }
                                    ) { Text("⭐ Собрать Секретные Звёзды + Достижения + Гарант Сундуков") }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💰 Редактор Валют и Сезонного Пасса", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                    OutlinedTextField(
                                        value = seasonPassPointsInput,
                                        onValueChange = { seasonPassPointsInput = it },
                                        label = { Text("🎟️ Очки Сезонного Пасса (seasonPassPoints)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (currencyDefinitions.isEmpty()) {
                                        Text("Дополнительные поля валют появятся после загрузки профиля.", fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        currencyDefinitions.forEach { def ->
                                            OutlinedTextField(
                                                value = currencyValues[def.keyOrTag] ?: "0",
                                                onValueChange = { currencyValues[def.keyOrTag] = it },
                                                label = { Text(def.label) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎁 Взлом Ежедневных Предложений и Рекламных Наград", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    OutlinedTextField(
                                        value = dealRewardAmountInput,
                                        onValueChange = { dealRewardAmountInput = it },
                                        label = { Text("Количество наград в каждом слоте") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            pushUndo()
                                            val amt = dealRewardAmountInput.toIntOrNull() ?: 9999
                                            CryptoAndSyncManager.hackDailyDealsAndAdRewards(innerProfile, amt, null)
                                            uiTrigger++
                                            log("🎁 Все слоты Daily Deals и наград за рекламу выставлены по $amt шт.!")
                                        }
                                    ) { Text("🎁 Установить награды магазина по $dealRewardAmountInput шт.") }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ==================== ВКЛАДКА 2: СЁРФЕРЫ И СКИНЫ ====================
                    2 -> {
                        val filteredSurfers = remember(surferStates, surferSearchQuery, surferFilterMode, uiTrigger) {
                            surferStates.filter { s ->
                                val matchesSearch = surferSearchQuery.isBlank() ||
                                        s.name.contains(surferSearchQuery, ignoreCase = true) ||
                                        s.id.toString().contains(surferSearchQuery)
                                val matchesFilter = when (surferFilterMode) {
                                    "Открытые" -> s.isUnlocked
                                    "Закрытые" -> !s.isUnlocked
                                    else -> true
                                }
                                matchesSearch && matchesFilter
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = surferSearchQuery,
                                onValueChange = { surferSearchQuery = it },
                                label = { Text("🔍 Поиск сёрфера по имени или DataTag...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Все", "Открытые", "Закрытые").forEach { mode ->
                                    FilterChip(
                                        selected = surferFilterMode == mode,
                                        onClick = { surferFilterMode = mode },
                                        label = { Text(mode, fontSize = 12.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        pushUndo()
                                        CryptoAndSyncManager.unlockAndMaxAllSurfers(innerProfile)
                                        CryptoAndSyncManager.unlockAllSkinsAndAvatars(innerProfile)
                                        refreshUIFromProfile(innerProfile)
                                        log("🏄 Все сёрферы (20 ур.), скины и аватарки открыты!")
                                    }
                                ) { Text("🏄 Открыть всех (20 ур.) + Все скины", fontSize = 11.sp) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredSurfers, key = { it.id }) { s ->
                                    val skinsForSurfer = GameDatabase.SURFER_SKINS_MAP[s.id].orEmpty()
                                    val isExpanded = expandedSurferId == s.id

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        text = if (s.isUnlocked) "✅ ОТКРЫТ | Ур. ${s.level}/20 | Скин #${s.selectedSkin}" else "🔒 ЗАКРЫТ",
                                                        color = if (s.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Switch(
                                                    checked = s.isUnlocked,
                                                    onCheckedChange = { checked ->
                                                        pushUndo()
                                                        s.isUnlocked = checked
                                                        if (checked && s.level < 20) s.level = 20
                                                        uiTrigger++
                                                    }
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = s.level.toString(),
                                                    onValueChange = { v ->
                                                        s.level = (v.toIntOrNull() ?: 1).coerceIn(1, 20)
                                                        uiTrigger++
                                                    },
                                                    label = { Text("Уровень (1-20)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = s.highScore.toString(),
                                                    onValueChange = { v ->
                                                        s.highScore = v.toLongOrNull() ?: 0L
                                                        uiTrigger++
                                                    },
                                                    label = { Text("Рекорд (HighScore)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1.3f)
                                                )
                                            }

                                            if (skinsForSurfer.isNotEmpty()) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedSurferId = if (isExpanded) null else s.id }
                                                ) {
                                                    Text(if (isExpanded) "🔼 Скрыть скины" else "🔽 Скины (${skinsForSurfer.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExpanded) {
                                                    skinsForSurfer.forEachIndexed { skinIdx, skinInfo ->
                                                        val tag = skinInfo.dataTag
                                                        val isSkinUnlocked = skinIdx == 0 || unlockedSkinIds.contains(tag)
                                                        val skinTitle = runCatching {
                                                            skinInfo.javaClass.getMethod("getName").invoke(skinInfo) as? String
                                                        }.getOrNull() ?: "Скин #$skinIdx ($tag)"

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Checkbox(
                                                                    checked = isSkinUnlocked,
                                                                    enabled = skinIdx != 0,
                                                                    onCheckedChange = { checked ->
                                                                        pushUndo()
                                                                        val mutable = unlockedSkinIds.toMutableSet()
                                                                        if (checked) mutable.add(tag) else mutable.remove(tag)
                                                                        unlockedSkinIds = mutable
                                                                        uiTrigger++
                                                                    }
                                                                )
                                                                Text(skinTitle, fontSize = 12.sp)
                                                            }
                                                            OutlinedButton(
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                onClick = {
                                                                    pushUndo()
                                                                    s.isUnlocked = true
                                                                    s.selectedSkin = skinIdx
                                                                    val mutable = unlockedSkinIds.toMutableSet()
                                                                    mutable.add(tag)
                                                                    unlockedSkinIds = mutable
                                                                    uiTrigger++
                                                                    log("👕 Выбран скин '$skinTitle' для ${s.name}")
                                                                }
                                                            ) {
                                                                Text(if (s.selectedSkin == skinIdx) "⭐ Надет" else "Надеть", fontSize = 10.sp)
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

                    // ==================== ВКЛАДКА 3: ДОСКИ ====================
                    3 -> {
                        val filteredBoards = remember(boardStates, boardSearchQuery, boardFilterMode, uiTrigger) {
                            boardStates.filter { b ->
                                val matchesSearch = boardSearchQuery.isBlank() ||
                                        b.name.contains(boardSearchQuery, ignoreCase = true) ||
                                        b.id.toString().contains(boardSearchQuery)
                                val matchesFilter = when (boardFilterMode) {
                                    "Открытые" -> b.isUnlocked
                                    "Закрытые" -> !b.isUnlocked
                                    else -> true
                                }
                                matchesSearch && matchesFilter
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = boardSearchQuery,
                                onValueChange = { boardSearchQuery = it },
                                label = { Text("🔍 Поиск доски по названию или DataTag...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Все", "Открытые", "Закрытые").forEach { mode ->
                                    FilterChip(
                                        selected = boardFilterMode == mode,
                                        onClick = { boardFilterMode = mode },
                                        label = { Text(mode, fontSize = 12.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    pushUndo()
                                    CryptoAndSyncManager.unlockAndMaxAllBoards(innerProfile)
                                    refreshUIFromProfile(innerProfile)
                                    log("🛹 Все доски открыты и прокачаны до 20 уровня!")
                                }
                            ) { Text("🛹 Открыть все доски на 20 уровень") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                                    text = if (b.isUnlocked) "✅ ОТКРЫТА | Уровень: ${b.level}/20" else "🔒 ЗАКРЫТА",
                                                    color = if (b.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
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
                                                modifier = Modifier.width(72.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Switch(
                                                checked = b.isUnlocked,
                                                onCheckedChange = { checked ->
                                                    pushUndo()
                                                    b.isUnlocked = checked
                                                    if (checked && b.level < 20) b.level = 20
                                                    uiTrigger++
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== ВКЛАДКА 4: КОПИЛКА И ДОНАТ / ПЛАТНЫЕ ПРОПУСКИ ====================
                    4 -> {
                        val purchaseHistory = remember(uiTrigger) {
                            innerProfile.optJSONObject("purchaseHistory") ?: JSONObject()
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👑 Разблокировка Платных Пропусков и Доната (IAP)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Активирует Premium Season Pass, отключает рекламу и добавляет все платные наборы в историю покупок:", fontSize = 12.sp, color = Color.Gray)

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        onClick = {
                                            pushUndo()
                                            CryptoAndSyncManager.injectAllPaidPassesAndDonations(innerProfile)
                                            uiTrigger++
                                            log("👑 Все платные пропуски, Premium Pass и IAP-пакеты активированы!")
                                        }
                                    ) { Text("👑 АКТИВИРОВАТЬ ВСЕ ПЛАТНЫЕ ПРОПУСКИ И ПАКЕТЫ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    GameDatabase.ALL_IAP_PURCHASES.forEach { prodId ->
                                        val isBought = remember(uiTrigger) { purchaseHistory.has(prodId) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(prodId, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Switch(
                                                checked = isBought,
                                                onCheckedChange = { checked ->
                                                    pushUndo()
                                                    CryptoAndSyncManager.toggleSinglePurchase(innerProfile, prodId, checked)
                                                    uiTrigger++
                                                    log(if (checked) "✅ Куплен пакет: $prodId" else "⏪ Удалён пакет: $prodId")
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🐷 Настройка Копилки (Piggy Bank)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedTextField(
                                        value = piggyProgressInput,
                                        onValueChange = { piggyProgressInput = it },
                                        label = { Text("Наполнение копилки (очки/гемы)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = piggyFilledToggle, onCheckedChange = { piggyFilledToggle = it })
                                        Text("Копилка полностью заполнена и бесплатна к сбору", fontSize = 12.sp)
                                    }
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            pushUndo()
                                            val firstPiggyId = GameDatabase.PIGGY_BANKS.firstOrNull()?.first ?: "piggy_bank_01"
                                            val prog = piggyProgressInput.toIntOrNull() ?: 5000
                                            CryptoAndSyncManager.configurePiggyBank(
                                                profile = innerProfile,
                                                piggyId = firstPiggyId,
                                                missionId = 1001L,
                                                progressAmount = prog,
                                                isFilled = piggyFilledToggle,
                                                makeFreeToClaim = true
                                            )
                                            uiTrigger++
                                            log("🐷 Копилка ($firstPiggyId) заполнена на $prog и разблокирована к сбору!")
                                        }
                                    ) { Text("🐷 Заполнить и разблокировать Копилку") }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ==================== ВКЛАДКА 5: БЭКАП И ЖУРНАЛ ====================
                    5 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📂 Файл Профиля и Резервная Копия", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedTextField(
                                        value = profilePath,
                                        onValueChange = { profilePath = it },
                                        label = { Text("Путь к зашифрованному профилю City") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                val ok = withContext(Dispatchers.IO) {
                                                    CryptoAndSyncManager.restoreBackup(profilePath, log)
                                                }
                                                if (ok) {
                                                    log("⏪ Оригинальный профиль восстановлен из .bak_editor!")
                                                    loadCityAsync()
                                                } else {
                                                    log("⚠️ Файл резервной копии .bak_editor не найден!")
                                                }
                                                isBusy = false
                                            }
                                        }
                                    ) { Text("⏪ Восстановить профиль из бэкапа (.bak_editor)") }
                                }
                            }

                            Text("📜 Журнал операций:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
