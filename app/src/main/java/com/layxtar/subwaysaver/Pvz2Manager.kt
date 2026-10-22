package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject

object Pvz2Manager {

    private val PACKAGES = listOf(
        Pvz2Database.PKG_PVZ2_ROW,
        Pvz2Database.PKG_PVZ2_NA
    )

    fun resolveNoBackupDir(log: (String) -> Unit): Pair<String, String> {
        for (pkg in PACKAGES) {
            val path = "/storage/emulated/0/Android/data/$pkg/files/No_Backup"
            val (_, out) = CryptoAndSyncManager.execSh("if [ -d '$path' ]; then echo 'FOUND'; fi")
            if (out.contains("FOUND")) return pkg to path
        }
        val defaultPkg = Pvz2Database.PKG_PVZ2_ROW
        return defaultPkg to "/storage/emulated/0/Android/data/$defaultPkg/files/No_Backup"
    }

    fun resolveActiveCdnDir(log: (String) -> Unit): String {
        val (_, noBackup) = resolveNoBackupDir(log)
        val cmd = "ls -1d '$noBackup'/CDN.* 2>/dev/null | sort -V | tail -n 1"
        val (_, out) = CryptoAndSyncManager.execSh(cmd)
        val found = out.trim()
        if (found.isNotEmpty()) return found
        return "$noBackup/CDN.13.5"
    }

