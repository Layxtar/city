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
    // Режим приложения: true = Subway Surfers City, false = Classic Subway Surfers
    var isCityMode by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("🔄 Автозагрузка профиля через Shizuku...") }
    var logs by remember { mutableStateOf("🚀 Старт CityEdit & Classic Ultimate...\n") }

    fun log(msg: String) {
        statusText = msg
        logs = "$msg\n$logs"
    }

    // Состояния Subway Surfers City
    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var activeProfilePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var isCityLoaded by remember { mutableStateOf(false) }
    var uiTrigger by remember { mutableIntStateOf(0) }

    val currencyStates = remember { mutableStateMapOf<Long, String>() }
    val iapCheckedStates = remember { mutableStateMapOf<String, Boolean>() }

    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("250000") }
    var piggyFilled by remember { mutableStateOf(true) }
    var piggyFreeClaim by remember { mutableStateOf(true) }

    var dealAmountInput by remember { mutableStateOf("9999") }
    var selectedDealTag by remember { mutableLongStateOf(-1878560402L) }
    var seasonPurchased by remember { mutableStateOf(true) }
    var seasonPoints by remember { mutableStateOf("15000") }
    var selectedSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.first().first) }

    var selectedCampaignId by remember { mutableStateOf(GameDatabase.SCHEDULED_CAMPAIGNS.first().first) }
    var selectedQuestId by remember { mutableStateOf(GameDatabase.QUESTS.first().first) }
    var selectedQuestMission by remember { mutableLongStateOf(GameDatabase.QUESTS.first().second.first) }

    var playerName by remember { mutableStateOf("Layxtar") }
    var playerTag by remember { mutableStateOf("9CVYN655YJEZHG") }
    var playerLevel by remember { mutableStateOf("50") }
    var playerXp by remember { mutableStateOf("99999") }
    var globalMultiplier by remember { mutableStateOf("0") }
    var boostedMultiplier by remember { mutableStateOf("3") }
    var leaderboardBestScore by remember { mutableStateOf("99999999") }
    var allSurfersHighScoreInput by remember { mutableStateOf("25000000") }

    var selectedSurferId by remember { mutableLongStateOf(-496667767L) }
    var selectedBoardId by remember { mutableLongStateOf(1711310740L) }
    var crewSlot1 by remember { mutableLongStateOf(1804257387L) }
    var crewSlot2 by remember { mutableLongStateOf(1614866432L) }
    var crewSlot3 by remember { mutableLongStateOf(852717139L) }
    var rawJsonText by remember { mutableStateOf("") }

    // ==================== СОСТОЯНИЯ CLASSIC SUBWAY SURFERS ====================
    val classicCurrencies = remember {
        mutableStateMapOf<String, String>().apply {
            ClassicDatabase.CURRENCIES.forEach { (id, _) -> put(id, "9999999") }
        }
    }
    var classicSearchQuery by remember { mutableStateOf("") }
    var selectedClassicLootbox by remember { mutableStateOf(ClassicDatabase.LOOTBOX_TYPES.first().first) }
    var classicLootboxCount by remember { mutableStateOf("50") }

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
        rawJsonText = prof.toString(2)
        uiTrigger++
    }

    fun applyUIToCityProfile(prof: JSONObject) {
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

        prof.put("seasonPassPurchased", seasonPurchased)
        prof.put("seasonPassPoints", seasonPoints.toIntOrNull() ?: 15000)
        prof.put("seasonPassId", selectedSeasonId)
        prof.put("hasSeenPremiumPassActivation", true)
        rawJsonText = prof.toString(2)
    }

    fun loadClassicWalletUI() {
        val (_, wallet) = ClassicCryptoAndSyncManager.readClassicJson("wallet.json", ::log)
        val currencies = wallet.optJSONObject("currencies")
        if (currencies != null) {
            for ((id, _) in ClassicDatabase.CURRENCIES) {
                val v = currencies.optJSONObject(id)?.optLong("value", 0L) ?: 0L
                classicCurrencies[id] = v.toString()
            }
            log("✅ Кошелёк Classic Subway Surfers успешно загружен!")
        }
    }

    fun loadProfileAutomatically() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, ::log)
            return
        }
        if (isCityMode) {
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
                    refreshUIFromCityProfile(prof)
                    isCityLoaded = true
                    log("✅ Профиль City автоматически загружен (${bytes.size} байт)!")
                } catch (e: Exception) {
                    log("❌ Ошибка расшифровки City: ${e.message}")
                }
            }
        } else {
            loadClassicWalletUI()
        }
    }

    fun saveCityProfile(launchAfterSave: Boolean) {
        val prof = profileJson ?: return
        val cont = containerJson ?: return
        val raw = rawFileBytes ?: return

        applyUIToCityProfile(prof)
        CryptoAndSyncManager.stopGameForSync(CryptoAndSyncManager.PKG_CITY, true, ::log)
        val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
        if (CryptoAndSyncManager.writeFileViaShizuku(activeProfilePath, encryptedBytes, ::log)) {
            rawFileBytes = encryptedBytes
            if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, ::log)
        }
    }

    LaunchedEffect(Unit) {
        CryptoAndSyncManager.onPermissionGrantedCallback = { granted ->
            if (granted) loadProfileAutomatically()
        }
        loadProfileAutomatically()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Subway Ultimate Editor", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { isCityMode = true; selectedTab = 0; loadProfileAutomatically() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isCityMode) Color(0xFF1976D2) else Color.DarkGray)
                            ) { Text("🏙️ CITY", fontSize = 12.sp) }

                            Button(
                                onClick = { isCityMode = false; selectedTab = 0; loadProfileAutomatically() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (!isCityMode) Color(0xFFE65100) else Color.DarkGray)
                            ) { Text("🛹 CLASSIC", fontSize = 12.sp) }
                        }
                    }
                )
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = if (isCityMode) {
                        listOf("💰 Валюта", "⚡ Мега-Читы", "💳 Донат", "🐷 Копилка & Триалы", "🎯 Миссии & Трассы", "📊 Статистика", "👥 Команда", "📂 Бэкап & Логи")
                    } else {
                        listOf("💰 Валюта & Билеты", "⚡ Читы Classic", "🔍 Поиск Сёрферов", "🛹 Поиск Досок", "🎁 Боксы & Сезон")
                    }
                    tabs.forEachIndexed { idx, title ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp)) {
                Text(text = statusText, fontSize = 12.sp, color = Color(0xFF81C784), maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(modifier = Modifier.weight(0.9f), onClick = { loadProfileAutomatically() }) {
                        Text("🔄 Загрузить", fontSize = 12.sp)
                    }
                    if (isCityMode) {
                        Button(modifier = Modifier.weight(1f), enabled = isCityLoaded, onClick = { saveCityProfile(false) }) {
                            Text("💾 Сохранить", fontSize = 12.sp)
                        }
                        Button(
                            modifier = Modifier.weight(1.2f),
                            enabled = isCityLoaded,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            onClick = { saveCityProfile(true) }
                        ) { Text("🚀 В City", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    } else {
                        Button(
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            onClick = {
                                val map = classicCurrencies.mapValues { it.value.toLongOrNull() ?: 9999999L }
                                ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, true, null, 0, ::log)
                            }
                        ) { Text("💾 Сохранить Classic", fontSize = 12.sp) }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            onClick = { CryptoAndSyncManager.launchGame(activity, ClassicCryptoAndSyncManager.PKG_CLASSIC, ::log) }
                        ) { Text("🚀 В Classic", fontSize = 12.sp) }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            if (isCityMode) {
                // ==================== РЕЖИМ SUBWAY SURFERS CITY ====================
                when (selectedTab) {
                    0 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(modifier = Modifier.weight(1f), onClick = {
                                GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                                log("💎 Все 9 валют City выставлены на 9 999 999")
                            }) { Text("9.99M MAX") }
                        }
                        GameDatabase.CURRENCIES.forEach { (tag, label) ->
                            OutlinedTextField(
                                value = currencyStates[tag] ?: "0",
                                onValueChange = { currencyStates[tag] = it },
                                label = { Text("$label [ID:$tag]") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    1 -> {
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
                                    CryptoAndSyncManager.configurePiggyBank(it, "shop.piggybankmlp7.3", -2000200805L, 250000, true, true)
                                    refreshUIFromCityProfile(it)
                                    log("👑 GOD-MODE CITY АКТИВИРОВАН: Открыто всё + Триалы + Бесплатная Свинья!")
                                }
                            }
                        ) { Text("👑 GOD-MODE CITY (ВСЁ + ТРИАЛЫ + БЕСПЛАТНАЯ СВИНЬЯ)", color = Color.White, fontWeight = FontWeight.Bold) }

                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.maxDailyTrialsAndSkipTutorials(it)
                                refreshUIFromCityProfile(it)
                                log("🏁 Все 6 видов Триалов (ивентовых забегов) открыты на 50 уровне без кулдаунов!")
                            }
                        }) { Text("🏁 Открыть все Триалы (Ивентовые забеги 50 Ур.)") }

                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.configurePiggyBank(it, "shop.piggybankmlp7.3", -2000200805L, 250000, true, true)
                                refreshUIFromCityProfile(it)
                                log("🐷 Свинья-копилка заполнена на 250 000 монет и разблокирована для БЕСПЛАТНОГО сбора!")
                            }
                        }) { Text("🐷 Сделать Свинью-Копилку БЕСПЛАТНОЙ (250 000 монет)") }
                    }

                    2 -> {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                refreshUIFromCityProfile(it)
                                log("💳 Весь донат City разблокирован!")
                            }
                        }) { Text("💳 Разблокировать ВЕСЬ Донат City") }

                        GameDatabase.IAP_CATALOG.forEach { (productId, title) ->
                            val isChecked = iapCheckedStates[productId] == true
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isChecked, onCheckedChange = { checked ->
                                    iapCheckedStates[productId] = checked
                                    profileJson?.let { CryptoAndSyncManager.toggleSinglePurchase(it, productId, checked) }
                                })
                                Text(title, fontSize = 13.sp)
                            }
                        }
                    }

                    3 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🐷 Свинья-Копилка (Бесплатный выкуп)", fontWeight = FontWeight.Bold)
                                OutlinedTextField(value = piggyProgress, onValueChange = { piggyProgress = it }, label = { Text("Монет в копилке") }, modifier = Modifier.fillMaxWidth())
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = piggyFreeClaim, onCheckedChange = { piggyFreeClaim = it })
                                    Text("Забрать копилку БЕСПЛАТНО (добавить в купленные IAP)")
                                }
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    profileJson?.let {
                                        CryptoAndSyncManager.configurePiggyBank(it, selectedPiggyId, selectedPiggyMission, piggyProgress.toIntOrNull() ?: 250000, piggyFilled, piggyFreeClaim)
                                        refreshUIFromCityProfile(it)
                                        log("🐷 Копилка обновлена (Бесплатный сбор = $piggyFreeClaim)!")
                                    }
                                }) { Text("🐷 Применить Копилку") }
                            }
                        }
                    }

                    4 -> {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.activateScheduledCampaign(it, selectedCampaignId)
                                refreshUIFromCityProfile(it)
                                log("🎢 Ивентовая трасса $selectedCampaignId открыта для всех сёрферов!")
                            }
                        }) { Text("🎢 Открыть выбранную трассу (Weekly Wonders)") }
                        GameDatabase.SCHEDULED_CAMPAIGNS.forEach { (cId, cTitle) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedCampaignId == cId, onClick = { selectedCampaignId = cId })
                                Text(cTitle, fontSize = 13.sp)
                            }
                        }
                    }

                    5 -> {
                        OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм игрока") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = boostedMultiplier, onValueChange = { boostedMultiplier = it }, label = { Text("Буст забега (x3..x50)") }, modifier = Modifier.fillMaxWidth())
                    }

                    6 -> {
                        Text("Активный сёрфер и команда Crew:", fontWeight = FontWeight.Bold)
                        GameDatabase.SURFERS.forEach { (id, name) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedSurferId == id, onClick = { selectedSurferId = id })
                                Text(name, fontSize = 13.sp)
                            }
                        }
                    }

                    7 -> {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black).padding(8.dp)) {
                            Text(text = logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                        }
                    }
                }
            } else {
                // ==================== РЕЖИМ CLASSIC SUBWAY SURFERS ====================
                when (selectedTab) {
                    // Вкладка 0: Валюта, Билеты Забегов, SkipAdTickets
                    0 -> {
                        Text("Редактор `wallet.json` (путь `/storage/emulated/0/Android/data/com.kiloo.subwaysurf/files/profile/wallet.json`):", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(modifier = Modifier.weight(1f), onClick = {
                                ClassicDatabase.CURRENCIES.forEach { (id, _) -> classicCurrencies[id] = "99999999" }
                                log("💎 Все валюты, билеты арен и жетоны Classic выставлены на 99 999 999!")
                            }) { Text("💎 ВСЁ НА 99.9M") }
                        }
                        ClassicDatabase.CURRENCIES.forEach { (id, title) ->
                            OutlinedTextField(
                                value = classicCurrencies[id] ?: "9999999",
                                onValueChange = { classicCurrencies[id] = it },
                                label = { Text("$title [ID:$id]") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Вкладка 1: Мега-Читы для Classic (Двойные монеты, Убрать рекламу, Открыть донат)
                    1 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    onClick = {
                                        val map = classicCurrencies.mapValues { 99999999L }
                                        ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, true, "super_mystery_box", 25, ::log)
                                        ClassicCryptoAndSyncManager.unlockDoubleCoinsAndMaxUpgrades(::log)
                                        ClassicCryptoAndSyncManager.unlockClassicSurfers(null, null, ::log)
                                        ClassicCryptoAndSyncManager.unlockClassicBoards(null, null, ::log)
                                        ClassicCryptoAndSyncManager.maxSeasonHuntMissionsAndAchievements(::log)
                                        log("👑 CLASSIC GOD-MODE АКТИВИРОВАН!")
                                    }
                                ) { Text("👑 АКТИВИРОВАТЬ GOD-MODE ДЛЯ CLASSIC", color = Color.White, fontWeight = FontWeight.Bold) }

                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    ClassicCryptoAndSyncManager.unlockDoubleCoinsAndMaxUpgrades(::log)
                                }) { Text("🪙 Двойные монеты + Множитель + Бустеры 6 Ур.") }

                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    val map = classicCurrencies.mapValues { it.value.toLongOrNull() ?: 9999999L }
                                    ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, true, null, 0, ::log)
                                    log("🚫 Реклама отключена (no_ads_bundle + SkipAdTickets), весь донат активирован!")
                                }) { Text("🚫 Убрать рекламу + Открыть Донат и Surfer Pass") }

                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    ClassicCryptoAndSyncManager.maxSeasonHuntMissionsAndAchievements(::log)
                                }) { Text("🏆 Закрыть Season Hunt (40 тиров) + Ачивки + Трофеи") }
                            }
                        }
                    }

                    // Вкладка 2: Поиск и разблокировка Сёрферов Classic
                    2 -> {
                        OutlinedTextField(
                            value = classicSearchQuery,
                            onValueChange = { classicSearchQuery = it },
                            label = { Text("🔍 Поиск сёрфера по имени или ID...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            ClassicCryptoAndSyncManager.unlockClassicSurfers(null, null, ::log)
                        }) { Text("🏄 Открыть ВСЕХ сёрферов и костюмы в 1 клик") }

                        val filteredSurfers = ClassicDatabase.CLASSIC_SURFERS.filter {
                            it.first.contains(classicSearchQuery, ignoreCase = true) ||
                            it.second.first.contains(classicSearchQuery, ignoreCase = true)
                        }

                        filteredSurfers.forEach { (id, pair) ->
                            val (name, outfits) = pair
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("ID: $id | Костюмов: ${outfits.size} (${outfits.joinToString()})", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(onClick = {
                                            ClassicCryptoAndSyncManager.unlockClassicSurfers(id, null, ::log)
                                        }) { Text("Открыть", fontSize = 11.sp) }
                                        Button(onClick = {
                                            ClassicCryptoAndSyncManager.unlockClassicSurfers(id, id, ::log)
                                        }) { Text("Выбрать", fontSize = 11.sp) }
                                    }
                                }
                            }
                        }
                    }

                    // Вкладка 3: Поиск и разблокировка Досок Classic
                    3 -> {
                        OutlinedTextField(
                            value = classicSearchQuery,
                            onValueChange = { classicSearchQuery = it },
                            label = { Text("🔍 Поиск ховерборда по названию или ID...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            ClassicCryptoAndSyncManager.unlockClassicBoards(null, null, ::log)
                        }) { Text("🛹 Открыть ВСЕ доски и апгрейды в 1 клик") }

                        val filteredBoards = ClassicDatabase.CLASSIC_BOARDS.filter {
                            it.first.contains(classicSearchQuery, ignoreCase = true) ||
                            it.second.first.contains(classicSearchQuery, ignoreCase = true)
                        }

                        filteredBoards.forEach { (id, pair) ->
                            val (name, upgrades) = pair
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("ID: $id \vert{} Способности: ${if (upgrades.isEmpty()) "Стандарт" else upgrades.joinToString()}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Button(onClick = {
                                        ClassicCryptoAndSyncManager.unlockClassicBoards(id, id, ::log)
                                    }) { Text("Открыть & Взять", fontSize = 11.sp) }
                                }
                            }
                        }
                    }

                    // Вкладка 4: Генератор Сундуков (Lootboxes) в очередь открытия
                    4 -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🎁 Выдача Сундуков в очередь (lootboxQueue)", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = classicLootboxCount,
                                    onValueChange = { classicLootboxCount = it },
                                    label = { Text("Количество боксов для выдачи") },
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
                                    val map = classicCurrencies.mapValues { it.value.toLongOrNull() ?: 9999999L }
                                    val cnt = classicLootboxCount.toIntOrNull() ?: 20
                                    ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, true, selectedClassicLootbox, cnt, ::log)
                                    log("🎁 Добавлено $cnt шт. $selectedClassicLootbox прямо на главный экран игры!")
                                }) { Text("🎁 Выдать выбранные сундуки") }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
