package com.layxtar.subwaysaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

private val CLASSIC_POPULAR_CITIES = listOf(
    "transylvania" to "🧛 Трансильвания (Хэллоуин S125)",
    "hauntedhood" to "🎃 Проклятый квартал (Haunted Hood)",
    "mexicocity" to "🌮 Мехико (Mexico City)",
    "buenosaires" to "🇦🇷 Буэнос-Айрес (Buenos Aires)",
    "cosmiccrossroads" to "🌌 Космический перекрёсток (Cosmic Crossroads)",
    "paris" to "🗼 Париж (Paris)",
    "sakuratokyo" to "🌸 Токио Сакура (Sakura Tokyo)",
    "rome" to "🏛️ Рим (Rome)",
    "irelandspring" to "🍀 Ирландия (Easter Ireland)",
    "copenhagensuperrunner" to "⚡ Копенгаген (Super Runner)",
    "subwaycity" to "🏙️ Сабвей Сити (Subway City)",
    "subwaycityfloorislava" to "🌋 Пол — это лава (Floor Is Lava)",
    "subwaypvp" to "🏁 Арена гонок (Subway Showdown PvP)",
    "newyork" to "🗽 Нью-Йорк (New York)",
    "rio" to "🌴 Рио-де-Жанейро (Rio)",
    "mumbai" to "🕌 Мумбаи (Mumbai)",
    "seoul" to "🏮 Сеул (Seoul)",
    "iceland" to "❄️ Исландия (Iceland)",
    "cairo" to "🐪 Каир (Cairo)",
    "hawaii" to "🌺 Гавайи (Hawaii)"
)

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

    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var activeProfilePath by remember { mutableStateOf(CryptoAndSyncManager.DEFAULT_GAME_PROFILE_PATH) }
    var isCityLoaded by remember { mutableStateOf(false) }
    var uiTrigger by remember { mutableIntStateOf(0) }

    val currencyStates = remember { mutableStateMapOf<Long, String>() }
    val iapCheckedStates = remember { mutableStateMapOf<String, Boolean>() }
    var iapFilterCategory by remember { mutableStateOf("Все") }

    // Все поля изначально нейтральные и заполняются строго из твоего сохранения при чтении!
    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("0") }
    var piggyFilled by remember { mutableStateOf(false) }
    var piggyFreeClaim by remember { mutableStateOf(false) }

    var seasonPurchased by remember { mutableStateOf(false) }
    var seasonPoints by remember { mutableStateOf("0") }
    var selectedSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.first().first) }

    var selectedCampaignId by remember { mutableStateOf(GameDatabase.SCHEDULED_CAMPAIGNS.first().first) }
    var selectedQuestId by remember { mutableStateOf(GameDatabase.QUESTS.first().first) }
    var selectedQuestMission by remember { mutableLongStateOf(GameDatabase.QUESTS.first().second.first) }

    // Поля статистики игрока — без зашитых девяток, берутся напрямую из сохранения!
    var playerName by remember { mutableStateOf("") }
    var playerTag by remember { mutableStateOf("") }
    var playerLevel by remember { mutableStateOf("1") }
    var playerXp by remember { mutableStateOf("0") }
    var globalMultiplier by remember { mutableStateOf("0") }
    var boostedMultiplier by remember { mutableStateOf("0") }
    var leaderboardBestScore by remember { mutableStateOf("0") }
    var allSurfersHighScoreInput by remember { mutableStateOf("0") }

    var selectedSurferId by remember { mutableLongStateOf(GameDatabase.SURFERS.first().first) }
    var selectedBoardId by remember { mutableLongStateOf(GameDatabase.BOARDS.first().first) }
    var crewSlot1 by remember { mutableLongStateOf(0L) }
    var crewSlot2 by remember { mutableLongStateOf(0L) }
    var crewSlot3 by remember { mutableLongStateOf(0L) }

    // Classic состояния
    val classicCurrencies = remember {
        mutableStateMapOf<String, String>().apply {
            ClassicDatabase.CURRENCIES.forEach { (id, _) -> put(id, "0") }
        }
    }
    var classicSearchQuery by remember { mutableStateOf("") }
    var selectedClassicLootbox by remember { mutableStateOf(ClassicDatabase.LOOTBOX_TYPES.first().first) }
    var classicLootboxCount by remember { mutableStateOf("10") }
    var selectedClassicCity by remember { mutableStateOf(CLASSIC_POPULAR_CITIES.first().first) }
    var customClassicCityInput by remember { mutableStateOf("transylvania") }

    // Чтение ВСЕХ параметров напрямую из твоего сохранения Subway Surfers City
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
        globalMultiplier = prof.optInt("globalScoreRunMultiplier", 0).toString()
        boostedMultiplier = prof.optJSONObject("boostedRunMultiplier")?.optInt("runMultiplier", 0)?.toString() ?: "0"

        // Берём твой реальный рекорд из leaderboardProfiles (или 0, если его нет)
        val lbArr = prof.optJSONArray("leaderboardProfiles")
        leaderboardBestScore = if (lbArr != null && lbArr.length() > 0) {
            lbArr.getJSONObject(0).optLong("bestScore", 0L).toString()
        } else {
            "0"
        }

        // Берём реальный рекорд первого сёрфера для отображения в поле рекордов сёрферов
        val surferArr = prof.optJSONArray("surferProfiles")
        allSurfersHighScoreInput = if (surferArr != null && surferArr.length() > 0) {
            surferArr.getJSONObject(0).optLong("highScore", 0L).toString()
        } else {
            "0"
        }

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

        val activeCamp = prof.optJSONObject("activeScheduledCampaignProfile")
        if (activeCamp != null && activeCamp.has("activeScheduledCampaignId")) {
            selectedCampaignId = activeCamp.optString("activeScheduledCampaignId", selectedCampaignId)
        }

        seasonPurchased = prof.optBoolean("seasonPassPurchased", false)
        seasonPoints = prof.optInt("seasonPassPoints", 0).toString()
        selectedSeasonId = prof.optString("seasonPassId", GameDatabase.SEASONS.first().first)
        uiTrigger++
    }

    // Бережная запись изменений обратно в профиль City
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
        prof.put("globalScoreRunMultiplier", globalMultiplier.toIntOrNull() ?: prof.optInt("globalScoreRunMultiplier", 0))

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

        val lbArr = prof.optJSONArray("leaderboardProfiles")
        val parsedLbScore = leaderboardBestScore.toLongOrNull() ?: 0L
        if (lbArr != null && lbArr.length() > 0) {
            for (i in 0 until lbArr.length()) {
                lbArr.getJSONObject(i).put("bestScore", parsedLbScore)
            }
            prof.put("leaderboardProfiles", lbArr)
        }

        prof.put("seasonPassPurchased", seasonPurchased)
        prof.put("seasonPassPoints", seasonPoints.toIntOrNull() ?: prof.optInt("seasonPassPoints", 0))
        if (selectedSeasonId.isNotBlank()) prof.put("seasonPassId", selectedSeasonId)
    }

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
                    refreshUIFromCityProfile(loaded.third.second)
                    isCityLoaded = true
                    log("✅ Профиль Subway City загружен (Игрок: $playerName, Ур: $playerLevel)!")
                } else {
                    log("❌ Профиль Subway City не найден.")
                }
            } else {
                val walletJson = withContext(Dispatchers.IO) {
                    CryptoAndSyncManager.stopGameForSync(ClassicCryptoAndSyncManager.PKG_CLASSIC, false) {}
                    ClassicCryptoAndSyncManager.readClassicJson("wallet.json") {}.second
                }
                val currencies = walletJson.optJSONObject("currencies")
                for ((id, _) in ClassicDatabase.CURRENCIES) {
                    val v = currencies?.optJSONObject(id)?.optLong("value", 0L) ?: 0L
                    classicCurrencies[id] = v.toString()
                }
                log("✅ Кошелёк Classic загружен (Монет: ${classicCurrencies["1"]}, Ключей: ${classicCurrencies["2"]})!")
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
                log("✅ Профиль Subway City сохранён!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, CryptoAndSyncManager.PKG_CITY, ::log)
            } else {
                log("❌ Ошибка сохранения профиля City!")
            }
            isBusy = false
        }
    }

    fun saveClassicWalletAsync(launchAfterSave: Boolean) {
        val map = classicCurrencies.mapValues { it.value.toLongOrNull() ?: 0L }
        scope.launch {
            isBusy = true
            statusText = "💾 Шифрование и запись wallet.json..."
            withContext(Dispatchers.IO) {
                ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, false, null, 0, ::log)
            }
            log("✅ Кошелёк Classic успешно сохранён!")
            if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, ClassicCryptoAndSyncManager.PKG_CLASSIC, ::log)
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        CryptoAndSyncManager.onPermissionGrantedCallback = { granted ->
            if (granted) loadProfileAsync()
        }
        loadProfileAsync()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Subway Ultimate Editor", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { isCityMode = true; selectedTab = 0; loadProfileAsync() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isCityMode) Color(0xFF1976D2) else Color.DarkGray)
                            ) { Text("🏙️ CITY", fontSize = 12.sp) }

                            Button(
                                onClick = { isCityMode = false; selectedTab = 0; loadProfileAsync() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (!isCityMode) Color(0xFFE65100) else Color.DarkGray)
                            ) { Text("🛹 CLASSIC", fontSize = 12.sp) }
                        }
                    }
                )
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = if (isCityMode) {
                        listOf("💰 Валюта", "⚡ Мега-Читы", "💳 Донат", "🐷 Копилка & Ивенты", "📊 Статистика", "👥 Команда", "📂 Бэкап & Логи")
                    } else {
                        listOf("💰 Кошелёк", "⚡ Мега-Читы", "🌍 Выбор Трассы", "🔍 Сёрферы", "🛹 Доски", "🎁 Сундуки")
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
                    OutlinedButton(modifier = Modifier.weight(0.9f), enabled = !isBusy, onClick = { loadProfileAsync() }) {
                        Text("🔄 Загрузить", fontSize = 12.sp)
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
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            onClick = { saveClassicWalletAsync(false) }
                        ) { Text("💾 Сохранить", fontSize = 12.sp) }
                        Button(
                            modifier = Modifier.weight(1.2f),
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            onClick = { saveClassicWalletAsync(true) }
                        ) { Text("🚀 В Classic", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isCityMode) {
                // ==================== РЕЖИМ SUBWAY SURFERS CITY ====================
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    when (selectedTab) {
                        // ВКЛАДКА 0: ВАЛЮТА CITY
                        0 -> {
                            Text("Твои текущие ресурсы из сохранения Subway Surfers City:", fontSize = 12.sp, color = Color.Gray)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    GameDatabase.CURRENCIES.forEach { (tag, _) ->
                                        val c = currencyStates[tag]?.toLongOrNull() ?: 0L
                                        currencyStates[tag] = (c + 100000L).toString()
                                    }
                                    log("➕ Ко всем валютам добавлено +100 000")
                                }) { Text("+100K ко всем") }

                                Button(modifier = Modifier.weight(1f), onClick = {
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
                        }

                        // ВКЛАДКА 1: МЕГА-ЧИТЫ CITY
                        1 -> {
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
                                                CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(it)
                                                CryptoAndSyncManager.configurePiggyBank(it, "shop.piggybankmlp7.3", -2000200805L, 250000, true, true)
                                                refreshUIFromCityProfile(it)
                                                log("👑 РЕЖИМ БОГА АКТИВИРОВАН (без накрутки твоих рекордов)!")
                                            }
                                        }
                                    ) { Text("👑 АКТИВИРОВАТЬ РЕЖИМ БОГА (ВСЁ В 1 КЛИК)", color = Color.White, fontWeight = FontWeight.Bold) }
                                    Text("Начисляет по 9.99 млн всех валют, открывает всех 27 сёрферов и 29 досок на 20 уровне (сохраняя твои честные рекорды), все 125 костюмов, 6 районов, все ивентовые забеги на 3 звезды и бесплатную копилку.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        profileJson?.let {
                                            CryptoAndSyncManager.unlockAllEventTrialsAnd3StarMissions(it)
                                            refreshUIFromCityProfile(it)
                                            log("🏁 Все ивентовые забеги (Триалы по 3 звезды) и еженедельные трассы разблокированы!")
                                        }
                                    }) { Text("🏁 Открыть все Ивентовые Забеги и Триалы (3 ⭐)") }
                                    Text("Безопасно проходит все миссии ивентовых забегов на 3 звезды, разрешает бегать за любого сёрфера без ограничений и обнуляет таймеры ожидания между попытками.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        profileJson?.let {
                                            CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                            CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                                            CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                            refreshUIFromCityProfile(it)
                                            log("🏄 Все 27 сёрферов, 29 досок, 125 скинов и 41 аватар открыты (рекорды не тронуты)!")
                                        }
                                    }) { Text("🏄 Открыть всех Сёрферов, Доски, Костюмы и Аватары") }
                                    Text("Разблокирует всех обычных, сезонных и секретных персонажей и ховерборды, прокачивает их до 20 уровня и открывает платиновые рамки, НЕ изменяя статистику их пробега.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        profileJson?.let {
                                            CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                                            CryptoAndSyncManager.unlockSecretStarsAchievementsAndJackpots(it)
                                            refreshUIFromCityProfile(it)
                                            log("🗺️ Все 6 районов на 3 звезды, Секретные Звёзды и 100% Джекпот активированы!")
                                        }
                                    }) { Text("🗺️ Пройти все 6 Районов (3 ⭐) + 100% Джекпот Сундуков") }
                                    Text("Открывает все районы города, выполняет все 196 этапов на 3 звезды, собирает секретные звёзды на трассах и выставляет счётчик гаранта сундуков на 9/10.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

                        // ВКЛАДКА 2: ДОНАТ С ФИЛЬТРАМИ КАТЕГОРИЙ
                        2 -> {
                            val _trigger = uiTrigger
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                profileJson?.let {
                                    CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                    refreshUIFromCityProfile(it)
                                    log("💳 Весь платный контент и пропуска активированы!")
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
                                                iapCheckedStates[productId] = checked
                                                profileJson?.let { CryptoAndSyncManager.toggleSinglePurchase(it, productId, checked) }
                                            }
                                        )
                                        Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 3: СВИНЬЯ-КОПИЛКА + ИВЕНТЫ
                        3 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🐷 Единый Центр Управления Свиньёй-Копилкой", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Текущее значение монет взято из твоего сохранения. Ты можешь выставить до 250 000 монет, сделать копилку полной и забрать её бесплатно.", fontSize = 12.sp, color = Color.Gray)

                                    OutlinedTextField(
                                        value = piggyProgress,
                                        onValueChange = { piggyProgress = it },
                                        label = { Text("Количество монет внутри копилки (до 250 000)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = piggyFilled, onCheckedChange = { piggyFilled = it })
                                        Text("Копилка полностью заполнена (готова к открытию)")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = piggyFreeClaim, onCheckedChange = { piggyFreeClaim = it })
                                        Text("Сделать открытие копилки БЕСПЛАТНЫМ (добавить в купленные)")
                                    }
                                    Text("Выбери уровень копилки:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                                        profileJson?.let {
                                            CryptoAndSyncManager.configurePiggyBank(it, selectedPiggyId, selectedPiggyMission, piggyProgress.toIntOrNull() ?: 250000, piggyFilled, piggyFreeClaim)
                                            refreshUIFromCityProfile(it)
                                            log("🐷 Свинья-копилка настроена на $piggyProgress монет (Бесплатно: $piggyFreeClaim)!")
                                        }
                                    }) { Text("🐷 Применить все настройки Свиньи-Копилки") }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🎢 Переключатель Еженедельных Ивентовых Трасс (Weekly Wonders)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Включает любую специальную трассу (ускорители, рельсы, слоу-мо или Хэллоуин), открывает на ней все 3 звезды и снимает ограничения по персонажам.", fontSize = 12.sp, color = Color.Gray)
                                    GameDatabase.SCHEDULED_CAMPAIGNS.forEach { (cId, cTitle) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selectedCampaignId == cId, onClick = { selectedCampaignId = cId })
                                            Text(cTitle, fontSize = 13.sp)
                                        }
                                    }
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        profileJson?.let {
                                            CryptoAndSyncManager.activateScheduledCampaign(it, selectedCampaignId)
                                            refreshUIFromCityProfile(it)
                                            log("🎢 Ивентовая трасса активирована и пройдена на 3 звезды!")
                                        }
                                    }) { Text("🎢 Активировать выбранную трассу") }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🏆 Включение Праздничных Квестов и Завершение Миссий", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Активирует любой секретный или праздничный квест с выполненной шкалой очков для мгновенного сбора наград.", fontSize = 12.sp, color = Color.Gray)
                                    GameDatabase.QUESTS.forEach { (qId, pair) ->
                                        val (mId, qTitle) = pair
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selectedQuestId == qId, onClick = { selectedQuestId = qId; selectedQuestMission = mId })
                                            Text(qTitle, fontSize = 13.sp)
                                        }
                                    }
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        profileJson?.let {
                                            CryptoAndSyncManager.activateAndCompleteQuest(it, selectedQuestId, selectedQuestMission)
                                            CryptoAndSyncManager.completeAllExistingMissions(it, 99999)
                                            refreshUIFromCityProfile(it)
                                            log("🏆 Квест активирован, а все текущие миссии выполнены!")
                                        }
                                    }) { Text("🏆 Активировать квест + Выполнить все миссии") }
                                }
                            }
                        }

                        // ВКЛАДКА 4: СТАТИСТИКА И РЕКОРДЫ (БЕРУТСЯ ИЗ СОХРАНЕНИЯ!)
                        4 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📊 Профиль Игрока и Рекорды (прочитано из сохранения)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Все поля ниже показывают твои реальные данные из файла сохранения. Они не меняются сами по себе, пока ты не введёшь новые:", fontSize = 12.sp, color = Color.Gray)

                                    OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм игрока") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = playerTag, onValueChange = { playerTag = it }, label = { Text("Сетевой тег профиля (Player Tag)") }, modifier = Modifier.fillMaxWidth())

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = playerXp, onValueChange = { playerXp = it }, label = { Text("Опыт (XP)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = globalMultiplier, onValueChange = { globalMultiplier = it }, label = { Text("Глобальный множитель") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = boostedMultiplier, onValueChange = { boostedMultiplier = it }, label = { Text("Буст забега") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    }

                                    OutlinedTextField(
                                        value = leaderboardBestScore,
                                        onValueChange = { leaderboardBestScore = it },
                                        label = { Text("🏆 Твой рекорд в еженедельном турнире") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Text("Отдельная установка рекорда пробега для всех сёрферов (по желанию):", fontSize = 12.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = allSurfersHighScoreInput,
                                            onValueChange = { allSurfersHighScoreInput = it },
                                            label = { Text("Рекорд сёрферов (highScore)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(onClick = {
                                            val score = allSurfersHighScoreInput.toLongOrNull() ?: 0L
                                            profileJson?.let {
                                                CryptoAndSyncManager.setCustomHighScoreForAllSurfers(it, score)
                                                refreshUIFromCityProfile(it)
                                                log("🏆 Всем сёрферам установлен рекорд $score!")
                                            }
                                        }) { Text("Применить") }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 5: КОМАНДА И ГЕРОИ
                        5 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏄 Выбери активного сёрфера или назначь его в слоты команды (С1, С2, С3):", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                                TextButton(onClick = { crewSlot1 = id; log("👥 $name назначен в Слот 1") }) { Text("С1", fontSize = 11.sp) }
                                                TextButton(onClick = { crewSlot2 = id; log("👥 $name назначен в Слот 2") }) { Text("С2", fontSize = 11.sp) }
                                                TextButton(onClick = { crewSlot3 = id; log("👥 $name назначен в Слот 3") }) { Text("С3", fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🛹 Выбери активный ховерборд:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    GameDatabase.BOARDS.forEach { (id, name) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selectedBoardId == id, onClick = { selectedBoardId = id })
                                            Text(name, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 6: БЭКАП И ЛОГИ
                        6 -> {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📂 Резервное копирование и восстановление", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Перед каждым сохранением редактор автоматически создаёт резервную копию `.bak_editor`. Если что-то пошло не так, нажми кнопку отката ниже:", fontSize = 12.sp, color = Color.Gray)
                                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                        scope.launch {
                                            val ok = withContext(Dispatchers.IO) { CryptoAndSyncManager.restoreBackup(activeProfilePath, ::log) }
                                            if (ok) {
                                                loadProfileAsync()
                                                log("⏪ Резервная копия успешно восстановлена!")
                                            } else {
                                                log("❌ Файл резервной копии не найден.")
                                            }
                                        }
                                    }) { Text("⏪ Восстановить предыдущее сохранение из бэкапа") }
                                }
                            }
                            Text("📜 Журнал операций:", fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black).padding(8.dp)) {
                                Text(text = logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(90.dp))
                }
            } else {
                // ==================== РЕЖИМ CLASSIC SUBWAY SURFERS ====================
                when (selectedTab) {
                    // ВКЛАДКА 0: КОШЕЛЁК CLASSIC
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Твои реальные значения из `wallet.json`. Если какой-то валюты в файле ещё не было, при сохранении она создастся автоматически:", fontSize = 12.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    ClassicDatabase.CURRENCIES.forEach { (id, _) ->
                                        val c = classicCurrencies[id]?.toLongOrNull() ?: 0L
                                        classicCurrencies[id] = (c + 100000L).toString()
                                    }
                                    log("➕ Ко всем ресурсам Classic добавлено +100 000")
                                }) { Text("+100K ко всем") }

                                Button(modifier = Modifier.weight(1f), onClick = {
                                    ClassicDatabase.CURRENCIES.forEach { (id, _) -> classicCurrencies[id] = "99999999" }
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

                    // ВКЛАДКА 1: МЕГА-ЧИТЫ ДЛЯ CLASSIC
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        onClick = {
                                            scope.launch {
                                                isBusy = true
                                                withContext(Dispatchers.IO) {
                                                    val map = classicCurrencies.mapValues { 99999999L }
                                                    ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, true, "super_mystery_box", 25, ::log)
                                                    ClassicCryptoAndSyncManager.unlockDoubleCoinsAndMaxUpgrades(::log)
                                                    ClassicCryptoAndSyncManager.unlockClassicSurfers(null, null, ::log)
                                                    ClassicCryptoAndSyncManager.unlockClassicBoards(null, null, ::log)
                                                    ClassicCryptoAndSyncManager.maxSeasonHuntMissionsAndAchievements(::log)
                                                    ClassicCryptoAndSyncManager.unlockAllPortraitsAndFrames(::log)
                                                }
                                                loadProfileAsync()
                                                log("👑 РЕЖИМ БОГА ДЛЯ CLASSIC АКТИВИРОВАН!")
                                                isBusy = false
                                            }
                                        }
                                    ) { Text("👑 АКТИВИРОВАТЬ GOD-MODE ДЛЯ CLASSIC", color = Color.White, fontWeight = FontWeight.Bold) }
                                    Text("Открывает всех 275 сёрферов и их костюмы, все 297 досок с апгрейдами, 114 портретов, 56 рамок, ставит по 99.9 млн ресурсов, включает Двойные Монеты, убирает рекламу и проходит Сезонную Охоту.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.unlockDoubleCoinsAndMaxUpgrades(::log) }
                                            isBusy = false
                                        }
                                    }) { Text("🪙 Включить Двойные Монеты + Множитель + Бустеры 6 Ур.") }
                                    Text("Записывает в `upgrades.json` удвоение монет (Double Coins), постоянные бонусы к множителю рекорда и прокачивает джетпак, магнит, кроссовки и x2 звёзды до 6 уровня.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        scope.launch {
                                            isBusy = true
                                            val map = classicCurrencies.mapValues { it.value.toLongOrNull() ?: 0L }
                                            withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, true, null, 0, ::log) }
                                            log("🚫 Реклама отключена, Surfer Pass Plus и платные наборы разблокированы!")
                                            isBusy = false
                                        }
                                    }) { Text("🚫 Убрать Рекламу + Открыть Весь Донат и Surfer Pass") }
                                    Text("Добавляет в `wallet.json` пакет отключения рекламы (No Ads Bundle), премиум-пропуска Surfer Pass Plus и стартовые наборы.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                ClassicCryptoAndSyncManager.maxSeasonHuntMissionsAndAchievements(::log)
                                                ClassicCryptoAndSyncManager.unlockAllPortraitsAndFrames(::log)
                                            }
                                            log("🏆 Все 40 наград Сезонной Охоты, 114 портретов, 56 рамок, Трофеи и Ачивки открыты!")
                                            isBusy = false
                                        }
                                    }) { Text("🏆 Пройти Сезонную Охоту + Открыть Портреты, Рамки и Ачивки") }
                                    Text("Обновляет `season_hunt.json`, `achievements.json`, `trophies.json`, `profile_portrait.json` и `profile_frame.json`.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 2: ВЫБОР ТРАССЫ (ГОРОДА) В CLASSIC
                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🌍 Переключатель Трасс (Городов) для Забегов Classic", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Меняет карту в `generic_challenges.json` для активных ивентовых забегов, проходит все этапы без рекламы и убирает таймеры отдыха сёрферов.", fontSize = 12.sp, color = Color.Gray)

                                    OutlinedTextField(
                                        value = customClassicCityInput,
                                        onValueChange = { customClassicCityInput = it },
                                        label = { Text("Выбранная трасса (или впиши код любого города)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                ClassicCryptoAndSyncManager.setClassicEventCityAndUnlockStages(
                                                    targetCityId = customClassicCityInput.trim(),
                                                    completeAllStages = true,
                                                    clearCooldowns = true,
                                                    log = ::log
                                                )
                                            }
                                            isBusy = false
                                        }
                                    }) { Text("🌍 Установить Трассу + Пройти все этапы + Убрать таймеры") }

                                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                ClassicCryptoAndSyncManager.setClassicEventCityAndUnlockStages(
                                                    targetCityId = customClassicCityInput.trim(),
                                                    completeAllStages = false,
                                                    clearCooldowns = true,
                                                    log = ::log
                                                )
                                            }
                                            isBusy = false
                                        }
                                    }) { Text("🏃 Только сменить Трассу и сбросить таймеры") }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Text("Популярные города и специальные карты:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    CLASSIC_POPULAR_CITIES.forEach { (cityCode, cityName) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selectedClassicCity == cityCode,
                                                onClick = {
                                                    selectedClassicCity = cityCode
                                                    customClassicCityInput = cityCode
                                                }
                                            )
                                            Text(cityName, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 3: БЫСТРЫЙ ПОИСК СЁРФЕРОВ
                    3 -> {
                        val filteredSurfers = remember(classicSearchQuery) {
                            ClassicDatabase.CLASSIC_SURFERS.filter {
                                it.first.contains(classicSearchQuery, ignoreCase = true) ||
                                it.second.first.contains(classicSearchQuery, ignoreCase = true)
                            }
                        }
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = classicSearchQuery,
                                onValueChange = { classicSearchQuery = it },
                                label = { Text("🔍 Поиск сёрфера по имени (${filteredSurfers.size} из ${ClassicDatabase.CLASSIC_SURFERS.size})...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                scope.launch {
                                    isBusy = true
                                    withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.unlockClassicSurfers(null, null, ::log) }
                                    isBusy = false
                                }
                            }) { Text("🏄 Открыть абсолютно ВСЕХ 275 сёрферов и костюмы") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredSurfers, key = { it.first }) { (id, pair) ->
                                    val (name, outfits) = pair
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Костюмов в наборе: ${outfits.size}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(onClick = {
                                                    scope.launch {
                                                        isBusy = true
                                                        withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.unlockClassicSurfers(id, null, ::log) }
                                                        isBusy = false
                                                    }
                                                }) { Text("Открыть", fontSize = 11.sp) }
                                                Button(onClick = {
                                                    scope.launch {
                                                        isBusy = true
                                                        withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.unlockClassicSurfers(id, id, ::log) }
                                                        isBusy = false
                                                    }
                                                }) { Text("Выбрать", fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ВКЛАДКА 4: БЫСТРЫЙ ПОИСК ДОСОК
                    4 -> {
                        val filteredBoards = remember(classicSearchQuery) {
                            ClassicDatabase.CLASSIC_BOARDS.filter {
                                it.first.contains(classicSearchQuery, ignoreCase = true) ||
                                it.second.first.contains(classicSearchQuery, ignoreCase = true)
                            }
                        }
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = classicSearchQuery,
                                onValueChange = { classicSearchQuery = it },
                                label = { Text("🔍 Поиск ховерборда по названию (${filteredBoards.size} из ${ClassicDatabase.CLASSIC_BOARDS.size})...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                scope.launch {
                                    isBusy = true
                                    withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.unlockClassicBoards(null, null, ::log) }
                                    isBusy = false
                                }
                            }) { Text("🛹 Открыть абсолютно ВСЕ 297 досок и их способности") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredBoards, key = { it.first }) { (id, pair) ->
                                    val (name, upgrades) = pair
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    if (upgrades.isEmpty()) "Базовая доска" else "Способности: ${upgrades.size} шт.",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Button(onClick = {
                                                scope.launch {
                                                    isBusy = true
                                                    withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.unlockClassicBoards(id, id, ::log) }
                                                    isBusy = false
                                                }
                                            }) { Text("Открыть & Взять", fontSize = 11.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ВКЛАДКА 5: ВЫДАЧА СУНДУКОВ В ОЧЕРЕДЬ ОТКРЫТИЯ
                    5 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎁 Выдача Сундуков (Lootboxes) на главный экран", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Добавляет выбранные мистические коробки в `wallet.json` прямо в очередь открытия при входе в игру.", fontSize = 12.sp, color = Color.Gray)
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
                                        scope.launch {
                                            isBusy = true
                                            val map = classicCurrencies.mapValues { it.value.toLongOrNull() ?: 0L }
                                            val cnt = classicLootboxCount.toIntOrNull() ?: 10
                                            withContext(Dispatchers.IO) {
                                                ClassicCryptoAndSyncManager.updateWalletAndInjectDonations(map, false, selectedClassicLootbox, cnt, ::log)
                                            }
                                            log("🎁 Добавлено $cnt сундуков в очередь открытия!")
                                            isBusy = false
                                        }
                                    }) { Text("🎁 Выдать выбранные сундуки") }
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
