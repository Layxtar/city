package com.layxtar.subwaysaver

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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

    var profilePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var rawBytesState by remember { mutableStateOf<ByteArray?>(null) }
    var containerJsonState by remember { mutableStateOf<JSONObject?>(null) }
    var innerProfileState by remember { mutableStateOf<JSONObject?>(null) }

    val currencies = remember {
        mutableStateMapOf<Long, String>().apply {
            GameDatabase.CURRENCIES.forEach { (tag, _) -> put(tag, "0") }
        }
    }

    val surferStates = remember { mutableStateListOf<CitySurferState>() }
    val unlockedSkinTags = remember { mutableStateListOf<Long>() }
    val boardStates = remember { mutableStateListOf<CityBoardState>() }
    var expandedSurferId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var playerNameInput by remember { mutableStateOf("") }
    var colorStartHex by remember { mutableStateOf("FF0055") }
    var colorEndHex by remember { mutableStateOf("00E5FF") }
    var rawJsonKeySearch by remember { mutableStateOf("") }

    fun syncStateFromJson(inner: JSONObject) {
        try {
            val map = CryptoAndSyncManager.readCityCurrencies(inner)
            GameDatabase.CURRENCIES.forEach { (tag, _) ->
                currencies[tag] = (map[tag] ?: 0L).toString()
            }

            surferStates.clear()
            surferStates.addAll(CryptoAndSyncManager.parseCitySurfers(inner))

            unlockedSkinTags.clear()
            unlockedSkinTags.addAll(CryptoAndSyncManager.parseUnlockedCitySkins(inner))

            boardStates.clear()
            boardStates.addAll(CryptoAndSyncManager.parseCityBoards(inner))

            val onlineProf = inner.optJSONObject("onlinePlayerProfile")
            playerNameInput = onlineProf?.optString("playerName", "") ?: ""
            uiTrigger++
        } catch (e: Exception) {
            log("⚠️ Предупреждение разбора профиля City: ${e.message}")
        }
    }

    fun applyUiToInnerJson(inner: JSONObject) {
        try {
            val newMap = currencies.mapValues { it.value.toLongOrNull() ?: 0L }
            CryptoAndSyncManager.writeCityCurrencies(inner, newMap)
            CryptoAndSyncManager.applyCitySurfers(inner, surferStates)
            CryptoAndSyncManager.applyUnlockedCitySkins(inner, unlockedSkinTags.toSet())
            CryptoAndSyncManager.applyCityBoards(inner, boardStates)

            if (playerNameInput.isNotBlank()) {
                val onlineProf = inner.optJSONObject("onlinePlayerProfile") ?: JSONObject()
                onlineProf.put("playerName", playerNameInput)
                onlineProf.put("renameableAt", "0001-01-01T00:00:00")
                inner.put("onlinePlayerProfile", onlineProf)
            }
        } catch (e: Exception) {
            log("❌ Ошибка сборки JSON City: ${e.message}")
        }
    }

    fun loadCityProfileAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Чтение и расшифровка профиля Subway Surfers City...")
            val result = withContext(Dispatchers.IO) {
                try {
                    CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, false) {}
                    val resolved = CryptoAndSyncManager.resolveCityProfilePath {}
                    val bytes = CryptoAndSyncManager.readFileViaShizuku(resolved, log)
                    if (bytes != null && bytes.size >= 32) {
                        val (cont, inner) = CryptoAndSyncManager.decryptCityProfile(bytes)
                        Triple(resolved, bytes, cont to inner)
                    } else null
                } catch (e: Exception) {
                    log("❌ Ошибка расшифровки профиля City: ${e.message}")
                    null
                }
            }
            if (result != null) {
                profilePath = result.first
                rawBytesState = result.second
                containerJsonState = result.third.first
                innerProfileState = result.third.second
                syncStateFromJson(result.third.second)
                log("✅ Профиль City загружен! Монет: ${currencies[-633437229L]}")
            } else {
                log("⚠️ Файл профиля City не найден или ещё не создан игрой.")
            }
            isBusy = false
        }
    }

    fun saveCityProfileAsync(launchGameAfter: Boolean) {
        val origBytes = rawBytesState ?: return
        val cont = containerJsonState ?: return
        val inner = innerProfileState ?: return
        applyUiToInnerJson(inner)

        scope.launch {
            isBusy = true
            val ok = withContext(Dispatchers.IO) {
                try {
                    CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, true, log)
                    val enc = CryptoAndSyncManager.encryptCityProfile(origBytes, cont, inner)
                    CryptoAndSyncManager.writeFileViaShizuku(profilePath, enc, log)
                } catch (e: Exception) {
                    log("❌ Ошибка сохранения City: ${e.message}")
                    false
                }
            }
            if (ok) {
                log("✅ Профиль City зашифрован и сохранён!")
                if (launchGameAfter) {
                    CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, log)
                }
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadCityProfileAsync()
    }

    val inner = innerProfileState
    val _trig = uiTrigger

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Валюты & Никнейм",
                        "🏄 Сёрферы & Скины (${surferStates.count { it.isUnlocked }}/${surferStates.size})",
                        "🛹 Доски (${boardStates.count { it.isUnlocked }}/${boardStates.size})",
                        "👑 Пропуски & Кампания",
                        "🛠️ Бэкапы & JSON"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title, fontSize = 12.sp) })
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
                        onClick = { loadCityProfileAsync() }
                    ) { Text("🔄 Чтение", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = inner != null && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        onClick = { saveCityProfileAsync(false) }
                    ) { Text("💾 Сохранить", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1.1f),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = {
                            if (inner != null) saveCityProfileAsync(true)
                            else CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, log)
                        }
                    ) { Text("🚀 В City", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (inner == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Профиль Subway Surfers City ещё не загружен.", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { loadCityProfileAsync() }) {
                        Text("🔄 Повторить чтение профиля")
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎨 Редактор Никнейма (Unity Градиент + Сброс таймера)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    OutlinedTextField(
                                        value = playerNameInput,
                                        onValueChange = { playerNameInput = it },
                                        label = { Text("Никнейм в профиле") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = colorStartHex,
                                            onValueChange = { colorStartHex = it },
                                            label = { Text("Цвет 1 (HEX)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = colorEndHex,
                                            onValueChange = { colorEndHex = it },
                                            label = { Text("Цвет 2 (HEX)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                playerNameInput = CryptoAndSyncManager.buildColoredUnityName(playerNameInput, colorStartHex, colorEndHex)
                                                log("🎨 Применён градиентный тег к нику!")
                                            }
                                        ) { Text("🌈 Сделать Градиент", fontSize = 11.sp) }

                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                playerNameInput = playerNameInput.replace(Regex("<[^>]*>"), "")
                                            }
                                        ) { Text("Очистить теги", fontSize = 11.sp) }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        GameDatabase.CURRENCIES.forEach { (tag, _) -> currencies[tag] = "9999999" }
                                        log("💎 Все валюты City выставлены на 9 999 999!")
                                    }
                                ) { Text("💎 Все на 9.99M", fontSize = 11.sp) }

                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        GameDatabase.CURRENCIES.forEach { (tag, _) -> currencies[tag] = "0" }
                                        log("🗑️ Все валюты City сброшены в 0")
                                    }
                                ) { Text("🗑️ Сбросить в 0", fontSize = 11.sp) }
                            }

                            GameDatabase.CURRENCIES.forEach { (tag, label) ->
                                OutlinedTextField(
                                    value = currencies[tag] ?: "0",
                                    onValueChange = { currencies[tag] = it },
                                    label = { Text(label) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    1 -> {
                        val filteredIndices = remember(searchQuery, uiTrigger) {
                            surferStates.indices.filter { idx ->
                                val s = surferStates[idx]
                                val skins = GameDatabase.SURFER_SKINS_MAP[s.id] ?: emptyList()
                                searchQuery.isBlank() ||
                                        s.name.contains(searchQuery, ignoreCase = true) ||
                                        skins.any { it.title.contains(searchQuery, ignoreCase = true) }
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("🔍 Поиск сёрфера или костюма City...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    CryptoAndSyncManager.unlockAndMaxAllSurfers(inner)
                                    CryptoAndSyncManager.unlockAllSkinsAndAvatars(inner)
                                    syncStateFromJson(inner)
                                    log("🏄 Все 27 сёрферов (20 ур., без накрутки рекордов) и 125 скинов открыты!")
                                }
                            ) { Text("🏄 Открыть всех 27 сёрферов (20 ур.) и все 125 скинов") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                itemsIndexed(filteredIndices, key = { _, idx -> surferStates[idx].id }) { _, idx ->
                                    val s = surferStates[idx]
                                    val skins = GameDatabase.SURFER_SKINS_MAP[s.id] ?: emptyList()
                                    val isExp = expandedSurferId == s.id

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
                                                        if (s.isUnlocked) "✅ Открыт (Ур. ${s.level} | Рекорд: ${s.highScore})" else "🔒 Закрыт",
                                                        fontSize = 12.sp,
                                                        color = if (s.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373)
                                                    )
                                                }
                                                Switch(
                                                    checked = s.isUnlocked,
                                                    onCheckedChange = { ch ->
                                                        surferStates[idx] = s.copy(isUnlocked = ch)
                                                        uiTrigger++
                                                    }
                                                )
                                            }

                                            if (s.isUnlocked) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Ур. ${s.level}", fontSize = 12.sp, modifier = Modifier.width(55.dp))
                                                    Slider(
                                                        value = s.level.toFloat(),
                                                        onValueChange = { surferStates[idx] = s.copy(level = it.toInt()) },
                                                        valueRange = 1f..20f,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            if (skins.isNotEmpty()) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedSurferId = if (isExp) null else s.id }
                                                ) {
                                                    Text(if (isExp) "🔼 Скрыть скины" else "🔽 Скины (${skins.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExp) {
                                                    skins.forEachIndexed { skinIdx, skinInfo ->
                                                        val hasSkin = skinIdx == 0 || unlockedSkinTags.contains(skinInfo.dataTag)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Checkbox(
                                                                checked = hasSkin,
                                                                enabled = skinIdx != 0,
                                                                onCheckedChange = { checked ->
                                                                    if (checked) {
                                                                        if (!unlockedSkinTags.contains(skinInfo.dataTag)) unlockedSkinTags.add(skinInfo.dataTag)
                                                                    } else {
                                                                        unlockedSkinTags.remove(skinInfo.dataTag)
                                                                    }
                                                                    uiTrigger++
                                                                }
                                                            )
                                                            Text(skinInfo.title, fontSize = 12.sp)
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

                    2 -> {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    CryptoAndSyncManager.unlockAndMaxAllBoards(inner)
                                    syncStateFromJson(inner)
                                    log("🛹 Все 29 досок открыты и прокачаны на 20 уровень!")
                                }
                            ) { Text("🛹 Открыть и прокачать все 29 досок (20 ур.)") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                itemsIndexed(boardStates, key = { _, b -> b.id }) { idx, b ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(b.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        if (b.isUnlocked) "✅ Открыта (Ур. ${b.level})" else "🔒 Закрыта",
                                                        fontSize = 12.sp,
                                                        color = if (b.isUnlocked) Color(0xFF81C784) else Color(0xFFE57373)
                                                    )
                                                }
                                                Switch(
                                                    checked = b.isUnlocked,
                                                    onCheckedChange = { ch ->
                                                        boardStates[idx] = b.copy(isUnlocked = ch)
                                                        uiTrigger++
                                                    }
                                                )
                                            }
                                            if (b.isUnlocked) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Ур. ${b.level}", fontSize = 12.sp, modifier = Modifier.width(55.dp))
                                                    Slider(
                                                        value = b.level.toFloat(),
                                                        onValueChange = { boardStates[idx] = b.copy(level = it.toInt()) },
                                                        valueRange = 1f..20f,
                                                        modifier = Modifier.weight(1f)
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
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👑 Пакетные Разблокировки Кампании и Доната", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        CryptoAndSyncManager.injectAllPaidPassesAndDonations(inner)
                                        log("👑 Премиум Пропуск, Отключение рекламы и все IAP-наборы активированы!")
                                    }) { Text("👑 Активировать Season Pass + Remove Ads + Все IAP") }

                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        CryptoAndSyncManager.completeAllDistrictsAndStages(inner)
                                        log("🏙️ Все 6 районов и этапы кампании пройдены на 3 звезды!")
                                    }) { Text("🏙️ Открыть все 6 Районов и пройти все этапы") }

                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        CryptoAndSyncManager.completeAllDailyAndWeeklyMissions(inner)
                                        CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(inner)
                                        log("📜 Все миссии, испытания и опыт сезона выполнены!")
                                    }) { Text("📜 Выполнить все миссии и испытания (Trials)") }

                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(inner)
                                        CryptoAndSyncManager.hackDailyDealsAndAdRewards(inner, 9999, null)
                                        log("⭐ Секретные звёзды, достижения и джекпоты открыты!")
                                    }) { Text("⭐ Открыть секретные звёзды, ачивки и сделки по 9999") }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    4 -> {
                        val allKeys = remember(inner, rawJsonKeySearch, uiTrigger) {
                            inner.keys().asSequence().toList().filter {
                                rawJsonKeySearch.isBlank() || it.contains(rawJsonKeySearch, ignoreCase = true)
                            }.sorted()
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📦 Мульти-Бэкапы Профиля City", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                withContext(Dispatchers.IO) {
                                                    CryptoAndSyncManager.exportBackupToDownload(profilePath, "City", log)
                                                }
                                                isBusy = false
                                            }
                                        }
                                    ) { Text("📦 Экспортировать текущий сейв в /Download") }

                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                val ok = withContext(Dispatchers.IO) {
                                                    CryptoAndSyncManager.restoreBackup(profilePath, log)
                                                }
                                                if (ok) loadCityProfileAsync()
                                                isBusy = false
                                            }
                                        }
                                    ) { Text("⏪ Откатить к оригинальному .bak_editor") }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🔍 Инспектор ключей внутреннего JSON (${allKeys.size} секций)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    OutlinedTextField(
                                        value = rawJsonKeySearch,
                                        onValueChange = { rawJsonKeySearch = it },
                                        label = { Text("Фильтр по названию ключа JSON...") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    allKeys.take(25).forEach { k ->
                                        val vPreview = inner.opt(k)?.toString()?.take(80) ?: "null"
                                        Text("• $k: $vPreview", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
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
