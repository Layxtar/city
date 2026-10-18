package com.layxtar.subwaysaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    var selectedTab by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("🔄 Автозагрузка профиля через Shizuku...") }
    var logs by remember { mutableStateOf("🚀 Старт CityEdit Ultimate...\n") }

    fun log(msg: String) {
        statusText = msg
        logs = "$msg\n$logs"
    }

    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var activeProfilePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var isLoaded by remember { mutableStateOf(false) }
    var uiTrigger by remember { mutableIntStateOf(0) } // Триггер для перерисовки списков миссий и покупок

    // 0: Валюта
    val currencyStates = remember { mutableStateMapOf<Long, String>() }

    // 2: Ручной донат (состояние галочек)
    val iapCheckedStates = remember { mutableStateMapOf<String, Boolean>() }

    // 3: Копилка, Магазин и Сезон
    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("250000") }
    var piggyFilled by remember { mutableStateOf(true) }
    var dealAmountInput by remember { mutableStateOf("9999") }
    var selectedDealTag by remember { mutableLongStateOf(-1878560402L) }
    var seasonPurchased by remember { mutableStateOf(true) }
    var seasonPoints by remember { mutableStateOf("15000") }
    var selectedSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.first().first) }

    // 4: Трассы, Квесты и Редактор Миссий
    var selectedCampaignId by remember { mutableStateOf(GameDatabase.SCHEDULED_CAMPAIGNS.first().first) }
    var selectedQuestId by remember { mutableStateOf(GameDatabase.QUESTS.first().first) }
    var selectedQuestMission by remember { mutableLongStateOf(GameDatabase.QUESTS.first().second.first) }
    var customMissionIdInput by remember { mutableStateOf("") }
    var customMissionProgressInput by remember { mutableStateOf("50000") }

    // 5: Статистика, Рекорды и Счётчики
    var playerName by remember { mutableStateOf("Layxtar") }
    var playerTag by remember { mutableStateOf("9CVYN655YJEZHG") }
    var playerLevel by remember { mutableStateOf("50") }
    var playerXp by remember { mutableStateOf("99999") }
    var globalMultiplier by remember { mutableStateOf("0") }
    var boostedMultiplier by remember { mutableStateOf("3") }
    var leaderboardBestScore by remember { mutableStateOf("99999999") }
    var allSurfersHighScoreInput by remember { mutableStateOf("25000000") }

    // 6: Команда (Crew) и Экипировка
    var selectedSurferId by remember { mutableLongStateOf(-496667767L) }
    var selectedBoardId by remember { mutableLongStateOf(1711310740L) }
    var crewSlot1 by remember { mutableLongStateOf(1804257387L) }
    var crewSlot2 by remember { mutableLongStateOf(1614866432L) }
    var crewSlot3 by remember { mutableLongStateOf(852717139L) }

    // 7: Classic и Сырой JSON
    var classicCoins by remember { mutableStateOf("99999999") }
    var classicKeys by remember { mutableStateOf("9999999") }
    var classicBoards by remember { mutableStateOf("99999") }
    var classicBoosters by remember { mutableStateOf("9999") }
    var rawJsonText by remember { mutableStateOf("") }

    fun refreshUIFromProfile(prof: JSONObject) {
        val wallet = prof.optJSONArray("wallet") ?: JSONArray()
        for ((tag, _) in GameDatabase.CURRENCIES) currencyStates[tag] = "0"
        for (i in 0 until wallet.length()) {
            val item = wallet.getJSONObject(i)
            currencyStates[item.getLong("dataTag")] = item.getLong("count").toString()
        }

        val purchases = prof.optJSONObject("purchaseHistory") ?: JSONObject()
        for ((pId, _) in GameDatabase.IAP_CATALOG) {
            iapCheckedStates[pId] = purchases.has(pId)
        }

        val online = prof.optJSONObject("onlinePlayerProfile")
        playerName = online?.optString("name", "Layxtar") ?: "Layxtar"
        playerTag = online?.optString("tag", "9CVYN655YJEZHG") ?: "9CVYN655YJEZHG"
        playerLevel = prof.optInt("level", 50).toString()
        playerXp = prof.optInt("xp", 99999).toString()
        globalMultiplier = prof.optInt("globalScoreRunMultiplier", 0).toString()
        boostedMultiplier = prof.optJSONObject("boostedRunMultiplier")?.optInt("runMultiplier", 3)?.toString() ?: "3"
        selectedSurferId = prof.optLong("character", -496667767L)
        selectedBoardId = prof.optLong("selectedBoard", 1711310740L)

        val crewArr = prof.optJSONArray("crew")
        if (crewArr != null && crewArr.length() >= 3) {
            crewSlot1 = crewArr.getJSONObject(0).optLong("SurferId", 1804257387L)
            crewSlot2 = crewArr.getJSONObject(1).optLong("SurferId", 1614866432L)
            crewSlot3 = crewArr.getJSONObject(2).optLong("SurferId", 852717139L)
        }

        val piggy = prof.optJSONObject("piggyBank")
        if (piggy != null) {
            selectedPiggyId = piggy.optString("currentId", "shop.piggybankmlp7.3")
            piggyProgress = piggy.optInt("lastSeenProgress", 250000).toString()
            piggyFilled = piggy.optBoolean("isFilled", true)
        }

        seasonPurchased = prof.optBoolean("seasonPassPurchased", true)
        seasonPoints = prof.optInt("seasonPassPoints", 15000).toString()
        selectedSeasonId = prof.optString("seasonPassId", GameDatabase.SEASONS.first().first)
        rawJsonText = prof.toString(2)
        uiTrigger++
    }

    fun applyUIToProfile(prof: JSONObject) {
        val wallet = JSONArray()
        for ((tag, _) in GameDatabase.CURRENCIES) {
            val count = currencyStates[tag]?.toLongOrNull() ?: 0L
            wallet.put(JSONObject().put("dataTag", tag).put("count", count))
        }
        prof.put("wallet", wallet)

        val onlineProf = prof.optJSONObject("onlinePlayerProfile") ?: JSONObject()
        onlineProf.put("name", playerName)
        onlineProf.put("tag", playerTag)
        onlineProf.put("renameableAt", "0001-01-01T00:00:00")
        prof.put("onlinePlayerProfile", onlineProf)

        prof.put("level", playerLevel.toIntOrNull() ?: 50)
        prof.put("xp", playerXp.toIntOrNull() ?: 99999)
        prof.put("globalScoreRunMultiplier", globalMultiplier.toIntOrNull() ?: 0)
        prof.put("boostedRunMultiplier", JSONObject().put("runMultiplier", boostedMultiplier.toIntOrNull() ?: 3))
        prof.put("character", selectedSurferId)
        prof.put("selectedBoard", selectedBoardId)

        val crewArr = JSONArray()
            .put(JSONObject().put("SurferId", crewSlot1))
            .put(JSONObject().put("SurferId", crewSlot2))
            .put(JSONObject().put("SurferId", crewSlot3))
        prof.put("crew", crewArr)

        val lbArr = prof.optJSONArray("leaderboardProfiles") ?: JSONArray()
        for (i in 0 until lbArr.length()) {
            lbArr.getJSONObject(i).put("bestScore", leaderboardBestScore.toLongOrNull() ?: 99999999L)
        }
        prof.put("leaderboardProfiles", lbArr)

        prof.put("seasonPassPurchased", seasonPurchased)
        prof.put("seasonPassPoints", seasonPoints.toIntOrNull() ?: 15000)
        prof.put("seasonPassId", selectedSeasonId)
        prof.put("hasSeenPremiumPassActivation", true)
        rawJsonText = prof.toString(2)
    }

    fun loadProfileAutomatically() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, ::log)
            return
        }
        CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, false, ::log)
        val path = CryptoAndSyncManager.resolveCityProfilePath(::log)
        activeProfilePath = path
        val bytes = CryptoAndSyncManager.readFileViaShizuku(path, ::log)
        if (bytes != null) {
            try {
                val (cont, prof) = CryptoAndSyncManager.decryptCityProfile(bytes)
                rawFileBytes = bytes
                containerJson = cont
                profileJson = prof
                refreshUIFromProfile(prof)
                isLoaded = true
                log("✅ Профиль автоматически загружен (${bytes.size} байт)!")
            } catch (e: Exception) {
                log("❌ Ошибка расшифровки профиля: ${e.message}")
            }
        } else {
            log("❌ Не удалось прочитать файл профиля. Запустите игру хотя бы раз.")
        }
    }

    fun saveProfileToGame(launchAfterSave: Boolean) {
        val prof = profileJson ?: return
        val cont = containerJson ?: return
        val raw = rawFileBytes ?: return

        applyUIToProfile(prof)
        CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, true, ::log)
        val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
        val ok = CryptoAndSyncManager.writeFileViaShizuku(activeProfilePath, encryptedBytes, ::log)
        if (ok) {
            rawFileBytes = encryptedBytes
            if (launchAfterSave) {
                CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, ::log)
            } else {
                log("✅ Сохранение успешно синхронизировано с игрой!")
            }
        }
    }

    // Автоматический запуск чтения при открытии приложения и при получении разрешения Shizuku
    LaunchedEffect(Unit) {
        CryptoAndSyncManager.onPermissionGrantedCallback = { granted ->
            if (granted) {
                log("✅ Разрешение Shizuku получено! Загружаем сохранение...")
                loadProfileAutomatically()
            } else {
                log("❌ В разрешении Shizuku отказано!")
            }
        }
        loadProfileAutomatically()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("CityEdit Ultimate (Auto-Sync)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    actions = {
                        if (!CryptoAndSyncManager.hasPermission()) {
                            TextButton(onClick = { CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, ::log) }) {
                                Text("⚡ Shizuku", color = Color(0xFFFFB74D))
                            }
                        }
                    }
                )
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Валюта",
                        "⚡ Мега-Читы",
                        "💳 Донат",
                        "🐷 Копилка & Магазин",
                        "🎯 Миссии & Трассы",
                        "📊 Статистика",
                        "👥 Команда & Герои",
                        "🧰 Бэкап & Classic"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background( Color(0xFF1E1E1E) ).padding(8.dp)) {
                Text(text = statusText, fontSize = 12.sp, color = Color(0xFF81C784), maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(0.9f),
                        onClick = { loadProfileAutomatically() }
                    ) {
                        Text("🔄 Загрузить", fontSize = 12.sp)
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = isLoaded,
                        onClick = { saveProfileToGame(launchAfterSave = false) }
                    ) {
                        Text("💾 Сохранить", fontSize = 12.sp)
                    }
                    Button(
                        modifier = Modifier.weight(1.2f),
                        enabled = isLoaded,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = { saveProfileToGame(launchAfterSave = true) }
                    ) {
                        Text("🚀 В Игру", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            when (selectedTab) {
                // ==================== ВКЛАДКА 0: ВАЛЮТА ====================
                0 -> {
                    Text("Управление кошельком (Wallet). Быстрые кнопки применяются ко всем 9 валютам сразу:", fontSize = 13.sp, color = Color.LightGray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(modifier = Modifier.weight(1f), onClick = {
                            GameDatabase.CURRENCIES.forEach { (tag, _) ->
                                val cur = currencyStates[tag]?.toLongOrNull() ?: 0L
                                currencyStates[tag] = (cur + 100000L).toString()
                            }
                            log("➕ Ко всем валютам добавлено +100 000")
                        }) { Text("+100K") }

                        Button(modifier = Modifier.weight(1f), onClick = {
                            GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                            log("💎 Все 9 валют выставлены на 9 999 999")
                        }) { Text("9.99M MAX") }

                        OutlinedButton(modifier = Modifier.weight(0.8f), onClick = {
                            GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "0" }
                            log("🧹 Все валюты обнулены")
                        }) { Text("Сброс") }
                    }

                    GameDatabase.CURRENCIES.forEach { (tag, label) ->
                        OutlinedTextField(
                            value = currencyStates[tag] ?: "0",
                            onValueChange = { currencyStates[tag] = it },
                            label = { Text("$label [ID: $tag]") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ==================== ВКЛАДКА 1: МЕГА-ЧИТЫ (1 КЛИК) ====================
                1 -> {
                    Text("Каждая кнопка ниже мгновенно вносит изменения в загруженный профиль. Подсказка под кнопкой объясняет, что именно она делает:", fontSize = 13.sp, color = Color.LightGray)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                onClick = {
                                    profileJson?.let {
                                        GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                                        CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                        CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                                        CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                        CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                                        CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                        CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(it)
                                        CryptoAndSyncManager.hackDailyDealsAndAdRewards(it)
                                        CryptoAndSyncManager.maxDailyTrialsAndSkipTutorials(it)
                                        refreshUIFromProfile(it)
                                        log("👑 РЕЖИМ БОГА АКТИВИРОВАН: Открыто и прокачано абсолютно всё!")
                                    }
                                }
                            ) { Text("👑 АКТИВИРОВАТЬ GOD-MODE (ВСЁ В 1 КЛИК)", color = Color.White, fontWeight = FontWeight.Bold) }
                            Text("Выставляет по 9.99 млн валют, прокачивает всех 27 сёрферов и 29 досок до 20 ур., открывает 125 скинов, 41 аватар, весь донат, все 6 районов на 3 звезды и 100% джекпот в боксах.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                    refreshUIFromProfile(it)
                                    log("🏄 Все 27 сёрферов открыты, 20 Ур. + 40 тир Platinum!")
                                }
                            }) { Text("🏄 Открыть всех 27 сёрферов (20 Ур. + Platinum)") }
                            Text("Разблокирует всех обычных, сезонных и скрытых персонажей (Lilah, Jenny, Monique, Rey Romeo), ставит им 20-й уровень и открывает 40-й платиновый ранг в ладдерах.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                                    refreshUIFromProfile(it)
                                    log("🛹 Все 29 ховербордов открыты и прокачаны до 20 уровня!")
                                }
                            }) { Text("🛹 Открыть все 29 досок + секретные (20 Ур.)") }
                            Text("Открывает все ховерборды, включая 13 скрытых досок и эксклюзивную доску Classic из Google Play, с максимальной прокачкой способностей до 20 уровня.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                    refreshUIFromProfile(it)
                                    log("🎭 Открыты все 125 костюмов и 41 аватар профиля!")
                                }
                            }) { Text("🎭 Открыть все 125 скинов и 41 аватар") }
                            Text("Разблокирует абсолютно все образы сёрферов (включая Trackstar, Incognito и Хэллоуин 2026) и все платные/секретные аватарки профиля.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                                    refreshUIFromProfile(it)
                                    log("🗺️ Все 6 районов и 196 этапов кампании закрыты на 3 звезды!")
                                }
                            }) { Text("🗺️ Пройти всю кампанию (Все 6 районов на 3 ⭐)") }
                            Text("Разблокирует все районы City Tour и отмечает все 3 цели (goals) в каждом из 196 этапов как выполненные.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(it)
                                    CryptoAndSyncManager.maxDailyTrialsAndSkipTutorials(it)
                                    refreshUIFromProfile(it)
                                    log("🎰 100% Джекпот боксов, Секретные Звёзды, 12 Ачивок и 50 ур. Daily Trials выданы!")
                                }
                            }) { Text("🎰 100% Джекпот Боксов + Секретные Звёзды + Без Туториалов") }
                            Text("Ставит счётчик жалости (PityCounter) на 9/10 для гарантированного джекпота в следующем сундуке, выдаёт все секретные звёзды, выполняет 12 достижений, ставит 50-й уровень Daily Trials и отключает обучающие окна.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                // ==================== ВКЛАДКА 2: РУЧНОЙ ДОНАТ И ПРОПУСКА ====================
                2 -> {
                    val _trigger = uiTrigger
                    Text("Управление платными покупками (purchaseHistory). Ты можешь выдать весь донат одной кнопкой или вручную включить/выключить любую покупку галочкой:", fontSize = 13.sp, color = Color.LightGray)

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                refreshUIFromProfile(it)
                                log("💳 Все 63 донатных пакета и платные пропуска трасс добавлены, спам баннеров отключён!")
                            }
                        }
                    ) { Text("💳 Разблокировать ВЕСЬ Донат + Убрать спам баннеров") }

                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Text("Ручное переключение отдельных покупок:", fontWeight = FontWeight.Bold)

                    GameDatabase.IAP_CATALOG.forEach { (productId, title) ->
                        val isChecked = iapCheckedStates[productId] == true
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        iapCheckedStates[productId] = checked
                                        profileJson?.let { prof ->
                                            CryptoAndSyncManager.toggleSinglePurchase(prof, productId, checked)
                                            log(if (checked) "✅ Добавлена покупка: $productId" else "🗑️ Удалена покупка: $productId")
                                        }
                                    }
                                )
                                Column {
                                    Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(text = "ID: $productId", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // ==================== ВКЛАДКА 3: КОПИЛКА, МАГАЗИН И СЕЗОН ====================
                3 -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🐷 Редактор Свиньи-Копилки (Piggy Bank)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Позволяет выбрать уровень копилки (до 250 000 монет и 5x множителя) и мгновенно сделать её полной для сбора.", fontSize = 12.sp, color = Color.Gray)

                            OutlinedTextField(
                                value = piggyProgress,
                                onValueChange = { piggyProgress = it },
                                label = { Text("Монет внутри копилки (до 250 000)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = piggyFilled, onCheckedChange = { piggyFilled = it })
                                Text("Копилка полностью заполнена (готова к открытию)")
                            }
                            GameDatabase.PIGGY_BANKS.forEach { (pId, pair) ->
                                val (mId, desc) = pair
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedPiggyId == pId,
                                        onClick = { selectedPiggyId = pId; selectedPiggyMission = mId }
                                    )
                                    Text("$desc [$pId]", fontSize = 13.sp)
                                }
                            }
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.configurePiggyBank(it, selectedPiggyId, selectedPiggyMission, piggyProgress.toIntOrNull() ?: 250000, piggyFilled)
                                    refreshUIFromProfile(it)
                                    log("🐷 Копилка $selectedPiggyId настроена на $piggyProgress монет!")
                                }
                            }) { Text("🐷 Применить Свинью-Копилку") }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛒 Генератор Витрины Магазина и Рулетки (Daily Deals)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Заменяет все 6 ежедневных слотов магазина и 9 слотов рекламной рулетки на выбранный ресурс в нужном количестве.", fontSize = 12.sp, color = Color.Gray)

                            OutlinedTextField(
                                value = dealAmountInput,
                                onValueChange = { dealAmountInput = it },
                                label = { Text("Количество ресурса в каждом слоте") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Что поставить на витрину:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            GameDatabase.CURRENCIES.forEach { (tag, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedDealTag == tag, onClick = { selectedDealTag = tag })
                                    Text(name, fontSize = 13.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    profileJson?.let {
                                        CryptoAndSyncManager.hackDailyDealsAndAdRewards(it, dealAmountInput.toIntOrNull() ?: 9999, selectedDealTag)
                                        refreshUIFromProfile(it)
                                        log("🛒 Все слоты магазина заполнены выбранным ресурсом ($dealAmountInput шт.)!")
                                    }
                                }) { Text("Выбранный ресурс") }

                                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                    profileJson?.let {
                                        CryptoAndSyncManager.hackDailyDealsAndAdRewards(it, dealAmountInput.toIntOrNull() ?: 9999, null)
                                        refreshUIFromProfile(it)
                                        log("🛒 Витрина заполнена миксом редких кредитов и жетонов!")
                                    }
                                }) { Text("Микс всех редких") }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎟️ Сезонный Пропуск (Season Pass)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = seasonPurchased, onCheckedChange = { seasonPurchased = it })
                                Text("Активировать платный Premium Pass")
                            }
                            OutlinedTextField(
                                value = seasonPoints,
                                onValueChange = { seasonPoints = it },
                                label = { Text("Очки сезона (15 000 открывает все тиры наград)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            GameDatabase.SEASONS.forEach { (sId, sName) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedSeasonId == sId, onClick = { selectedSeasonId = sId })
                                    Text(sName, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // ==================== ВКЛАДКА 4: МИССИИ, КВЕСТЫ И ТРАССЫ ====================
                4 -> {
                    val _trigger = uiTrigger
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🎢 Дополнительные трассы (Weekly Wonders)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Активирует любую специальную еженедельную трассу вне расписания и проходит все её 7 этапов на 3 звезды.", fontSize = 12.sp, color = Color.Gray)
                            GameDatabase.SCHEDULED_CAMPAIGNS.forEach { (cId, cTitle) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedCampaignId == cId, onClick = { selectedCampaignId = cId })
                                    Text(cTitle, fontSize = 13.sp)
                                }
                            }
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.activateScheduledCampaign(it, selectedCampaignId)
                                    refreshUIFromProfile(it)
                                    log("🎢 Трасса $selectedCampaignId активирована!")
                                }
                            }) { Text("🎢 Включить выбранную трассу") }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🏆 Праздничные и Секретные Квесты (Quests)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Добавляет выбранный квест в профиль с прогрессом 50 000 (готов к получению наград).", fontSize = 12.sp, color = Color.Gray)
                            GameDatabase.QUESTS.forEach { (qId, pair) ->
                                val (mId, qTitle) = pair
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedQuestId == qId,
                                        onClick = { selectedQuestId = qId; selectedQuestMission = mId }
                                    )
                                    Text(qTitle, fontSize = 13.sp)
                                }
                            }
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.activateAndCompleteQuest(it, selectedQuestId, selectedQuestMission)
                                    refreshUIFromProfile(it)
                                    log("🏆 Квест $selectedQuestId активирован и завершён!")
                                }
                            }) { Text("🏆 Активировать и выполнить квест") }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎯 Полный Редактор Миссий (missionProgresses)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Здесь отображаются все миссии вашего профиля. Нажмите «Завершить все», чтобы сделать их выполненными и готовыми к сбору наград в игре.", fontSize = 12.sp, color = Color.Gray)

                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.completeAllExistingMissions(it, 99999)
                                    refreshUIFromProfile(it)
                                    log("🎯 Все текущие миссии отмечены выполненными (готовы к сбору наград)!")
                                }
                            }) { Text("🎯 Завершить ВСЕ текущие миссии (isComplete = true)") }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = customMissionIdInput,
                                    onValueChange = { customMissionIdInput = it },
                                    label = { Text("ID новой миссии") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customMissionProgressInput,
                                    onValueChange = { customMissionProgressInput = it },
                                    label = { Text("Прогресс") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    val mId = customMissionIdInput.toLongOrNull() ?: return@Button
                                    val prog = customMissionProgressInput.toIntOrNull() ?: 50000
                                    profileJson?.let { prof ->
                                        val arr = prof.optJSONArray("missionProgresses") ?: JSONArray()
                                        arr.put(JSONObject().apply {
                                            put("id", mId); put("progress", prog)
                                            put("isComplete", true); put("isClaimed", false)
                                            put("featureType", "Custom")
                                        })
                                        prof.put("missionProgresses", arr)
                                        refreshUIFromProfile(prof)
                                        log("➕ Добавлена миссия ID $mId")
                                    }
                                }) { Text("+") }
                            }

                            val missionsArr = profileJson?.optJSONArray("missionProgresses") ?: JSONArray()
                            Text("Всего активных записей миссий: ${missionsArr.length()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                            val displayCount = minOf(missionsArr.length(), 40)
                            for (i in 0 until displayCount) {
                                val mObj = missionsArr.getJSONObject(i)
                                val mId = mObj.optLong("id")
                                val fType = mObj.optString("featureType", "Mission")
                                val prog = mObj.optInt("progress", 0)
                                val comp = mObj.optBoolean("isComplete", false)
                                val claimed = mObj.optBoolean("isClaimed", false)

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("[$fType] ID: $mId", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Прогресс: $prog | Забрана: $claimed", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Button(
                                        onClick = {
                                            mObj.put("progress", maxOf(prog, 50000))
                                            mObj.put("isComplete", true)
                                            mObj.put("isClaimed", false)
                                            uiTrigger++
                                            log("🎯 Миссия $mId готова к сбору награды!")
                                        }
                                    ) {
                                        Text(if (comp && !claimed) "✅ Готова" else "Выполнить", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== ВКЛАДКА 5: СТАТИСТИКА, СЧЁТЧИКИ И РЕКОРДЫ ====================
                5 -> {
                    val _trigger = uiTrigger
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📊 Основная Статистика и Лидерборды", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм игрока (сброшен кулдаун смены)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = playerTag, onValueChange = { playerTag = it }, label = { Text("Сетевой тег профиля (Player Tag)") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень (1-50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = playerXp, onValueChange = { playerXp = it }, label = { Text("Опыт (XP)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = globalMultiplier, onValueChange = { globalMultiplier = it }, label = { Text("Глобальный множитель") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = boostedMultiplier, onValueChange = { boostedMultiplier = it }, label = { Text("Буст забега (x3..x50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = leaderboardBestScore, onValueChange = { leaderboardBestScore = it }, label = { Text("🏆 Рекорд в еженедельном турнире (Leaderboard Score)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = allSurfersHighScoreInput,
                                    onValueChange = { allSurfersHighScoreInput = it },
                                    label = { Text("Рекорд для каждого из 27 сёрферов") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    val score = allSurfersHighScoreInput.toLongOrNull() ?: 25000000L
                                    profileJson?.let {
                                        CryptoAndSyncManager.unlockAndMaxAllSurfers(it, score)
                                        refreshUIFromProfile(it)
                                        log("🏆 Всем 27 сёрферам установлен личный рекорд $score!")
                                    }
                                }) { Text("Применить") }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔢 Внутренние Счётчики Игры (Counters)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Нажмите «+10» или «MAX», чтобы увеличить любой внутренний счётчик профиля (ранги сёрферов, секретные звёзды, гаранты боксов):", fontSize = 12.sp, color = Color.Gray)

                            val countersArr = profileJson?.optJSONArray("counters") ?: JSONArray()
                            val countLimit = minOf(countersArr.length(), 35)
                            for (i in 0 until countLimit) {
                                val cObj = countersArr.getJSONObject(i)
                                val cType = cObj.optString("CounterType")
                                val cId = cObj.optString("Id")
                                val cVal = cObj.optInt("Count", 0)
                                if (cType == "HomeOfferNotificationViewCounter") continue

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("$cType", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("ID: $cId | Значение: $cVal", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            cObj.put("Count", cVal + 10)
                                            uiTrigger++
                                        }) { Text("+10", fontSize = 11.sp) }
                                        Button(onClick = {
                                            cObj.put("Count", 40)
                                            uiTrigger++
                                        }) { Text("40", fontSize = 11.sp) }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== ВКЛАДКА 6: КОМАНДА (CREW) И ЭКИПИРОВКА ====================
                6 -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("👥 Конструктор Команды (Crew — 3 слота перков)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Вы можете поставить в команду любых 3 сёрферов (даже секретных), чтобы их пассивные навыки работали одновременно во время забега.", fontSize = 12.sp, color = Color.Gray)

                            OutlinedTextField(value = crewSlot1.toString(), onValueChange = { crewSlot1 = it.toLongOrNull() ?: crewSlot1 }, label = { Text("Слот команды #1 (Surfer ID)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = crewSlot2.toString(), onValueChange = { crewSlot2 = it.toLongOrNull() ?: crewSlot2 }, label = { Text("Слот команды #2 (Surfer ID)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = crewSlot3.toString(), onValueChange = { crewSlot3 = it.toLongOrNull() ?: crewSlot3 }, label = { Text("Слот команды #3 (Surfer ID)") }, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🏄 Выбор активного сёрфера (и быстрое добавление в Crew):", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            GameDatabase.SURFERS.forEach { (id, name) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        RadioButton(selected = selectedSurferId == id, onClick = { selectedSurferId = id })
                                        Text(name, fontSize = 13.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        TextButton(onClick = { crewSlot1 = id; log("👥 $name -> Слот 1") }) { Text("С1", fontSize = 11.sp) }
                                        TextButton(onClick = { crewSlot2 = id; log("👥 $name -> Слот 2") }) { Text("С2", fontSize = 11.sp) }
                                        TextButton(onClick = { crewSlot3 = id; log("👥 $name -> Слот 3") }) { Text("С3", fontSize = 11.sp) }
                                    }
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🛹 Выбор активного ховерборда:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            GameDatabase.BOARDS.forEach { (id, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedBoardId == id, onClick = { selectedBoardId = id })
                                    Text(name, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // ==================== ВКЛАДКА 7: БЭКАП, CLASSIC, ПАТЧЕР И ЛОГИ ====================
                7 -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📂 Экспорт в Download/CityEdit и Бэкапы", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Сохраняет копию профиля или расшифрованный JSON в папку /storage/emulated/0/Download/CityEdit (как в оригинальном CityEdit).", fontSize = 12.sp, color = Color.Gray)

                            Button(modifier = Modifier.fillMaxWidth(), enabled = isLoaded, onClick = {
                                profileJson?.let { prof ->
                                    applyUIToProfile(prof)
                                    CryptoAndSyncManager.exportToDownloadDir("profile_decrypted.json", prof.toString(2).toByteArray(Charsets.UTF_8), ::log)
                                }
                            }) { Text("📄 Экспортировать расшифрованный JSON в Download/CityEdit") }

                            Button(modifier = Modifier.fillMaxWidth(), enabled = isLoaded, onClick = {
                                rawFileBytes?.let { bytes ->
                                    CryptoAndSyncManager.exportToDownloadDir("profile_backup_encrypted.bin", bytes, ::log)
                                }
                            }) { Text("📦 Экспортировать зашифрованный сейв в Download/CityEdit") }

                            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                if (CryptoAndSyncManager.restoreBackup(activeProfilePath, ::log)) {
                                    loadProfileAutomatically()
                                    log("⏪ Оригинальный бэкап (.bak_editor) успешно восстановлен!")
                                } else {
                                    log("❌ Бэкап .bak_editor ещё не создан!")
                                }
                            }) { Text("⏪ Откатить изменения из резервной копии (.bak_editor)") }

                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                CryptoAndSyncManager.patchGameConfigsViaShizuku(activity.cacheDir, ::log)
                            }) { Text("🛠️ Пропатчить кэш конфигов (0 цена билетов + SHA-1)") }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🛹 Классический Subway Surfers (com.kiloo.subwaysurf)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            OutlinedTextField(value = classicCoins, onValueChange = { classicCoins = it }, label = { Text("🪙 Монеты Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = classicKeys, onValueChange = { classicKeys = it }, label = { Text("🔑 Ключи Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = classicBoards, onValueChange = { classicBoards = it }, label = { Text("🛹 Доски Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = classicBoosters, onValueChange = { classicBoosters = it }, label = { Text("🚀 Бустеры и Фора Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CLASSIC, true, ::log)
                                val bytes = CryptoAndSyncManager.readFileViaShizuku(CryptoAndSyncManager.CLASSIC_WALLET_PATH, ::log)
                                if (bytes != null) {
                                    val outer = JSONObject(String(bytes, Charsets.UTF_8))
                                    val innerData = JSONObject(outer.getString("data"))
                                    val currencies = innerData.optJSONObject("currencies") ?: JSONObject()
                                    fun setC(id: String, v: String) {
                                        val cObj = currencies.optJSONObject(id) ?: JSONObject()
                                        cObj.put("value", v.toLongOrNull() ?: 9999999L)
                                        currencies.put(id, cObj)
                                    }
                                    setC("1", classicCoins); setC("2", classicKeys)
                                    setC("3", classicBoards); setC("4", classicBoosters); setC("5", classicBoosters)
                                    innerData.put("currencies", currencies)
                                    outer.put("data", innerData.toString())
                                    val ok = CryptoAndSyncManager.writeFileViaShizuku(CryptoAndSyncManager.CLASSIC_WALLET_PATH, outer.toString().toByteArray(Charsets.UTF_8), ::log)
                                    if (ok) log("✅ Классический Subway Surfers синхронизирован!")
                                } else {
                                    log("❌ wallet.json классического Subway Surfers не найден!")
                                }
                            }) { Text("💾 Сохранить и синхронизировать Classic") }
                        }
                    }

                    Text("📜 Журнал операций Shizuku:", fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Color.Black).padding(8.dp)) {
                        Text(text = logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }

                    Text("📝 Прямой редактор расшифрованного JSON профиля:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = rawJsonText,
                        onValueChange = { rawJsonText = it },
                        label = { Text("Внутренний JSON (profile)") },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        try {
                            val updated = JSONObject(rawJsonText)
                            profileJson = updated
                            refreshUIFromProfile(updated)
                            log("✅ Изменения из сырого JSON применены в память!")
                        } catch (e: Exception) {
                            log("❌ Ошибка синтаксиса JSON: ${e.message}")
                        }
                    }) { Text("Применить правки из текстового JSON") }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
