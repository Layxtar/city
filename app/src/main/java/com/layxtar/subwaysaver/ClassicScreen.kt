package com.layxtar.subwaysaver

import androidx.activity.ComponentActivity
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

    // Точные ключи медалей Top Run: 106, 105, 104, 103, 102
    var medalBronze by remember { mutableStateOf("0") }
    var medalSilver by remember { mutableStateOf("0") }
    var medalGold by remember { mutableStateOf("0") }
    var medalDiamond by remember { mutableStateOf("0") }
    var medalChampion by remember { mutableStateOf("0") }

    // Активные события (ивенты)
    var activeChallengesList by remember { mutableStateOf<List<ActiveChallengeInfo>>(emptyList()) }
    var selectedChallengeId by remember { mutableStateOf("") }
    var customScoreInput by remember { mutableStateOf("1000000") }
    var unlockStagesCheckbox by remember { mutableStateOf(true) }

    // Поиск и фильтры сёрферов/досок
    var searchQuery by remember { mutableStateOf("") }
    var surferCollectionFilter by remember { mutableStateOf("Все") }
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

        val chList = ClassicCryptoAndSyncManager.getActiveChallenges(st)
        activeChallengesList = chList
        if (chList.isNotEmpty() && (selectedChallengeId.isEmpty() || chList.none { it.id == selectedChallengeId })) {
            selectedChallengeId = chList.first().id
            customScoreInput = maxOf(chList.first().maxScoreRequired, 1000000L).toString()
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
        log("↩️ Шаг назад выполнен в Classic")
    }

    fun loadClassicAsync() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Чтение файлов Classic Subway Surfers...")
            val loaded = withContext(Dispatchers.IO) {
                ClassicCryptoAndSyncManager.loadFullClassicState {}
            }
            classicState = loaded
            undoStack.clear()
            refreshUIFromState(loaded)
            isLoaded = true
            log("✅ Classic загружен (Монет: ${classicCurrencies["1"]}, Ключей: ${classicCurrencies["2"]})!")
            isBusy = false
        }
    }

    fun saveClassicAsync(launchAfterSave: Boolean) {
        val st = classicState ?: return
        applyUIToState(st)
        scope.launch {
            isBusy = true
            log("💾 Сохранение файлов Classic...")
            val ok = withContext(Dispatchers.IO) {
                ClassicCryptoAndSyncManager.saveFullClassicState(st) {}
            }
            if (ok) {
                log("✅ Все изменения Classic успешно сохранены!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, ClassicCryptoAndSyncManager.PKG_CLASSIC, log)
            } else {
                log("❌ Ошибка записи некоторых файлов Classic!")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadClassicAsync()
    }

    val st = classicState
    val _trig = uiTrigger

    // Счётчики открытых персонажей и досок
    val unlockedSurfersCount = if (st != null) ClassicDatabase.CLASSIC_SURFERS.count { ClassicCryptoAndSyncManager.isSurferUnlocked(st, it.first) } else 0
    val unlockedBoardsCount = if (st != null) ClassicDatabase.CLASSIC_BOARDS.count { ClassicCryptoAndSyncManager.isBoardUnlocked(st, it.first) } else 0

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Кошелёк",
                        "🏆 События & Медали",
                        "🏄 Сёрферы ($unlockedSurfersCount/${ClassicDatabase.CLASSIC_SURFERS.size})",
                        "🛹 Доски ($unlockedBoardsCount/${ClassicDatabase.CLASSIC_BOARDS.size})",
                        "🖼️ Рамки & Аватарки",
                        "⭐ Достижения"
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
                    OutlinedButton(
                        modifier = Modifier.weight(0.8f),
                        enabled = undoStack.isNotEmpty() && !isBusy,
                        onClick = { performUndo() }
                    ) { Text("↩️ Назад", fontSize = 11.sp) }

                    OutlinedButton(
                        modifier = Modifier.weight(0.9f),
                        enabled = !isBusy,
                        onClick = { loadClassicAsync() }
                    ) { Text("🔄 Чтение", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = isLoaded && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        onClick = { saveClassicAsync(false) }
                    ) { Text("💾 Сохранить", fontSize = 11.sp) }

                    Button(
                        modifier = Modifier.weight(1.1f),
                        enabled = isLoaded && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = { saveClassicAsync(true) }
                    ) { Text("🚀 В Classic", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (st == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка сохранения Classic...")
                }
            } else {
                when (selectedTab) {
                    // ВКЛАДКА 0: ЧИСТЫЙ КОШЕЛЁК (БЕЗ РЕКЛАМНЫХ И PVP БИЛЕТОВ)
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
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
                                            Text("🪙 Только Двойные Монеты (Double Coins)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Удвоение монет навсегда без прокачки бустеров.", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = doubleCoinsOnly,
                                            onCheckedChange = { enabled ->
                                                pushUndo()
                                                doubleCoinsOnly = enabled
                                                ClassicCryptoAndSyncManager.setDoubleCoinsOnly(st, enabled)
                                                log("🪙 Двойные монеты: $enabled")
                                            }
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.maxPowerupLevelsAndScoreMultiplier(st)
                                            log("🚀 Бустеры прокачаны на 6 ур., множитель очков +5!")
                                        }) { Text("🚀 Бустеры 6 Ур.", fontSize = 11.sp) }

                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.setCurrencyInMemory(st.wallet, "1", 9999999L)
                                            ClassicCryptoAndSyncManager.setCurrencyInMemory(st.wallet, "2", 999999L)
                                            classicCurrencies["1"] = "9999999"
                                            classicCurrencies["2"] = "999999"
                                            log("💎 9.99M Монет и 999K Ключей начислено!")
                                        }) { Text("💎 Монеты + Ключи MAX", fontSize = 11.sp) }
                                    }
                                }
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

                    // ВКЛАДКА 1: ВЫБОР АКТИВНОГО ИВЕНТА, НАКРУТКА КУБКОВ И МЕДАЛИ TOP RUN
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏆 Накрутка Очков / Кубков в Активные Забеги", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Редактор сам определяет, какие события сейчас включены в игре. Выбери режим и укажи желаемое количество очков:", fontSize = 12.sp, color = Color.Gray)

                                    if (activeChallengesList.isEmpty()) {
                                        Text("⚠️ Активных ивентов в generic_challenges.json не обнаружено.", color = Color.Yellow, fontSize = 12.sp)
                                    } else {
                                        activeChallengesList.forEach { info ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = selectedChallengeId == info.id,
                                                    onClick = {
                                                        selectedChallengeId = info.id
                                                        customScoreInput = maxOf(info.maxScoreRequired, info.currentScore).toString()
                                                    }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(info.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                    Text(
                                                        "Текущий счёт: ${info.currentScore} | Для финала: ${info.maxScoreRequired} (${info.targetCity})",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = customScoreInput,
                                            onValueChange = { customScoreInput = it },
                                            label = { Text("Очки / Кубки для выбранного ивента") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = unlockStagesCheckbox, onCheckedChange = { unlockStagesCheckbox = it })
                                            Text("Снять рекламу со сбора наград и кулдауны персонажей", fontSize = 12.sp)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    pushUndo()
                                                    val score = customScoreInput.toLongOrNull() ?: 1000000L
                                                    ClassicCryptoAndSyncManager.setChallengeScoreCustom(
                                                        state = st,
                                                        challengeId = selectedChallengeId,
                                                        targetScore = score,
                                                        unlockStagesReward = unlockStagesCheckbox
                                                    )
                                                    uiTrigger++
                                                    log("🏆 В ивент '$selectedChallengeId' записано $score очков!")
                                                }
                                            ) { Text("Применить в выбранный", fontSize = 11.sp) }

                                            OutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    pushUndo()
                                                    val score = customScoreInput.toLongOrNull() ?: 5000000L
                                                    activeChallengesList.forEach { info ->
                                                        ClassicCryptoAndSyncManager.setChallengeScoreCustom(
                                                            state = st,
                                                            challengeId = info.id,
                                                            targetScore = maxOf(info.maxScoreRequired, score),
                                                            unlockStagesReward = unlockStagesCheckbox
                                                        )
                                                    }
                                                    uiTrigger++
                                                    log("🏆 Очки накручены во ВСЕ активные ивенты!")
                                                }
                                            ) { Text("Накрутить во все", fontSize = 11.sp) }
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
                                    OutlinedTextField(
                                        value = medalChampion,
                                        onValueChange = { medalChampion = it },
                                        label = { Text("👑 Чемпион (102)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            applyUIToState(st)
                                            log("🏅 Медали Top Run сохранены!")
                                        }) { Text("Применить медали") }

                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.fixEventsTabFreezeInMemory(st)
                                            refreshUIFromState(st)
                                            log("🛠️ Ошибки вкладки событий устранены!")
                                        }) { Text("Починить фриз") }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 2: СЁРФЕРЫ (ФИЛЬТРЫ КОЛЛЕКЦИЙ + УМНЫЙ ПОИСК ПО ПОДСКИНАМ)
                    2 -> {
                        val filteredSurfers = remember(searchQuery, surferCollectionFilter, uiTrigger) {
                            ClassicDatabase.CLASSIC_SURFERS.filter { (id, pair) ->
                                val (name, outfits) = pair
                                val matchesSearch = searchQuery.isBlank() ||
                                        id.contains(searchQuery, ignoreCase = true) ||
                                        name.contains(searchQuery, ignoreCase = true) ||
                                        outfits.any { it.contains(searchQuery, ignoreCase = true) }

                                val isUnlocked = ClassicCryptoAndSyncManager.isSurferUnlocked(st, id)
                                val matchesFilter = when (surferCollectionFilter) {
                                    "Открытые" -> isUnlocked
                                    "Закрытые" -> !isUnlocked
                                    "Brawl Stars" -> ClassicDatabase.BRAWL_STARS_SURFERS.contains(id)
                                    "Rivals" -> ClassicDatabase.RIVALS_SURFERS.contains(id)
                                    "Core Crew" -> ClassicDatabase.CORE_CREW_SURFERS.contains(id)
                                    else -> true
                                }
                                matchesSearch && matchesFilter
                            }
                        }
                        val currentSelectedChar = st.chars.optJSONObject("selected")?.optString("character", "jake") ?: "jake"

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("🔍 Поиск по имени сёрфера или подскину (напр. zombie, robo, catrina)...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Все", "Открытые", "Закрытые", "Brawl Stars", "Rivals", "Core Crew").forEach { filterName ->
                                    FilterChip(
                                        selected = surferCollectionFilter == filterName,
                                        onClick = { surferCollectionFilter = filterName },
                                        label = { Text(filterName, fontSize = 12.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushUndo()
                                ClassicCryptoAndSyncManager.unlockAllClassicSurfersInMemory(st)
                                uiTrigger++
                                log("🏄 Все 275 сёрферов и костюмы открыты!")
                            }) { Text("🏄 Открыть всех сёрферов и все костюмы") }
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
                                    val isOutfitMatch = searchQuery.isNotBlank() && outfits.any { it.contains(searchQuery, ignoreCase = true) }
                                    val isExpanded = expandedSurferId == id || isOutfitMatch

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        text = if (isUnlocked) "✅ ОТКРЫТ (Костюмов: ${ownedOutfits.size}/${outfits.size})" else "🔒 ЗАКРЫТ",
                                                        color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Switch(
                                                        checked = isUnlocked,
                                                        onCheckedChange = { checked ->
                                                            pushUndo()
                                                            ClassicCryptoAndSyncManager.toggleSurferUnlock(st, id, checked, outfits)
                                                            uiTrigger++
                                                        }
                                                    )
                                                    Button(
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        onClick = {
                                                            pushUndo()
                                                            ClassicCryptoAndSyncManager.toggleSurferUnlock(st, id, true, outfits)
                                                            ClassicCryptoAndSyncManager.selectSurfer(st, id, "default")
                                                            uiTrigger++
                                                            log("🏄 Выбран сёрфер: $name")
                                                        }
                                                    ) {
                                                        Text(if (currentSelectedChar == id) "⭐ Взят" else "Выбрать", fontSize = 11.sp)
                                                    }
                                                }
                                            }

                                            if (outfits.size > 1) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedSurferId = if (isExpanded) null else id }
                                                ) {
                                                    Text(if (isExpanded) "🔼 Скрыть подскины" else "🔽 Подскины (${outfits.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExpanded) {
                                                    outfits.forEach { outfitId ->
                                                        val hasOutfit = ownedOutfits.contains(outfitId)
                                                        val isHighlight = searchQuery.isNotBlank() && outfitId.contains(searchQuery, ignoreCase = true)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = if (isHighlight) Modifier.background(Color(0xFF33691E)) else Modifier
                                                        ) {
                                                            Checkbox(
                                                                checked = hasOutfit,
                                                                onCheckedChange = { checked ->
                                                                    pushUndo()
                                                                    ClassicCryptoAndSyncManager.toggleSurferOutfit(st, id, outfitId, checked)
                                                                    uiTrigger++
                                                                }
                                                            )
                                                            Text("Костюм: $outfitId", fontSize = 12.sp, fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal)
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

                    // ВКЛАДКА 3: ДОСКИ
                    3 -> {
                        val filteredBoards = remember(searchQuery, uiTrigger) {
                            ClassicDatabase.CLASSIC_BOARDS.filter { (id, pair) ->
                                id.contains(searchQuery, ignoreCase = true) ||
                                pair.first.contains(searchQuery, ignoreCase = true) ||
                                pair.second.any { it.contains(searchQuery, ignoreCase = true) }
                            }
                        }
                        val currentSelectedBoard = st.boards.optString("selected", "default")

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("🔍 Поиск доски или способности...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushUndo()
                                ClassicCryptoAndSyncManager.unlockAllClassicBoardsInMemory(st)
                                uiTrigger++
                                log("🛹 Все доски и способности открыты!")
                            }) { Text("🛹 Открыть все доски и способности") }
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredBoards, key = { it.first }) { (id, pair) ->
                                    val (name, upgrades) = pair
                                    val isUnlocked = ClassicCryptoAndSyncManager.isBoardUnlocked(st, id)
                                    val ownedUpgrades = ClassicCryptoAndSyncManager.getOwnedUpgradesForBoard(st, id)
                                    val isExpanded = expandedBoardId == id

                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        text = if (isUnlocked) "✅ ОТКРЫТА (Апгрейдов: ${ownedUpgrades.size}/${upgrades.size})" else "🔒 ЗАКРЫТА",
                                                        color = if (isUnlocked) Color(0xFF81C784) else Color(0xFFE57373),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Switch(
                                                        checked = isUnlocked,
                                                        onCheckedChange = { checked ->
                                                            pushUndo()
                                                            ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, checked, upgrades)
                                                            uiTrigger++
                                                        }
                                                    )
                                                    Button(
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        onClick = {
                                                            pushUndo()
                                                            ClassicCryptoAndSyncManager.toggleBoardUnlock(st, id, true, upgrades)
                                                            ClassicCryptoAndSyncManager.selectBoard(st, id)
                                                            uiTrigger++
                                                            log("🛹 Выбрана доска: $name")
                                                        }
                                                    ) {
                                                        Text(if (currentSelectedBoard == id) "⭐ Взята" else "Выбрать", fontSize = 11.sp)
                                                    }
                                                }
                                            }

                                            if (upgrades.isNotEmpty()) {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { expandedBoardId = if (isExpanded) null else id }
                                                ) {
                                                    Text(if (isExpanded) "🔼 Скрыть способности" else "🔽 Способности (${upgrades.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExpanded) {
                                                    upgrades.forEach { upgId ->
                                                        val hasUpg = ownedUpgrades.contains(upgId)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Checkbox(
                                                                checked = hasUpg,
                                                                onCheckedChange = { checked ->
                                                                    pushUndo()
                                                                    ClassicCryptoAndSyncManager.toggleBoardUpgrade(st, id, upgId, checked)
                                                                    uiTrigger++
                                                                }
                                                            )
                                                            Text("Способность: $upgId", fontSize = 12.sp)
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

                    // ВКЛАДКА 4: РАМКИ И АВАТАРКИ ПО ВЫБОРУ (С КНОПКАМИ ЭКИПИРОВКИ)
                    4 -> {
                        val currentFrame = st.frames.optString("selected", "default_frame")
                        val currentPortrait = st.portraits.optString("selected", "portrait_jake_default")

                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🖼️ Рамки Профиля (Выбор & Разблокировка)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    ClassicDatabase.ALL_FRAMES.forEach { (frameId, frameTitle) ->
                                        val isUnlocked = ClassicCryptoAndSyncManager.isFrameUnlocked(st, frameId)
                                        val isEquipped = currentFrame == frameId

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Checkbox(
                                                    checked = isUnlocked,
                                                    onCheckedChange = { checked ->
                                                        pushUndo()
                                                        ClassicCryptoAndSyncManager.toggleFrameUnlock(st, frameId, checked)
                                                        uiTrigger++
                                                    }
                                                )
                                                Text(frameTitle, fontSize = 13.sp)
                                            }
                                            OutlinedButton(
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                onClick = {
                                                    pushUndo()
                                                    ClassicCryptoAndSyncManager.toggleFrameUnlock(st, frameId, true)
                                                    ClassicCryptoAndSyncManager.selectFrame(st, frameId)
                                                    uiTrigger++
                                                    log("🖼️ Выбрана рамка: $frameTitle")
                                                }
                                            ) {
                                                Text(if (isEquipped) "⭐ Надет" else "Надеть", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👤 Портреты / Аватарки (Выбор & Разблокировка)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    ClassicDatabase.ALL_PORTRAITS.forEach { (portraitId, portraitTitle) ->
                                        val isUnlocked = ClassicCryptoAndSyncManager.isPortraitUnlocked(st, portraitId)
                                        val isEquipped = currentPortrait == portraitId

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Checkbox(
                                                    checked = isUnlocked,
                                                    onCheckedChange = { checked ->
                                                        pushUndo()
                                                        ClassicCryptoAndSyncManager.togglePortraitUnlock(st, portraitId, checked)
                                                        uiTrigger++
                                                    }
                                                )
                                                Text(portraitTitle, fontSize = 13.sp)
                                            }
                                            OutlinedButton(
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                onClick = {
                                                    pushUndo()
                                                    ClassicCryptoAndSyncManager.togglePortraitUnlock(st, portraitId, true)
                                                    ClassicCryptoAndSyncManager.selectPortrait(st, portraitId)
                                                    uiTrigger++
                                                    log("👤 Выбран портрет: $portraitTitle")
                                                }
                                            ) {
                                                Text(if (isEquipped) "⭐ Надет" else "Надеть", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 5: ДОСТИЖЕНИЯ (BRAWL STARS, ДРУЗЬЯ И ТРОФЕИ)
                    5 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⭐ Эксклюзивные Достижения", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Выполняет все скрытые и медийные ачивки (друг звезды, сбор состава Brawl Stars, Core Crew и т.д.):", fontSize = 12.sp, color = Color.Gray)

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.unlockAllAchievementsInMemory(st)
                                            log("⭐ Все достижения (включая медийного друга и Brawl Stars) выполнены на максимум!")
                                        }
                                    ) { Text("⭐ ВЫПОЛНИТЬ ВСЕ ДОСТИЖЕНИЯ (МАКСИМУМ)", color = Color.White, fontWeight = FontWeight.Bold) }

                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    ClassicDatabase.ACHIEVEMENTS_LIST.forEach { ach ->
                                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text(ach.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(ach.description, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎁 Выдача Сундуков (Lootboxes)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    OutlinedTextField(
                                        value = lootboxCount,
                                        onValueChange = { lootboxCount = it },
                                        label = { Text("Количество коробок") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    ClassicDatabase.LOOTBOX_TYPES.forEach { (bId, bTitle) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selectedLootbox == bId, onClick = { selectedLootbox = bId })
                                            Text(bTitle, fontSize = 13.sp)
                                        }
                                    }
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushUndo()
                                        val cnt = lootboxCount.toIntOrNull() ?: 10
                                        ClassicCryptoAndSyncManager.addLootboxesToQueueInMemory(st, selectedLootbox, cnt)
                                        log("🎁 Добавлено $cnt сундуков в очередь!")
                                    }) { Text("🎁 Добавить сундуки") }
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
