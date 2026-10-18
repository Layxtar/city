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
    var logs by remember { mutableStateOf("🔄 Приложение запущено. Ожидание действий...\n") }

    fun log(msg: String) {
        logs = "$msg\n$logs"
    }

    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var detectedCityPath by remember { mutableStateOf<String?>(null) }

    // Вкладка 0: Валюта
    val currencyStates = remember { mutableStateMapOf<Long, String>() }

    // Вкладка 2: Донат и Свинья-Копилка
    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("250000") }
    var piggyFilled by remember { mutableStateOf(true) }

    // Вкладка 3: Дополнительные трассы и Квесты
    var selectedCampaignId by remember { mutableStateOf(GameDatabase.SCHEDULED_CAMPAIGNS.first().first) }
    var selectedQuestId by remember { mutableStateOf(GameDatabase.QUESTS.first().first) }
    var selectedQuestMission by remember { mutableLongStateOf(GameDatabase.QUESTS.first().second.first) }

    // Вкладка 4: Команда, Профиль и Лидерборды
    var playerName by remember { mutableStateOf("Layxtar") }
    var playerTag by remember { mutableStateOf("9CVYN655YJEZHG") }
    var playerLevel by remember { mutableStateOf("50") }
    var playerXp by remember { mutableStateOf("99999") }
    var globalMultiplier by remember { mutableStateOf("0") }
    var leaderboardBestScore by remember { mutableStateOf("99999999") }
    var selectedSurferId by remember { mutableLongStateOf(-496667767L) }
    var selectedBoardId by remember { mutableLongStateOf(1711310740L) }
    var crewSlot1 by remember { mutableLongStateOf(1804257387L) }
    var crewSlot2 by remember { mutableLongStateOf(1614866432L) }
    var crewSlot3 by remember { mutableLongStateOf(852717139L) }

    // Вкладка 5: Магазин и Сезон
    var seasonPurchased by remember { mutableStateOf(true) }
    var seasonPoints by remember { mutableStateOf("15000") }
    var selectedSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.first().first) }
    var dealAmountInput by remember { mutableStateOf("9999") }

    // Вкладка 6: Classic Subway Surfers
    var classicCoins by remember { mutableStateOf("99999999") }
    var classicKeys by remember { mutableStateOf("9999999") }
    var classicBoards by remember { mutableStateOf("99999") }
    var classicBoosters by remember { mutableStateOf("9999") }

    // Вкладка 7: Сырой JSON
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
                        "📜 Логи & JSON"
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
                            log("=== НАЧАЛО ЗАГРУЗКИ CITY ===")
                            if (!CryptoAndSyncManager.checkShizukuPermission(::log)) {
                                log("⚠️ Разрешите доступ в Shizuku!")
                                return@Button
                            }
                            val path = CryptoAndSyncManager.findCitySavePath(::log)
                            if (path.isEmpty()) {
                                log("❌ Путь к файлу не найден!")
                                return@Button
                            }
                            detectedCityPath = path
                            val tempFile = File(activity.cacheDir, "city_profile.bin")
                            if (CryptoAndSyncManager.pullSaveFile(path, tempFile, CryptoAndSyncManager.PKG_CITY, ::log)) {
                                val bytes = tempFile.readBytes()
                                val (cont, prof) = CryptoAndSyncManager.decryptCityProfile(bytes)
                                rawFileBytes = bytes
                                containerJson = cont
                                profileJson = prof
                                refreshUIFromProfile(prof)
                                log("✅ УСПЕХ: Профиль City расшифрован и загружен!")
                            } else {
                                log("❌ Ошибка чтения файла через Shizuku.")
                            }
                        }
                    ) { Text("📥 Загрузить") }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = profileJson != null,
                        onClick = {
                            log("=== НАЧАЛО СИНХРОНИЗАЦИИ ===")
                            val prof = profileJson ?: return@Button
                            val cont = containerJson ?: return@Button
                            val raw = rawFileBytes ?: return@Button
                            val path = detectedCityPath ?: return@Button

                            applyUIToProfile(prof)
                            val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
                            val tempOut = File(activity.cacheDir, "city_profile_out.bin")
                            tempOut.writeBytes(encryptedBytes)

                            val ok = CryptoAndSyncManager.pushAndSyncSaveFile(tempOut, path, CryptoAndSyncManager.PKG_CITY, ::log)
                            if (ok) {
                                log("✅ УСПЕХ: Файл записан! Запускаем игру...")
                                CryptoAndSyncManager.launchGame(CryptoAndSyncManager.PKG_CITY, ::log)
                            } else {
                                log("❌ Ошибка записи через Shizuku.")
                            }
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

            when (selectedTab) {
                // ВКЛАДКА 0: ВАЛЮТА
                0 -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                            log("💎 Все 9 валют заполнены по 9 999 999!")
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

                // ВКЛАДКА 1: МЕГА-ЧИТЫ
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
                                log("👑 GOD-MODE АКТИВИРОВАН: Открыто абсолютно всё!")
                            }
                        }
                    ) { Text("👑 АКТИВИРОВАТЬ GOD-MODE (Всё на максимум)") }

                    Divider()

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let { CryptoAndSyncManager.unlockAndMaxAllSurfers(it); log("🏄 Все 27 сёрферов открыты и прокачаны!") }
                    }) { Text("🏄 Открыть всех 27 сёрферов (20 Ур. + Platinum)") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let { CryptoAndSyncManager.unlockAndMaxAllBoards(it); log("🛹 Все 29 досок открыты!") }
                    }) { Text("🛹 Открыть все 29 досок + секретные (20 Ур.)") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let { CryptoAndSyncManager.unlockAllSkinsAndAvatars(it); log("🎭 Все скины и аватары разблокированы!") }
                    }) { Text("🎭 Открыть все 125 костюмов и 41 аватар") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let { CryptoAndSyncManager.completeAllDistrictsAndStages(it); log("🗺️ Кампания пройдена на 3 звезды!") }
                    }) { Text("🗺️ Пройти все 6 районов кампании на 3 ⭐") }

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let { CryptoAndSyncManager.unlockSecretStarsAndAchievements(it); log("⭐ Звёзды и ачивки выданы!") }
                    }) { Text("⭐ Выдать все Секретные Звёзды + 12 Ачивок") }
                }

                // ВКЛАДКА 2: КОПИЛКА И ДОНАТ
                2 -> {
                    Text("💎 Инжектор Доната и Платных пропусков трасс:", fontWeight = FontWeight.Bold)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                log("💳 Все платные пропуски районов и мега-донат внедрены!")
                            }
                        }
                    ) { Text("💳 Выдать все Платные Пропуска Трасс (001-006) + Донат") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("🐷 Редактор Свиньи-Копилки (Piggy Bank):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = piggyProgress,
                        onValueChange = { piggyProgress = it },
                        label = { Text("Монет в копилке") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = piggyFilled, onCheckedChange = { piggyFilled = it })
                        Text("Копилка полностью заполнена (isFilled = true)")
                    }

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
                            Text(desc, fontSize = 13.sp)
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.configurePiggyBank(
                                    it, selectedPiggyId, selectedPiggyMission,
                                    piggyProgress.toIntOrNull() ?: 250000, piggyFilled
                                )
                                log("🐷 Свинья-копилка настроена!")
                            }
                        }
                    ) { Text("🐷 Применить настройки Свиньи-Копилки") }
                }

                // ВКЛАДКА 3: ТРАССЫ И КВЕСТЫ
                3 -> {
                    Text("🎢 Еженедельные трассы (Weekly Wonders):", fontWeight = FontWeight.Bold)
                    GameDatabase.SCHEDULED_CAMPAIGNS.forEach { (cId, cTitle) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedCampaignId == cId, onClick = { selectedCampaignId = cId })
                            Text(cTitle, fontSize = 13.sp)
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.activateScheduledCampaign(it, selectedCampaignId)
                                log("🎢 Трасса $selectedCampaignId активирована!")
                            }
                        }
                    ) { Text("🎢 Активировать выбранную трассу") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("🏆 Секретные Квесты:", fontWeight = FontWeight.Bold)
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
                            Text(qTitle, fontSize = 13.sp)
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.activateAndCompleteQuest(it, selectedQuestId, selectedQuestMission)
                                log("🏆 Квест $selectedQuestId добавлен и завершен!")
                            }
                        }
                    ) { Text("🏆 Добавить и завершить квест") }
                }

                // ВКЛАДКА 4: ПРОФИЛЬ И КОМАНДА
                4 -> {
                    Text("🌐 Онлайн-профиль и Лидерборды:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = playerTag, onValueChange = { playerTag = it }, label = { Text("Тег игрока (Tag)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = leaderboardBestScore, onValueChange = { leaderboardBestScore = it }, label = { Text("Рекорд в лидербордах") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень аккаунта (1-50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("👥 Смешанная Команда (Crew Slots 1-3):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = crewSlot1.toString(), onValueChange = { crewSlot1 = it.toLongOrNull() ?: crewSlot1 }, label = { Text("Слот команды 1 (SurferId)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = crewSlot2.toString(), onValueChange = { crewSlot2 = it.toLongOrNull() ?: crewSlot2 }, label = { Text("Слот команды 2 (SurferId)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = crewSlot3.toString(), onValueChange = { crewSlot3 = it.toLongOrNull() ?: crewSlot3 }, label = { Text("Слот команды 3 (SurferId)") }, modifier = Modifier.fillMaxWidth())
                }

                // ВКЛАДКА 5: МАГАЗИН И СЕЗОН
                5 -> {
                    Text("🛒 Витрина магазина и Рулетка:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = dealAmountInput,
                        onValueChange = { dealAmountInput = it },
                        label = { Text("Количество ресурсов в слотах магазина") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.hackDailyDealsAndAdRewards(it, dealAmountInput.toIntOrNull() ?: 9999)
                                log("🛒 Магазин и рулетка забиты редкими ресурсами!")
                            }
                        }
                    ) { Text("🛒 Заполнить витрину магазина и рулетку") }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("🎟️ Сезонный Пропуск (Season Pass):", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = seasonPurchased, onCheckedChange = { seasonPurchased = it })
                        Text("Premium Pass куплен")
                    }
                    OutlinedTextField(
                        value = seasonPoints,
                        onValueChange = { seasonPoints = it },
                        label = { Text("Очки сезона") },
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

                // ВКЛАДКА 6: CLASSIC SUBWAY SURFERS
                6 -> {
                    Text("🛹 Редактор классического Subway Surfers", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = classicCoins, onValueChange = { classicCoins = it }, label = { Text("🪙 Монеты Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = classicKeys, onValueChange = { classicKeys = it }, label = { Text("🔑 Ключи Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = classicBoards, onValueChange = { classicBoards = it }, label = { Text("🛹 Доски Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = classicBoosters, onValueChange = { classicBoosters = it }, label = { Text("🚀 Бустеры Classic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            log("=== ЗАГРУЗКА CLASSIC ===")
                            if (!CryptoAndSyncManager.checkShizukuPermission(::log)) return@Button
                            val tempWallet = File(activity.cacheDir, "classic_wallet.json")
                            if (CryptoAndSyncManager.pullSaveFile(CryptoAndSyncManager.CLASSIC_WALLET_PATH, tempWallet, CryptoAndSyncManager.PKG_CLASSIC, ::log)) {
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

                                val ok = CryptoAndSyncManager.pushAndSyncSaveFile(tempWallet, CryptoAndSyncManager.CLASSIC_WALLET_PATH, CryptoAndSyncManager.PKG_CLASSIC, ::log)
                                if (ok) {
                                    log("✅ УСПЕХ: Classic синхронизирован!")
                                    CryptoAndSyncManager.launchGame(CryptoAndSyncManager.PKG_CLASSIC, ::log)
                                } else {
                                    log("❌ Ошибка записи Classic.")
                                }
                            } else {
                                log("❌ Файл wallet.json для Classic не найден.")
                            }
                        }
                    ) { Text("💾 Сохранить и синхронизировать Classic") }
                }

                // ВКЛАДКА 7: ЛОГИ И JSON
                7 -> {
                    Text("📜 Логи выполнения в реальном времени:", fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
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

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = rawJsonText,
                        onValueChange = { rawJsonText = it },
                        label = { Text("Сырой JSON профиля City") },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            try {
                                val updated = JSONObject(rawJsonText)
                                profileJson = updated
                                refreshUIFromProfile(updated)
                                log("✅ Изменения из сырого JSON применены в память!")
                            } catch (e: Exception) {
                                log("❌ Ошибка JSON: ${e.message}")
                            }
                        }
                    ) { Text("Применить правки из сырого JSON") }
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