    fun listAllCdnDirs(): List<String> {
        val (_, noBackup) = resolveNoBackupDir {}
        val (_, out) = CryptoAndSyncManager.execSh("ls -1d '$noBackup'/CDN.* 2>/dev/null | sort -V -r")
        return out.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun readRtonAsJson(fullPath: String, log: (String) -> Unit): JSONObject? {
        val rawBytes = CryptoAndSyncManager.readFileViaShizuku(fullPath, log) ?: return null
        return try {
            val textCheck = runCatching { String(rawBytes, Charsets.UTF_8).trim() }.getOrNull()
            if (textCheck != null && textCheck.startsWith("{")) {
                JSONObject(textCheck)
            } else {
                RtonConverter.rtonToJson(rawBytes)
            }
        } catch (e: Exception) {
            log("❌ Ошибка чтения ${fullPath.substringAfterLast("/")}: ${e.message}")
            null
        }
    }

    fun writeJsonAsRton(fullPath: String, json: JSONObject, log: (String) -> Unit): Boolean {
        val (pkg, _) = resolveNoBackupDir {}
        CryptoAndSyncManager.stopGameForSync(pkg, true, log)
        return try {
            val rtonBytes = RtonConverter.jsonToRton(json)
            val ok = CryptoAndSyncManager.writeFileViaShizuku(fullPath, rtonBytes, log)
            if (ok) log("✅ Записан RTON (${rtonBytes.size} байт): ${fullPath.substringAfterLast("/")}")
            ok
        } catch (e: Exception) {
            log("❌ Ошибка записи ${fullPath.substringAfterLast("/")}: ${e.message}")
            false
        }
    }

    fun getPpDatPath(): String {
        val (_, noBackup) = resolveNoBackupDir {}
        return "$noBackup/pp.dat"
    }

    fun loadPlayerSavePpDat(log: (String) -> Unit): JSONObject? = readRtonAsJson(getPpDatPath(), log)

    private fun findPlayerDataObject(ppJson: JSONObject): JSONObject {
        val objects = ppJson.optJSONArray("objects")
        if (objects != null) {
            for (i in 0 until objects.length()) {
                val obj = objects.getJSONObject(i)
                val objData = obj.optJSONObject("objdata") ?: obj
                if (objData.has("c") || objData.has("g") || objData.has("pl") || objData.has("m")) return objData
            }
            if (objects.length() > 0) {
                val first = objects.getJSONObject(0)
                return first.optJSONObject("objdata") ?: first
            }
        }
        return ppJson
    }

    fun readPpDatCurrencies(ppJson: JSONObject): Map<String, Long> {
        val data = findPlayerDataObject(ppJson)
        val res = mutableMapOf<String, Long>()
        for ((key, _) in Pvz2Database.CURRENCIES) res[key] = data.optLong(key, 0L)
        return res
    }

    fun updatePpDatAndUnlockPlants(
        ppJson: JSONObject,
        currencies: Map<String, Long>,
        unlockAllPlants: Boolean,
        targetPlantLevel: Int,
        targetMasteryLevel: Int,
        log: (String) -> Unit
    ): Boolean {
        val data = findPlayerDataObject(ppJson)
        for ((k, v) in currencies) data.put(k, v)

        if (unlockAllPlants) {
            val plObj = data.optJSONObject("pl") ?: JSONObject()
            for ((plantId, _) in Pvz2Database.PLANTS) {
                plObj.put(plantId, JSONObject().apply {
                    put("l", targetPlantLevel.coerceIn(1, 20))
                    put("m", targetMasteryLevel.coerceIn(0, 200))
                    put("xp", 999999)
                })
            }
            data.put("pl", plObj)
        }

        return writeJsonAsRton(getPpDatPath(), ppJson, log)
    }

    // ==================== 1. РЕАЛЬНЫЙ ПАТЧ МАГАЗИНА И СЕМЯН ПО СХЕМЕ LINHYM ====================

    fun patchShopAllFor1CoinAndInjectSeeds(
        cdnDir: String,
        selectedPlantId: String,
        seedAmount: Int,
        unlockAllPlantsAndSkinsInShop: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val productsPath = "$cdnDir/products.rton"
        val marketSchedPath = "$cdnDir/market_schedule.rton"
        val marketLayoutPath = "$cdnDir/market_layout.rton"
        val magentoPath = "$cdnDir/magento_filter.rton"
        val pinataPath = "$cdnDir/pinatacoreloot.rton"

        val productsJson = readRtonAsJson(productsPath, log) ?: return false
        val schedJson = readRtonAsJson(marketSchedPath, log) ?: return false

        // 1. Разрешаем все растения в magento_filter.rton
        readRtonAsJson(magentoPath, log)?.let { magento ->
            val objs = magento.optJSONArray("objects")
            if (objs != null && objs.length() > 0) {
                val filters = objs.getJSONObject(0).optJSONObject("objdata")?.optJSONArray("ObjectTypeFilters")
                if (filters != null) {
                    for (i in 0 until filters.length()) {
                        val f = filters.getJSONObject(i)
                        if (f.optString("Type") == "plant") {
                            val sub = f.optJSONArray("SubTypeFilters") ?: JSONArray()
                            val set = mutableSetOf<String>()
                            for (j in 0 until sub.length()) set.add(sub.getString(j))
                            for ((pId, _) in Pvz2Database.PLANTS) if (set.add(pId)) sub.put(pId)
                            f.put("SubTypeFilters", sub)
                        }
                    }
                }
            }
            writeJsonAsRton(magentoPath, magento, log)
        }

        // 2. В pinatacoreloot.rton добавляем конфигурацию слотов для семян растений (как у LinhYM: free_linhym_<plant>1)
        readRtonAsJson(pinataPath, log)?.let { pinataJson ->
            val objs = pinataJson.optJSONArray("objects")
            if (objs != null) {
                for (i in 0 until objs.length()) {
                    val o = objs.getJSONObject(i)
                    if (o.optString("objclass") == "CoreLoopRewardsSlotConfigProps") {
                        val slotConfigs = o.optJSONObject("objdata")?.optJSONObject("SlotConfigs")
                        if (slotConfigs != null) {
                            for ((pId, _) in Pvz2Database.PLANTS) {
                                val pinataKey = "free_linhym_${pId}1"
                                if (!slotConfigs.has(pinataKey)) {
                                    slotConfigs.put(pinataKey, JSONObject().apply {
                                        put("BonusTable", "bonus_standard")
                                        put("PercentBonusAward", 0)
                                        put("PercentPinataAward", 1)
                                        put("PinataType", "free")
                                        put("RewardsTable", "egypt_standard")
                                        put("Slots", JSONArray().apply { repeat(10) { put("PlantXP") } })
                                    })
                                }
                            }
                        }
                    }
                }
            }
            writeJsonAsRton(pinataPath, pinataJson, log)
        }

        // 3. В products.rton переводим все существующие товары и семена на 1 монету и увеличиваем их количество!
        val pObjects = productsJson.optJSONArray("objects") ?: JSONArray()
        val existingSkus = mutableSetOf<String>()
        val scheduledTopSkus = mutableListOf<String>()

        for (i in 0 until pObjects.length()) {
            val d = pObjects.getJSONObject(i).optJSONObject("objdata") ?: continue
            val sku = d.optString("Sku", "")
            if (sku.isNotEmpty()) existingSkus.add(sku)

            // Делаем любой товар за 1 монету
            d.put("PriceCoins", "1")
            d.put("PriceGems", "0")
            d.put("PriceMints", 0)
            d.put("Price", 0)
            d.put("IsFree", false)
            d.put("IsAdPlacement", false)

            // Если это товар с пиньятой или семенами (включая кнопки Find More!), увеличиваем ObjectCount до выбранного числа (например 500 или 10000)!
            val objType = d.optString("ObjectType", "").lowercase()
            if (objType == "pinata" || objType.contains("seed")) {
                val curCnt = d.optInt("ObjectCount", 1)
                if (curCnt < seedAmount) {
                    d.put("ObjectCount", maxOf(500, seedAmount))
                }
            }
        }

        // Создаем прямой товар для выбранного растения и его пакета семян
        val targetPlantSku = "linhym.Plant.${selectedPlantId.uppercase()}"
        if (existingSkus.add(targetPlantSku)) {
            pObjects.put(JSONObject().apply {
                put("aliases", JSONArray().put(targetPlantSku))
                put("objclass", "StoreProductProps")
                put("objdata", JSONObject().apply {
                    put("IsFree", false)
                    put("IsOneTimePurchase", false)
                    put("LawnDescription", selectedPlantId.uppercase())
                    put("LawnShortDescription", selectedPlantId.uppercase())
                    put("Name", "Plant - $selectedPlantId")
                    put("NoTooltip", false)
                    put("ObjectCount", 1)
                    put("ObjectItem", selectedPlantId)
                    put("ObjectType", "plant")
                    put("PriceCoins", "1")
                    put("OneTimeDays", 0)
                    put("Sku", targetPlantSku)
                })
            })
        }
        scheduledTopSkus.add(targetPlantSku)

        // Пакет семян выбранного растения (через механику пиньяты, которую игра 100% отрисовывает в магазине!)
        val targetSeedSku = "linhym.SeedPacket.${selectedPlantId.uppercase()}.$seedAmount"
        if (existingSkus.add(targetSeedSku)) {
            pObjects.put(JSONObject().apply {
                put("aliases", JSONArray().put(targetSeedSku))
                put("objclass", "StoreProductProps")
                put("objdata", JSONObject().apply {
                    put("IsFree", false)
                    put("IsOneTimePurchase", false)
                    put("LawnDescription", "STORE_CATEGORY_DAILY_OFFERS")
                    put("LawnShortDescription", "${selectedPlantId.uppercase()} x$seedAmount")
                    put("Name", "SeedPacket - $selectedPlantId x$seedAmount")
                    put("NoTooltip", false)
                    put("ObjectCount", maxOf(1, seedAmount / 10))
                    put("ObjectItem", "egypt_gems")
                    put("ObjectType", "pinata")
                    put("PriceCoins", "1")
                    put("OneTimeDays", 0)
                    put("Sku", targetSeedSku)
                })
            })
        }
        scheduledTopSkus.add(targetSeedSku)

        if (unlockAllPlantsAndSkinsInShop) {
            for ((pId, _) in Pvz2Database.PLANTS) {
                val pSku = "linhym.Plant.${pId.uppercase()}"
                if (existingSkus.add(pSku)) {
                    pObjects.put(JSONObject().apply {
                        put("aliases", JSONArray().put(pSku))
                        put("objclass", "StoreProductProps")
                        put("objdata", JSONObject().apply {
                            put("IsFree", false)
                            put("IsOneTimePurchase", false)
                            put("LawnDescription", pId.uppercase())
                            put("LawnShortDescription", pId.uppercase())
                            put("Name", "Plant - $pId")
                            put("NoTooltip", false)
                            put("ObjectCount", 1)
                            put("ObjectItem", pId)
                            put("ObjectType", "plant")
                            put("PriceCoins", "1")
                            put("OneTimeDays", 0)
                            put("Sku", pSku)
                        })
                    })
                }
                scheduledTopSkus.add(pSku)
            }
        }

        productsJson.put("objects", pObjects)
        writeJsonAsRton(productsPath, productsJson, log)

        // 4. Внедряем все созданные SKU прямо в ПЕРВЫЙ активный блок MarketScheduleEntries[0] в market_schedule.rton!
        val schedObjs = schedJson.optJSONArray("objects")
        if (schedObjs != null && schedObjs.length() > 0) {
            val entries = schedObjs.getJSONObject(0).optJSONObject("objdata")?.optJSONArray("MarketScheduleEntries")
            if (entries != null && entries.length() > 0) {
                // Делаем все существующие расписания вечными (StartDateTime = 0, EndDateTime = 0) и по 1 монете
                for (i in 0 until entries.length()) {
                    val entry = entries.getJSONObject(i)
                    entry.put("StartDateTime", 0)
                    entry.put("EndDateTime", 0)
                    val spArr = entry.optJSONArray("ScheduledProducts") ?: continue
                    for (j in 0 until spArr.length()) {
                        val sp = spArr.getJSONObject(j)
                        sp.put("SoftCost", -1)
                        sp.put("MaxPurchase", -1)
                    }
                }

                // Вставляем наши новые товары прямо в начало первого расписания во вкладки Plants, Seeds и Promoted Offers
                val firstEntryProducts = entries.getJSONObject(0).optJSONArray("ScheduledProducts") ?: JSONArray()
                for (sku in scheduledTopSkus) {
                    val cat = if (sku.contains("SeedPacket")) "Seeds" else "Plants"
                    firstEntryProducts.put(JSONObject().apply {
                        put("Comment", "")
                        put("Category", cat)
                        put("Priority", "")
                        put("ABTest", "")
                        put("PromotionGroup", "")
                        put("AdditionalSegment", "")
                        put("SoftCurrency", "")
                        put("SoftCost", -1)
                        put("MaxPurchase", -1)
                        put("SoftCostPerPurchase", -1)
                        put("Visuals", "RTID(0)")
                        put("Sku", JSONArray().put(sku).put(sku))
                    })
                }
                entries.getJSONObject(0).put("ScheduledProducts", firstEntryProducts)
            }
        }
        writeJsonAsRton(marketSchedPath, schedJson, log)

        // 5. Проверяем, чтобы в market_layout.rton были открыты вкладки Plants и Seeds
        readRtonAsJson(marketLayoutPath, log)?.let { layoutJson ->
            val layoutObjs = layoutJson.optJSONArray("objects")
            if (layoutObjs != null && layoutObjs.length() > 0) {
                val markets = layoutObjs.getJSONObject(0).optJSONObject("objdata")?.optJSONArray("Markets")
                if (markets != null && markets.length() > 0) {
                    val mainClusters = markets.getJSONObject(0).optJSONArray("Clusters")
                    if (mainClusters != null) {
                        val existing = mutableSetOf<String>()
                        for (i in 0 until mainClusters.length()) existing.add(mainClusters.getString(i))
                        for (c in listOf("Promotions", "Plants", "Seeds", "Costumes", "Bundles", "Pinatas")) {
                            if (existing.add(c)) mainClusters.put(c)
                        }
                    }
                }
            }
            writeJsonAsRton(marketLayoutPath, layoutJson, log)
        }

        log("🛒 Магазин PvZ 2 обновлён: все пиньяты и Find More дают по $seedAmount семян за 1🪙!")
        return true
    }

    // ==================== 2. РАБОЧИЙ ПАТЧ «0 СОЛНЦА И 0 СЕК ПЕРЕЗАРЯДКИ» ====================
    // Точно по структуре твоего PlantLevels.json (FloatStats -> Cost / Cooldown -> Values: [0, 0...]) и PlantProperties.json!
    fun patchZeroSunAndInstantCooldown(cdnDir: String, log: (String) -> Unit): Boolean {
        var ok = false

        // 1. Патчим PlantLevels.rton (где реально хранятся Cost и Cooldown для уровней 1..20!)
        val levelsPath = "$cdnDir/PlantLevels.rton"
        readRtonAsJson(levelsPath, log)?.let { levelsJson ->
            zeroFloatStatsInPlantLevels(levelsJson)
            if (writeJsonAsRton(levelsPath, levelsJson, log)) ok = true
        }

        // 2. Патчим PlantProperties.rton (на случай базовых значений без уровня)
        val propsPath = "$cdnDir/PlantProperties.rton"
        readRtonAsJson(propsPath, log)?.let { propsJson ->
            zeroBasePropsInPlantProperties(propsJson)
            if (writeJsonAsRton(propsPath, propsJson, log)) ok = true
        }

        if (ok) log("🌞 Готово! Все растения теперь стоят 0 солнца и перезаряжаются мгновенно (0 сек)!")
        return ok
    }

    // Ищет блоки {"Name": "Cost", "Values": [...]} и {"Name": "Cooldown" / "StartingCooldown", "Values": [...]} в PlantLevels.rton
    private fun zeroFloatStatsInPlantLevels(node: Any?) {
        when (node) {
            is JSONObject -> {
                if (node.has("Name") && node.has("Values")) {
                    val statName = node.optString("Name", "")
                    if (statName.equals("Cost", ignoreCase = true) ||
                        statName.equals("Cooldown", ignoreCase = true) ||
                        statName.equals("StartingCooldown", ignoreCase = true) ||
                        statName.equals("PacketCooldown", ignoreCase = true)
                    ) {
                        val vals = node.optJSONArray("Values")
                        if (vals != null) {
                            for (i in 0 until vals.length()) {
                                vals.put(i, 0)
                            }
                        }
                    }
                }
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    zeroFloatStatsInPlantLevels(node.opt(k))
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    zeroFloatStatsInPlantLevels(node.opt(i))
                }
            }
        }
    }

