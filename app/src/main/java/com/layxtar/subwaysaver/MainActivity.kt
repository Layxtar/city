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
                Surface(modifier = Modifier.fillMaxSize()) { UltimateEditorApp(this) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltimateEditorApp(activity: ComponentActivity) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var logs by remember { mutableStateOf("🔄 Приложение запущено. Ожидание...\n") }
    fun log(msg: String) { logs = "$msg\n$logs" }

    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var detectedCityPath by remember { mutableStateOf<String?>(null) }

    val currencyStates = remember { mutableStateMapOf<Long, String>() }

    var playerName by remember { mutableStateOf("Layxtar") }
    var playerTag by remember { mutableStateOf("9CVYN655YJEZHG") }
    var playerLevel by remember { mutableStateOf("50") }
    var playerXp by remember { mutableStateOf("99999") }
    var globalMultiplier by remember { mutableStateOf("0") }
    var selectedSurferId by remember { mutableLongStateOf(-496667767L) }
    var selectedBoardId by remember { mutableLongStateOf(1711310740L) }

    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("250000") }
    var piggyFilled by remember { mutableStateOf(true) }

    var selectedCampaignId by remember { mutableStateOf(GameDatabase.SCHEDULED_CAMPAIGNS.first().first) }
    var selectedQuestId by remember { mutableStateOf(GameDatabase.QUESTS.first().first) }
    var selectedQuestMission by remember { mutableLongStateOf(GameDatabase.QUESTS.first().second.first) }

    var seasonPurchased by remember { mutableStateOf(true) }
    var seasonPoints by remember { mutableStateOf("15000") }
    var selectedSeasonId by remember { mutableStateOf(GameDatabase.SEASONS.first().first) }
    var dealAmountInput by remember { mutableStateOf("9999") }

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
        val online = prof.optJSONObject("onlinePlayerProfile")
        playerName = online?.optString("name", "Layxtar") ?: "Layxtar"
        playerTag = online?.optString("tag", "9CVYN655YJEZHG") ?: "9CVYN655YJEZHG"
        playerLevel = prof.optInt("level", 50).toString()
        playerXp = prof.optInt("xp", 9999).toString()
        globalMultiplier = prof.optInt("globalScoreRunMultiplier", 0).toString()
        selectedSurferId = prof.optLong("character", -496667767L)
        selectedBoardId = prof.optLong("selectedBoard", 1711310740L)
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
        onlineProf.put("name", playerName).put("tag", playerTag)
        prof.put("onlinePlayerProfile", onlineProf)
        prof.put("level", playerLevel.toIntOrNull() ?: 50)
        prof.put("xp", playerXp.toIntOrNull() ?: 9999)
        prof.put("globalScoreRunMultiplier", globalMultiplier.toIntOrNull() ?: 0)
        prof.put("character", selectedSurferId)
        prof.put("selectedBoard", selectedBoardId)
        prof.put("seasonPassPurchased", seasonPurchased)
        prof.put("seasonPassPoints", seasonPoints.toIntOrNull() ?: 15000)
        prof.put("seasonPassId", selectedSeasonId)
        rawJsonText = prof.toString(2)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Subway Save Editor GOD-MODE", fontWeight = FontWeight.Bold) })
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    listOf("💰 Валюта", "⚡ Читы", "🐷 Копилка", "🎢 Трассы", "👥 Профиль", "🛒 Сезон", "🛹 Classic", "📜 Логи & JSON").forEachIndexed { idx, title ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                    }
                }
            }
        },
        bottomBar = {
            if (selectedTab != 6) {
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = {
                        log("=== ЗАГРУЗКА CITY ===")
                        if (!CryptoAndSyncManager.checkShizukuPermission(::log)) return@Button
                        val path = CryptoAndSyncManager.findCitySavePath(::log) ?: return@Button
                        detectedCityPath = path
                        val tempFile = File(activity.cacheDir, "city_profile.bin")
                        if (CryptoAndSyncManager.pullSaveFile(path, tempFile, CryptoAndSyncManager.PKG_CITY, ::log)) {
                            val bytes = tempFile.readBytes()
                            val (cont, prof) = CryptoAndSyncManager.decryptCityProfile(bytes)
                            rawFileBytes = bytes; containerJson = cont; profileJson = prof
                            refreshUIFromProfile(prof)
                            log("✅ УСПЕХ: Профиль загружен!")
                        } else { log("❌ Ошибка чтения файла.") }
                    }) { Text("📥 Загрузить") }

                    Button(modifier = Modifier.weight(1f), enabled = profileJson != null, onClick = {
                        log("=== СИНХРОНИЗАЦИЯ ===")
                        val prof = profileJson ?: return@Button
                        val cont = containerJson ?: return@Button
                        val raw = rawFileBytes ?: return@Button
                        val path = detectedCityPath ?: return@Button
                        applyUIToProfile(prof)
                        val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
                        val tempOut = File(activity.cacheDir, "city_profile_out.bin")
                        tempOut.writeBytes(encryptedBytes)
                        val ok = CryptoAndSyncManager.pushAndSyncSaveFile(tempOut, path, CryptoAndSyncManager.PKG_CITY, ::log)
                        if (ok) log("✅ УСПЕХ: Синхронизировано с игрой!") else log("❌ Ошибка записи.")
                    }) { Text("💾 Синхронизировать") }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(modifier = Modifier.height(4.dp))
            when (selectedTab) {
                0 -> {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                        log("💎 Валюты на максимум")
                    }) { Text("💎 Выставить все 9 валют на 9 999 999") }
                    GameDatabase.CURRENCIES.forEach { (tag, label) ->
                        OutlinedTextField(value = currencyStates[tag] ?: "0", onValueChange = { currencyStates[tag] = it }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                }
                1 -> {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let {
                            CryptoAndSyncManager.unlockAndMaxAllSurfers(it)
                            CryptoAndSyncManager.unlockAndMaxAllBoards(it)
                            CryptoAndSyncManager.unlockAllSkinsAndAvatars(it)
                            CryptoAndSyncManager.completeAllDistrictsAndStages(it)
                            CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                            CryptoAndSyncManager.unlockSecretStarsAndAchievements(it)
                            CryptoAndSyncManager.hackDailyDealsAndAdRewards(it)
                            refreshUIFromProfile(it)
                            log("👑 GOD-MODE Активирован!")
                        }
                    }) { Text("👑 АКТИВИРОВАТЬ GOD-MODE (Всё в 1 клик)") }
                }
                2 -> {
                    Text("🐷 Свинья-Копилка и Донат", fontWeight = FontWeight.Bold)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let { CryptoAndSyncManager.injectAllPaidPassesAndDonations(it); log("💳 Донат выдан!") }
                    }) { Text("💳 Выдать все Платные Пропуска Трасс") }
                    OutlinedTextField(value = piggyProgress, onValueChange = { piggyProgress = it }, label = { Text("Монет в копилке") }, modifier = Modifier.fillMaxWidth())
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        profileJson?.let {
                            CryptoAndSyncManager.configurePiggyBank(it, selectedPiggyId, selectedPiggyMission, piggyProgress.toIntOrNull() ?: 250000, piggyFilled)
                            log("🐷 Копилка настроена!")
                        }
                    }) { Text("🐷 Применить Копилку") }
                }
                3 -> {
                    Text("🎢 Трассы и Квесты", fontWeight = FontWeight.Bold)
                    GameDatabase.SCHEDULED_CAMPAIGNS.forEach { (cId, cTitle) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedCampaignId == cId, onClick = { selectedCampaignId = cId })
                            Text(cTitle)
                        }
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { profileJson?.let { CryptoAndSyncManager.activateScheduledCampaign(it, selectedCampaignId); log("🎢 Трасса активирована!") } }) { Text("🎢 Включить трассу") }
                }
                4 -> {
                    OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = playerLevel, onValueChange = { playerLevel = it }, label = { Text("Уровень (1-50)") }, modifier = Modifier.fillMaxWidth())
                }
                5 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = seasonPurchased, onCheckedChange = { seasonPurchased = it })
                        Text("Premium Pass куплен")
                    }
                    OutlinedTextField(value = seasonPoints, onValueChange = { seasonPoints = it }, label = { Text("Очки сезона") }, modifier = Modifier.fillMaxWidth())
                }
                6 -> {
                    Text("🛹 Редактор Classic", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = classicCoins, onValueChange = { classicCoins = it }, label = { Text("🪙 Монеты Classic") }, modifier = Modifier.fillMaxWidth())
                }
                7 -> {
                    Text("📜 Логи в реальном времени:", fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black).padding(8.dp)) {
                        Text(text = logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }
                    OutlinedTextField(value = rawJsonText, onValueChange = { rawJsonText = it }, label = { Text("Сырой JSON") }, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
