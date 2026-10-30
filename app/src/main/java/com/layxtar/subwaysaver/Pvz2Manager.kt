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
            if (out.contains("FOUND")) {
                return pkg to path
            }
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
            log("🌻 Найдена актуальная папка кэша: $found")
            return found
        }
        val fallback = "$noBackup/CDN.13.5"
        log("⚠️ Папка CDN.* не найдена автоматически, используем: $fallback")
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
            log("❌ Ошибка декодирования RTON ($fullPath): ${e.message}")
            null
        }
    }

    // Читает чистый оригинальный RTON (из .bak_editor, если бэкап уже создавался, иначе из самого файла),
    // чтобы всегда сохранять оригинальные товары и строение магазина игры по умолчанию!
    private fun readCleanOriginalRtonAsJson(fullPath: String, log: (String) -> Unit): JSONObject? {
        val bakPath = "$fullPath.bak_editor"
        val (_, out) = CryptoAndSyncManager.execSh("if [ -f '$bakPath' ]; then echo 'HAS_BAK'; fi")
        return if (out.contains("HAS_BAK")) {
            readRtonAsJson(bakPath, log) ?: readRtonAsJson(fullPath, log)
        } else {
            readRtonAsJson(fullPath, log)
        }
    }

    fun writeJsonAsRton(fullPath: String, json: JSONObject, log: (String) -> Unit): Boolean {
        val (pkg, _) = resolveNoBackupDir {}
        CryptoAndSyncManager.stopGameForSync(pkg, true, log)
        return try {
            val rtonBytes = RtonConverter.jsonToRton(json)
            val ok = CryptoAndSyncManager.writeFileViaShizuku(fullPath, rtonBytes, log)
            if (ok) log("✅ Записан RTON (${rtonBytes.size / 1024} КБ): ${fullPath.substringAfterLast("/")}")
            ok
        } catch (e: Exception) {
            log("❌ Ошибка упаковки RTON ($fullPath): ${e.message}")
            false
        }
    }

    fun getPpDatPath(): String {
        val (_, noBackup) = resolveNoBackupDir {}
        return "$noBackup/pp.dat"
    }

    fun loadPlayerSavePpDat(log: (String) -> Unit): JSONObject? {
        val path = getPpDatPath()
        val json = readRtonAsJson(path, log)
        if (json != null) log("✅ Сохранение pp.dat успешно расшифровано!")
        return json
    }

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
        for ((k, v) in currencies) {
            data.put(k, v)
        }

        if (unlockAllPlants) {
            val plObj = data.optJSONObject("pl") ?: JSONObject()
            for (plantId in Pvz2Database.ALL_PLANT_IDS) {
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

    // Автоматически собирает все ID растений: из нашей базы (включая baneberry) + сканирует PlantLevels.rton / magento_filter.rton из кэша игры,
    // чтобы ни одно новое растение никогда не было пропущено!
    private fun collectAllPlantIds(cdnDir: String): List<String> {
        val combined = LinkedHashSet<String>()
        Pvz2Database.ALL_PLANT_IDS.forEach { combined.add(it.lowercase()) }

        // Дополнительно проверяем PlantLevels.rton в папке CDN
        val levelsJson = readCleanOriginalRtonAsJson("$cdnDir/PlantLevels.rton") {}
            ?: readCleanOriginalRtonAsJson("$cdnDir/plantlevels.rton") {}
        val lvlObjs = levelsJson?.optJSONArray("objects")
        if (lvlObjs != null) {
            for (i in 0 until lvlObjs.length()) {
                val obj = lvlObjs.optJSONObject(i) ?: continue
                val aliases = obj.optJSONArray("aliases")
                if (aliases != null && aliases.length() > 0) {
                    val alias = aliases.optString(0, "").trim().lowercase()
                    if (alias.isNotEmpty() && !alias.contains("level") && !alias.contains("default")) {
                        combined.add(alias)
                    }
                }
            }
        }
        return combined.toList()
    }

    // ==================== 1. МАГАЗИН С РОДНЫМ СТРОЕНИЕМ + ВСЕ РАСТЕНИЯ (ВКЛЮЧАЯ BANEBERRY), СКИНЫ, 2 ВКЛАДКИ СЕМЯН И DELETE ITEMS ====================

    fun installFullMegaShopAllPlantsSkinsAndSeeds(cdnDir: String, log: (String) -> Unit): Boolean {
        val allPlants = collectAllPlantIds(cdnDir)
        log("🌿 Всего обнаружено растений (включая Baneberry и новые): ${allPlants.size}")

        log("⏳ 1/5: Обновление фильтра магазина magento_filter.rton...")
        val magentoJson = buildMagentoFilterJson(cdnDir, allPlants)
        if (!writeJsonAsRton("$cdnDir/magento_filter.rton", magentoJson, log)) return false

        log("⏳ 2/5: Дополнение родного макета магазина market_layout.rton (сохранены все родные вкладки + добавлены Costumes, Seeds, Seeds2, DeleteItems)...")
        val layoutJson = buildMarketLayoutPreservingDefault(cdnDir, log)
        if (!writeJsonAsRton("$cdnDir/market_layout.rton", layoutJson, log)) return false

        log("⏳ 3/5: Прошивка индивидуальных таблиц семян в pinatacoreloot.rton для всех ${allPlants.size} растений...")
        patchPinataCoreLootExactMethod(cdnDir, allPlants, log)

        log("⏳ 4/5: Обновление products.rton и market_schedule.rton (сохранены все дефолтные товары за 1🪙 + добавлены все растения, скины, семена и списание валют)...")
        val (productsJson, scheduleJson) = buildProductsAndSchedulePreservingDefault(cdnDir, allPlants, log)
        if (!writeJsonAsRton("$cdnDir/products.rton", productsJson, log)) return false

        log("⏳ 5/5: Запись расписания витрины market_schedule.rton...")
        if (!writeJsonAsRton("$cdnDir/market_schedule.rton", scheduleJson, log)) return false

        log("🎉 Готово! Родной магазин сохранён (всё по 1🪙), Baneberry и все растения добавлены в Plants и в 2 вкладки семян (Seeds / Seeds2)!")
        return true
    }

    private fun buildMagentoFilterJson(cdnDir: String, allPlants: List<String>): JSONObject {
        val plantSubTypes = JSONArray()
        allPlants.forEach { plantSubTypes.put(it) }

        val upgradeSubTypes = JSONArray()
        Pvz2Database.GAME_UPGRADES.forEach { upgradeSubTypes.put(it) }

        val costumeSubTypes = JSONArray()
        for (i in 1..342) costumeSubTypes.put(i.toString())

        val filters = JSONArray().apply {
            put(JSONObject().put("Type", "coin").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "gem").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "ticket").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "fuel").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "mint").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "bundle").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "keygate").put("WarnEnsureRealMoneyOnly", true))
            put(JSONObject().put("Type", "stargate").put("WarnEnsureRealMoneyOnly", true))
            put(JSONObject().put("Type", "eventenergy").put("WarnEnsureGemsOnly", false))
            put(JSONObject().put("Type", "crown").put("WarnEnsureRealMoneyOnly", true))
            put(JSONObject().put("Type", "tapjoy").put("WarnEnsureGemsOnly", false))
            put(JSONObject().put("Type", "iftoken").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "rewardmultiplier").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "sprout").put("WarnEnsureGemsOnly", false))
            put(JSONObject().put("Type", "plant").put("SubTypeFilters", plantSubTypes))
            put(JSONObject().put("Type", "gameupgrade").put("WarnEnsureRealMoneyOnly", false).put("SubTypeFilters", upgradeSubTypes))
            put(JSONObject().put("Type", "costume").put("SubTypeFilters", costumeSubTypes))
            put(JSONObject().put("Type", "pinata").put("WarnEnsureRealMoneyOnly", false))
        }

        val sheetObj = JSONObject().apply {
            put("objclass", "ProductFilterPropertySheet")
            put("aliases", JSONArray().put("MagentoFilter"))
            put("objdata", JSONObject().put("ObjectTypeFilters", filters))
        }

        return JSONObject().apply {
            put("version", 1)
            put("objects", JSONArray().put(sheetObj))
        }
    }

    // БЕРЁТ РОДНОЙ MARKET_LAYOUT.RTON ИГРЫ И ДОБАВЛЯЕТ К НЕМУ ТОЛЬКО COSTUMES, 2 ВКЛАДКИ СЕМЯН (SEEDS, SEEDS2) И УДАЛЕНИЕ ТОВАРОВ (DELET)!
    private fun buildMarketLayoutPreservingDefault(cdnDir: String, log: (String) -> Unit): JSONObject {
        val origLayout = readCleanOriginalRtonAsJson("$cdnDir/market_layout.rton", log)
        val objData = origLayout?.optJSONArray("objects")?.optJSONObject(0)?.optJSONObject("objdata")

        if (origLayout != null && objData != null) {
            // 1. В Main Market Clusters добавляем наши дополнительные кластеры (сохраняя все дефолтные!)
            val markets = objData.optJSONArray("Markets") ?: JSONArray()
            for (i in 0 until markets.length()) {
                val m = markets.optJSONObject(i) ?: continue
                if (m.optString("Type") == "Main") {
                    val clArr = m.optJSONArray("Clusters") ?: JSONArray()
                    val existing = mutableSetOf<String>()
                    for (j in 0 until clArr.length()) existing.add(clArr.optString(j))
                    listOf("Plants", "Costumes", "Seeds", "Seeds2", "Delet").forEach { extra ->
                        if (existing.add(extra)) clArr.put(extra)
                    }
                    m.put("Clusters", clArr)
                }
            }

            // 2. В Clusters добавляем определения кластеров, если их ещё нет
            val clusters = objData.optJSONArray("Clusters") ?: JSONArray()
            val existingClusters = mutableSetOf<String>()
            for (i in 0 until clusters.length()) {
                existingClusters.add(clusters.optJSONObject(i)?.optString("Type") ?: "")
            }
            val extraClusters = listOf(
                "Plants" to JSONArray().put("Plants"),
                "Costumes" to JSONArray().put("Costumes"),
                "Seeds" to JSONArray().put("Seeds"),
                "Seeds2" to JSONArray().put("Seeds2"),
                "Delet" to JSONArray().put("Delet")
            )
            for ((cType, cTabs) in extraClusters) {
                if (existingClusters.add(cType)) {
                    clusters.put(JSONObject().put("Type", cType).put("Tabs", cTabs))
                }
            }
            objData.put("Clusters", clusters)

            // 3. В Tabs добавляем вкладки Costumes, Seeds, Seeds2 и Delet (DeleteItems), сохраняя все родные вкладки!
            val tabs = objData.optJSONArray("Tabs") ?: JSONArray()
            val existingTabs = mutableSetOf<String>()
            for (i in 0 until tabs.length()) {
                existingTabs.add(tabs.optJSONObject(i)?.optString("Type") ?: "")
            }
            if (existingTabs.add("Plants")) {
                tabs.put(JSONObject().put("Type", "Plants").put("Name", "[STORE_TAB_PLANTS]").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Plants")))
            }
            if (existingTabs.add("Costumes")) {
                tabs.put(JSONObject().put("Type", "Costumes").put("Name", "Costumes").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Costumes")))
            }
            if (existingTabs.add("Seeds")) {
                tabs.put(JSONObject().put("Type", "Seeds").put("Name", "Seeds 1").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds")))
            }
            if (existingTabs.add("Seeds2")) {
                tabs.put(JSONObject().put("Type", "Seeds2").put("Name", "Seeds 2").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds2")))
            }
            if (existingTabs.add("Delet")) {
                tabs.put(JSONObject().put("Type", "Delet").put("Name", "DeleteItems").put("ImageDown", "IMAGE_UI_STORE_GEMSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_GEMSTAB_ACTIVE").put("Categories", JSONArray().put("DeletCoin").put("DeletGem").put("DeletTicket").put("DeletFuel").put("DeletSprout").put("DeletMint")))
            }
            objData.put("Tabs", tabs)

            // 4. В Categories снимаем лимит MaxCount (ставим -1) у родных категорий и добавляем категории для Seeds, Seeds2, Costumes и Delet*
            val categories = objData.optJSONArray("Categories") ?: JSONArray()
            val existingCats = mutableSetOf<String>()
            for (i in 0 until categories.length()) {
                val catObj = categories.optJSONObject(i) ?: continue
                catObj.put("MaxCount", -1)
                existingCats.add(catObj.optString("Type"))
            }

            val extraCategories = listOf(
                Triple("Plants", "Plants", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"),
                Triple("Costumes", "Costumes", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"),
                Triple("Seeds", "Seed Packets (Part 1)", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"),
                Triple("Seeds2", "Seed Packets (Part 2 & New)", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"),
                Triple("DeletCoin", "Delete Coins", "IMAGE_UI_CARDS_STORE_STORE_COIN_CARD"),
                Triple("DeletGem", "Delete Gems", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"),
                Triple("DeletTicket", "Delete Tickets", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"),
                Triple("DeletFuel", "Delete Fuels", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"),
                Triple("DeletSprout", "Delete Sprouts", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"),
                Triple("DeletMint", "Delete Mints", "IMAGE_UI_CARDS_STORE_STORE_PINATA_CARD")
            )
            for ((catType, label, cardImg) in extraCategories) {
                if (existingCats.add(catType)) {
                    categories.put(JSONObject().put("Type", catType).put("LabelText", label).put("MaxCount", -1).put("Card", cardImg))
                }
            }
            objData.put("Categories", categories)

            return origLayout
        }

        // Fallback, если оригинальный market_layout.rton не прочитался
        return JSONObject()
    }

    private fun replaceDeepPlantIdAndCount(
        node: Any?,
        sourcePlantName: String,
        targetPlantId: String,
        targetTableId: String,
        targetAmount: Int
    ) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    when {
                        k.equals("Id", ignoreCase = true) || k.equals("TableId", ignoreCase = true) -> {
                            node.put(k, targetTableId)
                        }
                        v is String -> {
                            if (v.equals(sourcePlantName, ignoreCase = true) ||
                                v.equals("peashooter", ignoreCase = true) ||
                                v.equals("akee", ignoreCase = true)
                            ) {
                                node.put(k, targetPlantId)
                            } else if (v.contains(sourcePlantName, ignoreCase = true)) {
                                node.put(k, v.replace(sourcePlantName, targetPlantId, ignoreCase = true))
                            }
                        }
                        v is Number -> {
                            if (k.equals("Min", ignoreCase = true) ||
                                k.equals("Max", ignoreCase = true) ||
                                k.equals("Amount", ignoreCase = true) ||
                                k.equals("Count", ignoreCase = true)
                            ) {
                                node.put(k, targetAmount)
                            }
                        }
                        v is JSONArray || v is JSONObject -> {
                            replaceDeepPlantIdAndCount(v, sourcePlantName, targetPlantId, targetTableId, targetAmount)
                        }
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    val elem = node.opt(i)
                    if (elem is String) {
                        if (elem.equals(sourcePlantName, ignoreCase = true) ||
                            elem.equals("peashooter", ignoreCase = true) ||
                            elem.equals("akee", ignoreCase = true)
                        ) {
                            node.put(i, targetPlantId)
                        }
                    } else if (elem is JSONObject || elem is JSONArray) {
                        replaceDeepPlantIdAndCount(elem, sourcePlantName, targetPlantId, targetTableId, targetAmount)
                    }
                }
            }
        }
    }

    private fun patchPinataCoreLootExactMethod(
        cdnDir: String,
        allPlants: List<String>,
        log: (String) -> Unit
    ) {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readCleanOriginalRtonAsJson(path, log) ?: return
        val objects = json.optJSONArray("objects") ?: return

        var slotConfigsObj: JSONObject? = null
        var tablesParentData: JSONObject? = null

        for (i in 0 until objects.length()) {
            val obj = objects.getJSONObject(i)
            val data = obj.optJSONObject("objdata") ?: continue
            if (data.has("SlotConfigs")) {
                slotConfigsObj = data.optJSONObject("SlotConfigs")
            } else {
                tablesParentData = data
            }
        }

        if (slotConfigsObj != null) {
            for (plantId in allPlants) {
                val slotKey = "free_seed_${plantId}1"
                val tableKey = "seed_${plantId}1"

                slotConfigsObj.put(slotKey, JSONObject().apply {
                    put("BonusTable", "bonus_standard")
                    put("PercentBonusAward", 0)
                    put("PercentPinataAward", 1)
                    put("PinataType", "")
                    put("RewardsTable", tableKey)
                    put("Slots", JSONArray().put("PlantXP"))
                })
            }

            val multiSlots = JSONArray().apply {
                repeat(allPlants.size) { put("PlantXP") }
            }
            val megaPinataKeys = listOf(
                "free_all_xp_plants_bronze" to "all_xp_plants_bronze_0",
                "free_all_xp_plants_silver" to "all_xp_plants_silver_0",
                "free_all_xp_plants_gold" to "all_xp_plants_gold_0",
                "free_all_xp_plants_max" to "all_xp_plants_max_0",
                "free_all_xp_plants_100000000" to "all_xp_plants_100000000_0"
            )
            for ((sKey, tKey) in megaPinataKeys) {
                slotConfigsObj.put(sKey, JSONObject().apply {
                    put("BonusTable", "bonus_standard")
                    put("PercentBonusAward", 0)
                    put("PercentPinataAward", 1)
                    put("PinataType", "free")
                    put("RewardsTable", tKey)
                    put("Slots", multiSlots)
                })
            }
        }

        if (tablesParentData != null) {
            val tableKeyName = tablesParentData.keys().asSequence().firstOrNull { k ->
                tablesParentData.opt(k) is JSONObject || tablesParentData.opt(k) is JSONArray
            }

            if (tableKeyName != null) {
                val container = tablesParentData.opt(tableKeyName)

                if (container is JSONObject) {
                    var sourcePlant = "peashooter"
                    var templateObj = container.optJSONObject("peashooter1")
                    if (templateObj == null) {
                        sourcePlant = "akee"
                        templateObj = container.optJSONObject("akee1")
                    }
                    if (templateObj == null) {
                        sourcePlant = "sunflower"
                        templateObj = container.optJSONObject("sunflower50")
                    }

                    for (plantId in allPlants) {
                        val tableKey = "seed_${plantId}1"
                        if (templateObj != null) {
                            val cloned = JSONObject(templateObj.toString())
                            replaceDeepPlantIdAndCount(cloned, sourcePlant, plantId, tableKey, 1)
                            container.put(tableKey, cloned)
                        } else {
                            container.put(tableKey, JSONObject().apply {
                                put("Rewards", JSONArray().put(JSONObject().apply {
                                    put("Entities", JSONArray().put(plantId))
                                    put("Plant", plantId)
                                    put("Item", plantId)
                                    put("Min", 1)
                                    put("Max", 1)
                                    put("Amount", 1)
                                    put("Type", "PlantXP")
                                    put("Weight", 100)
                                }))
                            })
                        }
                    }

                    val megaTiers = listOf(
                        "all_xp_plants_bronze_0" to 100,
                        "all_xp_plants_silver_0" to 1000,
                        "all_xp_plants_gold_0" to 5000,
                        "all_xp_plants_max_0" to 50000,
                        "all_xp_plants_100000000_0" to 100000000
                    )
                    for ((tKey, amt) in megaTiers) {
                        val allRewards = JSONArray()
                        for (plantId in allPlants) {
                            if (templateObj != null) {
                                val singleClone = JSONObject(templateObj.toString())
                                replaceDeepPlantIdAndCount(singleClone, sourcePlant, plantId, tKey, amt)
                                val subArr = singleClone.optJSONArray("Rewards") ?: singleClone.optJSONArray("SubTables")
                                if (subArr != null && subArr.length() > 0) {
                                    allRewards.put(subArr.getJSONObject(0))
                                }
                            } else {
                                allRewards.put(JSONObject().apply {
                                    put("Entities", JSONArray().put(plantId))
                                    put("Plant", plantId)
                                    put("Min", amt)
                                    put("Max", amt)
                                    put("Amount", amt)
                                    put("Type", "PlantXP")
                                    put("Weight", 100)
                                })
                            }
                        }
                        container.put(tKey, JSONObject().put("Rewards", allRewards))
                    }
                } else if (container is JSONArray) {
                    var sourcePlant = "peashooter"
                    var templateObj: JSONObject? = null
                    val existingIds = mutableMapOf<String, Int>()

                    for (i in 0 until container.length()) {
                        val item = container.optJSONObject(i) ?: continue
                        val idStr = item.optString("Id", item.optString("TableId", ""))
                        if (idStr.isNotEmpty()) existingIds[idStr] = i
                        if (idStr == "peashooter1") {
                            sourcePlant = "peashooter"
                            templateObj = item
                        } else if (templateObj == null && idStr == "akee1") {
                            sourcePlant = "akee"
                            templateObj = item
                        }
                    }

                    for (plantId in allPlants) {
                        val tableKey = "seed_${plantId}1"
                        val newEntry = if (templateObj != null) {
                            JSONObject(templateObj.toString()).also { cloned ->
                                replaceDeepPlantIdAndCount(cloned, sourcePlant, plantId, tableKey, 1)
                            }
                        } else {
                            JSONObject().apply {
                                put("Id", tableKey)
                                put("Rewards", JSONArray().put(JSONObject().apply {
                                    put("Entities", JSONArray().put(plantId))
                                    put("Plant", plantId)
                                    put("Min", 1)
                                    put("Max", 1)
                                    put("Amount", 1)
                                    put("Type", "PlantXP")
                                    put("Weight", 100)
                                }))
                            }
                        }
                        val existIdx = existingIds[tableKey]
                        if (existIdx != null) {
                            container.put(existIdx, newEntry)
                        } else {
                            container.put(newEntry)
                        }
                    }
                }
            }
        }

        writeJsonAsRton(path, json, log)
    }

    // СОХРАНЯЕТ ВСЕ ТОВАРЫ И РАСПИСАНИЯ МАГАЗИНА ИГРЫ ПО УМОЛЧАНИЮ (СДЕЛАВ ИХ ЗА 1 МОНЕТУ)
    // И ДОБАВЛЯЕТ ВСЕ РАСТЕНИЯ (ВКЛЮЧАЯ BANEBERRY) В PLANTS, ВСЕ 342 СКИНА В COSTUMES, СЕМЕНА В 2 ВКЛАДКИ (SEEDS / SEEDS2) И УДАЛЕНИЕ ВАЛЮТ В DELET!
    private fun buildProductsAndSchedulePreservingDefault(
        cdnDir: String,
        allPlants: List<String>,
        log: (String) -> Unit
    ): Pair<JSONObject, JSONObject> {
        val origProductsJson = readCleanOriginalRtonAsJson("$cdnDir/products.rton", log)
        val origScheduleJson = readCleanOriginalRtonAsJson("$cdnDir/market_schedule.rton", log)

        val productsArr = JSONArray()
        val addedSkus = mutableSetOf<String>()

        // 1. Бережно переносим ВСЕ оригинальные товары игры из products.rton и ставим им цену 1 монета
        val origObjects = origProductsJson?.optJSONArray("objects")
        if (origObjects != null) {
            for (i in 0 until origObjects.length()) {
                val obj = origObjects.getJSONObject(i)
                val data = obj.optJSONObject("objdata")
                if (data != null) {
                    val sku = data.optString("Sku", "")
                    if (sku.isNotEmpty()) addedSkus.add(sku)
                    data.put("PriceCoins", "1")
                    data.put("PriceGems", 0)
                    data.put("PriceMints", 0)
                    data.put("Price", 0)
                    data.put("IsFree", false)
                    data.put("IsAdPlacement", false)
                }
                productsArr.put(obj)
            }
        }

        fun addStoreProduct(
            sku: String,
            name: String,
            desc: String,
            shortDesc: String,
            objectType: String,
            objectItem: String,
            objectCount: Int,
            bundleProps: JSONArray? = null
        ) {
            if (!addedSkus.add(sku)) return
            val objData = JSONObject().apply {
                if (bundleProps != null) put("BundleProps", bundleProps)
                put("IsFree", false)
                put("IsOneTimePurchase", false)
                put("LawnDescription", desc)
                put("LawnShortDescription", shortDesc)
                put("Name", name)
                put("NoTooltip", false)
                put("ObjectCount", objectCount)
                put("ObjectItem", objectItem)
                put("ObjectType", objectType)
                put("PriceCoins", "1")
                put("OneTimeDays", 0)
                put("Sku", sku)
            }
            productsArr.put(JSONObject().apply {
                put("aliases", JSONArray().put(sku))
                put("objclass", "StoreProductProps")
                put("objdata", objData)
            })
        }

        val scheduledPlants = JSONArray()
        val scheduledCostumes = JSONArray()
        val scheduledSeeds1 = JSONArray()
        val scheduledSeeds2 = JSONArray()
        val scheduledDeleteItems = JSONArray()

        fun makeScheduleEntry(category: String, sku: String, visualTag: String = ""): JSONObject {
            return JSONObject().apply {
                put("Comment", "")
                put("Category", category)
                put("Priority", "")
                put("ABTest", "")
                put("PromotionGroup", "")
                put("AdditionalSegment", "")
                put("SoftCurrency", "")
                put("SoftCost", -1)
                put("MaxPurchase", -1)
                put("SoftCostPerPurchase", -1)
                if (visualTag.isNotEmpty()) {
                    put("Visuals", JSONArray().put(visualTag))
                } else {
                    put("Visuals", "RTID(0)")
                }
                put("Sku", JSONArray().put(sku).put(sku))
            }
        }

        // 2. Добавляем ВСЕ растения (включая Baneberry) во вкладку Plants и их семена в 2 вкладки (Seeds и Seeds2)
        val halfIndex = allPlants.size / 2
        val classicSeedTiers = listOf(1, 100, 10000)
        val newestSeedTiers = listOf(1, 100, 10000, 100000, 1000000)

        allPlants.forEachIndexed { index, plantId ->
            val upper = plantId.uppercase()
            val plantSku = "custom.Plant.$plantId"

            addStoreProduct(
                sku = plantSku,
                name = "Plant - $plantId",
                desc = upper,
                shortDesc = upper,
                objectType = "plant",
                objectItem = plantId,
                objectCount = 1
            )
            scheduledPlants.put(makeScheduleEntry("Plants", plantSku, if (index < 30) "new" else ""))

            // Распределяем семена ровно на 2 вкладки: Seeds2 (новые, включая baneberry) и Seeds (классические)
            val isFirstHalfNew = index < halfIndex
            val targetCat = if (isFirstHalfNew) "Seeds2" else "Seeds"
            val targetArr = if (isFirstHalfNew) scheduledSeeds2 else scheduledSeeds1
            val tiers = if (index < 80) newestSeedTiers else classicSeedTiers

            for (amt in tiers) {
                val seedSku = "custom.SeedPacket.$plantId.$amt"
                addStoreProduct(
                    sku = seedSku,
                    name = "SeedPacket - $plantId.$amt",
                    desc = upper,
                    shortDesc = upper,
                    objectType = "pinata",
                    objectItem = "free_seed_${plantId}1",
                    objectCount = amt
                )
                targetArr.put(makeScheduleEntry(targetCat, seedSku, if (index < 40) "new" else ""))
            }
        }

        // 3. Добавляем все 342 скина (костюма) во вкладку Costumes
        for (costumeId in 1..342) {
            val cSku = "custom.Costume.$costumeId"
            addStoreProduct(
                sku = cSku,
                name = "Costume $costumeId",
                desc = "Costume $costumeId",
                shortDesc = "Costume $costumeId",
                objectType = "costume",
                objectItem = costumeId.toString(),
                objectCount = 1
            )
            scheduledCostumes.put(makeScheduleEntry("Costumes", cSku))
        }

        // 4. Добавляем удаление валют во вкладку DeleteItems (Delet)
        val deleteDefs = listOf(
            Triple("Coin", "coin", "DeletCoin"),
            Triple("Gem", "gem", "DeletGem"),
            Triple("Mint", "mint", "DeletMint"),
            Triple("Ticket", "ticket", "DeletTicket"),
            Triple("Fuel", "fuel", "DeletFuel"),
            Triple("Sprout", "sprout", "DeletSprout")
        )
        val negativeDeleteAmounts = listOf(-1000, -10000, -100000, -1000000, -10000000, -100000000, -1000000000)
        for ((prefix, objType, delCat) in deleteDefs) {
            for (negAmt in negativeDeleteAmounts) {
                val absVal = kotlin.math.abs(negAmt)
                val delSku = "custom.Delete.$prefix.$absVal"
                addStoreProduct(
                    sku = delSku,
                    name = "Delete $negAmt $prefix",
                    desc = "Remove $negAmt $prefix",
                    shortDesc = "$negAmt $prefix",
                    objectType = objType,
                    objectItem = objType,
                    objectCount = negAmt
                )
                scheduledDeleteItems.put(makeScheduleEntry(delCat, delSku))
            }
        }

        val finalProductsJson = JSONObject().apply {
            put("version", 1)
            put("objects", productsArr)
        }

        // 5. БЕРЁМ ВСЕ ОРИГИНАЛЬНЫЕ ЗАПИСИ ИЗ MARKET_SCHEDULE.RTON (ПЕРЕВОДЯ ИХ ЦЕНУ В 1 МОНЕТУ)
        // И ДОБАВЛЯЕМ К НИМ НАШИ ВКЛАДКИ PLANTS, COSTUMES, SEEDS, SEEDS2 И DELET!
        val entriesArr = JSONArray()
        val origSchedObjects = origScheduleJson?.optJSONArray("objects")
        if (origSchedObjects != null && origSchedObjects.length() > 0) {
            val existingEntries = origSchedObjects.getJSONObject(0)
                .optJSONObject("objdata")
                ?.optJSONArray("MarketScheduleEntries")
            if (existingEntries != null) {
                for (i in 0 until existingEntries.length()) {
                    val entry = existingEntries.optJSONObject(i) ?: continue
                    val entryName = entry.optString("Name", "")
                    // Пропускаем только наши собственные кастомные блоки, если они вдруг были, а все дефолтные блоки игры сохраняем!
                    if (entryName == "CustomPlantsAll" || entryName == "CustomCostumesAll" ||
                        entryName == "Seeds" || entryName == "Seeds2" || entryName == "Delet"
                    ) continue

                    // Переводим все существующие дефолтные предложения игры в 1 монету и снимаем лимит покупок
                    val schedProds = entry.optJSONArray("ScheduledProducts")
                    if (schedProds != null) {
                        for (j in 0 until schedProds.length()) {
                            val sp = schedProds.optJSONObject(j) ?: continue
                            if (sp.optInt("SoftCost", -1) > 0) {
                                sp.put("SoftCurrency", "coins")
                                sp.put("SoftCost", 1)
                            }
                            sp.put("MaxPurchase", -1)
                            sp.put("SoftCostPerPurchase", -1)
                        }
                    }
                    entriesArr.put(entry)
                }
            }
        }

        // Дописываем наши полные списки в конец расписания:
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "CustomPlantsAll").put("ScheduledProducts", scheduledPlants))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "CustomCostumesAll").put("ScheduledProducts", scheduledCostumes))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds").put("ScheduledProducts", scheduledSeeds1))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds2").put("ScheduledProducts", scheduledSeeds2))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Delet").put("ScheduledProducts", scheduledDeleteItems))

        val finalScheduleJson = JSONObject().apply {
            put("version", 1)
            put("objects", JSONArray().put(JSONObject().apply {
                put("aliases", JSONArray().put("MainMarketSchedule"))
                put("objclass", "MarketScheduleProps")
                put("objdata", JSONObject().put("MarketScheduleEntries", entriesArr))
            }))
        }

        return finalProductsJson to finalScheduleJson
    }

    // ==================== 2. МОМЕНТАЛЬНАЯ ПЕРЕЗАРЯДКА (-999) И ОТРИЦАТЕЛЬНАЯ ЦЕНА СОЛНЦА (-500) ====================

    fun patchZeroSunAndInstantCooldown(
        cdnDir: String,
        useNegativeSunBonus: Boolean = true,
        log: (String) -> Unit
    ): Boolean {
        val targetSunValue = if (useNegativeSunBonus) -500 else 0
        val targetCooldownValue = -999

        val allCdnDirs = listAllCdnDirs().ifEmpty { listOf(cdnDir) }.toMutableSet().apply { add(cdnDir) }
        var patchedFilesCount = 0

        for (dir in allCdnDirs) {
            val candidateFiles = listOf(
                "$dir/PlantLevels.rton",
                "$dir/plantlevels.rton",
                "$dir/PlantProperties.rton",
                "$dir/plantproperties.rton"
            )
            for (filePath in candidateFiles) {
                val json = readRtonAsJson(filePath) {} ?: continue
                applyInstantAndSunPatchRecursively(json, targetSunValue, targetCooldownValue)
                if (writeJsonAsRton(filePath, json, log)) {
                    patchedFilesCount++
                }
            }
        }

        if (patchedFilesCount > 0) {
            val modeDesc = if (useNegativeSunBonus) "Цена = -500 солнца (даёт +500☀️ при посадке!)" else "Цена = 0 солнца"
            log("🌞 Успешно пропатчено файлов: $patchedFilesCount! ($modeDesc, Перезарядка = -999 / Мгновенная!)")
            return true
        } else {
            log("❌ В папке $cdnDir не найдены файлы PlantLevels.rton / PlantProperties.rton!")
            return false
        }
    }

    private fun applyInstantAndSunPatchRecursively(
        node: Any?,
        sunCostVal: Int,
        cooldownVal: Int
    ) {
        when (node) {
            is JSONObject -> {
                val statName = node.optString("Name", "")
                if (statName.equals("Cost", ignoreCase = true) ||
                    statName.equals("SunCost", ignoreCase = true)
                ) {
                    val valuesArr = node.optJSONArray("Values")
                    if (valuesArr != null) {
                        for (i in 0 until valuesArr.length()) {
                            valuesArr.put(i, sunCostVal)
                        }
                    }
                } else if (statName.equals("PacketCooldown", ignoreCase = true) ||
                    statName.equals("StartingCooldown", ignoreCase = true) ||
                    statName.equals("Cooldown", ignoreCase = true) ||
                    statName.equals("FirstPacketCooldown", ignoreCase = true)
                ) {
                    val valuesArr = node.optJSONArray("Values")
                    if (valuesArr != null) {
                        for (i in 0 until valuesArr.length()) {
                            valuesArr.put(i, cooldownVal)
                        }
                    }
                }

                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (k.equals("Cost", ignoreCase = true) || k.equals("SunCost", ignoreCase = true)) {
                        if (v is Number) node.put(k, sunCostVal)
                        if (v is JSONArray) {
                            for (i in 0 until v.length()) v.put(i, sunCostVal)
                        }
                    } else if (k.equals("PacketCooldown", ignoreCase = true) ||
                        k.equals("StartingCooldown", ignoreCase = true) ||
                        k.equals("Cooldown", ignoreCase = true) ||
                        k.equals("FirstPacketCooldown", ignoreCase = true)
                    ) {
                        if (v is Number) node.put(k, cooldownVal)
                        if (v is JSONArray) {
                            for (i in 0 until v.length()) v.put(i, cooldownVal)
                        }
                    } else {
                        applyInstantAndSunPatchRecursively(v, sunCostVal, cooldownVal)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    applyInstantAndSunPatchRecursively(node.opt(i), sunCostVal, cooldownVal)
                }
            }
        }
    }

    // ==================== 3. ПРОКАЧКА ЛУТА ПИНЬЯТ И МЕГА-НАГРАДЫ АРЕНЫ ====================

    fun patchPinataCoreLoot(cdnDir: String, multiplier: Int, minSeedsPerSlot: Int, log: (String) -> Unit): Boolean {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readRtonAsJson(path, log) ?: run {
            log("❌ Файл $path не найден!")
            return false
        }
        boostPinataLootRecursively(json, multiplier, minSeedsPerSlot)
        val ok = writeJsonAsRton(path, json, log)
        if (ok) log("🪅 Лут пиньят увеличен в x$multiplier раз!")
        return ok
    }

    private fun boostPinataLootRecursively(node: Any?, mult: Int, minSeeds: Int) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (v is Number) {
                        val lowKey = k.lowercase()
                        if (lowKey.contains("min") || lowKey.contains("max") || lowKey.contains("amount") || lowKey.contains("count")) {
                            if (!lowKey.contains("slot") && !lowKey.contains("weight") && !lowKey.contains("percent")) {
                                node.put(k, maxOf(v.toInt() * mult, minSeeds))
                            }
                        }
                    } else {
                        boostPinataLootRecursively(v, mult, minSeeds)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) boostPinataLootRecursively(node.opt(i), mult, minSeeds)
            }
        }
    }

    fun patchArenaAndCalendarMegaRewards(cdnDir: String, log: (String) -> Unit): Boolean {
        val filesToBoost = listOf(
            "joust_crownrewards.rton", "joust_progressiverewards.rton",
            "joust_season_rewards.rton", "joust_tournamentrewards.rton",
            "calendar_schedule.rton", "dailyquestdata.rton"
        )
        var count = 0
        for (fName in filesToBoost) {
            val fullPath = "$cdnDir/$fName"
            val json = readRtonAsJson(fullPath, log) ?: continue
            boostRewardsRecursively(json)
            if (writeJsonAsRton(fullPath, json, log)) count++
        }
        log("🏆 Пропатчено файлов Арены и Календаря: $count (награды x100, порог корон = 1)")
        return count > 0
    }

    private fun boostRewardsRecursively(node: Any?) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    val low = k.lowercase()
                    if (v is Number) {
                        if (low.contains("crown") || low.contains("pointsneeded") || low.contains("required")) {
                            node.put(k, 1)
                        } else if (low.contains("amount") || low.contains("count") || low.contains("value")) {
                            node.put(k, maxOf(v.toInt() * 100, 1000))
                        }
                    } else {
                        boostRewardsRecursively(v)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) boostRewardsRecursively(node.opt(i))
            }
        }
    }

    fun restoreAllCdnBackups(cdnDir: String, log: (String) -> Unit) {
        val (pkg, _) = resolveNoBackupDir {}
        CryptoAndSyncManager.stopGameForSync(pkg, true, log)
        val cmd = """
            for f in '$cdnDir'/*.bak_editor; do
                if [ -f "${'$'}f" ]; then
                    orig="${'$'}{f%.bak_editor}"
                    cp "${'$'}f" "${'$'}orig"
                fi
            done
            sync
        """.trimIndent()
        CryptoAndSyncManager.execSh(cmd, log)
        log("⏪ Оригинальные файлы восстановлены из бэкапов!")
    }
}