    private fun zeroBasePropsInPlantProperties(node: Any?) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (k.equals("Cost", ignoreCase = true) ||
                        k.equals("SunCost", ignoreCase = true) ||
                        k.equals("PacketCooldown", ignoreCase = true) ||
                        k.equals("StartingCooldown", ignoreCase = true)
                    ) {
                        if (v is Number) node.put(k, 0)
                    } else {
                        zeroBasePropsInPlantProperties(v)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) zeroBasePropsInPlantProperties(node.opt(i))
            }
        }
    }

    fun patchPinataCoreLoot(cdnDir: String, multiplier: Int, minSeedsPerSlot: Int, log: (String) -> Unit): Boolean {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readRtonAsJson(path, log) ?: return false
        val objs = json.optJSONArray("objects")
        if (objs != null) {
            for (i in 0 until objs.length()) {
                val o = objs.getJSONObject(i)
                val slotConfigs = o.optJSONObject("objdata")?.optJSONObject("SlotConfigs")
                if (slotConfigs != null) {
                    val keys = slotConfigs.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val cfg = slotConfigs.getJSONObject(k)
                        val slots = cfg.optJSONArray("Slots") ?: JSONArray()
                        while (slots.length() < 30) slots.put("PlantXP")
                        cfg.put("Slots", slots)
                        cfg.put("PercentPinataAward", 1)
                    }
                } else {
                    boostPinataNumbersRecursively(o, multiplier, minSeedsPerSlot)
                }
            }
        }
        return writeJsonAsRton(path, json, log)
    }

    private fun boostPinataNumbersRecursively(node: Any?, mult: Int, minSeeds: Int) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (v is Number) {
                        val low = k.lowercase()
                        if ((low.contains("min") || low.contains("max") || low.contains("amount") || low.contains("count")) &&
                            !low.contains("weight") && !low.contains("percent")
                        ) {
                            node.put(k, maxOf(v.toInt() * mult, minSeeds))
                        }
                    } else {
                        boostPinataNumbersRecursively(v, mult, minSeeds)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) boostPinataNumbersRecursively(node.opt(i), mult, minSeeds)
            }
        }
    }

    fun patchArenaAndCalendarMegaRewards(cdnDir: String, log: (String) -> Unit): Boolean {
        listOf("joust_crownrewards.rton", "joust_progressiverewards.rton", "calendar_schedule.rton").forEach { f ->
            readRtonAsJson("$cdnDir/$f", log)?.let { j ->
                boostRewards(j)
                writeJsonAsRton("$cdnDir/$f", j, log)
            }
        }
        return true
    }

    private fun boostRewards(node: Any?) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    val l = k.lowercase()
                    if (v is Number) {
                        if (l.contains("crown") || l.contains("required")) node.put(k, 1)
                        if (l.contains("amount") || l.contains("count")) node.put(k, maxOf(v.toInt() * 100, 1000))
                    }
                    boostRewards(v)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) boostRewards(node.opt(i))
            }
        }
    }

    fun restoreAllCdnBackups(cdnDir: String, log: (String) -> Unit) {
        val (pkg, _) = resolveNoBackupDir {}
        CryptoAndSyncManager.stopGameForSync(pkg, true, log)
        CryptoAndSyncManager.execSh("""
            for f in '$cdnDir'/*.bak_editor; do
                if [ -f "${'$'}f" ]; then cp "${'$'}f" "${'$'}{f%.bak_editor}"; fi
            done
            sync
        """.trimIndent(), log)
        log("⏪ Оригинальные файлы восстановлены из бэкапов!")
    }
}
