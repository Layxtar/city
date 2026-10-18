package com.layxtar.subwaysaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UltimateEditorApp(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltimateEditorApp(activity: ComponentActivity) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var statusBanner by remember { mutableStateOf("⚡ Готов к работе. Запустите Shizuku и нажмите «Загрузить City».") }

    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var detectedCityPath by remember { mutableStateOf<String?>(null) }

    // 0: Валюта
    val currencyStates = remember { mutableStateMapOf<Long, String>() }

    // 2: Донат и Свинья-Копилка
    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("250000") }
    var piggyFilled by remember { mutableStateOf(true) }

    // 3: Дополнительные трассы (Weekly Wonders) и Квесты
    var selectedCampaignId by remember { mutableStateOf(GameDatabase.SCHEDULED_CAMPAIGNS.first().first) }
    var selectedQuestId by remember { mutableStateOf(GameDatabase.QUESTS.first().first) }
    var selectedQuestMission by remember { mutableLongStateOf(GameDatabase.QUESTS.first().second.first) }

    // 4: Команда (Crew 3 слота), Профиль и Лидерборды
    var playerName by remember { mutableStateOf("Layxtar") }
    var playerTag by remember { mutableStateOf("9CVYN655YJEZHG") }
    var playerLevel by remember { mutableStateOf("50") }
    var playerXp by remember { mutableStateOf("99999") }
    var globalMultiplier by remember { mutableStateOf("0") }
    var boostedMultiplier by remember { mutableStateOf("3") }
    var leaderboardBestScore by remember { mutableStateOf("99999999") }
    var selectedSurferId by remember { mutableLongStateOf(-496667767L) }
    var selectedBoardId by remember { mutableLongStateOf(1711310740L) }
    var crewSlot1 by remember { mutableLongStateOf(1804257387L) }
    var crewSlot2 by remember { mutableLongStateOf(1614866432L) }
    var crewSlot3 by remember { mutableLongStateOf(852717139L) }

    // 5: Сезон и Магазин
    var seasonPurchased by remember { mutableStateOf(true) }
    var seasonPoints by remember { mutableStateOf("15000") }
    var selectedSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.first().first) }
    var dealAmountInput by remember { mutableStateOf("9999") }

    // 6: Classic Subway Surfers
    var classicCoins by remember { mutableStateOf("99999999") }
    var classicKeys by remember { mutableStateOf("9999999") }
    var classicBoards by remember { mutableStateOf("99999") }
    var classicBoosters by remember { mutableStateOf("9999") }

    // 7: Сырой JSON редактор
    var rawJsonText by remember { mutableStateOf("") }

    fun refreshUIFromProfile(prof: JSONObject) {
        val wallet = prof.optJSONArray("wallet") ?: JSONArray()
        for ((tag, _) in GameDatabase.CURRENCIES) currencyStates[tag] = "0"
        for (i in 0 until wallet.length()) {
            val item = wallet.getJSONObject(i)
            currencyStates[item.getLong("dataTag")] = item.getLong("count").toString()
        }
        val online = prof.optJSONObject("onlinePlayerProfile")
        playerName = online?.optString("name", "Layxtar") ?: "Layxtar"
        playerTag = online?.optString("tag", "9CVYN655YJEZHG") ?: "9CVYN655YJEZHG"
        playerLevel = prof.optInt("level", 50).toString()
        playerXp = prof.optInt("xp", 9999).toString()
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
        prof.put("xp", playerXp.toIntOrNull() ?: 9999)
        prof.put("globalScoreRunMultiplier", globalMultiplier.toIntOrNull() ?: 0)
        prof.put("boostedRunMultiplier", JSONObject().put("runMultiplier", boostedMultiplier.toIntOrNull() ?: 3))
        prof.put("character", selectedSurferId)
        prof.put("selectedBoard", selectedBoardId)

        val crewArr = JSONArray()
            .put(JSONObject().put("SurferId", crewSlot1))
            .put(JSONObject().put("SurferId", crewSlot2))
            .put(JSONObject().put("SurferId", crewSlot3))
        prof.put("crew", crewArr)

        // Обновляем рекорд в таблице лидеров
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Subway Save Editor GOD-MODE", fontWeight = FontWeight.Bold) }
                )
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Валюта",
                        "⚡ Мега-Читы",
                        "🐷 Копилка & Донат",
                        "🎢 Трассы & Квесты",
                        "👥 Команда & Профиль",
                        "🛒 Магазин & Сезон",
                        "🛹 Classic",
                        "🛠️ JSON & Патчер"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                    }
                }
            }
        },
        bottomBar = {
            if (selectedTab != 6) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!CryptoAndSyncManager.checkShizukuPermission()) {
                                statusBanner = "⚠️ Разрешите доступ в окне Shizuku и нажмите снова!"
                                return@Button
                            }
                            val path = CryptoAndSyncManager.findCitySavePath()
                            if (path == null) {
                                statusBanner = "❌ Файл сохранения City не найден!"
                                return@Button
                            }
                            detectedCityPath = path
                            val tempFile = File(activity.cacheDir, "city_profile.bin")
                            if (CryptoAndSyncManager.pullSaveFile(path, tempFile, CryptoAndSyncManager.PKG_CITY)) {
                                val bytes = tempFile.readBytes()
                                val (cont, prof) = CryptoAndSyncManager.decryptCityProfile(bytes)
                                rawFileBytes = bytes
                                containerJson = cont
                                profileJson = prof
                                refreshUIFromProfile(prof)
                                statusBanner = "✅ Профиль загружен: $path"
                            } else {
                                statusBanner = "❌ Ошибка чтения через Shizuku!"
                            }
                        }
                    ) { Text("📥 Загрузить") }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = profileJson != null,
                        onClick = {
                            val prof = profileJson ?: return@Button
                            val cont = containerJson ?: return@Button
                            val raw = rawFileBytes ?: return@Button
                            val path = detectedCityPath ?: return@Button

                            applyUIToProfile(prof)
                            val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
                            val tempOut = File(activity.cacheDir, "city_profile_out.bin")
                            tempOut.writeBytes(encryptedBytes)

                            val ok = CryptoAndSyncManager.pushAndSyncSaveFile(tempOut, path, CryptoAndSyncManager.PKG_CITY)
                            statusBanner = if (ok) "✅ Сохранено и синхронизировано (+5 мин от отката)!" else "❌ Ошибка записи!"
                        }
                    ) { Text("💾 Синхронизировать") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = statusBanner, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }

            when (selectedTab) {
                // ВКЛАДКА 0: ВАЛЮТА
                0 -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                            statusBanner = "💎 Все 9 валют выставлены на 9 999 999!"
                        }
                    ) { Text("💎 Выставить все 9 валют на 9 999 999") }

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

                // ВКЛАДКА 1: МЕГА-ЧИТЫ В 1 КЛИК
                1 -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                                CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                                CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                                CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                CryptoAndSyncManager.unlockSecretStarsAndAchievements(it)
                                CryptoAndSyncManager.hackDailyDealsAndAdRewards(it)
                                refreshUIFromProfile(it)
                                statusBanner = "👑 РЕЖИМ БОГА АКТИВИРОВАН: Открыто всё (герои, доски, скины, донат, трассы, звёзды)!"
                            }
                        }
                    ) { Text("👑 АКТИВИРОВАТЬ ВСЁ СРАЗУ (GOD-MODE В 1 КЛИК)") }

                    Divider()

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let {
                            CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                            statusBanner = "🏄 Все 27 сёрферов открыты (20 Ур. + Platinum 40 тир)!"
                        }
                    }) { Text("🏄 Открыть всех 27 сёрферов (20 Ур. + Platinum)") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let {
                            CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                            statusBanner = "🛹 Все 29 досок (включая 13 скрытых и Classic) прокачаны до 20 Ур.!"
                        }
                    }) { Text("🛹 Открыть все 29 досок + секретные (20 Ур.)") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let {
                            CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                            statusBanner = "🎭 Открыты все 125 скинов и 41 аватар!"
                        }
                    }) { Text("🎭 Открыть все 125 костюмов и 41 аватар") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let {
                            CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                            statusBanner = "🗺️ Все 6 районов и 196 этапов закрыты на 3 звезды!"
                        }
                    }) { Text("🗺️ Пройти все 6 районов кампании на 3 ⭐") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let {
                            CryptoAndSyncManager.unlockSecretStarsAndAchievements(it)
                            statusBanner = "⭐ Секретные звёзды, 12 ачивок и 100% Джекпот боксов активированы!"
                        }
                    }) { Text("⭐ Выдать все Секретные Звёзды + 12 Ачивок + 100% Джекпот") }
                }

                // ВКЛАДКА 2: СВИНЬЯ-КОПИЛКА И ДОНАТ
                2 -> {
                    Text("💎 Инжектор Доната и Платных пропусков трасс:", fontWeight = FontWeight.Bold)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                statusBanner = "💳 Выданы все 6 платных пропусков районов (Growth Funds), отключена реклама и спам баннеров!"
                            }
                        }
                    ) { Text("💳 Выдать все Платные Пропуска Трасс (001-006) + Мега-Донат") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("🐷 Редактор Свиньи-Копилки (Piggy Bank):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = piggyProgress,
                        onValueChange = { piggyProgress = it },
                        label = { Text("Накоплено монет в копилке (до 250 000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = piggyFilled, onCheckedChange = { piggyFilled = it })
                        Text("Копилка полностью заполнена (isFilled = true)")
                    }

                    Text("Выберите уровень копилки:", fontWeight = FontWeight.SemiBold)
                    GameDatabase.PIGGY_BANKS.forEach { (pId, pair) ->
                        val (mId, desc) = pair
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPiggyId == pId,
                                onClick = {
                                    selectedPiggyId = pId
                                    selectedPiggyMission = mId
                                }
                            )
                            Text(desc)
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.configurePiggyBank(
                                    it,
                                    selectedPiggyId,
                                    selectedPiggyMission,
                                    piggyProgress.toIntOrNull() ?: 250000,
                                    piggyFilled
                                )
                                statusBanner = "🐷 Копилка $selectedPiggyId установлена и наполнена!"
                            }
                        }
                    ) { Text("🐷 Применить настройки Свиньи-Копилки") }
                }

                // ВКЛАДКА 3: ДОПОЛНИТЕЛЬНЫЕ ТРАССЫ (WEEKLY WONDERS) И КВЕСТЫ
                3 -> {
                    Text("🎢 Открыть дополнительные еженедельные трассы (Weekly Wonders):", fontWeight = FontWeight.Bold)
                    GameDatabase.SCHEDULED_CAMPAIGNS.forEach { (cId, cTitle) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedCampaignId == cId, onClick = { selectedCampaignId = cId })
                            Text(cTitle)
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.activateScheduledCampaign(it, selectedCampaignId)
                                statusBanner = "🎢 Режим $selectedCampaignId активирован и пройден на 3 звезды!"
                            }
                        }
                    ) { Text("🎢 Активировать выбранную трассу в профиле") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("🏆 Запустить и выполнить секретные Квесты:", fontWeight = FontWeight.Bold)
                    GameDatabase.QUESTS.forEach { (qId, pair) ->
                        val (mId, qTitle) = pair
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedQuestId == qId,
                                onClick = {
                                    selectedQuestId = qId
                                    selectedQuestMission = mId
                                }
                            )
                            Text(qTitle)
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.activateAndCompleteQuest(it, selectedQuestId, selectedQuestMission)
                                statusBanner = "🏆 Квест $selectedQuestId добавлен с выполненным прогрессом!"
                            }
                        }
                    ) { Text("🏆 Добавить и завершить выбранный квест") }
                }

                // ВКЛАДКА 4: КОМАНДА (CREW), ПРОФИЛЬ И ЛИДЕРБОРДЫ
                4 -> {
                    Text("🌐 Онлайн-профиль и Лидерборды:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = playerTag, onValueChange = { playerTag = it }, label = { Text("Тег игрока (Tag)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = leaderboardBestScore, onValueChange = { leaderboardBestScore = it }, label = { Text("Рекорд в еженедельном турнире (Leaderboard Score)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень аккаунта (1-50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = boostedMultiplier, onValueChange = { boostedMultiplier = it }, label = { Text("Бонусный множитель забега (Boosted Run Multiplier)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("👥 ID сёрферов в Команде (Crew Slots 1-3):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = crewSlot1.toString(), onValueChange = { crewSlot1 = it.toLongOrNull() ?: crewSlot1 }, label = { Text("Слот команды 1 (SurferId)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = crewSlot2.toString(), onValueChange = { crewSlot2 = it.toLongOrNull() ?: crewSlot2 }, label = { Text("Слот команды 2 (SurferId)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = crewSlot3.toString(), onValueChange = { crewSlot3 = it.toLongOrNull() ?: crewSlot3 }, label = { Text("Слот команды 3 (SurferId)") }, modifier = Modifier.fillMaxWidth())

                    Text("Выбрать главного сёрфера:", fontWeight = FontWeight.Bold)
                    GameDatabase.SURFERS.forEach { (id, name) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedSurferId == id, onClick = { selectedSurferId = id })
                            Text("$name ($id)")
                        }
                    }
                }

                // ВКЛАДКА 5: МАГАЗИН СДЕЛОК, РУЛЕТКА И СЕЗОН
                5 -> {
                    Text("🛒 Кастомная витрина магазина (Daily Deals & Ad Roulette):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = dealAmountInput,
                        onValueChange = { dealAmountInput = it },
                        label = { Text("Количество предметов в каждом слоте магазина") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.hackDailyDealsAndAdRewards(it, dealAmountInput.toIntOrNull() ?: 9999)
                                statusBanner = "🛒 Витрина магазина и 9 слотов рекламной рулетки заполнены редкими ресурсами!"
                            }
                        }
                    ) { Text("🛒 Заполнить витрину магазина и рулетку поМаксимуму") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("🎟️ Управление Сезонным Пропуском (Season Pass):", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = seasonPurchased, onCheckedChange = { seasonPurchased = it })
                        Text("Premium Pass куплен")
                    }
                    OutlinedTextField(
                        value = seasonPoints,
                        onValueChange = { seasonPoints = it },
                        label = { Text("Очки сезона (15 000 открывает все уровни)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    GameDatabase.SEASONS.forEach { (sId, sName) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedSeasonId == sId, onClick = { selectedSeasonId = sId })
                            Text(sName)
                        }
                    }
                }

                // ВКЛАДКА 6: ОРИГИНАЛЬНЫЙ SUBWAY SURFERS (CLASSIC)
                6 -> {
                    Text("🛹 Редактор оригинального Subway Surfers (Classic)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = classicCoins, onValueChange = { classicCoins = it }, label = { Text("🪙 Монеты (Coins)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = classicKeys, onValueChange = { classicKeys = it }, label = { Text("🔑 Ключи (Keys)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = classicBoards, onValueChange = { classicBoards = it }, label = { Text("🛹 Ховерборды (Hoverboards)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = classicBoosters, onValueChange = { classicBoosters = it }, label = { Text("🚀 Бустеры и Фора (Boosters & Headstarts)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (!CryptoAndSyncManager.checkShizukuPermission()) {
                                statusBanner = "Активируйте Shizuku!"
                                return@Button
                            }
                            val tempWallet = File(activity.cacheDir, "classic_wallet.json")
                            if (CryptoAndSyncManager.pullSaveFile(CryptoAndSyncManager.CLASSIC_WALLET_PATH, tempWallet, CryptoAndSyncManager.PKG_CLASSIC)) {
                                val outer = JSONObject(tempWallet.readText())
                                val innerData = JSONObject(outer.getString("data"))
                                val currencies = innerData.optJSONObject("currencies") ?: JSONObject()

                                fun setClassicCurr(id: String, value: String) {
                                    val cObj = currencies.optJSONObject(id) ?: JSONObject()
                                    cObj.put("value", value.toLongOrNull() ?: 9999999L)
                                    currencies.put(id, cObj)
                                }
                                setClassicCurr("1", classicCoins)
                                setClassicCurr("2", classicKeys)
                                setClassicCurr("3", classicBoards)
                                setClassicCurr("4", classicBoosters)
                                setClassicCurr("5", classicBoosters)

                                innerData.put("currencies", currencies)
                                outer.put("data", innerData.toString())
                                tempWallet.writeText(outer.toString())

                                val ok = CryptoAndSyncManager.pushAndSyncSaveFile(tempWallet, CryptoAndSyncManager.CLASSIC_WALLET_PATH, CryptoAndSyncManager.PKG_CLASSIC)
                                statusBanner = if (ok) "✅ Сохранение Classic синхронизировано!" else "❌ Ошибка записи Classic!"
                            } else {
                                statusBanner = "❌ Файл wallet.json классического Subway Surfers не найден!"
                            }
                        }
                    ) { Text("💾 Сохранить и синхронизировать Classic") }
                }

                // ВКЛАДКА 7: ПАТЧЕР КОНФИГОВ SHA-1, БЭКАП И СЫРОЙ JSON
                7 -> {
                    Text("🛠️ Живой патчер конфигов игры (обход SHA-1 manifest.json):", fontWeight = FontWeight.Bold)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            statusBanner = CryptoAndSyncManager.patchGameConfigsViaShizuku(activity.cacheDir)
                        }
                    ) { Text("⚡ Пропатчить билеты (0 стоимость) и вырезать рекламу в кэше") }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val path = detectedCityPath ?: CryptoAndSyncManager.findCitySavePath()
                            if (path != null && CryptoAndSyncManager.restoreBackup(path, CryptoAndSyncManager.PKG_CITY)) {
                                statusBanner = "⏪ Оригинальный бэкап (.bak_editor) успешно восстановлен!"
                            } else {
                                statusBanner = "❌ Файл резервной копии не найден!"
                            }
                        }
                    ) { Text("⏪ Восстановить резервную копию профиля (.bak_editor)") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("📝 Прямой редактор расшифрованного JSON профиля:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = rawJsonText,
                        onValueChange = { rawJsonText = it },
                        label = { Text("Внутренний JSON (profile)") },
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            try {
                                val updated = JSONObject(rawJsonText)
                                profileJson = updated
                                refreshUIFromProfile(updated)
                                statusBanner = "✅ Изменения из текстового JSON успешно применены!"
                            } catch (e: Exception) {
                                statusBanner = "❌ Ошибка синтаксиса JSON: ${e.message}"
                            }
                        }
                    ) { Text("Применить правки из сырого JSON") }
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}