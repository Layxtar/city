package com.layxtar.subwaysaver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    var manualFileCallback: ((Uri) -> Unit)? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { manualFileCallback?.invoke(it) }
    }

    fun openFilePicker(callback: (Uri) -> Unit) {
        manualFileCallback = callback
        filePickerLauncher.launch(arrayOf("*/*"))
    }

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
fun UltimateEditorApp(activity: MainActivity) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var statusBanner by remember { mutableStateOf("⚡ Готов. Нажмите «Загрузить City» или выберите файл вручную.") }

    var rawFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var containerJson by remember { mutableStateOf<JSONObject?>(null) }
    var profileJson by remember { mutableStateOf<JSONObject?>(null) }
    var detectedCityPath by remember { mutableStateOf<String?>(null) }
    var manualSelectedPath by remember { mutableStateOf<String?>(null) }

    // 0: Валюта
    val currencyStates = remember { mutableStateMapOf<Long, String>() }

    // 2: Донат и Свинья-Копилка
    var selectedPiggyId by remember { mutableStateOf(GameDatabase.PIGGY_BANKS.first().first) }
    var selectedPiggyMission by remember { mutableLongStateOf(GameDatabase.PIGGY_BANKS.first().second.first) }
    var piggyProgress by remember { mutableStateOf("250000") }
    var piggyFilled by remember { mutableStateOf(true) }

    // 3: Дополнительные трассы и Квесты
    var selectedCampaignId by remember { mutableStateOf(GameDatabase.SCHEDULED_CAMPAIGNS.first().first) }
    var selectedQuestId by remember { mutableStateOf(GameDatabase.QUESTS.first().first) }
    var selectedQuestMission by remember { mutableLongStateOf(GameDatabase.QUESTS.first().second.first) }

    // 4: Команда и Профиль
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

    // 6: Classic
    var classicCoins by remember { mutableStateOf("99999999") }
    var classicKeys by remember { mutableStateOf("999999") }
    var classicBoards by remember { mutableStateOf("99999") }
    var classicBoosters by remember { mutableStateOf("9999") }

    // 7: JSON
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
                    listOf("💰 Валюта", "⚡ Мега-Читы", "🐷 Копилка", "🎢 Трассы", "👥 Команда", "🛒 Магазин", "🛹 Classic", "🛠️ JSON").forEachIndexed { idx, title ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                    }
                }
            }
        },
        bottomBar = {
            if (selectedTab < 6) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!CryptoAndSyncManager.checkShizukuPermission()) {
                                statusBanner = "⚠️ Дайте разрешение в приложении Shizuku!"
                                return@Button
                            }
                            val path = CryptoAndSyncManager.findCitySavePath()
                            if (path == null) {
                                statusBanner = "❌ Путь не найден через Shizuku. Используйте ручной выбор файла!"
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
                                statusBanner = "✅ Загружено через Shizuku: $path"
                            } else {
                                statusBanner = "❌ Ошибка чтения. Закройте игру!"
                            }
                        }
                    ) { Text("📥 Загрузить (Shizuku)") }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = profileJson != null,
                        onClick = {
                            val prof = profileJson ?: return@Button
                            val cont = containerJson ?: return@Button
                            val raw = rawFileBytes ?: return@Button
                            val path = detectedCityPath ?: manualSelectedPath

                            if (path == null) {
                                statusBanner = "❌ Сначала выберите файл сохранения!"
                                return@Button
                            }

                            applyUIToProfile(prof)
                            val encryptedBytes = CryptoAndSyncManager.encryptCityProfile(raw, cont, prof)
                            val tempOut = File(activity.cacheDir, "city_profile_out.bin")
                            tempOut.writeBytes(encryptedBytes)

                            val ok = CryptoAndSyncManager.pushAndSyncSaveFile(tempOut, path, CryptoAndSyncManager.PKG_CITY)
                            statusBanner = if (ok) "✅ Сохранено и синхронизировано!" else "❌ Ошибка записи!"
                        }
                    ) { Text("💾 Записать") }
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
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = statusBanner, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            activity.openFilePicker { uri ->
                                try {
                                    val inputStream = activity.contentResolver.openInputStream(uri)
                                    val bytes = inputStream?.readBytes() ?: return@openFilePicker
                                    inputStream.close()

                                    // Сохраняем путь для Shizuku из URI если возможно
                                    manualSelectedPath = uri.path?.let { p ->
                                        if (p.contains("primary:")) "/storage/emulated/0/" + p.substringAfter("primary:")
                                        else p
                                    }

                                    val tempFile = File(activity.cacheDir, "manual_profile.bin")
                                    tempFile.writeBytes(bytes)

                                    val (cont, prof) = CryptoAndSyncManager.decryptCityProfile(bytes)
                                    rawFileBytes = bytes
                                    containerJson = cont
                                    profileJson = prof
                                    refreshUIFromProfile(prof)
                                    statusBanner = "📂 Файл выбран вручную! Путь скопирован в память."
                                } catch (e: Exception) {
                                    statusBanner = "❌ Ошибка чтения файла: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📁 Выбрать файл сохранения вручную через проводник")
                    }

                    if (manualSelectedPath != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Путь: $manualSelectedPath", style = MaterialTheme.typography.labelSmall)
                        Button(
                            onClick = {
                                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SavePath", manualSelectedPath))
                                Toast.makeText(activity, "Путь скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📋 Копировать путь для Shizuku")
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            GameDatabase.CURRENCIES.forEach { (tag, _) -> currencyStates[tag] = "9999999" }
                            statusBanner = "💎 Все валюты выставлены на 9 999 999!"
                        }
                    ) { Text("💎 Выставить все валюты на 9 999 999") }

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
                                statusBanner = "👑 GOD-MODE: Всё открыто и прокачано на максимум!"
                            }
                        }
                    ) { Text("👑 АКТИВИРОВАТЬ ВСЁ СРАЗУ (GOD-MODE)") }
                }
                2 -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.injectAllPaidPassesAndDonations(it)
                                statusBanner = "💳 Выданы все платные пропуски районов и убран спам баннеров!"
                            }
                        }
                    ) { Text("💳 Выдать все Платные Пропуска Трасс + Донат") }
                }
                3 -> {
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
                                statusBanner = "🎢 Трасса $selectedCampaignId активирована!"
                            }
                        }
                    ) { Text("🎢 Активировать трассу") }
                }
                4 -> {
                    OutlinedTextField(value = playerName, onValueChange = { playerName = it }, label = { Text("Никнейм") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = playerTag, onValueChange = { playerTag = it }, label = { Text("Тег") }, modifier = Modifier.fillMaxWidth())
                }
                5 -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            profileJson?.let {
                                CryptoAndSyncManager.hackDailyDealsAndAdRewards(it, dealAmountInput.toIntOrNull() ?: 9999)
                                statusBanner = "🛒 Магазин и рулетка заполнены!"
                            }
                        }
                    ) { Text("🛒 Заполнить магазин и рулетку") }
                }
                6 -> {
                    Text("Classic Subway Surfers (com.kiloo.subwaysurf)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = classicCoins, onValueChange = { classicCoins = it }, label = { Text("Монеты") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = classicKeys, onValueChange = { classicKeys = it }, label = { Text("Ключи") }, modifier = Modifier.fillMaxWidth())
                }
                7 -> {
                    OutlinedTextField(
                        value = rawJsonText,
                        onValueChange = { rawJsonText = it },
                        label = { Text("JSON Профиля") },
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
