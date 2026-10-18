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

    val currencyStates = remember { mutableStateMapOf<Long, String>() }

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
        boostedMultiplier = prof.optJSONObject("boostedRunMultiplier")?.optInt("runMultiplier", 3)?.toString() ?: "3"
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
        prof.put("seasonPassPurchased", seasonPurchased)
        prof.put("seasonPassPoints", seasonPoints.toIntOrNull() ?: 15000)
        prof.put("seasonPassId", selectedSeasonId)
        rawJsonText = prof.toString(2)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Subway Save Editor (DEBUG)", fontWeight = FontWeight.Bold) })
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    listOf("💰 Валюта", "⚡ Читы", "🐷 Копилка", "🎢 Трассы", "👥 Профиль", "🛒 Сезон", "🛹 Classic", "📜 Логи & JSON").forEachIndexed { idx, title ->
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
                                log("⚠️ Требуется выдать разрешение в Shizuku!")
                                return@Button
                            }
                            val path = CryptoAndSyncManager.findCitySavePath(::log)
                            if (path == null) {
                                log("❌ Не удалось найти файл сохранения автоматически.")
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
                                log("✅ УСПЕХ: Файл успешно расшифрован и загружен!")
                            } else {
                                log("❌ Ошибка чтения файла через Shizuku.")
                            }
                        }
                    ) { Text("📥 Загрузить City") }

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
                            if (ok) log("✅ УСПЕХ: Файл записан и синхронизирован с игрой!")
                            else log("❌ Ошибка записи через Shizuku.")
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
                0 -> {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                        log("💎 Валюты заполнены на 9 999 999")
                    }) { Text("💎 Заполнить валюты по 9 999 999") }

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
                            log("👑 Активирован GOD-MODE: Всё разблокировано и прокачано!")
                        }
                    }) { Text("👑 АКТИВИРОВАТЬ GOD-MODE (Всё на максимум)") }
                }
                7 -> {
                    Text("📜 Логи выполнения в реальном времени:", fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Color.Black)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = logs,
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawJsonText,
                        onValueChange = { rawJsonText = it },
                        label = { Text("Сырой JSON профиля") },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
                else -> {
                    Text("Используйте вкладки Валюта, Читы или Логи.")
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
