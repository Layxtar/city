package com.layxtar.subwaysaver

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ClassicScreen(
    activity: ComponentActivity,
    statusText: String,
    logs: String,
    log: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var uiTrigger by remember { mutableIntStateOf(0) }

    var classicState by remember { mutableStateOf<ClassicMemoryState?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    val undoStack = remember { mutableStateListOf<ClassicMemoryState>() }

    val classicCurrencies = remember {
        mutableStateMapOf<String, String>().apply {
            ClassicDatabase.CURRENCIES.forEach { (id, _) -> put(id, "0") }
        }
    }
    var doubleCoinsOnly by remember { mutableStateOf(false) }

    // Медали Top Run: 106=Бронза, 105=Серебро, 104=Золото, 103=Алмаз, 102=Чемпион
    var medalBronze by remember { mutableStateOf("0") }
    var medalSilver by remember { mutableStateOf("0") }
    var medalGold by remember { mutableStateOf("0") }
    var medalDiamond by remember { mutableStateOf("0") }
    var medalChampion by remember { mutableStateOf("0") }

    var activeChallengesList by remember { mutableStateOf<List<ActiveChallengeInfo>>(emptyList()) }
    var selectedChallengeId by remember { mutableStateOf("") }
    var customScoreInput by remember { mutableStateOf("1000000") }
    var unlockStagesCheckbox by remember { mutableStateOf(true) }

    var surferSearchQuery by remember { mutableStateOf("") }
    var boardSearchQuery by remember { mutableStateOf("") }
    var expandedSurferId by remember { mutableStateOf<String?>(null) }
    var expandedBoardId by remember { mutableStateOf<String?>(null) }
    var selectedLootbox by remember { mutableStateOf(ClassicDatabase.LOOTBOX_TYPES.first().first) }
    var lootboxCount by remember { mutableStateOf("10") }

    fun applyUIToState(st: ClassicMemoryState) {
        for ((id, valStr) in classicCurrencies) {
            val amount = valStr.toLongOrNull() ?: 0L
            ClassicCryptoAndSyncManager.setCurrencyInMemory(st.wallet, id, amount)
        }
        val medalsMap = mapOf(
            "106" to (medalBronze.toIntOrNull() ?: 0),
            "105" to (medalSilver.toIntOrNull() ?: 0),
            "104" to (medalGold.toIntOrNull() ?: 0),
            "103" to (medalDiamond.toIntOrNull() ?: 0),
            "102" to (medalChampion.toIntOrNull() ?: 0)
        )
        ClassicCryptoAndSyncManager.setTopRunMedals(st, medalsMap)
    }

    fun refreshUIFromState(st: ClassicMemoryState) {
        val currencies = st.wallet.optJSONObject("currencies")
        for ((id, _) in ClassicDatabase.CURRENCIES) {
            val v = currencies?.optJSONObject(id)?.optLong("value", 0L) ?: 0L
            classicCurrencies[id] = v.toString()
        }
        doubleCoinsOnly = ClassicCryptoAndSyncManager.isDoubleCoinsEnabled(st)
        val medals = ClassicCryptoAndSyncManager.getTopRunMedals(st)
        medalBronze = (medals["106"] ?: 0).toString()
        medalSilver = (medals["105"] ?: 0).toString()
        medalGold = (medals["104"] ?: 0).toString()
        medalDiamond = (medals["103"] ?: 0).toString()
        medalChampion = (medals["102"] ?: 0).toString()

        val list = ClassicCryptoAndSyncManager.getActiveChallenges(st)
        activeChallengesList = list
        if (list.isNotEmpty() && (selectedChallengeId.isEmpty() || list.none { it.id == selectedChallengeId })) {
            selectedChallengeId = list.first().id
            customScoreInput = maxOf(list.first().maxScoreRequired, 1000000L).toString()
        }
        uiTrigger++
    }

    fun pushUndo() {
        val st = classicState ?: return
        applyUIToState(st)
        if (undoStack.size >= 20) undoStack.removeAt(0)
        undoStack.add(st.deepCopy())
    }

    fun performUndo() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeAt(undoStack.lastIndex)
        classicState = prev
        refreshUIFromState(prev)
        log("↩️ Отменено последнее действие в Classic")
    }

    fun loadClassicAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Чтение файлов Classic Subway Surfers...")
            val loaded = withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.loadFullClassicState {} }
            classicState = loaded
            undoStack.clear()
            refreshUIFromState(loaded)
            isLoaded = true
            log("✅ Classic загружен!")
            isBusy = false
        }
    }

    fun saveClassicAsync(launchAfterSave: Boolean) {
        val st = classicState ?: return
        applyUIToState(st)
        scope.launch {
            isBusy = true
            log("💾 Сохранение Classic...")
            val ok = withContext(Dispatchers.IO) { ClassicCryptoAndSyncManager.saveFullClassicState(st) {} }
            if (ok) {
                log("✅ Все файлы Classic сохранены!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, ClassicCryptoAndSyncManager.PKG_CLASSIC, log)
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) { loadClassicAsync() }

    val st = classicState
    val _trig = uiTrigger

    // Счётчики 557 сёрферов/скинов и 573 досок/апгрейдов
    val (unlockedSurfersCount, totalSurfersCount) = remember(st, _trig) {
        if (st != null) ClassicCryptoAndSyncManager.getSurfersAndOutfitsCount(st) else (0 to 557)
    }
    val (unlockedBoardsCount, totalBoardsCount) = remember(st, _trig) {
        if (st != null) ClassicCryptoAndSyncManager.getBoardsAndUpgradesCount(st) else (0 to 573)
    }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Кошелёк",
                        "🏆 События & Медали",
                        "🏄 Сёрферы ($unlockedSurfersCount/$totalSurfersCount)",
                        "🛹 Доски ($unlockedBoardsCount/$totalBoardsCount)",
                        "🎁 Сундуки & Ачивки"
                    )
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                classicState?.let { applyUIToState(it) }
                                selectedTab = idx
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp)) {
                Text(text = statusText, fontSize = 12.sp, color = Color(0xFF81C784), maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(modifier = Modifier.weight(0.8f), enabled = undoStack.isNotEmpty() && !isBusy, onClick = { performUndo() }) {
                        Text("↩️ Назад", fontSize = 11.sp)
                    }
                    OutlinedButton(modifier = Modifier.weight(0.9f), enabled = !isBusy, onClick = { loadClassicAsync() }) {
                        Text("🔄 Чтение", fontSize = 11.sp)
                    }
                    Button(modifier = Modifier.weight(1f), enabled = isLoaded && !isBusy, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)), onClick = { saveClassicAsync(false) }) {
                        Text("💾 Сохранить", fontSize = 11.sp)
                    }
                    Button(modifier = Modifier.weight(1.1f), enabled = isLoaded && !isBusy, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), onClick = { saveClassicAsync(true) }) {
                        Text("🚀 В Classic", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (st == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Загрузка Classic...") }
            } else {
                when (selectedTab) {
                    // ВКЛАДКА 0: РЕСУРСЫ И ТОКЕНЫ
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("🪙 Только Двойные Монеты", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Включает x2 монет навсегда без прокачки бустеров.", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(checked = doubleCoinsOnly, onCheckedChange = { enabled ->
                                            pushUndo()
                                            doubleCoinsOnly = enabled
                                            ClassicCryptoAndSyncManager.setDoubleCoinsOnly(st, enabled)
                                            log("🪙 Двойные монеты: $enabled")
                                        })
                                    }

                                    Divider()
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.maxPowerupLevelsAndScoreMultiplier(st)
                                            log("🚀 Способности прокачаны до 6 ур.!")
                                        }) { Text("🚀 Бустеры 6 Ур.", fontSize = 11.sp) }

                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.addOwnedProductsToWallet(
                                                st.wallet,
                                                ClassicDatabase.ONE_TIME_IAP_PRODUCTS.map { it.first }
                                            )
                                            log("🚫 Реклама отключена!")
                                        }) { Text("🚫 Убрать рекламу", fontSize = 11.sp) }
                                    }
                                }
                            }

                            Text("Ресурсы и токены персонажей (Шапки, Бумбоксы, Гитары, НЛО):", fontSize = 12.sp, color = Color.Gray)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(modifier = Modifier.weight(1f), onClick = {
                                    pushUndo()
                                    ClassicDatabase.CURRENCIES.forEach { (id, _) ->
                                        val c = classicCurrencies[id]?.toLongOrNull() ?: 0L
                                        classicCurrencies[id] = (c + 100000L).toString()
                                    }
                                    applyUIToState(st)
                                    log("➕ Ко всем ресурсам добавлено +100 000")
                                }) { Text("+100K ко всем") }

                                Button(modifier = Modifier.weight(1f), onClick = {
                                    pushUndo()
                                    ClassicDatabase.CURRENCIES.forEach { (id, _) -> classicCurrencies[id] = "99999999" }
                                    applyUIToState(st)
                                    log("💎 Все ресурсы выставлены на 99 999 999!")
                                }) { Text("💎 99.9M MAX") }
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

                    // ВКЛАДКА 1: СОБЫТИЯ И МЕДАЛИ
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🛠️ Починка Зависания Вкладки «События»", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.fixEventsTabFreezeInMemory(st)
                                            refreshUIFromState(st)
                                            log("🛠️ Ошибки вкладки событий устранены!")
                                        }
                                    ) { Text("🛠️ ПОЧИНИТЬ ВКЛАДКУ СОБЫТИЙ", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏆 Накрутка Очков в Активные Ивенты", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (activeChallengesList.isEmpty()) {
                                        Text("⚠️ Активных ивентов не найдено.", color = Color.Yellow, fontSize = 13.sp)
                                    } else {
                                        activeChallengesList.forEach { info ->
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = selectedChallengeId == info.id,
                                                    onClick = {
                                                        selectedChallengeId = info.id
                                                        customScoreInput = maxOf(info.maxScoreRequired, info.currentScore).toString()
                                                    }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(info.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text("Сейчас: ${info.currentScore} | Для финала: ${info.maxScoreRequired}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = customScoreInput,
                                            onValueChange = { customScoreInput = it },
                                            label = { Text("Очки / Кубки для выбранного режима") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = unlockStagesCheckbox, onCheckedChange = { unlockStagesCheckbox = it })
                                            Text("Снять рекламу с этапов", fontSize = 12.sp)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(modifier = Modifier.weight(1f), onClick = {
                                                pushUndo()
                                                val score = customScoreInput.toLongOrNull() ?: 1000000L
                                                ClassicCryptoAndSyncManager.setChallengeScoreCustom(st, selectedChallengeId, score, unlockStagesCheckbox)
                                                refreshUIFromState(st)
                                                log("🏆 В ивент '$selectedChallengeId' записано $score очков!")
                                            }) { Text("Применить к этому") }

                                            OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                                pushUndo()
                                                val score = customScoreInput.toLongOrNull() ?: 5000000L
                                                activeChallengesList.forEach { info ->
                                                    ClassicCryptoAndSyncManager.setChallengeScoreCustom(st, info.id, maxOf(info.maxScoreRequired, score), unlockStagesCheckbox)
                                                }
                                                refreshUIFromState(st)
                                                log("🏆 Очки накручены во все активные ивенты!")
                                            }) { Text("Во все ивенты") }
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏅 Редактор Медалей Top Run", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = medalBronze, onValueChange = { medalBronze = it }, label = { Text("🥉 Бронза (106)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = medalSilver, onValueChange = { medalSilver = it }, label = { Text("🥈 Серебро (105)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = medalGold, onValueChange = { medalGold = it }, label = { Text("🥇 Золото (104)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = medalDiamond, onValueChange = { medalDiamond = it }, label = { Text("💎 Алмаз (103)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    }
                                    OutlinedTextField(value = medalChampion, onValueChange = { medalChampion = it }, label = { Text("👑 Чемпион (102)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushUndo()
                                        applyUIToState(st)
                                        log("🏅 Медали Top Run сохранены!")
                                    }) { Text("Применить медали Top Run") }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 2: СЁРФЕРЫ С УМНЫМ ПОИСКОМ ПО ПОДСКИНАМ (557)
                    2 -> {
                        val allMergedSurfers = remember(st, _trig) {
                            ClassicCryptoAndSyncManager.getMergedSurfersList(st)
                        }
                        val filteredSurfers = remember(surferSearchQuery, allMergedSurfers) {
                            allMergedSurfers.filter {
                                it.first.contains(surferSearchQuery, true) ||
                                it.second.first.contains(surferSearchQuery, true) ||
                                it.second.second.any { outfit -> outfit.contains(surferSearchQuery, true) }
                            }
                        }
                        val currentSelectedChar = st.chars.optJSONObject("selected")?.optString("character", "jake") ?: "jake"

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = surferSearchQuery,
                                onValueChange = { surferSearchQuery = it },
                                label = { Text("🔍 Поиск сёрфера или скина (например: dark, punk, jake)...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushUndo()
                                ClassicCryptoAndSyncManager.unlockAllClassicSurfersInMemory(st)
                                uiTrigger++
                                log("🏄 Все сёрферы и их костюмы открыты!")
                            }) { Text("🏄 Открыть всех сёрферов и костюмы (557)") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredSurfers, key = { it.first }) { (id, pair) ->
                                    val (name, outfits) = pair
                                    val isUnlocked = ClassicCryptoAndSyncManager.isSurferUnlocked(st, id)
                                    val ownedOutfits = ClassicCryptoAndSyncManager.getOwnedOutfitsForSurfer(st, id)
                                    val isExpanded = expandedSurferId == id || (surferSearchQuery.isNotBlank() && outfits.any { it.contains(surferSearchQuery, true) })

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        if (isUnlocked) "✅ ОТКРЫТ (Скинов: ${ownedOutfits.size}/${outfits.size})" else "🔒 ЗАКРЫТ",
                                                        color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Switch(checked = isUnlocked, onCheckedChange = { checked ->
                                                        pushUndo()
                                                        ClassicCryptoAndSyncManager.toggleSurferUnlock(st, id, checked, outfits)
                                                        uiTrigger++
                                                    })
                                                    Button(
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        onClick = {
                                                            pushUndo()
                                                            ClassicCryptoAndSyncManager.toggleSurferUnlock(st, id, true, outfits)
                                                            ClassicCryptoAndSyncManager.selectSurfer(st, id, "default")
                                                            uiTrigger++
                                                            log("🏄 Выбран сёрфер: $name")
                                                        }
                                                    ) { Text(if (currentSelectedChar == id) "⭐ Взят" else "Выбрать", fontSize = 11.sp) }
                                                }
                                            }

                                            if (outfits.size > 1) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedSurferId = if (expandedSurferId == id) null else id }
                                                ) {
                                                    Text(if (isExpanded) "🔼 Скрыть подскины" else "🔽 Выбрать подскины (${outfits.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExpanded) {
                                                    outfits.forEach { outfitId ->
                                                        val hasOutfit = ownedOutfits.contains(outfitId)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Checkbox(
                                                                    checked = hasOutfit,
                                                                    onCheckedChange = { checked ->
                                                                        pushUndo()
                                                                        ClassicCryptoAndSyncManager.toggleSurferOutfit(st, id, outfitId, checked)
                                                                        uiTrigger++
                                                                    }
                                                                )
                                                                Text("Скин: $outfitId", fontSize = 12.sp)
                                                            }
                                                            Button(
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                onClick = {
                                                                    pushUndo()
                                                                    ClassicCryptoAndSyncManager.toggleSurferOutfit(st, id, outfitId, true)
                                                                    ClassicCryptoAndSyncManager.selectSurfer(st, id, outfitId)
                                                                    uiTrigger++
                                                                    log("🎭 Надет скин: $outfitId")
                                                                }
                                                            ) { Text("Надеть", fontSize = 10.sp) }
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

                    // ВКЛАДКА 3: ДОСКИ И СПОСОБНОСТИ (573)
                    3 -> {
                        val allMergedBoards = remember(st, _trig) {
                            ClassicCryptoAndSyncManager.getMergedBoardsList(st)
                        }
                        val filteredBoards = remember(boardSearchQuery, allMergedBoards) {
                            allMergedBoards.filter {
                                it.first.contains(boardSearchQuery, true) ||
                                it.second.first.contains(boardSearchQuery, true) ||
                                it.second.second.any { upg -> upg.contains(boardSearchQuery, true) }
                            }
                        }
                        val currentSelectedBoard = st.boards.optString("selected", "default")

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = boardSearchQuery,
                                onValueChange = { boardSearchQuery = it },
                                label = { Text("🔍 Поиск доски или способности (например: double_jump)...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushUndo()
                                ClassicCryptoAndSyncManager.unlockAllClassicBoardsInMemory(st)
                                uiTrigger++
                                log("🛹 Все доски и способности открыты!")
                            }) { Text("🛹 Открыть все доски и способности (573)") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                                items(filteredBoards, key = { it.first }) { (id, pair) ->
                                    val (name, upgrades) = pair
                                    val isUnlocked = ClassicCryptoAndSyncManager.isBoardUnlocked(st, id)
                                    val ownedUpgrades = ClassicCryptoAndSyncManager.getOwnedUpgradesForBoard(st, id)
                                    val isExpanded = expandedBoardId == id || (boardSearchQuery.isNotBlank() && upgrades.any { it.contains(boardSearchQuery, true) })

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        if (isUnlocked) "✅ ОТКРЫТА (Апгрейдов: ${ownedUpgrades.size}/${upgrades.size})" else "🔒 ЗАКРЫТА",
                                                        color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Switch(checked = isUnlocked, onCheckedChange = { checked ->
                                                        pushUndo()
                                                        ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, checked, upgrades)
                                                        uiTrigger++
                                                    })
                                                    Button(
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        onClick = {
                                                            pushUndo()
                                                            ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, true, upgrades)
                                                            ClassicCryptoAndSyncManager.selectBoard(st, id)
                                                            uiTrigger++
                                                            log("🛹 Выбрана доска: $name")
                                                        }
                                                    ) { Text(if (currentSelectedBoard == id) "⭐ Взята" else "Выбрать", fontSize = 11.sp) }
                                                }
                                            }

                                            if (upgrades.isNotEmpty()) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedBoardId = if (expandedBoardId == id) null else id }
                                                ) {
                                                    Text(if (isExpanded) "🔼 Скрыть способности" else "🔽 Выбрать способности (${upgrades.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExpanded) {
                                                    upgrades.forEach { upgId ->
                                                        val hasUpg = ownedUpgrades.contains(upgId)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Checkbox(
                                                                    checked = hasUpg,
                                                                    onCheckedChange = { checked ->
                                                                        pushUndo()
                                                                        ClassicCryptoAndSyncManager.toggleBoardUpgrade(st, id, upgId, checked)
                                                                        uiTrigger++
                                                                    }
                                                                )
                                                                Text("Апгрейд: $upgId", fontSize = 12.sp)
                                                            }
                                                            Button(
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                onClick = {
                                                                    pushUndo()
                                                                    ClassicCryptoAndSyncManager.toggleBoardUpgrade(st, id, upgId, true)
                                                                    uiTrigger++
                                                                    log("⚡ Включен апгрейд: $upgId")
                                                                }
                                                            ) { Text("Включить", fontSize = 10.sp) }
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

                    // ВКЛАДКА 4: СУНДУКИ И ПОЛНАЯ РАЗБЛОКИРОВКА ДОСТИЖЕНИЙ (Браво Старс, Друзья и др.)
                    4 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🌟 Полная Разблокировка Достижений (Achievements)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Разблокирует абсолютно все достижения: добавление медийных персон в друзья, сбор персонажей Brawl Stars, социальные квесты и алмазные бейджи!", fontSize = 12.sp, color = Color.Gray)
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.unlockAllAchievementsCompletely(st)
                                            log("🌟 Все достижения (друзья, Brawl Stars, алмазные бейджи) успешно разблокированы!")
                                        }
                                    ) { Text("🌟 РАЗБЛОКИРОВАТЬ ВСЕ ДОСТИЖЕНИЯ В 1 КЛИК", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎁 Выдача Сундуков и Косметики", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    OutlinedTextField(value = lootboxCount, onValueChange = { lootboxCount = it }, label = { Text("Количество коробок") }, modifier = Modifier.fillMaxWidth())
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushUndo()
                                        val cnt = lootboxCount.toIntOrNull() ?: 10
                                        ClassicCryptoAndSyncManager.addLootboxesToQueueInMemory(st, selectedLootbox, cnt)
                                        log("🎁 Добавлено $cnt сундуков в очередь!")
                                    }) { Text("🎁 Добавить сундуки") }

                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushUndo()
                                        ClassicCryptoAndSyncManager.unlockAllPortraitsFramesAndTrophiesInMemory(st)
                                        log("🖼️ Все портреты, рамки и трофеи открыты!")
                                    }) { Text("🖼️ Открыть Портреты, Рамки и Трофеи") }
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
