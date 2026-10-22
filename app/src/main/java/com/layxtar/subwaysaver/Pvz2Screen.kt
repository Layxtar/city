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
    var plantSearch by remember { mutableStateOf("") }
    var selectedPlantId by remember { mutableStateOf(Pvz2Database.PLANTS.first().first) }
    var selectedSeedTierAmount by remember { mutableIntStateOf(1000000) }
    var customSeedInput by remember { mutableStateOf("1000000") }
    var unlockAllPlantsInShop by remember { mutableStateOf(true) }
    var targetPlantLevel by remember { mutableStateOf("20") }
    var targetMastery by remember { mutableStateOf("200") }
    var pinataMultiplier by remember { mutableStateOf("100") }
    var pinataMinSeeds by remember { mutableStateOf("1000") }

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
                log("🌻 PvZ 2 успешно прочитан! Папка: ${cdn.substringAfterLast("/")}")
            } else {
                log("🌻 Папка кэша обнаружена: ${cdn.substringAfterLast("/")}")
            }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) { loadPvz2Async() }

    Scaffold(
        topBar = {
            Column {
                if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    val tabs = listOf(
                        "🛒 Магазин за 1🪙",
                        "🌱 Все Семена (1..1M)",
                        "🪅 Пиньяты & Моды",
                        "💰 Профиль pp.dat",
                        "📂 Папка CDN"
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
                    OutlinedButton(modifier = Modifier.weight(1f), enabled = !isBusy, onClick = { loadPvz2Async() }) {
                        Text("🔄 Обновить CDN", fontSize = 12.sp)
                    }
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
                // ВКЛАДКА 0: МАГАЗИН ЗА 1 МОНЕТУ
                0 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛒 Всё по 1 Монете в Магазине PvZ 2", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Насильно внедряет на витрину все растения, 342 костюма и пакеты семян через связку products + market_schedule + market_layout + magento_filter за 1 монету!", fontSize = 12.sp, color = Color.Gray)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = unlockAllPlantsInShop, onCheckedChange = { unlockAllPlantsInShop = it })
                                    Text("Добавить в магазин абсолютно все растения, скины и семена")
                                }

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            withContext(Dispatchers.IO) {
                                                Pvz2Manager.patchShopAllFor1CoinAndInjectSeeds(
                                                    cdnDir = activeCdnDir,
                                                    selectedPlantId = selectedPlantId,
                                                    seedAmount = 250,
                                                    unlockAllPlantsAndSkinsInShop = unlockAllPlantsInShop,
                                                    log = log
                                                )
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("🛒 СДЕЛАТЬ ВЕСЬ МАГАЗИН ПО 1 МОНЕТЕ", color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 1: ГЕНЕРАТОР СЕМЯН
                1 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🌱 Генератор Семян (От 1 до 1 000 000 шт.)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Выбери любое растение (включая Scorchoak / Pyroak), количество семян и внедри в магазин за 1 монету:", fontSize = 12.sp, color = Color.Gray)

                                Pvz2Database.SEED_PACKET_TIERS.forEach { (amount, desc) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = selectedSeedTierAmount == amount, onClick = {
                                            selectedSeedTierAmount = amount
                                            customSeedInput = amount.toString()
                                        })
                                        Text(desc, fontSize = 13.sp)
                                    }
                                }

                                OutlinedTextField(
                                    value = customSeedInput,
                                    onValueChange = { customSeedInput = it },
                                    label = { Text("Или впиши любое количество семян") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                    onClick = {
                                        scope.launch {
                                            isBusy = true
                                            val seeds = (customSeedInput.toIntOrNull() ?: selectedSeedTierAmount).coerceIn(1, 1000000)
                                            withContext(Dispatchers.IO) {
                                                Pvz2Manager.patchShopAllFor1CoinAndInjectSeeds(
                                                    cdnDir = activeCdnDir,
                                                    selectedPlantId = selectedPlantId,
                                                    seedAmount = seeds,
                                                    unlockAllPlantsAndSkinsInShop = false,
                                                    log = log
                                                )
                                            }
                                            isBusy = false
                                        }
                                    }
                                ) { Text("🌱 ВНЕДРИТЬ СЕМЕНА ЭТОГО РАСТЕНИЯ (1🪙)", color = Color.White, fontWeight = FontWeight.Bold) }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                OutlinedTextField(
                                    value = plantSearch,
                                    onValueChange = { plantSearch = it },
                                    label = { Text("🔍 Поиск растения по имени или ID...") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val filteredPlants = Pvz2Database.PLANTS.filter {
                                    it.first.contains(plantSearch, true) || it.second.contains(plantSearch, true)
                                }

                                filteredPlants.forEach { (pId, pName) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = selectedPlantId == pId, onClick = { selectedPlantId = pId })
                                        Text(pName, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 2: ПИНЬЯТЫ И МОДЫ
                2 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🪅 Лут Пиньят (pinatacoreloot.rton)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = pinataMultiplier, onValueChange = { pinataMultiplier = it }, label = { Text("Множитель") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = pinataMinSeeds, onValueChange = { pinataMinSeeds = it }, label = { Text("Мин. семян") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    scope.launch {
                                        isBusy = true
                                        val mult = pinataMultiplier.toIntOrNull() ?: 100
                                        val minS = pinataMinSeeds.toIntOrNull() ?: 1000
                                        withContext(Dispatchers.IO) { Pvz2Manager.patchPinataCoreLoot(activeCdnDir, mult, minS, log) }
                                        isBusy = false
                                    }
                                }) { Text("🪅 Увеличить Лут Пиньят") }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🌞 Мод: 0 Солнца + 0 сек Перезарядки", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    scope.launch {
                                        isBusy = true
                                        withContext(Dispatchers.IO) { Pvz2Manager.patchZeroSunAndInstantCooldown(activeCdnDir, log) }
                                        isBusy = false
                                    }
                                }) { Text("🌞 Включить 0 Солнца + Мгновенный Кулдаун") }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🏆 Мега-Награды Арены (x100)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

                // ВКЛАДКА 3: PP.DAT
                3 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
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
                                Text("🌿 Прокачка растений в pp.dat", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = targetPlantLevel, onValueChange = { targetPlantLevel = it }, label = { Text("Уровень (1-20)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = targetMastery, onValueChange = { targetMastery = it }, label = { Text("Mastery (0-200)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                                    val pp = ppDatJson ?: return@Button
                                    scope.launch {
                                        isBusy = true
                                        val map = currencies.mapValues { it.value.toLongOrNull() ?: 0L }
                                        val lvl = targetPlantLevel.toIntOrNull() ?: 20
                                        val mst = targetMastery.toIntOrNull() ?: 200
                                        withContext(Dispatchers.IO) { Pvz2Manager.updatePpDatAndUnlockPlants(pp, map, true, lvl, mst, log) }
                                        isBusy = false
                                    }
                                }) { Text("💾 Сохранить pp.dat") }
                            }
                        }
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // ВКЛАДКА 4: CDN
                4 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📂 Путь к кэшу CDN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                OutlinedTextField(value = activeCdnDir, onValueChange = { activeCdnDir = it }, label = { Text("Путь к CDN") }, modifier = Modifier.fillMaxWidth())
                                Divider()
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    scope.launch {
                                        isBusy = true
                                        withContext(Dispatchers.IO) { Pvz2Manager.restoreAllCdnBackups(activeCdnDir, log) }
                                        isBusy = false
                                    }
                                }) { Text("⏪ Восстановить оригиналы .rton") }
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
