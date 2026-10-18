package com.city.editor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

enum class GameModeType { SUBWAY_CITY, SUBWAY_CLASSIC }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E5FF),
                    secondary = Color(0xFFFFEA00),
                    background = Color(0xFF0D0F14),
                    surface = Color(0xFF191C24)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CityEditUltimateApp()
                }
            }
        }
    }

    @Composable
    fun CityEditUltimateApp() {
        val scope = rememberCoroutineScope()
        var currentMode by remember { mutableStateOf(GameModeType.SUBWAY_CITY) }
        var statusMsg by remember { mutableStateOf("Проверка Shizuku...") }
        var isLoading by remember { mutableStateOf(false) }

        // Состояние Subway Surfers City
        var cityJson by remember { mutableStateOf<JSONObject?>(null) }

        // Состояние Subway Surfers Classic (кэш загруженных файлов профиля)
        val classicFiles = remember { mutableStateMapOf<String, JSONObject>() }
        val classicFormats = remember { mutableStateMapOf<String, CryptoManager.ClassicDecryptResult>() }

        fun loadCityFromShizuku() {
            scope.launch {
                isLoading = true
                statusMsg = "Чтение save.dat (Subway City)..."
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        if (!ShizukuHelper.checkPermission()) throw Exception("Выдайте доступ в приложении Shizuku!")
                        ShizukuHelper.readFileBytes(ShizukuHelper.CITY_SAVE_PATH)
                    }
                    val decrypted = CryptoManager.decryptCitySave(bytes)
                    cityJson = JSONObject(decrypted)
                    statusMsg = "✅ City save.dat загружен (${bytes.size / 1024} КБ)"
                } catch (e: Exception) {
                    statusMsg = "❌ Ошибка City: ${e.message}"
                }
                isLoading = false
            }
        }

        fun saveCityToShizuku(autoLaunch: Boolean = true) {
            val root = cityJson ?: return
            scope.launch {
                isLoading = true
                statusMsg = "Шифрование и запись save.dat + manifest.json..."
                try {
                    withContext(Dispatchers.IO) {
                        val encryptedBytes = CryptoManager.encryptCitySave(root.toString())
                        val newHash = CryptoManager.sha1Hex(encryptedBytes)
                        ShizukuHelper.writeFileBytes(ShizukuHelper.CITY_SAVE_PATH, encryptedBytes, ShizukuHelper.CITY_PKG)

                        try {
                            val manifestBytes = ShizukuHelper.readFileBytes(ShizukuHelper.CITY_MANIFEST_PATH)
                            val manifestStr = manifestBytes.toString(Charsets.UTF_8)
                            val patchedManifest = CryptoManager.patchCityManifest(manifestStr, newHash)
                            ShizukuHelper.writeFileBytes(
                                ShizukuHelper.CITY_MANIFEST_PATH,
                                patchedManifest.toByteArray(Charsets.UTF_8)
                            )
                        } catch (_: Exception) {}

                        if (autoLaunch) ShizukuHelper.launchGame(ShizukuHelper.CITY_PKG)
                    }
                    statusMsg = "✅ Сохранено в Subway City!"
                    Toast.makeText(this@MainActivity, "Успешно сохранено!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    statusMsg = "❌ Ошибка записи City: ${e.message}"
                }
                isLoading = false
            }
        }

        fun loadClassicAllFiles() {
            scope.launch {
                isLoading = true
                statusMsg = "Чтение профиля Classic через Shizuku..."
                try {
                    withContext(Dispatchers.IO) {
                        if (!ShizukuHelper.checkPermission()) throw Exception("Выдайте доступ в Shizuku!")
                        val targetFiles = listOf(
                            "wallet", "characters_inventory", "boards_inventory",
                            "upgrades", "achievements", "collections", "collectionsrevamp",
                            "season_hunt", "profile_frame", "profile_portrait",
                            "quests", "generic_challenges", "missions", "trophies", "word_hunt"
                        )
                        var loadedCount = 0
                        for (base in targetFiles) {
                            try {
                                val path = ShizukuHelper.resolveClassicFilePath(base)
                                val bytes = ShizukuHelper.readFileBytes(path)
                                val res = CryptoManager.decryptClassicFile(bytes)
                                val obj = JSONObject(res.json)
                                classicFiles[base] = obj
                                classicFormats[base] = res
                                loadedCount++
                            } catch (_: Exception) {}
                        }
                        statusMsg = if (loadedCount > 0) {
                            "✅ Загружено $loadedCount файлов профиля Classic!"
                        } else {
                            "⚠️ Файлы в ${ShizukuHelper.CLASSIC_PROFILE_DIR} не найдены"
                        }
                    }
                } catch (e: Exception) {
                    statusMsg = "❌ Ошибка Classic: ${e.message}"
                }
                isLoading = false
            }
        }

        fun saveClassicFile(baseName: String, autoLaunch: Boolean = false) {
            val jsonObj = classicFiles[baseName] ?: return
            scope.launch {
                isLoading = true
                statusMsg = "Сохранение $baseName.json..."
                try {
                    withContext(Dispatchers.IO) {
                        jsonObj.put("lastSaved", Instant.now().toString())
                        val formatInfo = classicFormats[baseName]
                        val mode = formatInfo?.mode ?: CryptoManager.ClassicFormatMode.HEADER4_IV16_CBC
                        val iv = formatInfo?.extractedIv ?: ByteArray(16)
                        val outBytes = CryptoManager.encryptClassicFile(jsonObj.toString(2), mode, iv)
                        val remotePath = ShizukuHelper.resolveClassicFilePath(baseName)
                        ShizukuHelper.writeFileBytes(remotePath, outBytes, ShizukuHelper.CLASSIC_PKG)
                        if (autoLaunch) ShizukuHelper.launchGame(ShizukuHelper.CLASSIC_PKG)
                    }
                    statusMsg = "✅ Файл $baseName успешно сохранён!"
                    Toast.makeText(this@MainActivity, "$baseName сохранён!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    statusMsg = "❌ Ошибка записи $baseName: ${e.message}"
                }
                isLoading = false
            }
        }

        fun saveAllClassicFiles(autoLaunch: Boolean = true) {
            scope.launch {
                isLoading = true
                statusMsg = "Запись всех изменённых файлов Classic..."
                try {
                    withContext(Dispatchers.IO) {
                        ShizukuHelper.exec("am force-stop ${ShizukuHelper.CLASSIC_PKG}")
                        for ((baseName, jsonObj) in classicFiles) {
                            jsonObj.put("lastSaved", Instant.now().toString())
                            val formatInfo = classicFormats[baseName]
                            val mode = formatInfo?.mode ?: CryptoManager.ClassicFormatMode.HEADER4_IV16_CBC
                            val iv = formatInfo?.extractedIv ?: ByteArray(16)
                            val outBytes = CryptoManager.encryptClassicFile(jsonObj.toString(2), mode, iv)
                            val remotePath = ShizukuHelper.resolveClassicFilePath(baseName)
                            ShizukuHelper.writeFileBytes(remotePath, outBytes, null)
                        }
                        if (autoLaunch) ShizukuHelper.launchGame(ShizukuHelper.CLASSIC_PKG)
                    }
                    statusMsg = "✅ Все файлы Classic записаны и синхронизированы!"
                } catch (e: Exception) {
                    statusMsg = "❌ Ошибка: ${e.message}"
                }
                isLoading = false
            }
        }

        LaunchedEffect(currentMode) {
            if (currentMode == GameModeType.SUBWAY_CITY && cityJson == null) {
                loadCityFromShizuku()
            } else if (currentMode == GameModeType.SUBWAY_CLASSIC && classicFiles.isEmpty()) {
                loadClassicAllFiles()
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // ВЕРХНЯЯ ПАНЕЛЬ И ПЕРЕКЛЮЧАТЕЛЬ ИГРЫ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ CITYEDIT ULTIMATE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = currentMode == GameModeType.SUBWAY_CITY,
                        onClick = { currentMode = GameModeType.SUBWAY_CITY },
                        label = { Text("🏙️ CITY") }
                    )
                    FilterChip(
                        selected = currentMode == GameModeType.SUBWAY_CLASSIC,
                        onClick = { currentMode = GameModeType.SUBWAY_CLASSIC },
                        label = { Text("🛹 CLASSIC") }
                    )
                }
            }

            // СТАТУС-БАР
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(statusMsg, fontSize = 12.sp, color = Color(0xFFB0BEC5), modifier = Modifier.weight(1f))
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }

            // ОСНОВНОЙ КОНТЕНТ В ЗАВИСИМОСТИ ОТ ВЫБРАННОЙ ИГРЫ
            Box(modifier = Modifier.weight(1f)) {
                if (currentMode == GameModeType.SUBWAY_CITY) {
                    SubwayCityScreen(
                        root = cityJson,
                        onReload = { loadCityFromShizuku() },
                        onSave = { saveCityToShizuku(true) },
                        onUpdateStatus = { statusMsg = it }
                    )
                } else {
                    SubwayClassicScreen(
                        classicFiles = classicFiles,
                        onReload = { loadClassicAllFiles() },
                        onSaveSingle = { base -> saveClassicFile(base, false) },
                        onSaveAll = { saveAllClassicFiles(true) },
                        onUpdateStatus = { statusMsg = it }
                    )
                }
            }
        }
    }

    // ========================================================================
    // ЭКРАН 1: SUBWAY SURFERS CITY (ВКЛЮЧАЯ ТРИАЛЫ И СВИНЬЮ-КОПИЛКУ)
    // ========================================================================
    @Composable
    fun SubwayCityScreen(
        root: JSONObject?,
        onReload: () -> Unit,
        onSave: () -> Unit,
        onUpdateStatus: (String) -> Unit
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("💰 Кошелек & Свинья", "🏆 Триалы & Сезон", "🏃 Сёрферы & Доски", "⚙️ Хардкор")

        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (root == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = onReload) { Text("🔄 Загрузить save.dat через Shizuku") }
                }
                return@Column
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (selectedTab) {
                    0 -> {
                        // КОШЕЛЕК И СВИНЬЯ-КОПИЛКА (PIGGY BANK)
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🐷 Свинья-Копилка (Piggy Bank Hack)", fontWeight = FontWeight.Bold, color = Color(0xFFFF80AB))
                                    Text(
                                        "Переполняет копилку до максимума, ставит максимальный уровень и разблокирует сбор награды.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Button(
                                        onClick = {
                                            val piggy = root.optJSONObject("piggyBank") ?: JSONObject().also { root.put("piggyBank", it) }
                                            piggy.put("level", 10)
                                            piggy.put("currentCoins", 9999999)
                                            piggy.put("savedCoins", 9999999)
                                            piggy.put("points", 999999)
                                            piggy.put("isFull", true)
                                            piggy.put("isUnlocked", true)
                                            piggy.put("canClaimFree", true)
                                            piggy.put("cooldownEndTime", 0)
                                            onUpdateStatus("🐷 Свинья-копилка прокачана на максимум!")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD81B60))
                                    ) {
                                        Text("🐷 ВЗЛОМАТЬ СВИНЬЮ-КОПИЛКУ (MAX + FREE CLAIM)", color = Color.White)
                                    }
                                }
                            }
                        }

                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💎 Быстрая накрутка валюты City", fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = {
                                            val wallet = root.optJSONObject("wallet") ?: JSONObject().also { root.put("wallet", it) }
                                            val currencies = wallet.optJSONObject("currencies") ?: JSONObject().also { wallet.put("currencies", it) }
                                            listOf("coins", "keys", "gems", "cans", "sprayCans", "energy", "trialTokens", "reviveTokens").forEach { cur ->
                                                currencies.put(cur, 99999999)
                                            }
                                            onUpdateStatus("💎 Все валюты City выставлены на 99,999,999!")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("💰 99,999,999 ВСЕХ ВАЛЮТ И БАНКИ КРАСКИ")
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ОТКРЫТИЕ ВСЕХ ТРИАЛОВ (ИВЕНТОВЫЕ ЗАБЕГИ) В SUBWAY CITY
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏁 Ивентовые забеги (Триалы / Trials)", fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                    Text(
                                        "Открывает доступ ко всем триалам (Daily, Weekly, Character/Board Showcase, Hardcore), снимает кулдауны, выдает билеты и засчитывает все этапы наград.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    Button(
                                        onClick = {
                                            // 1. Открываем глобальный доступ к триалам
                                            val features = root.optJSONObject("unlockedFeatures") ?: JSONObject().also { root.put("unlockedFeatures", it) }
                                            features.put("trials", true)
                                            features.put("featuredTrials", true)
                                            features.put("dailyTrials", true)
                                            features.put("eventTrials", true)

                                            // 2. Прописываем все триалы в секцию trials / trialStates
                                            val trialsObj = root.optJSONObject("trials") ?: JSONObject().also { root.put("trials", it) }
                                            trialsObj.put("isUnlocked", true)
                                            trialsObj.put("tickets", 9999)
                                            trialsObj.put("cooldownEndTime", 0)

                                            val statesObj = trialsObj.optJSONObject("trialStates") ?: JSONObject().also { trialsObj.put("trialStates", it) }
                                            GameDataRepository.cityTrials.forEach { trialId ->
                                                val st = statesObj.optJSONObject(trialId) ?: JSONObject()
                                                st.put("id", trialId)
                                                st.put("unlocked", true)
                                                st.put("isAvailable", true)
                                                st.put("completed", true)
                                                st.put("highScore", 9999999)
                                                st.put("stars", 3)
                                                st.put("cooldown", 0)
                                                st.put("attemptsLeft", 999)
                                                statesObj.put(trialId, st)
                                            }

                                            // 3. Если в сохранении уже есть кастомные активные триалы — открываем их все
                                            val existingKeys = statesObj.keys()
                                            while (existingKeys.hasNext()) {
                                                val k = existingKeys.next()
                                                statesObj.optJSONObject(k)?.apply {
                                                    put("unlocked", true)
                                                    put("isAvailable", true)
                                                    put("cooldown", 0)
                                                    put("attemptsLeft", 999)
                                                }
                                            }

                                            onUpdateStatus("🏁 Все Триалы (Ивентовые забеги) открыты и разблокированы!")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                    ) {
                                        Text("🔓 ОТКРЫТЬ ВСЕ ТРИАЛЫ + 9999 БИЛЕТОВ", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val pass = root.optJSONObject("seasonPass") ?: JSONObject().also { root.put("seasonPass", it) }
                                            pass.put("isPremium", true)
                                            pass.put("hasPlus", true)
                                            pass.put("currentTier", 50)
                                            pass.put("points", 999999)
                                            onUpdateStatus("🎫 Surfer Pass City полностью открыт (Tier 50 + Premium)!")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("🎫 ОТКРЫТЬ ВЕСЬ SURFER PASS (PREMIUM + TIER 50)")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // СЁРФЕРЫ И ДОСКИ CITY
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏃 Сёрферы и Ховерборды City", fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = {
                                            val chars = root.optJSONObject("characters") ?: JSONObject().also { root.put("characters", it) }
                                            GameDataRepository.citySurfers.forEach { id ->
                                                val c = chars.optJSONObject(id) ?: JSONObject()
                                                c.put("id", id)
                                                c.put("unlocked", true)
                                                c.put("level", 15)
                                                c.put("tokens", 9999)
                                                chars.put(id, c)
                                            }
                                            val boards = root.optJSONObject("boards") ?: JSONObject().also { root.put("boards", it) }
                                            GameDataRepository.cityBoards.forEach { id ->
                                                val b = boards.optJSONObject(id) ?: JSONObject()
                                                b.put("id", id)
                                                b.put("unlocked", true)
                                                b.put("level", 15)
                                                boards.put(id, b)
                                            }
                                            onUpdateStatus("🔥 Все сёрферы и доски City открыты и прокачаны до 15 уровня!")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("🌟 ОТКРЫТЬ ВСЕХ ПЕРСОНАЖЕЙ И ДОСКИ (LV 15)")
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // ХАРДКОР И РАЙОНЫ CITY
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🗺️ Районы (Districts) и Бесконечная Энергия", fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = {
                                            val districts = root.optJSONObject("districts") ?: JSONObject().also { root.put("districts", it) }
                                            for (i in 1..10) {
                                                val d = districts.optJSONObject("district_$i") ?: JSONObject()
                                                d.put("unlocked", true)
                                                d.put("level", 10)
                                                d.put("stars", 99)
                                                districts.put("district_$i", d)
                                            }
                                            onUpdateStatus("🗺️ Все районы City открыты!")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("🗺️ ОТКРЫТЬ ВСЕ РАЙОНЫ ГОРОДА")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // КНОПКИ СОХРАНЕНИЯ CITY
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReload, modifier = Modifier.weight(1f)) {
                    Text("🔄 Перечитать")
                }
                Button(onClick = onSave, modifier = Modifier.weight(2f)) {
                    Text("💾 СОХРАНИТЬ И ЗАПУСТИТЬ CITY")
                }
            }
        }
    }

    // ========================================================================
    // ЭКРАН 2: SUBWAY SURFERS CLASSIC (С ПОИСКОМ ПЕРСОНАЖЕЙ И ВСЕМИ ФАЙЛАМИ)
    // ========================================================================
    @Composable
    fun SubwayClassicScreen(
        classicFiles: MutableMap<String, JSONObject>,
        onReload: () -> Unit,
        onSaveSingle: (String) -> Unit,
        onSaveAll: () -> Unit,
        onUpdateStatus: (String) -> Unit
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf(
            "💰 Кошелек & Бусты",
            "🔍 Сёрферы (Поиск)",
            "🛹 Доски (Поиск)",
            "🏆 Коллекции & Ивенты",
            "🖼️ Профиль & Ачивки"
        )

        var searchQuery by remember { mutableStateOf("") }
        var refreshTrigger by remember { mutableStateOf(0) }

        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (classicFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = onReload) { Text("🔄 Загрузить файлы профиля Classic") }
                }
                return@Column
            }

            // Поле поиска для вкладок Персонажей и Досок
            if (selectedTab == 1 || selectedTab == 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (selectedTab == 1) "🔍 Поиск персонажа или костюма (например, Corvin, Jake, Brawl)..." else "🔍 Поиск доски или способности...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            TextButton(onClick = { searchQuery = "" }) { Text("✖") }
                        }
                    }
                )
            }

            key(refreshTrigger) {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (selectedTab) {
                        // ВКЛАДКА 0: КОШЕЛЕК (wallet.json) И АПГРЕЙДЫ (upgrades.json)
                        0 -> {
                            val walletObj = classicFiles["wallet"]
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("💰 Редактор wallet.json", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        if (walletObj == null) {
                                            Text("Файл wallet.json не загружен", color = Color.Red)
                                        } else {
                                            val currencies = walletObj.optJSONObject("currencies") ?: JSONObject().also { walletObj.put("currencies", it) }

                                            GameDataRepository.classicCurrencyMap.forEach { (id, label, icon) ->
                                                val entry = currencies.optJSONObject(id) ?: JSONObject().also { currencies.put(id, it) }
                                                var amountStr by remember(refreshTrigger) { mutableStateOf(entry.optLong("value", 0L).toString()) }

                                                OutlinedTextField(
                                                    value = amountStr,
                                                    onValueChange = { newVal ->
                                                        amountStr = newVal
                                                        newVal.toLongOrNull()?.let { v ->
                                                            entry.put("value", v)
                                                            if (!entry.has("expirationType")) entry.put("expirationType", 0)
                                                            if (!entry.has("contextType")) entry.put("contextType", 0)
                                                            if (!entry.has("contextData")) entry.put("contextData", JSONObject())
                                                        }
                                                    },
                                                    label = { Text("$icon $label [ID: $id]") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val currenciesObj = walletObj.optJSONObject("currencies") ?: JSONObject().also { walletObj.put("currencies", it) }
                                                    GameDataRepository.classicCurrencyMap.forEach { (id, _, _) ->
                                                        val c = currenciesObj.optJSONObject(id) ?: JSONObject().also { currenciesObj.put(id, it) }
                                                        c.put("value", 999999999L)
                                                        if (id == "6") {
                                                            c.put("expirationType", 1)
                                                            c.put("expirationValue", 1999999999999L)
                                                        } else {
                                                            c.put("expirationType", 0)
                                                        }
                                                    }
                                                    refreshTrigger++
                                                    onUpdateStatus("💰 Все валюты в wallet.json выставлены на 999,999,999!")
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🚀 999,999,999 ВСЕХ ВАЛЮТ В ОДИН КЛИК")
                                            }

                                            Button(
                                                onClick = {
                                                    val queue = walletObj.optJSONObject("lootboxQueue") ?: JSONObject().also { walletObj.put("lootboxQueue", it) }
                                                    val arr = queue.optJSONArray("unopenedLootboxes") ?: JSONArray().also { queue.put("unopenedLootboxes", it) }
                                                    repeat(25) {
                                                        arr.put(JSONObject().put("lootboxId", "super_mystery_box").put("source", "cheat"))
                                                    }
                                                    onUpdateStatus("🎁 Добавлено 25 Super Mystery Box в очередь открытия!")
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                                            ) {
                                                Text("🎁 ДОБАВИТЬ 25 SUPER MYSTERY BOX В ОЧЕРЕДЬ", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("⚡ Улучшения, Множители и Донат (upgrades.json)", fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = {
                                                val upg = classicFiles.getOrPut("upgrades") { JSONObject() }
                                                // Максимальный уровень джетпака, кроссовок, магнита и 2x очков (Lv 6)
                                                val pLevels = JSONObject().apply {
                                                    put("jetpack", 6)
                                                    put("superSneakers", 6)
                                                    put("magnet", 6)
                                                    put("doubleScore", 6)
                                                }
                                                upg.put("powerupLevels", pLevels)

                                                // Перманентные множители счета (+5, +3, +1) и удвоение монет (Double Coins)
                                                val scoreMods = upg.optJSONObject("scoreModifiers") ?: JSONObject().also { upg.put("scoreModifiers", it) }
                                                listOf("permanent_score_multiplier" to 5, "permanent_score_multiplier_3" to 3, "permanent_score_multiplier_1" to 1).forEach { (id, v) ->
                                                    scoreMods.put(id, JSONObject().apply {
                                                        put("value", JSONObject().put("id", id).put("type", 2).put("subType", 3).put("value", v))
                                                        put("expirationType", 0)
                                                        put("contextType", 0)
                                                        put("contextData", JSONObject())
                                                    })
                                                }

                                                val pickupMods = upg.optJSONObject("currencyPickupModifiers") ?: JSONObject().also { upg.put("currencyPickupModifiers", it) }
                                                pickupMods.put("doubleCoins", JSONObject().apply {
                                                    put("value", JSONObject().put("id", "doubleCoins").put("type", 0).put("subType", 0).put("value", 2))
                                                    put("expirationType", 0)
                                                    put("contextType", 0)
                                                    put("contextData", JSONObject())
                                                })
                                                onUpdateStatus("⚡ Удвоение монет (Double Coins), Множители и Бустеры Lv.6 активированы!")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("⚡ АКТИВИРОВАТЬ DOUBLE COINS + ВСЕ БУСТЫ LV.6")
                                        }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 1: ПЕРСОНАЖИ С ПОИСКОМ (characters_inventory.json)
                        1 -> {
                            val charInv = classicFiles.getOrPut("characters_inventory") {
                                JSONObject().put("patchVersion", 3).put("owned", JSONObject())
                            }
                            val ownedChars = charInv.optJSONObject("owned") ?: JSONObject().also { charInv.put("owned", it) }
                            val selectedObj = charInv.optJSONObject("selected") ?: JSONObject().also { charInv.put("selected", it) }
                            val currentSelectedChar = selectedObj.optString("character", "jake")

                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            GameDataRepository.classicCharacters.forEach { info ->
                                                val outfitsArr = JSONArray()
                                                info.outfits.forEach { outfitsArr.put(it) }
                                                ownedChars.put(info.id, JSONObject().apply {
                                                    put("id", info.id)
                                                    put("ownedOutfits", outfitsArr)
                                                    put("lastOutfit", info.outfits.last())
                                                })
                                            }
                                            refreshTrigger++
                                            onUpdateStatus("🏃 Открыты все ${GameDataRepository.classicCharacters.size} персонажей и все костюмы!")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                    ) {
                                        Text("🔓 ОТКРЫТЬ ВСЕХ (${GameDataRepository.classicCharacters.size})", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            val filteredChars = GameDataRepository.classicCharacters.filter {
                                searchQuery.isBlank() ||
                                    it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.id.contains(searchQuery, ignoreCase = true) ||
                                    it.outfits.any { o -> o.contains(searchQuery, ignoreCase = true) }
                            }

                            items(filteredChars, key = { it.id }) { charInfo ->
                                val isOwned = ownedChars.has(charInfo.id)
                                val isSelected = currentSelectedChar == charInfo.id

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOwned) Color(0xFF1E2736) else Color(0xFF14161D)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${charInfo.name} (${charInfo.id})",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFFFFEA00) else Color.White
                                            )
                                            Text(
                                                text = "Костюмы: ${charInfo.outfits.joinToString(", ")}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (isOwned && !isSelected) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedObj.put("character", charInfo.id)
                                                        selectedObj.put("outfit", "default")
                                                        refreshTrigger++
                                                        onUpdateStatus("Выбран персонаж: ${charInfo.name}")
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Выбрать", fontSize = 11.sp)
                                                }
                                            }
                                            Switch(
                                                checked = isOwned,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        val outfitsArr = JSONArray()
                                                        charInfo.outfits.forEach { outfitsArr.put(it) }
                                                        ownedChars.put(charInfo.id, JSONObject().apply {
                                                            put("id", charInfo.id)
                                                            put("ownedOutfits", outfitsArr)
                                                            put("lastOutfit", "default")
                                                        })
                                                    } else {
                                                        ownedChars.remove(charInfo.id)
                                                    }
                                                    refreshTrigger++
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 2: ХОВЕРБОРДЫ С ПОИСКОМ (boards_inventory.json)
                        2 -> {
                            val boardInv = classicFiles.getOrPut("boards_inventory") {
                                JSONObject().put("patchVersion", 2).put("owned", JSONObject())
                            }
                            val ownedBoards = boardInv.optJSONObject("owned") ?: JSONObject().also { boardInv.put("owned", it) }
                            val currentSelectedBoard = boardInv.optString("selected", "default")

                            item {
                                Button(
                                    onClick = {
                                        GameDataRepository.classicBoards.forEach { info ->
                                            val upgObj = JSONObject()
                                            info.upgrades.forEach { upgObj.put(it, true) }
                                            ownedBoards.put(info.id, JSONObject().apply {
                                                put("id", info.id)
                                                put("ownedUpgrades", upgObj)
                                            })
                                        }
                                        refreshTrigger++
                                        onUpdateStatus("🛹 Открыты все ${GameDataRepository.classicBoards.size} досок со всеми способностями!")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                ) {
                                    Text("🔓 ОТКРЫТЬ ВСЕ ДОСКИ + АПГРЕЙДЫ (${GameDataRepository.classicBoards.size})", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            val filteredBoards = GameDataRepository.classicBoards.filter {
                                searchQuery.isBlank() ||
                                    it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.id.contains(searchQuery, ignoreCase = true) ||
                                    it.upgrades.any { u -> u.contains(searchQuery, ignoreCase = true) }
                            }

                            items(filteredBoards, key = { it.id }) { bInfo ->
                                val isOwned = ownedBoards.has(bInfo.id)
                                val isSelected = currentSelectedBoard == bInfo.id

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOwned) Color(0xFF1E2736) else Color(0xFF14161D)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${bInfo.name} (${bInfo.id})",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFFFFEA00) else Color.White
                                            )
                                            if (bInfo.upgrades.isNotEmpty()) {
                                                Text(
                                                    text = "Способности: ${bInfo.upgrades.joinToString(", ")}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF80D8FF)
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (isOwned && !isSelected) {
                                                OutlinedButton(
                                                    onClick = {
                                                        boardInv.put("selected", bInfo.id)
                                                        refreshTrigger++
                                                        onUpdateStatus("Выбрана доска: ${bInfo.name}")
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Выбрать", fontSize = 11.sp)
                                                }
                                            }
                                            Switch(
                                                checked = isOwned,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        val upgObj = JSONObject()
                                                        bInfo.upgrades.forEach { upgObj.put(it, true) }
                                                        ownedBoards.put(bInfo.id, JSONObject().apply {
                                                            put("id", bInfo.id)
                                                            put("ownedUpgrades", upgObj)
                                                        })
                                                    } else {
                                                        ownedBoards.remove(bInfo.id)
                                                    }
                                                    refreshTrigger++
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 3: КОЛЛЕКЦИИ, СЕЗОННАЯ ОХОТА, КВЕСТЫ И ИВЕНТОВЫЕ ЗАБЕГИ (CLASSIC)
                        3 -> {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("📚 Коллекции (collections & collectionsrevamp)", fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = {
                                                classicFiles["collections"]?.let { colObj ->
                                                    val states = colObj.optJSONObject("collectionsState")
                                                    states?.keys()?.forEach { k ->
                                                        val c = states.optJSONObject(k)
                                                        val items = c?.optJSONArray("items")
                                                        if (items != null) {
                                                            for (i in 0 until items.length()) {
                                                                items.optJSONObject(i)?.put("claimed", true)
                                                            }
                                                        }
                                                        c?.put("upgradesState", JSONArray().put(2))
                                                    }
                                                    val seasonal = colObj.optJSONObject("seasonalCollectionsState")
                                                    seasonal?.keys()?.forEach { k ->
                                                        val c = seasonal.optJSONObject(k)
                                                        val items = c?.optJSONArray("items")
                                                        if (items != null) {
                                                            for (i in 0 until items.length()) {
                                                                items.optJSONObject(i)?.put("claimed", true)
                                                            }
                                                        }
                                                        c?.put("upgradesState", JSONArray().put(2))
                                                    }
                                                }
                                                onUpdateStatus("📚 Все стандартные и сезонные коллекции полностью закрыты!")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("📚 ЗАКРЫТЬ ВСЕ СТАНДАРТНЫЕ И СЕЗОННЫЕ КОЛЛЕКЦИИ")
                                        }
                                    }
                                }
                            }

                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🏁 Ивентовые забеги и Сезонная охота (generic_challenges & season_hunt)", fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = {
                                                // 1. Проходим все активные челенджи (Trick or Treat, Floor is Lava, Marathon, Tag Time Attack, Showdown)
                                                classicFiles["generic_challenges"]?.let { gc ->
                                                    val chStates = gc.optJSONObject("challengeStates")
                                                    chStates?.keys()?.forEach { chKey ->
                                                        val ch = chStates.optJSONObject(chKey) ?: return@forEach
                                                        ch.put("currentScore", 5000000)
                                                        ch.put("highScore", 5000000)
                                                        ch.put("lastSeenScore", 5000000)
                                                        // Сброс кулдаунов персонажей в ивентах
                                                        ch.optJSONObject("requirements")?.put("cooldownItems", JSONObject())
                                                        val rStates = ch.optJSONArray("rewardStates")
                                                        if (rStates != null) {
                                                            for (i in 0 until rStates.length()) {
                                                                rStates.optJSONObject(i)?.apply {
                                                                    put("State", 2) // Готово к сбору!
                                                                    put("AdRequired", false)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // 2. Прокачиваем Season Hunt
                                                classicFiles["season_hunt"]?.apply {
                                                    put("collectedTokens", 99999)
                                                }

                                                // 3. Выполняем все квесты (quests_2.json)
                                                classicFiles["quests"]?.apply {
                                                    put("progressPoints", 10000)
                                                    put("lastSeenProgressPoints", 10000)
                                                }

                                                onUpdateStatus("🏁 Все Ивентовые Забеги, Квесты и Season Hunt пройдены (без рекламы и кулдаунов)!")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                                        ) {
                                            Text("🏁 ПРОЙТИ ВСЕ ИВЕНТОВЫЕ ЗАБЕГИ + СНЯТЬ КУЛДАУНЫ", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // ВКЛАДКА 4: ПРОФИЛЬ, РАМКИ, ПОРТРЕТЫ, ТРОФЕИ И АЧИВКИ
                        4 -> {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🏅 Достижения, Портреты, Рамки и Трофеи", fontWeight = FontWeight.Bold)

                                        Button(
                                            onClick = {
                                                // 1. Все 41 достижение
                                                classicFiles["achievements"]?.let { ach ->
                                                    val entries = ach.optJSONObject("achievementEntries")
                                                    entries?.keys()?.forEach { k ->
                                                        val item = entries.optJSONObject(k) ?: return@forEach
                                                        val goals = item.optJSONArray("goals")
                                                        if (goals != null && goals.length() > 0) {
                                                            val maxGoal = goals.optInt(goals.length() - 1, 100)
                                                            item.put("currentTier", goals.length() - 1)
                                                            item.put("progress", maxGoal)
                                                            item.put("highestProgress", maxGoal)
                                                            val claimArr = JSONArray()
                                                            for (i in 0 until goals.length()) claimArr.put(true)
                                                            item.put("claimState", claimArr)
                                                            item.put("markAsViewed", true)
                                                        }
                                                    }
                                                }

                                                // 2. Все рамки профиля
                                                val framesObj = classicFiles.getOrPut("profile_frame") { JSONObject().put("patchVersion", 1) }
                                                val ownedFrames = framesObj.optJSONObject("owned") ?: JSONObject().also { framesObj.put("owned", it) }
                                                GameDataRepository.classicFrames.forEach { fId ->
                                                    ownedFrames.put(fId, JSONObject().put("id", fId).put("isSeen", true))
                                                }

                                                // 3. Все портреты профиля
                                                val portraitsObj = classicFiles.getOrPut("profile_portrait") { JSONObject().put("patchVersion", 1) }
                                                val ownedPortraits = portraitsObj.optJSONObject("owned") ?: JSONObject().also { portraitsObj.put("owned", it) }
                                                GameDataRepository.classicPortraits.forEach { pId ->
                                                    ownedPortraits.put(pId, JSONObject().put("id", pId).put("isSeen", true))
                                                }

                                                // 4. Все трофеи
                                                val trophiesObj = classicFiles.getOrPut("trophies") { JSONObject() }
                                                val tArr = JSONArray()
                                                GameDataRepository.classicTrophies.forEach { tArr.put(it) }
                                                trophiesObj.put("trophiesEarned", tArr)
                                                trophiesObj.put("trophyCompletions", JSONObject().put("TrophySet1", 4).put("TrophySet2", 4))

                                                onUpdateStatus("🏅 Все 41 достижение, ${GameDataRepository.classicPortraits.size} портретов, ${GameDataRepository.classicFrames.size} рамок и трофеи разблокированы!")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
                                        ) {
                                            Text("🏆 ОТКРЫТЬ ВСЕ АЧИВКИ, ПОРТРЕТЫ, РАМКИ И ТРОФЕИ", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // НИЖНЯЯ ПАНЕЛЬ СОХРАНЕНИЯ CLASSIC
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReload, modifier = Modifier.weight(1f)) {
                    Text("🔄 Перечитать")
                }
                Button(onClick = onSaveAll, modifier = Modifier.weight(2f)) {
                    Text("💾 СОХРАНИТЬ ВСЁ В CLASSIC")
                }
            }
        }
    }
}
