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
        if (found.isNotEmpty()) {
            log("🌻 Найдена папка кэша: $found")
            return found
        }
        val fallback = "$noBackup/CDN.13.5"
        log("⚠️ Папка CDN.* не найдена, используем: $fallback")
        return fallback
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
            log("❌ Ошибка чтения RTON ($fullPath): ${e.message}")
            null
        }
    }

    fun writeJsonAsRton(fullPath: String, json: JSONObject, log: (String) -> Unit): Boolean {
        val (pkg, _) = resolveNoBackupDir {}
        CryptoAndSyncManager.stopGameForSync(pkg, true, log)
        return try {
            val rtonBytes = RtonConverter.jsonToRton(json)
            val ok = CryptoAndSyncManager.writeFileViaShizuku(fullPath, rtonBytes, log)
            if (ok) log("✅ Записан RTON: ${fullPath.substringAfterLast("/")}")
            ok
        } catch (e: Exception) {
            log("❌ Ошибка записи RTON ($fullPath): ${e.message}")
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
                if (objData.has("c") || objData.has("g") || objData.has("pl") || objData.has("m")) {
                    return objData
                }
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
        for ((key, _) in Pvz2Database.CURRENCIES) {
            res[key] = data.optLong(key, 0L)
        }
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

    // ==================== НАСИЛЬНОЕ ВНЕДРЕНИЕ СЕМЯН И РАСТЕНИЙ В МАГАЗИН И FIND MORE ====================

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

        val productsJson = readRtonAsJson(productsPath, log) ?: return false
        val schedJson = readRtonAsJson(marketSchedPath, log) ?: return false
        val layoutJson = readRtonAsJson(marketLayoutPath, log) ?: return false

        // 1. Патчим magento_filter.rton: разрешаем показ всех растений
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
                            for ((pId, _) in Pvz2Database.PLANTS) {
                                if (set.add(pId)) sub.put(pId)
                            }
                            f.put("SubTypeFilters", sub)
                        }
                    }
                }
            }
            writeJsonAsRton(magentoPath, magento, log)
        }

        // 2. В products.rton создаём позиции на 1 монету
        val pObjects = productsJson.optJSONArray("objects") ?: JSONArray()
        val allPlantSkus = mutableListOf<String>()
        val allSeedSkus = mutableListOf<String>()

        // Создаём кастомный пак семян для выбранного растения
        val customSeedSku = "editor.SeedPacket.$selectedPlantId.$seedAmount"
        allSeedSkus.add(customSeedSku)
        pObjects.put(JSONObject().apply {
            put("aliases", JSONArray().put(customSeedSku))
            put("objclass", "StoreProductProps")
            put("objdata", JSONObject().apply {
                put("IsFree", false)
                put("IsOneTimePurchase", false)
                put("Name", "Seed - $selectedPlantId x$seedAmount")
                put("ObjectCount", seedAmount)
                put("ObjectItem", selectedPlantId)
                put("ObjectType", "seedpacket")
                put("PriceCoins", "1")
                put("Sku", customSeedSku)
            })
        })

        if (unlockAllPlantsAndSkinsInShop) {
            for ((pId, pName) in Pvz2Database.PLANTS) {
                // Прямой анлок растения
                val plantSku = "editor.Plant.$pId"
                allPlantSkus.add(plantSku)
                pObjects.put(JSONObject().apply {
                    put("aliases", JSONArray().put(plantSku))
                    put("objclass", "StoreProductProps")
                    put("objdata", JSONObject().apply {
                        put("IsFree", false)
                        put("IsOneTimePurchase", false)
                        put("Name", "Plant - $pName")
                        put("ObjectCount", 1)
                        put("ObjectItem", pId)
                        put("ObjectType", "plant")
                        put("PriceCoins", "1")
                        put("Sku", plantSku)
                    })
                })

                // Пакеты семян по 500 и 10 000 для каждого растения (перехватывают кнопку Find More!)
                for (amt in listOf(500, 10000)) {
                    val sSku = "editor.SeedPacket.$pId.$amt"
                    allSeedSkus.add(sSku)
                    pObjects.put(JSONObject().apply {
                        put("aliases", JSONArray().put(sSku))
                        put("objclass", "StoreProductProps")
                        put("objdata", JSONObject().apply {
                            put("IsFree", false)
                            put("IsOneTimePurchase", false)
                            put("Name", "Seed - $pName x$amt")
                            put("ObjectCount", amt)
                            put("ObjectItem", pId)
                            put("ObjectType", "seedpacket")
                            put("PriceCoins", "1")
                            put("Sku", sSku)
                        })
                    })
                }
            }

            // Добавляем все 342 скина
            for (cNum in 1..342) {
                val cSku = "editor.Costume.$cNum"
                pObjects.put(JSONObject().apply {
                    put("aliases", JSONArray().put(cSku))
                    put("objclass", "StoreProductProps")
                    put("objdata", JSONObject().apply {
                        put("IsFree", false)
                        put("IsOneTimePurchase", false)
                        put("Name", "Costume $cNum")
                        put("ObjectCount", 1)
                        put("ObjectItem", cNum.toString())
                        put("ObjectType", "costume")
                        put("PriceCoins", "1")
                        put("Sku", cSku)
                    })
                })
            }
        }

        // Обнуляем стоимость всех остальных товаров на 1 монету
        for (i in 0 until pObjects.length()) {
            val d = pObjects.getJSONObject(i).optJSONObject("objdata") ?: continue
            d.put("PriceCoins", "1")
            d.put("PriceGems", 0)
            d.put("Price", 0.0)
            d.put("IsFree", false)
            // Увеличиваем стандартные пакеты Find More с 5 штук до 500!
            if (d.optString("ObjectType").contains("seed", ignoreCase = true)) {
                val curCnt = d.optInt("ObjectCount", 0)
                if (curCnt in 1..20) d.put("ObjectCount", 500)
            }
        }
        productsJson.put("objects", pObjects)
        writeJsonAsRton(productsPath, productsJson, log)

        // 3. Внедряем в market_schedule.rton (категории "Plants" и "Seeds")
        val schedObjs = schedJson.optJSONArray("objects")
        if (schedObjs != null && schedObjs.length() > 0) {
            val entries = schedObjs.getJSONObject(0).optJSONObject("objdata")?.optJSONArray("MarketScheduleEntries")
            if (entries != null) {
                val newScheduledProducts = JSONArray()

                for (sku in allPlantSkus) {
                    newScheduledProducts.put(JSONObject().apply {
                        put("Category", "Plants")
                        put("SoftCurrency", "Coins")
                        put("SoftCost", 1)
                        put("MaxPurchase", -1)
                        put("Visuals", "RTID(0)")
                        put("Sku", JSONArray().put(sku).put(sku))
                    })
                }
                for (sku in allSeedSkus) {
                    newScheduledProducts.put(JSONObject().apply {
                        put("Category", "Seeds")
                        put("SoftCurrency", "Coins")
                        put("SoftCost", 1)
                        put("MaxPurchase", -1)
                        put("Visuals", "RTID(0)")
                        put("Sku", JSONArray().put(sku).put(sku))
                    })
                }

                entries.put(JSONObject().apply {
                    put("Name", "EditorPlantsAndSeeds")
                    put("StartDateTime", 0)
                    put("EndDateTime", 0)
                    put("ScheduledProducts", newScheduledProducts)
                })
            }
        }
        writeJsonAsRton(marketSchedPath, schedJson, log)

        // 4. Патчим market_layout.rton: регистрируем вкладки Plants и Seeds
        val layoutObjs = layoutJson.optJSONArray("objects")
        if (layoutObjs != null && layoutObjs.length() > 0) {
            val markets = layoutObjs.getJSONObject(0).optJSONObject("objdata")?.optJSONArray("Markets")
            if (markets != null && markets.length() > 0) {
                val mainClusters = markets.getJSONObject(0).optJSONArray("Clusters")
                if (mainClusters != null) {
                    val needed = listOf("Promotions", "Plants", "Seeds", "Costumes", "Bundles", "Pinatas")
                    val existing = mutableSetOf<String>()
                    for (i in 0 until mainClusters.length()) existing.add(mainClusters.getString(i))
                    for (c in needed) {
                        if (existing.add(c)) mainClusters.put(c)
                    }
                }
            }
        }
        writeJsonAsRton(marketLayoutPath, layoutJson, log)

        log("🛒 Магазин и кнопка Find More полностью пропатчены (семена по 500 и 10 000 шт. за 1🪙)!")
        return true
    }

    fun patchPinataCoreLoot(cdnDir: String, multiplier: Int, minSeedsPerSlot: Int, log: (String) -> Unit): Boolean {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readRtonAsJson(path, log) ?: return false
        val objs = json.optJSONArray("objects")
        if (objs != null && objs.length() > 0) {
            val slotConfigs = objs.getJSONObject(0).optJSONObject("objdata")?.optJSONObject("SlotConfigs")
            if (slotConfigs != null) {
                val keys = slotConfigs.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val cfg = slotConfigs.getJSONObject(k)
                    val slots = cfg.optJSONArray("Slots") ?: JSONArray()
                    while (slots.length() < 20) slots.put("PlantXP")
                    cfg.put("Slots", slots)
                    cfg.put("PercentPinataAward", 1)
                }
            }
        }
        return writeJsonAsRton(path, json, log)
    }

    fun patchZeroSunAndInstantCooldown(cdnDir: String, log: (String) -> Unit): Boolean {
        var ok = true
        readRtonAsJson("$cdnDir/PlantProperties.rton", log)?.let { j ->
            zeroSunRecursively(j)
            ok = ok and writeJsonAsRton("$cdnDir/PlantProperties.rton", j, log)
        }
        readRtonAsJson("$cdnDir/PlantLevels.rton", log)?.let { j ->
            zeroSunRecursively(j)
            ok = ok and writeJsonAsRton("$cdnDir/PlantLevels.rton", j, log)
        }
        return ok
    }

    private fun zeroSunRecursively(node: Any?) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (k.equals("Cost", true) || k.equals("SunCost", true) || k.equals("Cooldown", true) || k.equals("PacketCooldown", true)) {
                        if (v is Number) node.put(k, 0)
                        if (v is JSONArray) { for (i in 0 until v.length()) v.put(i, 0) }
                    }
                    zeroSunRecursively(v)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) zeroSunRecursively(node.opt(i))
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
