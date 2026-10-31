package com.layxtar.subwaysaver

import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun Pvz2Screen(
    activity: ComponentActivity,
    statusText: String,
    logs: String,
    log: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }

    var activeCdnDir by remember {
        mutableStateOf("/storage/emulated/0/Android/data/com.ea.game.pvz2_row/files/No_Backup/CDN.13.5")
    }
    var detectedCdnList by remember { mutableStateOf<List<String>>(emptyList()) }
    var ppDatJson by remember { mutableStateOf<JSONObject?>(null) }

    val currencies = remember {
        mutableStateMapOf<String, String>().apply {
            Pvz2Database.CURRENCIES.forEach { (k, _) -> put(k, "0") }
        }
    }
    var targetPlantLevel by remember { mutableStateOf("20") }
    var targetMastery by remember { mutableStateOf("200") }
    var pinataMultiplier by remember { mutableStateOf("100") }
    var pinataMinSeeds by remember { mutableStateOf("1000") }
    var useNegativeSunBonus by remember { mutableStateOf(true) }
    var translatePlantNames by remember { mutableStateOf(true) }

    fun loadPvz2Async() {
        if (!CryptoAndSyncManager.hasPermission()) {
            CryptoAndSyncManager.requestPermissionOrOpenShizuku(activity, log)
            return
        }
        scope.launch {
            isBusy = true
            log("⏳ Поиск актуальной папки кэша CDN и чтение pp.dat...")
            val (cdn, allCdns, pp) = withContext(Dispatchers.IO) {
                val active = Pvz2Manager.resolveActiveCdnDir {}
                val list = Pvz2Manager.listAllCdnDirs()
                val ppObj = Pvz2Manager.loadPlayerSavePpDat {}
                Triple(active, list, ppObj)
            }
            activeCdnDir = cdn
            detectedCdnList = allCdns
            ppDatJson = pp
            if (pp != null) {
                val curMap = Pvz2Manager.readPpDatCurrencies(pp)
                for ((k, v) in curMap) currencies[k] = v.toString()
                log("🌻 PvZ 2 успешно прочитан! Активный кэш: ${cdn.substringAfterLast("/")}")
            } else {
                log("🌻 Папка кэша обнаружена: ${cdn.substringAfterLast("/")}")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) {
        loadPvz2Async()
    }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "🛒 Мега-Магазин (1🪙)",
                        "🇷🇺 Русификатор",
                        "🌞 -500 Солнца & Моды",
                        "💰 Профиль pp.dat",
                        "📂 Папка CDN & Откат"
                    )
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
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isBusy,
                        onClick = { loadPvz2Async() }
                    ) { Text("🔄 Обновить CDN", fontSize = 12.sp) }

                    Button(
                        modifier = Modifier.weight(1.2f),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        onClick = {
                            val (pkg, _) = Pvz2Manager.resolveNoBackupDir {}
                            CryptoAndSyncManager.launchGame(activity, pkg, log)
                        }
                    ) { Text("🚀 Запустить PvZ 2", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                // ВКЛАДКА 0: МЕГА-МАГАЗИН
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛒 Полный Магазин за 1 Монету + Семена + Большие Валюты + Удаление", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Собирает магазин в `${activeCdnDir.substringAfterLast("/")}`:\n" +
                                    "• Все дефолтные товары игры сохранены и стоят 1 монету.\n" +
                                    "• Вкладка Resources: после обычных пакетов добавлены огромные значения (до 1 Миллиарда и MAX 2.14B) для всех валют (Монеты, Алмазы, Конфеты/Мяты, Перчатки, Топливо, Ростки, Энергия Ивентов, Короны, Ключи и Жетоны).\n" +
                                    "• Вкладка Plants & Costumes: все 217+ растений (включая Baneberry, Belfright, Cactalog, Pyroak, Thornwhip, Volt Snapdragon) и все 342 скина.\n" +
                                    "• Вкладки Seeds / Seeds2 / Seeds3: индивидуальные пакеты семян для каждого растения.\n" +
                                    "• Вкладка DeleteItems: отрицательные паки (-1K .. -1B) для списания лишних валют.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                Pvz2Manager.installFullMegaShopAllPlantsSkinsAndSeeds(activeCdnDir, log)
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) {
                                    Text("🛒 УСТАНОВИТЬ ПОЛНЫЙ МАГАЗИН (ВСЕ ВКЛАДКИ + БОЛЬШИЕ ВАЛЮТЫ)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("🔥 Новейшие растения во вкладках Plants и Seeds2:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF81C784))
                                Pvz2Database.NEWEST_PLANTS_HIGHLIGHT.forEach { (id, title) ->
                                    Text("• $title (ID: $id)", fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 1: РУСИФИКАТОР PVZ 2
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🇷🇺 Русификатор Plants vs. Zombies 2 (LawnStrings-en-us.rton)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Прошивает русский перевод главного меню, кнопок боя, Альманаха, магазина, Арены, миров и всех 217+ растений (включая новейшие Baneberry / Воронец, Belfright / Колокол Ужаса, Cactalog / Кактусовое Бревно, Scorchoak / Огненный Дуб и др.) прямо в активный файл `LawnStrings-en-us.rton` без потери новых строк обновления!",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = translatePlantNames,
                                        onCheckedChange = { translatePlantNames = it }
                                    )
                                    Text("Переводить названия растений на русский язык", fontSize = 13.sp)
                                }

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                Pvz2RusLocalizer.installRussianLocalization(
                                                    cdnDir = activeCdnDir,
                                                    translatePlantNames = translatePlantNames,
                                                    log = log
                                                )
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) {
                                    Text("🇷🇺 УСТАНОВИТЬ РУСИФИКАТОР PVZ 2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    "💡 Совет: Если у тебя также есть внешний полный файл перевода `LawnStrings-ru.json`, ты можешь положить его в `/storage/emulated/0/Download/LawnStrings-ru.json` — при нажатии на кнопку выше программа автоматически объединит его со встроенным переводом и зашьёт в игру!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 2: -500 СОЛНЦА И МОДЫ
                2 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🌞 Моментальная Перезарядка (-999) + Отрицательная Цена Солнца", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Прописывает кулдаун `-999` (мгновенный откат слота!) во все файлы `PlantLevels.rton` и `PlantProperties.rton` во всех папках CDN.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = useNegativeSunBonus,
                                        onCheckedChange = { useNegativeSunBonus = it }
                                    )
                                    Text(
                                        text = if (useNegativeSunBonus) "Отрицательная цена (-500☀️: посадка ДАЁТ +500 солнца!)" else "Обычная цена 0☀️",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                Pvz2Manager.patchZeroSunAndInstantCooldown(activeCdnDir, useNegativeSunBonus, log)
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (useNegativeSunBonus) "🌞 ВКЛЮЧИТЬ -500 СОЛНЦА + МГНОВЕННЫЙ ОТКАТ (-999)" else "🌞 ВКЛЮЧИТЬ 0 СОЛНЦА + МГНОВЕННЫЙ ОТКАТ (-999)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🪅 Умножитель Лута Пиньят (pinatacoreloot.rton)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = pinataMultiplier,
                                        onValueChange = { pinataMultiplier = it },
                                        label = { Text("Множитель") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = pinataMinSeeds,
                                        onValueChange = { pinataMinSeeds = it },
                                        label = { Text("Мин. семян") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    scope.launch {
                                        isBusy = true
                                        val mult = pinataMultiplier.toIntOrNull() ?: 100
                                        val minS = pinataMinSeeds.toIntOrNull() ?: 1000
                                        withContext(Dispatchers.IO) {
                                            Pvz2Manager.patchPinataCoreLoot(activeCdnDir, mult, minS, log)
                                        }
                                        isBusy = false
                                    }
                                }) { Text("🪅 Увеличить Лут Пиньят") }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🏆 Мега-Награды Арены (x100) и Календаря", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    scope.launch {
                                        isBusy = true
                                        withContext(Dispatchers.IO) { Pvz2Manager.patchArenaAndCalendarMegaRewards(activeCdnDir, log) }
                                        isBusy = false
                                    }
                                }) { Text("🏆 Включить Мега-Награды Арены") }
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 3: ПРОФИЛЬ PP.DAT
                3 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Прямой редактор сохранения `pp.dat` (все 217 растений):", fontSize = 12.sp, color = Color.Gray)

                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            Pvz2Database.CURRENCIES.forEach { (k, _) -> currencies[k] = "99999999" }
                            log("💎 Все валюты выставлены на 99 999 999")
                        }) { Text("💎 Все валюты на 99.9M") }

                        Pvz2Database.CURRENCIES.forEach { (k, label) ->
                            OutlinedTextField(
                                value = currencies[k] ?: "0",
                                onValueChange = { currencies[k] = it },
                                label = { Text(label) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🌿 Открыть все 217 растений напрямую в pp.dat", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = targetPlantLevel,
                                        onValueChange = { targetPlantLevel = it },
                                        label = { Text("Уровень (1-20)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = targetMastery,
                                        onValueChange = { targetMastery = it },
                                        label = { Text("Mastery (0-200)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    val pp = ppDatJson
                                    if (pp == null) {
                                        log("❌ pp.dat не загружен!")
                                        return@Button
                                    }
                                    scope.launch {
                                        isBusy = true
                                        val map = currencies.mapValues { it.value.toLongOrNull() ?: 0L }
                                        val lvl = targetPlantLevel.toIntOrNull() ?: 20
                                        val mst = targetMastery.toIntOrNull() ?: 200
                                        withContext(Dispatchers.IO) {
                                            Pvz2Manager.updatePpDatAndUnlockPlants(pp, map, true, lvl, mst, log)
                                        }
                                        isBusy = false
                                    }
                                }) { Text("💾 Сохранить Валюты и 217 Растений в pp.dat") }
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 4: ПАПКА CDN И ОТКАТ
                4 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📂 Путь к кэшу CDN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedTextField(
                                    value = activeCdnDir,
                                    onValueChange = { activeCdnDir = it },
                                    label = { Text("Путь к папке CDN") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (detectedCdnList.isNotEmpty()) {
                                    Text("Обнаруженные CDN:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    detectedCdnList.forEach { dirPath ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = activeCdnDir == dirPath, onClick = { activeCdnDir = dirPath })
                                            Text(dirPath.substringAfterLast("/"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Divider()
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    scope.launch {
                                        isBusy = true
                                        withContext(Dispatchers.IO) { Pvz2Manager.restoreAllCdnBackups(activeCdnDir, log) }
                                        isBusy = false
                                    }
                                }) { Text("⏪ Восстановить оригиналы .rton из бэкапов") }
                            }
                        }

                        Text("📜 Журнал:", fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black).padding(8.dp)) {
                            Text(text = logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }
}
