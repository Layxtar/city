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

    // Точные медали Top Run: 106=Бронза(55), 105=Серебро(98), 104=Золото(63), 103=Алмаз(16), 102=Чемпион
    var medalBronze by remember { mutableStateOf("0") }
    var medalSilver by remember { mutableStateOf("0") }
    var medalGold by remember { mutableStateOf("0") }
    var medalDiamond by remember { mutableStateOf("0") }
    var medalChampion by remember { mutableStateOf("0") }

    var searchQuery by remember { mutableStateOf("") }
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
        log("↩️ Шаг назад выполнен в Classic (осталось: ${undoStack.size})")
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
            log("✅ Профиль Classic загружен (Монет: ${classicCurrencies["1"]}, Ключей: ${classicCurrencies["2"]})!")
            isBusy = false
        }
    }

    fun saveClassicAsync(launchAfterSave: Boolean) {
        val st = classicState ?: return
        applyUIToState(st)
        scope.launch {
            isBusy = true
            log("💾 Шифрование и запись файлов Classic...")
            val ok = withContext(Dispatchers.IO) {
                ClassicCryptoAndSyncManager.saveFullClassicState(st) {}
            }
            if (ok) {
                log("✅ Все изменения Classic сохранены в игру!")
                if (launchAfterSave) CryptoAndSyncManager.launchGame(activity, ClassicCryptoAndSyncManager.PKG_CLASSIC, log)
            } else {
                log("⚠️ Часть файлов Classic не удалось записать.")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadClassicAsync()
    }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "💰 Кошелёк & Билеты",
                        "🏆 Медали & События",
                        "🏄 Сёрферы (${ClassicDatabase.CLASSIC_SURFERS.size})",
                        "🛹 Доски (${ClassicDatabase.CLASSIC_BOARDS.size})",
                        "🎁 Сундуки & Профиль"
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
        val st = classicState
        val _trig = uiTrigger
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (st == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка сохранения Classic...")
                }
            } else {
                when (selectedTab) {
                    // ВКЛАДКА 0: КОШЕЛЁК, ДВОЙНЫЕ МОНЕТЫ И ДОНАТНЫЕ ПАКЕТЫ БИЛЕТОВ
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
                                            Text("🪙 Только Двойные Монеты (Double Coins x2)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Включает удвоение монет навсегда БЕЗ прокачки бустеров.", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = doubleCoinsOnly,
                                            onCheckedChange = { enabled ->
                                                pushUndo()
                                                doubleCoinsOnly = enabled
                                                ClassicCryptoAndSyncManager.setDoubleCoinsOnly(st, enabled)
                                                log(if (enabled) "🪙 Двойные монеты включены!" else "🪙 Двойные монеты выключены.")
                                            }
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.maxPowerupLevelsAndScoreMultiplier(st)
                                            log("🚀 Способности (Джетпак, Кроссовки, Магнит, x2) прокачаны до 6 ур.!")
                                        }) { Text("🚀 Бустеры 6 Ур.", fontSize = 11.sp) }

                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.addOwnedProductsToWallet(
                                                st.wallet,
                                                ClassicDatabase.ONE_TIME_IAP_PRODUCTS.map { it.first }
                                            )
                                            log("🚫 Реклама отключена, весь донат добавлен!")
                                        }) { Text("🚫 Убрать рекламу", fontSize = 11.sp) }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎬 Донатные Пакеты Билетов Пропуска Рекламы (SkipAdTickets)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Добавляет выбранное количество билетов (как при покупке офферов 10 / 40 / 60 / 250 шт.) вместе с регистрацией времени покупки в профиле:", fontSize = 12.sp, color = Color.Gray)

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(10L, 40L, 60L, 250L).forEach { packAmt ->
                                            Button(
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 6.dp),
                                                onClick = {
                                                    pushUndo()
                                                    applyUIToState(st)
                                                    ClassicCryptoAndSyncManager.addSkipAdTicketsBundle(st, packAmt)
                                                    refreshUIFromState(st)
                                                    log("🎬 Добавлен донатный пакет +$packAmt Билетов пропуска рекламы!")
                                                }
                                            ) { Text("+$packAmt", fontSize = 12.sp) }
                                        }
                                    }

                                    Divider()
                                    Text("🏁 Донатные Пакеты Билетов Забегов / Событий (Showdown & Event):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(25L, 100L, 500L, 2500L).forEach { raceAmt ->
                                            OutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 6.dp),
                                                onClick = {
                                                    pushUndo()
                                                    applyUIToState(st)
                                                    ClassicCryptoAndSyncManager.addShowdownRaceTicketsBundle(st, raceAmt)
                                                    refreshUIFromState(st)
                                                    log("🏁 Добавлено +$raceAmt билетов и жетонов событий + сброшены кулдауны!")
                                                }
                                            ) { Text("+$raceAmt", fontSize = 12.sp) }
                                        }
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

                    // ВКЛАДКА 1: ПОЧИНКА ВКЛАДКИ СОБЫТИЙ, МЕДАЛИ TOP RUN (106..102) И СЕЗОННАЯ ОХОТА
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🛠️ Починка Зависания Вкладки «События» (Events)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Если после прошлой кнопки игра зависает при нажатии на вкладку «События» — нажми эту кнопку! Она удалит конфликтующие флаги из `season_hunt.json` и `generic_challenges.json`, а также сбросит сломанное число 2147483647 у медали Чемпиона.", fontSize = 12.sp, color = Color.Gray)

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.fixEventsTabFreezeInMemory(st)
                                            refreshUIFromState(st)
                                            log("🛠️ Вкладка «События» починена, медаль Чемпиона сброшена на 10! Нажми 💾 Сохранить.")
                                        }
                                    ) { Text("🛠️ ПОЧИНИТЬ ВКЛАДКУ «СОБЫТИЯ» И СБРОСИТЬ 2147483647", color = Color.White, fontWeight = FontWeight.Bold) }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🏅 Точный Редактор Медалей Top Run", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Все 5 медалей строго привязаны к твоим ID из сохранения (Бронза=106, Серебро=105, Золото=104, Алмаз=103, Чемпион=102):", fontSize = 12.sp, color = Color.Gray)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = medalBronze,
                                            onValueChange = { medalBronze = it },
                                            label = { Text("🥉 Бронза (ID 106)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = medalSilver,
                                            onValueChange = { medalSilver = it },
                                            label = { Text("🥈 Серебро (ID 105)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = medalGold,
                                            onValueChange = { medalGold = it },
                                            label = { Text("🥇 Золото (ID 104)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = medalDiamond,
                                            onValueChange = { medalDiamond = it },
                                            label = { Text("💎 Алмаз (ID 103)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    OutlinedTextField(
                                        value = medalChampion,
                                        onValueChange = { medalChampion = it },
                                        label = { Text("👑 Чемпион (ID 102 — вместо 2147483647)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            applyUIToState(st)
                                            log("🏅 Медали Top Run (Чемпион = $medalChampion) сохранены в память!")
                                        }) { Text("Применить медали") }

                                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                            pushUndo()
                                            medalBronze = "55"; medalSilver = "98"; medalGold = "63"; medalDiamond = "16"; medalChampion = "25"
                                            applyUIToState(st)
                                            log("🏅 Выставлены твои честные медали + 25 медалей Чемпиона!")
                                        }) { Text("Мои + 25 Чемпиона") }
                                    }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🎃 Безопасная Прокачка Сезонной Охоты (Surfer Pass)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Начисляет 50 000 Жетонов Охоты и открывает Surfer Pass Plus без ручной ломки массива тиров (вкладка События не зависнет!).", fontSize = 12.sp, color = Color.Gray)
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        onClick = {
                                            pushUndo()
                                            ClassicCryptoAndSyncManager.completeAllEventsAndSeasonPassSafely(st)
                                            refreshUIFromState(st)
                                            log("🎃 Начислено 50 000 жетонов Сезонной Охоты + открыт Surfer Pass Plus!")
                                        }
                                    ) { Text("🎃 Начислить 50 000 Жетонов Охоты + Surfer Pass") }
                                }
                            }
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }

                    // ВКЛАДКА 2: СЁРФЕРЫ CLASSIC
                    2 -> {
                        val filteredSurfers = remember(searchQuery, uiTrigger) {
                            ClassicDatabase.CLASSIC_SURFERS.filter {
                                it.first.contains(searchQuery, ignoreCase = true) ||
                                it.second.first.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        val currentSelectedChar = st.chars.optJSONObject("selected")?.optString("character", "jake") ?: "jake"

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("🔍 Поиск сёрфера (${filteredSurfers.size} из ${ClassicDatabase.CLASSIC_SURFERS.size})...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushUndo()
                                ClassicCryptoAndSyncManager.unlockAllClassicSurfersInMemory(st)
                                uiTrigger++
                                log("🏄 Все 275 сёрферов и костюмы открыты!")
                            }) { Text("🏄 Открыть всех 275 сёрферов и костюмы") }
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
                                    val isExpanded = expandedSurferId == id

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
                                                    Text(if (isExpanded) "🔼 Скрыть костюмы" else "🔽 Костюмы (${outfits.size} шт.)", fontSize = 11.sp)
                                                }
                                                if (isExpanded) {
                                                    outfits.forEach { outfitId ->
                                                        val hasOutfit = ownedOutfits.contains(outfitId)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Checkbox(
                                                                checked = hasOutfit,
                                                                onCheckedChange = { checked ->
                                                                    pushUndo()
                                                                    ClassicCryptoAndSyncManager.toggleSurferOutfit(st, id, outfitId, checked)
                                                                    uiTrigger++
                                                                }
                                                            )
                                                            Text("Костюм: $outfitId", fontSize = 12.sp)
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

                    // ВКЛАДКА 3: ДОСКИ CLASSIC
                    3 -> {
                        val filteredBoards = remember(searchQuery, uiTrigger) {
                            ClassicDatabase.CLASSIC_BOARDS.filter {
                                it.first.contains(searchQuery, ignoreCase = true) ||
                                it.second.first.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        val currentSelectedBoard = st.boards.optString("selected", "default")

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("🔍 Поиск ховерборда (${filteredBoards.size} из ${ClassicDatabase.CLASSIC_BOARDS.size})...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                pushUndo()
                                ClassicCryptoAndSyncManager.unlockAllClassicBoardsInMemory(st)
                                uiTrigger++
                                log("🛹 Все 297 досок и способности открыты!")
                            }) { Text("🛹 Открыть все 297 досок и способности") }
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

                    // ВКЛАДКА 4: СУНДУКИ И КОСМЕТИКА
                    4 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎁 Выдача Сундуков (Lootboxes) в очередь", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    OutlinedTextField(
                                        value = lootboxCount,
                                        onValueChange = { lootboxCount = it },
                                        label = { Text("Количество коробок для выдачи") },
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
                                        log("🎁 Добавлено $cnt сундуков в очередь (нажми 💾 Сохранить)!")
                                    }) { Text("🎁 Добавить сундуки в очередь") }
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🖼️ Портреты, Рамки и Трофеи", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                        pushUndo()
                                        ClassicCryptoAndSyncManager.unlockAllPortraitsFramesAndTrophiesInMemory(st)
                                        log("🖼️ Все 114 портретов, 56 рамок и трофеи разблокированы!")
                                    }) { Text("🖼️ Открыть 114 Портретов, 56 Рамок и Трофеи") }
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
