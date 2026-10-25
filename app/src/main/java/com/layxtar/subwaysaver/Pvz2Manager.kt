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

    // ==================== 1. ПОЛНЫЙ МЕГА-МАГАЗИН ЗА 1 МОНЕТУ (КАК В LINHYM) ====================
    // Создаёт magento_filter.rton, market_layout.rton, pinatacoreloot.rton, products.rton и market_schedule.rton!

    fun installFullMegaShopAllPlantsSkinsAndSeeds(cdnDir: String, log: (String) -> Unit): Boolean {
        log("⏳ 1/5: Снятие фильтра магазина (magento_filter.rton) для всех 214 растений и 342 скинов...")
        val magentoJson = buildMagentoFilterJson()
        if (!writeJsonAsRton("$cdnDir/magento_filter.rton", magentoJson, log)) return false

        log("⏳ 2/5: Создание вкладок магазина (market_layout.rton: Plants, Costumes, Seeds 1-3, Upgrades)...")
        val layoutJson = buildMarketLayoutJson()
        if (!writeJsonAsRton("$cdnDir/market_layout.rton", layoutJson, log)) return false

        log("⏳ 3/5: Инъекция слотов семян в pinatacoreloot.rton для всех 214 растений...")
        patchPinataCoreLootForAllSeedPackets(cdnDir, log)

        log("⏳ 4/5: Генерация товаров в products.rton (214 растений, 342 костюма, семена 1..1M за 1 монету)...")
        val (productsJson, scheduleJson) = buildProductsAndMarketScheduleJson(cdnDir, log)
        if (!writeJsonAsRton("$cdnDir/products.rton", productsJson, log)) return false

        log("⏳ 5/5: Запись расписания витрины (market_schedule.rton)...")
        if (!writeJsonAsRton("$cdnDir/market_schedule.rton", scheduleJson, log)) return false

        log("🎉 МЕГА-МАГАЗИН УСТАНОВЛЕН! Зайди в игру -> Магазин (вкладки Plants, Costumes, Seeds, Seeds2, Seeds3, Resources)!")
        return true
    }

    // Строит magento_filter.rton со всеми 214 растениями (включая pyroak/Scorchoak, thornwhip, voltsnapdragon) и 342 костюмами
    private fun buildMagentoFilterJson(): JSONObject {
        val plantSubTypes = JSONArray()
        Pvz2Database.ALL_PLANT_IDS.forEach { plantSubTypes.put(it) }

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
            put(JSONObject().put("Type", "sprout").put("WarnEnsureGemsOnly", false))
            put(JSONObject().put("Type", "pinata").put("WarnEnsureRealMoneyOnly", false))
            put(JSONObject().put("Type", "plant").put("SubTypeFilters", plantSubTypes))
            put(JSONObject().put("Type", "gameupgrade").put("WarnEnsureRealMoneyOnly", false).put("SubTypeFilters", upgradeSubTypes))
            put(JSONObject().put("Type", "costume").put("SubTypeFilters", costumeSubTypes))
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

    // Строит market_layout.rton со специальными вкладками Plants, Costumes, Seeds, Seeds2, Seeds3, Bundles, Upgrades, Resources
    private fun buildMarketLayoutJson(): JSONObject {
        val mainClusters = JSONArray().apply {
            put("Promotions"); put("Plants"); put("Costumes")
            put("Seeds"); put("Seeds2"); put("Seeds3")
            put("Bundles"); put("Upgrades"); put("Pinatas")
        }

        val markets = JSONArray().apply {
            put(JSONObject().put("Type", "Main").put("Clusters", mainClusters))
            put(JSONObject().put("Type", "CoinQuickStore").put("Clusters", JSONArray().put("CoinQuickStore")).put("QuickStoreInnerHeader", "[STORE_COINS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "GemQuickStore").put("Clusters", JSONArray().put("GemQuickStore")).put("QuickStoreInnerHeader", "[STORE_GEMS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "SproutQuickStore").put("Clusters", JSONArray().put("SproutQuickStore")).put("QuickStoreInnerHeader", "[STORE_SPROUTS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "GauntletQuickStore").put("Clusters", JSONArray().put("GauntletQuickStore")).put("QuickStoreInnerHeader", "[STORE_TICKETS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "FuelQuickStore").put("Clusters", JSONArray().put("FuelQuickStore")).put("QuickStoreInnerHeader", ""))
            put(JSONObject().put("Type", "MintQuickStore").put("Clusters", JSONArray().put("MintQuickStore")).put("QuickStoreInnerHeader", ""))
        }

        val clusters = JSONArray().apply {
            put(JSONObject().put("Type", "Promotions").put("Tabs", JSONArray().put("Promoted Offers").put("Resources")))
            put(JSONObject().put("Type", "Plants").put("Tabs", JSONArray().put("Plants")))
            put(JSONObject().put("Type", "Costumes").put("Tabs", JSONArray().put("Costumes")))
            put(JSONObject().put("Type", "Seeds").put("Tabs", JSONArray().put("Seeds")))
            put(JSONObject().put("Type", "Seeds2").put("Tabs", JSONArray().put("Seeds2")))
            put(JSONObject().put("Type", "Seeds3").put("Tabs", JSONArray().put("Seeds3")))
            put(JSONObject().put("Type", "Bundles").put("Tabs", JSONArray().put("Bundles")))
            put(JSONObject().put("Type", "Upgrades").put("Tabs", JSONArray().put("Upgrades")))
            put(JSONObject().put("Type", "Pinatas").put("Tabs", JSONArray().put("Pinatas")))
            put(JSONObject().put("Type", "CoinQuickStore").put("Tabs", JSONArray().put("CoinQuickStore")))
            put(JSONObject().put("Type", "GemQuickStore").put("Tabs", JSONArray().put("GemQuickStore")))
            put(JSONObject().put("Type", "SproutQuickStore").put("Tabs", JSONArray().put("SproutQuickStore")))
            put(JSONObject().put("Type", "GauntletQuickStore").put("Tabs", JSONArray().put("GauntletQuickStore")))
            put(JSONObject().put("Type", "FuelQuickStore").put("Tabs", JSONArray().put("FuelQuickStore")))
            put(JSONObject().put("Type", "MintQuickStore").put("Tabs", JSONArray().put("MintQuickStore")))
        }

        val tabs = JSONArray().apply {
            put(JSONObject().put("Type", "Promoted Offers").put("Name", "Promoted").put("ImageDown", "IMAGE_UI_STORE_GEMSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_GEMSTAB_ACTIVE").put("Categories", JSONArray().put("Promoted Offers")))
            put(JSONObject().put("Type", "Resources").put("Name", "Resources").put("ImageDown", "IMAGE_UI_STORE_COINSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_COINSTAB_ACTIVE").put("Categories", JSONArray().put("Coins").put("Gems").put("Tickets").put("Fuel").put("Sprouts").put("Mint")))
            put(JSONObject().put("Type", "Plants").put("Name", "Plants").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Plants")))
            put(JSONObject().put("Type", "Costumes").put("Name", "Costumes").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Costumes")))
            put(JSONObject().put("Type", "Seeds").put("Name", "Seeds 1").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds")))
            put(JSONObject().put("Type", "Seeds2").put("Name", "Seeds 2 (New)").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds2")))
            put(JSONObject().put("Type", "Seeds3").put("Name", "Seeds 3").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds3")))
            put(JSONObject().put("Type", "Bundles").put("Name", "Bundles").put("ImageDown", "IMAGE_UI_STORE_BUNDLESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_BUNDLESTAB_ACTIVE").put("Categories", JSONArray().put("Bundles")))
            put(JSONObject().put("Type", "Upgrades").put("Name", "Upgrades").put("ImageDown", "IMAGE_UI_STORE_UPGRADESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_UPGRADESTAB_ACTIVE").put("Categories", JSONArray().put("Upgrades")))
            put(JSONObject().put("Type", "Pinatas").put("Name", "Piñatas").put("ImageDown", "IMAGE_UI_STORE_PINATASTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PINATASTAB_ACTIVE").put("Categories", JSONArray().put("Pinatas")))
            put(JSONObject().put("Type", "CoinQuickStore").put("Name", "Coins").put("ImageDown", "IMAGE_UI_STORE_COINSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_COINSTAB_ACTIVE").put("Categories", JSONArray().put("Coins")))
            put(JSONObject().put("Type", "GemQuickStore").put("Name", "Gems").put("ImageDown", "IMAGE_UI_STORE_GEMSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_GEMSTAB_ACTIVE").put("Categories", JSONArray().put("Gems")))
            put(JSONObject().put("Type", "SproutQuickStore").put("Name", "Sprouts").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Sprouts")))
            put(JSONObject().put("Type", "GauntletQuickStore").put("Name", "Tickets").put("ImageDown", "IMAGE_UI_STORE_UPGRADESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_UPGRADESTAB_ACTIVE").put("Categories", JSONArray().put("Tickets")))
            put(JSONObject().put("Type", "FuelQuickStore").put("Name", "Fuel").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Fuel")))
            put(JSONObject().put("Type", "MintQuickStore").put("Name", "Mints").put("ImageDown", "IMAGE_UI_STORE_COINSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_COINSTAB_ACTIVE").put("Categories", JSONArray().put("Mint")))
        }

        val categories = JSONArray().apply {
            put(JSONObject().put("Type", "Promoted Offers").put("LabelText", "Shop 1 Coin").put("MaxCount", -1).put("PromotionSlots", JSONArray().put("Slot1").put("Slot2")))
            put(JSONObject().put("Type", "Coins").put("LabelText", "Coins").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_COIN_CARD"))
            put(JSONObject().put("Type", "Gems").put("LabelText", "Gems").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "Tickets").put("LabelText", "Tickets").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "Fuel").put("LabelText", "Fuel").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Sprouts").put("LabelText", "Sprouts").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Mint").put("LabelText", "Mints").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PINATA_CARD"))
            put(JSONObject().put("Type", "Plants").put("LabelText", "All 214 Plants (1 Coin)").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Costumes").put("LabelText", "All 342 Costumes (1 Coin)").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Seeds").put("LabelText", "Seed Packets (Part 1)").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Seeds2").put("LabelText", "Seed Packets (New Plants)").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Seeds3").put("LabelText", "Seed Packets (Part 3)").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Bundles").put("LabelText", "Mega Unlock Bundles").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_BUNDLE_CARD"))
            put(JSONObject().put("Type", "Upgrades").put("LabelText", "Game Upgrades").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "Pinatas").put("LabelText", "Piñatas").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
        }

        val promoSlots = JSONArray().apply {
            put(JSONObject().put("Type", "Slot1").put("GroupPriority", JSONArray().put("AllPlantsPremium")).put("FallbackCategory", "Bundles"))
            put(JSONObject().put("Type", "Slot2").put("GroupPriority", JSONArray().put("Shop1CoinUpdate")).put("FallbackCategory", "Plants"))
        }

        val layoutObj = JSONObject().apply {
            put("objclass", "MarketLayoutProps")
            put("aliases", JSONArray().put("MarketLayoutProps"))
            put("objdata", JSONObject().apply {
                put("Markets", markets)
                put("Clusters", clusters)
                put("Tabs", tabs)
                put("Categories", categories)
                put("PromotionSlots", promoSlots)
            })
        }

        return JSONObject().apply {
            put("version", 1)
            put("objects", JSONArray().put(layoutObj))
        }
    }

    // Прописывает в pinatacoreloot.rton слоты "free_linhym_<plantId>1" и таблицы "linhym_<plantId>1" для выдачи семян!
    private fun patchPinataCoreLootForAllSeedPackets(cdnDir: String, log: (String) -> Unit) {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readRtonAsJson(path, log) ?: return
        val objects = json.optJSONArray("objects") ?: return

        var slotConfigsObj: JSONObject? = null
        var rewardTablesObj: JSONObject? = null

        for (i in 0 until objects.length()) {
            val obj = objects.getJSONObject(i)
            val data = obj.optJSONObject("objdata") ?: continue
            if (data.has("SlotConfigs")) slotConfigsObj = data.optJSONObject("SlotConfigs")
            if (data.has("RewardsTables")) rewardTablesObj = data.optJSONObject("RewardsTables")
            if (data.has("RewardTables")) rewardTablesObj = data.optJSONObject("RewardTables")
        }

        if (slotConfigsObj != null) {
            for (plantId in Pvz2Database.ALL_PLANT_IDS) {
                val slotKey = "free_linhym_${plantId}1"
                val tableKey = "linhym_${plantId}1"
                slotConfigsObj.put(slotKey, JSONObject().apply {
                    put("BonusTable", "bonus_standard")
                    put("PercentBonusAward", 0)
                    put("PercentPinataAward", 1)
                    put("PinataType", "")
                    put("RewardsTable", tableKey)
                    put("Slots", JSONArray().put("PlantXP"))
                })

                rewardTablesObj?.put(tableKey, JSONObject().apply {
                    put("Rewards", JSONArray().put(JSONObject().apply {
                        put("Amount", 1)
                        put("Plant", plantId)
                        put("Type", "PlantXP")
                        put("Weight", 100)
                    }))
                })
            }
        }

        writeJsonAsRton(path, json, log)
    }

    // Генерирует products.rton и market_schedule.rton по формату LinhYM (StoreProductProps + ScheduledProducts)
    private fun buildProductsAndMarketScheduleJson(
        cdnDir: String,
        log: (String) -> Unit
    ): Pair<JSONObject, JSONObject> {
        // Сохраняем оригинальные системные продукты из текущего products.rton, чтобы не сломать внутренние ссылки
        val origProductsJson = readRtonAsJson("$cdnDir/products.rton", log)
        val productsArr = JSONArray()
        val addedSkus = mutableSetOf<String>()

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
        val scheduledSeeds3 = JSONArray()
        val scheduledUpgrades = JSONArray()
        val scheduledResources = JSONArray()
        val scheduledBundles = JSONArray()

        fun makeScheduleEntry(category: String, sku: String): JSONObject {
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
                put("Visuals", "RTID(0)")
                put("Sku", JSONArray().put(sku).put(sku))
            }
        }

        // 1. Все 214 Растений (и Мега-Бандл на открытие всех растений разом за 1 монету)
        val allPlantSkusForBundle = JSONArray()
        Pvz2Database.ALL_PLANT_IDS.forEachIndexed { index, plantId ->
            val upper = plantId.uppercase()
            val plantSku = "linhym.Plant.$plantId"
            allPlantSkusForBundle.put(plantSku)

            addStoreProduct(
                sku = plantSku,
                name = "Plant - $plantId",
                desc = upper,
                shortDesc = upper,
                objectType = "plant",
                objectItem = plantId,
                objectCount = 1
            )
            scheduledPlants.put(makeScheduleEntry("Plants", plantSku))

            // 2. Наборы семян (1, 100, 10000, 100000, 1000000) для каждого растения!
            // Новейшие 75 растений идут во вкладку Seeds2, средние в Seeds3, классические в Seeds
            val targetSeedCat = when {
                index < 75 -> "Seeds2"
                index < 145 -> "Seeds3"
                else -> "Seeds"
            }
            val targetSeedArr = when (targetSeedCat) {
                "Seeds2" -> scheduledSeeds2
                "Seeds3" -> scheduledSeeds3
                else -> scheduledSeeds1
            }

            for (amt in Pvz2Database.SEED_PACK_AMOUNTS) {
                val seedSku = "linhym.SeedPacket.$plantId.$amt"
                addStoreProduct(
                    sku = seedSku,
                    name = "Seed - $plantId x$amt",
                    desc = "$amt Seed Packets for $upper",
                    shortDesc = "$upper x$amt",
                    objectType = "pinata",
                    objectItem = "free_linhym_${plantId}1",
                    objectCount = amt
                )
                targetSeedArr.put(makeScheduleEntry(targetSeedCat, seedSku))
            }
        }

        // Мега-Бандл: Разблокировать все 214 растений в 1 клик за 1 монету
        val totalBundleSku = "linhym.Bundle.Total"
        addStoreProduct(
            sku = totalBundleSku,
            name = "Bundle - All 214 Plants",
            desc = "Unlock All 214 Plants (Including Scorchoak, Thornwhip, Volt Snapdragon)!",
            shortDesc = "All 214 Plants Bundle",
            objectType = "bundle",
            objectItem = "greenthumb",
            objectCount = 1,
            bundleProps = allPlantSkusForBundle
        )
        scheduledBundles.put(makeScheduleEntry("Bundles", totalBundleSku))

        // 3. Все 342 Костюма (Скина) во вкладку Costumes за 1 монету
        for (costumeId in 1..342) {
            val cSku = "linhym.Costume.$costumeId"
            addStoreProduct(
                sku = cSku,
                name = "Costume $costumeId",
                desc = "Costume #$costumeId",
                shortDesc = "Costume $costumeId",
                objectType = "costume",
                objectItem = costumeId.toString(),
                objectCount = 1
            )
            scheduledCostumes.put(makeScheduleEntry("Costumes", cSku))
        }

        // 4. Все 40 Игровых Апгрейдов во вкладку Upgrades
        for (upgId in Pvz2Database.GAME_UPGRADES) {
            val uSku = "linhym.Upgrade.$upgId"
            addStoreProduct(
                sku = uSku,
                name = "Upgrade - $upgId",
                desc = upgId.uppercase(),
                shortDesc = upgId,
                objectType = "gameupgrade",
                objectItem = upgId,
                objectCount = 1
            )
            scheduledUpgrades.put(makeScheduleEntry("Upgrades", uSku))
        }

        // 5. Ресурсы (Монеты, Алмазы, Мяты, Перчатки, Топливо, Ростки) по 1 000 .. 100 000 000 за 1 монету
        val resourceDefs = listOf(
            Triple("Coin", "coin", "Coins"),
            Triple("Gem", "gem", "Gems"),
            Triple("Mint", "mint", "Mint"),
            Triple("Ticket", "ticket", "Tickets"),
            Triple("Fuel", "fuel", "Fuel"),
            Triple("Sprout", "sprout", "Sprouts")
        )
        val resourceAmounts = listOf(1000, 10000, 100000, 1000000, 10000000)
        for ((prefix, objType, catName) in resourceDefs) {
            for (amt in resourceAmounts) {
                val rSku = "linhym.$prefix.$amt"
                addStoreProduct(
                    sku = rSku,
                    name = "$prefix $amt",
                    desc = "$amt $catName!",
                    shortDesc = "$catName x$amt",
                    objectType = objType,
                    objectItem = objType,
                    objectCount = amt
                )
                scheduledResources.put(makeScheduleEntry(catName, rSku))
            }
        }

        // Переносим оригинальные системные продукты (сделав их по 1 монете), чтобы не было пустых ссылок
        val origArr = origProductsJson?.optJSONArray("objects")
        if (origArr != null) {
            for (i in 0 until origArr.length()) {
                val obj = origArr.getJSONObject(i)
                val data = obj.optJSONObject("objdata")
                val sku = data?.optString("Sku") ?: ""
                if (sku.isNotEmpty() && addedSkus.add(sku)) {
                    data.put("PriceCoins", "1")
                    productsArr.put(obj)
                }
            }
        }

        val finalProductsJson = JSONObject().apply {
            put("version", 1)
            put("objects", productsArr)
        }

        val entriesArr = JSONArray().apply {
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Plants").put("ScheduledProducts", scheduledPlants))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds2").put("ScheduledProducts", scheduledSeeds2))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds3").put("ScheduledProducts", scheduledSeeds3))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds").put("ScheduledProducts", scheduledSeeds1))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Costumes").put("ScheduledProducts", scheduledCostumes))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Bundles").put("ScheduledProducts", scheduledBundles))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Upgrades").put("ScheduledProducts", scheduledUpgrades))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Resources").put("ScheduledProducts", scheduledResources))
        }

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

    // ==================== 2. ИСПРАВЛЕННЫЙ МОД: 0 СОЛНЦА И МГНОВЕННАЯ ПЕРЕЗАРЯДКА ====================
    // Обнуляет как базовые поля Cost/PacketCooldown в PlantProperties.rton, так и массивы FloatStats в PlantLevels.rton!

    fun patchZeroSunAndInstantCooldown(cdnDir: String, log: (String) -> Unit): Boolean {
        val propsPath = "$cdnDir/PlantProperties.rton"
        val levelsPath = "$cdnDir/PlantLevels.rton"
        var anyPatched = false

        readRtonAsJson(propsPath, log)?.let { json ->
            zeroSunAndCooldownRecursively(json)
            if (writeJsonAsRton(propsPath, json, log)) anyPatched = true
        }
        readRtonAsJson(levelsPath, log)?.let { json ->
            zeroSunAndCooldownRecursively(json)
            if (writeJsonAsRton(levelsPath, json, log)) anyPatched = true
        }
        if (anyPatched) {
            log("🌞 Мод успешно применён (FloatStats + BaseProps): все растения стоят 0 солнца и перезаряжаются за 0 сек на любом уровне!")
        }
        return anyPatched
    }

    private fun zeroSunAndCooldownRecursively(node: Any?) {
        when (node) {
            is JSONObject -> {
                // 1. Проверяем структуру FloatStats из PlantLevels.rton: {"Name": "Cost" / "PacketCooldown" / "StartingCooldown", "Values": [...]}
                val statName = node.optString("Name", "")
                if (statName.equals("Cost", ignoreCase = true) ||
                    statName.equals("SunCost", ignoreCase = true) ||
                    statName.equals("PacketCooldown", ignoreCase = true) ||
                    statName.equals("StartingCooldown", ignoreCase = true) ||
                    statName.equals("Cooldown", ignoreCase = true)
                ) {
                    val valuesArr = node.optJSONArray("Values")
                    if (valuesArr != null) {
                        for (i in 0 until valuesArr.length()) {
                            valuesArr.put(i, 0)
                        }
                    }
                }

                // 2. Проверяем прямые поля в PlantProperties.rton
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (k.equals("Cost", ignoreCase = true) ||
                        k.equals("SunCost", ignoreCase = true) ||
                        k.equals("PacketCooldown", ignoreCase = true) ||
                        k.equals("StartingCooldown", ignoreCase = true) ||
                        k.equals("Cooldown", ignoreCase = true)
                    ) {
                        if (v is Number) node.put(k, 0)
                        if (v is JSONArray) {
                            for (i in 0 until v.length()) v.put(i, 0)
                        }
                    } else {
                        zeroSunAndCooldownRecursively(v)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    zeroSunAndCooldownRecursively(node.opt(i))
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
