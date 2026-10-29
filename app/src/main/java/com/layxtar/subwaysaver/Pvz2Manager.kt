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

    // ==================== 1. ПОЛНЫЙ МАГАЗИН ЗА 1 МОНЕТУ (ТОЧНАЯ КОПИЯ МЕТОДА ИЗ ФАЙЛОВ + НОВЫЕ РАСТЕНИЯ + УДАЛЕНИЕ ВАЛЮТ) ====================

    fun installFullMegaShopAllPlantsSkinsAndSeeds(cdnDir: String, log: (String) -> Unit): Boolean {
        log("⏳ 1/5: Запись фильтра магазина magento_filter.rton (все 214 растений и 342 костюма)...")
        val magentoJson = buildMagentoFilterJson()
        if (!writeJsonAsRton("$cdnDir/magento_filter.rton", magentoJson, log)) return false

        log("⏳ 2/5: Запись макета вкладок market_layout.rton (включая DeleteItems, Seeds 1-3, Plants, Costumes)...")
        val layoutJson = buildMarketLayoutJson()
        if (!writeJsonAsRton("$cdnDir/market_layout.rton", layoutJson, log)) return false

        log("⏳ 3/5: Прошивка слотов и таблиц лута семян в pinatacoreloot.rton (включая новые растения и All Plants XP)...")
        patchPinataCoreLootExactMethod(cdnDir, log)

        log("⏳ 4/5: Генерация товаров в products.rton (сохранены существующие товары по 1🪙 + семена, скины, удаление валют и бонусы)...")
        val (productsJson, scheduleJson) = buildProductsAndMarketScheduleExactMethod(cdnDir, log)
        if (!writeJsonAsRton("$cdnDir/products.rton", productsJson, log)) return false

        log("⏳ 5/5: Запись расписания витрины market_schedule.rton...")
        if (!writeJsonAsRton("$cdnDir/market_schedule.rton", scheduleJson, log)) return false

        log("🎉 Магазин успешно установлен! Проверяй вкладки Plants, Costumes, Seeds / Seeds2 / Seeds3, DeleteItems и Piñatas!")
        return true
    }

    // 1. Точный magento_filter.rton из твоего файла magento_filter_2.json
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

    // 2. Точный market_layout.rton из твоего файла market_layout_2.json (со вкладкой Delet / DeleteItems и без чужих имён)
    private fun buildMarketLayoutJson(): JSONObject {
        val mainClusters = JSONArray().apply {
            put("Promotions")
            put("Costumes")
            put("Upgrades")
            put("Plants")
            put("Bundles")
            put("Delet")
            put("Seeds")
            put("Seeds2")
            put("Seeds3")
            put("Pinatas")
        }

        val markets = JSONArray().apply {
            put(JSONObject().put("Type", "Main").put("Clusters", mainClusters))
            put(JSONObject().put("Type", "Tutorial").put("Clusters", JSONArray().put("Tutorial")))
            put(JSONObject().put("Type", "Tutorial50MB").put("Clusters", JSONArray().put("Tutorial50MB")))
            put(JSONObject().put("Type", "CoinQuickStore").put("Clusters", JSONArray().put("CoinQuickStore")).put("QuickStoreInnerHeader", "[STORE_COINS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "GemQuickStore").put("Clusters", JSONArray().put("GemQuickStore")).put("QuickStoreInnerHeader", "[STORE_GEMS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "SproutQuickStore").put("Clusters", JSONArray().put("SproutQuickStore")).put("QuickStoreInnerHeader", "[STORE_SPROUTS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "GauntletQuickStore").put("Clusters", JSONArray().put("GauntletQuickStore")).put("QuickStoreInnerHeader", "[STORE_TICKETS_CURRENT_PROFILE]"))
            put(JSONObject().put("Type", "FuelQuickStore").put("Clusters", JSONArray().put("FuelQuickStore")).put("QuickStoreInnerHeader", ""))
            put(JSONObject().put("Type", "MintQuickStore").put("Clusters", JSONArray().put("MintQuickStore")).put("QuickStoreInnerHeader", ""))
        }

        val clusters = JSONArray().apply {
            put(JSONObject().put("Type", "Promotions").put("Tabs", JSONArray().put("Promoted Offers").put("Resources")))
            put(JSONObject().put("Type", "Pinatas").put("Tabs", JSONArray().put("Pinatas")))
            put(JSONObject().put("Type", "Bundles").put("Tabs", JSONArray().put("Bundles")))
            put(JSONObject().put("Type", "Plants").put("Tabs", JSONArray().put("Plants")))
            put(JSONObject().put("Type", "Costumes").put("Tabs", JSONArray().put("Costumes")))
            put(JSONObject().put("Type", "Seeds").put("Tabs", JSONArray().put("Seeds")))
            put(JSONObject().put("Type", "Seeds2").put("Tabs", JSONArray().put("Seeds2")))
            put(JSONObject().put("Type", "Seeds3").put("Tabs", JSONArray().put("Seeds3")))
            put(JSONObject().put("Type", "Upgrades").put("Tabs", JSONArray().put("Upgrades")))
            put(JSONObject().put("Type", "Delet").put("Tabs", JSONArray().put("Delet")))
            put(JSONObject().put("Type", "Tutorial").put("Tabs", JSONArray().put("Tutorial")))
            put(JSONObject().put("Type", "Tutorial50MB").put("Tabs", JSONArray().put("Tutorial50MB")))
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
            put(JSONObject().put("Type", "Pinatas").put("Name", "Piñatas").put("ImageDown", "IMAGE_UI_STORE_PINATASTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PINATASTAB_ACTIVE").put("Categories", JSONArray().put("Pinatas")))
            put(JSONObject().put("Type", "Bundles").put("Name", "Bundles").put("ImageDown", "IMAGE_UI_STORE_BUNDLESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_BUNDLESTAB_ACTIVE").put("Categories", JSONArray().put("Bundles")))
            put(JSONObject().put("Type", "Plants").put("Name", "Plants").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Plants")))
            put(JSONObject().put("Type", "Costumes").put("Name", "Costumes").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Costumes")))
            put(JSONObject().put("Type", "Seeds").put("Name", "Seeds").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds")))
            put(JSONObject().put("Type", "Seeds2").put("Name", "Seeds2").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds2")))
            put(JSONObject().put("Type", "Seeds3").put("Name", "Seeds3").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds3")))
            put(JSONObject().put("Type", "Upgrades").put("Name", "Upgrades").put("ImageDown", "IMAGE_UI_STORE_UPGRADESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_UPGRADESTAB_ACTIVE").put("Categories", JSONArray().put("Upgrades")))
            put(JSONObject().put("Type", "Delet").put("Name", "DeleteItems").put("ImageDown", "IMAGE_UI_STORE_GEMSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_GEMSTAB_ACTIVE").put("Categories", JSONArray().put("DeletCoin").put("DeletGem").put("DeletTicket").put("DeletFuel").put("DeletSprout").put("DeletMint")))
            put(JSONObject().put("Type", "Tutorial").put("Name", "Tutorial").put("ImageDown", "IMAGE_UI_STORE_PINATASTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Tutorial")))
            put(JSONObject().put("Type", "Tutorial50MB").put("Name", "Tutorial50MB").put("ImageDown", "IMAGE_UI_STORE_PINATASTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Tutorial50MB")))
            put(JSONObject().put("Type", "CoinQuickStore").put("Name", "Coins").put("ImageDown", "IMAGE_UI_STORE_COINSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_COINSTAB_ACTIVE").put("Categories", JSONArray().put("QuickCoins")))
            put(JSONObject().put("Type", "GemQuickStore").put("Name", "Gems").put("ImageDown", "IMAGE_UI_STORE_GEMSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_GEMSTAB_ACTIVE").put("Categories", JSONArray().put("QuickGems")))
            put(JSONObject().put("Type", "SproutQuickStore").put("Name", "Sprouts").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("QuickSprouts")))
            put(JSONObject().put("Type", "GauntletQuickStore").put("Name", "Tickets").put("ImageDown", "IMAGE_UI_STORE_UPGRADESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_UPGRADESTAB_ACTIVE").put("Categories", JSONArray().put("QuickGauntlets")))
            put(JSONObject().put("Type", "FuelQuickStore").put("Name", "Fuel").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("QuickFuel")))
            put(JSONObject().put("Type", "MintQuickStore").put("Name", "Mints").put("ImageDown", "IMAGE_UI_STORE_COINSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_COINSTAB_ACTIVE").put("Categories", JSONArray().put("Coins")))
        }

        val categories = JSONArray().apply {
            put(JSONObject().put("Type", "Promoted Offers").put("LabelText", "Shop 1 Coin").put("MaxCount", -1).put("PromotionSlots", JSONArray().put("Slot1").put("Slot2").put("Slot3").put("Slot4").put("Slot5").put("Slot6").put("Slot7")))
            put(JSONObject().put("Type", "Coins").put("LabelText", "Coins").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_COIN_CARD"))
            put(JSONObject().put("Type", "Gems").put("LabelText", "Gems").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "Tickets").put("LabelText", "Tickets").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "Fuel").put("LabelText", "Fuel").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Sprouts").put("LabelText", "Sprouts").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Mint").put("LabelText", "Mints").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PINATA_CARD"))
            put(JSONObject().put("Type", "Pinatas").put("LabelText", "Piñatas").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "Bundles").put("LabelText", "Bundles").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_BUNDLE_CARD"))
            put(JSONObject().put("Type", "Plants").put("LabelText", "Plants").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Costumes").put("LabelText", "Costumes").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Seeds").put("LabelText", "Seed Packets").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Seeds2").put("LabelText", "Seed Packets (New)").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Seeds3").put("LabelText", "Seed Packets").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Upgrades").put("LabelText", "Upgrades").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "DeletCoin").put("LabelText", "Delete Coins").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_COIN_CARD"))
            put(JSONObject().put("Type", "DeletGem").put("LabelText", "Delete Gems").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "DeletTicket").put("LabelText", "Delete Tickets").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "DeletFuel").put("LabelText", "Delete Fuels").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "DeletSprout").put("LabelText", "Delete Sprouts").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "DeletMint").put("LabelText", "Delete Mints").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PINATA_CARD"))
            put(JSONObject().put("Type", "Tutorial").put("LabelText", "Tutorial").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Tutorial50MB").put("LabelText", "Tutorial50MB").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "QuickCoins").put("LabelText", "Coins").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_COIN_CARD"))
            put(JSONObject().put("Type", "QuickGems").put("LabelText", "Gems").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "QuickGauntlets").put("LabelText", "Gauntlets").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "QuickFuel").put("LabelText", "Fuel").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "QuickSprouts").put("LabelText", "Sprouts").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
        }

        val promoSlots = JSONArray().apply {
            put(JSONObject().put("Type", "Slot1").put("GroupPriority", JSONArray().put("Shop1CoinPreview")).put("FallbackCategory", "Resources"))
            put(JSONObject().put("Type", "Slot2").put("GroupPriority", JSONArray().put("Shop1CoinUpdate")).put("FallbackCategory", "Bundles"))
            put(JSONObject().put("Type", "Slot3").put("GroupPriority", JSONArray().put("AllPlantsPremium")).put("FallbackCategory", "Bundles"))
            put(JSONObject().put("Type", "Slot4").put("GroupPriority", JSONArray().put("100MillionSeedPacketsAllPlants")).put("FallbackCategory", "Bundles"))
            put(JSONObject().put("Type", "Slot5").put("GroupPriority", JSONArray().put("UnlockShopChinesePlants")).put("FallbackCategory", "Bundles"))
            put(JSONObject().put("Type", "Slot6").put("GroupPriority", JSONArray().put("NewSeeds")).put("FallbackCategory", "Bundles"))
            put(JSONObject().put("Type", "Slot7").put("GroupPriority", JSONArray().put("NoAdsPacks")).put("FallbackCategory", "Bundles"))
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

    // 3. ТОЧНЫЙ МЕТОД СЕМЯН ИЗ PINATACORELOOT_2.JSON + АВТО-КЛОНИРОВАНИЕ СТРУКТУРЫ ТАБЛИЦ ДЛЯ НОВЫХ РАСТЕНИЙ И МЕГА-ПИНЬЯТ!
    private fun patchPinataCoreLootExactMethod(cdnDir: String, log: (String) -> Unit) {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readRtonAsJson(path, log) ?: return
        val objects = json.optJSONArray("objects") ?: return

        var slotConfigsObj: JSONObject? = null
        var tablesParentData: JSONObject? = null

        for (i in 0 until objects.length()) {
            val obj = objects.getJSONObject(i)
            val data = obj.optJSONObject("objdata") ?: continue
            if (data.has("SlotConfigs")) {
                slotConfigsObj = data.optJSONObject("SlotConfigs")
            } else if (obj.optString("objclass").contains("Table", ignoreCase = true) ||
                data.has("Tables") || data.has("RewardsTables") || data.has("RewardTables") || data.has("RewardSubTables")
            ) {
                tablesParentData = data
            }
        }

        if (slotConfigsObj != null) {
            // 1. Создаём слоты "free_seed_<plantId>1" для всех 214 растений (точно как в pinatacoreloot_2.json!)
            for (plantId in Pvz2Database.ALL_PLANT_IDS) {
                val slotKey = "free_seed_${plantId}1"
                val tableKey = "seed_${plantId}1"

                slotConfigsObj.put(slotKey, JSONObject().apply {
                    put("BonusTable", "bonus_standard")
                    put("PercentBonusAward", 0)
                    put("PercentPinataAward", 1)
                    put("PinataType", "free")
                    put("RewardsTable", tableKey)
                    put("Slots", JSONArray().put("PlantXP"))
                })
            }

            // 2. Создаём 5 супер-слотов прокачки ВСЕХ растений сразу (Bronze, Silver, Gold, Max, 100M) как в pinatacoreloot_2.json!
            val multiSlots = JSONArray().apply {
                repeat(Pvz2Database.ALL_PLANT_IDS.size) { put("PlantXP") }
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

        // 3. Автоматически определяем точный формат таблиц наград в текущем pinatacoreloot.rton (на примере "peashooter1" / "akee1")
        // и создаём по этому же шаблону таблицы "seed_<plantId>1" для ВСЕХ 214 растений (включая новые voltsnapdragon, thornwhip, pyroak)!
        if (tablesParentData != null) {
            val tableDictKey = tablesParentData.keys().asSequence().firstOrNull { k ->
                tablesParentData.opt(k) is JSONObject || tablesParentData.opt(k) is JSONArray
            }

            if (tableDictKey != null) {
                val container = tablesParentData.opt(tableDictKey)
                if (container is JSONObject) {
                    // Находим существующий рабочий шаблон одиночного семени (например, "peashooter1" или "akee1")
                    val templateTable = container.optJSONObject("peashooter1")
                        ?: container.optJSONObject("akee1")
                        ?: container.optJSONObject("sunflower50")

                    for (plantId in Pvz2Database.ALL_PLANT_IDS) {
                        val tableKey = "seed_${plantId}1"
                        if (templateTable != null) {
                            // Клонируем родную структуру игры и заменяем в ней имя растения и количество = 1!
                            val cloned = JSONObject(templateTable.toString())
                            val rewardsArr = cloned.optJSONArray("Rewards") ?: cloned.optJSONArray("SubTables")
                            if (rewardsArr != null && rewardsArr.length() > 0) {
                                val firstReward = rewardsArr.optJSONObject(0)
                                if (firstReward != null) {
                                    if (firstReward.has("Entities")) firstReward.put("Entities", JSONArray().put(plantId))
                                    if (firstReward.has("Plant")) firstReward.put("Plant", plantId)
                                    if (firstReward.has("Item")) firstReward.put("Item", plantId)
                                    if (firstReward.has("Min")) firstReward.put("Min", 1)
                                    if (firstReward.has("Max")) firstReward.put("Max", 1)
                                    if (firstReward.has("Amount")) firstReward.put("Amount", 1)
                                }
                            }
                            container.put(tableKey, cloned)
                        } else {
                            container.put(tableKey, JSONObject().apply {
                                put("Rewards", JSONArray().put(JSONObject().apply {
                                    put("Entities", JSONArray().put(plantId))
                                    put("Plant", plantId)
                                    put("Min", 1)
                                    put("Max", 1)
                                    put("Amount", 1)
                                    put("Type", "PlantXP")
                                    put("Weight", 100)
                                }))
                            })
                        }
                    }

                    // Создаём таблицы для 5 мега-пиньят (Bronze=100, Silver=1000, Gold=5000, Max=50000, 100M=100000000 семян на каждое растение!)
                    val megaTiers = listOf(
                        "all_xp_plants_bronze_0" to 100,
                        "all_xp_plants_silver_0" to 1000,
                        "all_xp_plants_gold_0" to 5000,
                        "all_xp_plants_max_0" to 50000,
                        "all_xp_plants_100000000_0" to 100000000
                    )
                    for ((tKey, amt) in megaTiers) {
                        val allRewards = JSONArray()
                        for (plantId in Pvz2Database.ALL_PLANT_IDS) {
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
                        container.put(tKey, JSONObject().put("Rewards", allRewards))
                    }
                }
            }
        }

        writeJsonAsRton(path, json, log)
    }

    // 4. Сборка products.rton и market_schedule.rton:
    // - делает все существующие товары игры по 1 монете
    // - добавляет все 214 растений, 342 скина, семена по методу из твоих файлов, удаление валют и крутые бонусы!
    private fun buildProductsAndMarketScheduleExactMethod(
        cdnDir: String,
        log: (String) -> Unit
    ): Pair<JSONObject, JSONObject> {
        val origProductsJson = readRtonAsJson("$cdnDir/products.rton", log)
        val productsArr = JSONArray()
        val addedSkus = mutableSetOf<String>()

        // Сначала переносим существующие продукты игры и ставим им цену 1 монета (как в нашем первом удачном магазине!)
        val origObjects = origProductsJson?.optJSONArray("objects")
        if (origObjects != null) {
            for (i in 0 until origObjects.length()) {
                val obj = origObjects.getJSONObject(i)
                val data = obj.optJSONObject("objdata")
                if (data != null) {
                    val sku = data.optString("Sku", "")
                    if (sku.isNotEmpty() && addedSkus.add(sku)) {
                        data.put("PriceCoins", "1")
                        data.put("PriceGems", 0)
                        data.put("PriceMints", 0)
                        data.put("Price", 0)
                        data.put("IsFree", false)
                        data.put("IsAdPlacement", false)
                        productsArr.put(obj)
                    }
                }
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
        val scheduledSeeds3 = JSONArray()
        val scheduledUpgrades = JSONArray()
        val scheduledResources = JSONArray()
        val scheduledDeleteItems = JSONArray()
        val scheduledBundles = JSONArray()
        val scheduledPinatas = JSONArray()

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

        // 1. Все 214 Растений + их Семена точно по методу из твоих файлов (ObjectType="pinata", ObjectItem="free_seed_<plantId>1")
        val allPlantSkus = JSONArray()
        val classicSeedTiers = listOf(1, 100, 10000)
        val newestSeedTiers = listOf(1, 100, 10000, 100000, 1000000)

        Pvz2Database.ALL_PLANT_IDS.forEachIndexed { index, plantId ->
            val upper = plantId.uppercase()
            val plantSku = "custom.Plant.$plantId"
            allPlantSkus.put(plantSku)

            addStoreProduct(
                sku = plantSku,
                name = "Plant - $plantId",
                desc = upper,
                shortDesc = upper,
                objectType = "plant",
                objectItem = plantId,
                objectCount = 1
            )
            scheduledPlants.put(makeScheduleEntry("Plants", plantSku, if (index < 25) "new" else ""))

            val isNewest = index < 75
            val targetCat = when {
                index < 75 -> "Seeds2"
                index < 145 -> "Seeds3"
                else -> "Seeds"
            }
            val targetArr = when (targetCat) {
                "Seeds2" -> scheduledSeeds2
                "Seeds3" -> scheduledSeeds3
                else -> scheduledSeeds1
            }

            val tiers = if (isNewest) newestSeedTiers else classicSeedTiers
            for (amt in tiers) {
                val seedSku = "custom.SeedPacket.$plantId.$amt"
                addStoreProduct(
                    sku = seedSku,
                    name = "SeedPacket - $plantId x$amt",
                    desc = "$amt Seed Packets ($upper)",
                    shortDesc = upper,
                    objectType = "pinata",
                    objectItem = "free_seed_${plantId}1",
                    objectCount = amt
                )
                targetArr.put(makeScheduleEntry(targetCat, seedSku, if (isNewest) "new" else ""))
            }
        }

        // 2. ИНТЕРЕСНЫЕ БОНУСНЫЕ ТОВАРЫ (Умножители наград Календаря/Квестов x200, Мега-Пиньяты прокачки всех растений сразу, No Ads Pack)
        val bonusMultipliers = listOf(
            Triple("custom.Bonus.Calendar200x", "Calendar - 200x Rewards!", "calendar"),
            Triple("custom.Bonus.Quest200x", "Quest - 200x Rewards!", "quest")
        )
        for ((bSku, bName, bItem) in bonusMultipliers) {
            addStoreProduct(
                sku = bSku,
                name = bName,
                desc = bName,
                shortDesc = bName,
                objectType = "rewardmultiplier",
                objectItem = bItem,
                objectCount = 200
            )
            scheduledBundles.put(makeScheduleEntry("Bundles", bSku, "new"))
        }

        // Мега-пиньяты прокачки сразу всех 214 растений во вкладке Piñatas!
        val allXpPinatas = listOf(
            Triple("custom.Pinata.All.Bronze", "All Plants XP (Bronze +100)", "free_all_xp_plants_bronze"),
            Triple("custom.Pinata.All.Silver", "All Plants XP (Silver +1,000)", "free_all_xp_plants_silver"),
            Triple("custom.Pinata.All.Gold", "All Plants XP (Gold +5,000)", "free_all_xp_plants_gold"),
            Triple("custom.Pinata.All.Max", "All Plants XP (MEGA MAX +50,000)", "free_all_xp_plants_max"),
            Triple("custom.Pinata.All.100M", "All Plants XP (100 MILLION)", "free_all_xp_plants_100000000")
        )
        for ((pSku, pTitle, pItem) in allXpPinatas) {
            addStoreProduct(
                sku = pSku,
                name = pTitle,
                desc = pTitle,
                shortDesc = pTitle,
                objectType = "pinata",
                objectItem = pItem,
                objectCount = 1
            )
            scheduledPinatas.put(makeScheduleEntry("Pinatas", pSku, "new"))
        }

        // Классические пиньяты миров и стихий (по 1, 100 и 10 000 шт.)
        val worldPinatas = listOf(
            "egypt_gems" to "Ancient Egypt Piñata",
            "pirate_gems" to "Pirate Seas Piñata",
            "cowboy_gems" to "Wild West Piñata",
            "future_gems" to "Far Future Piñata",
            "dark_gems" to "Dark Ages Piñata",
            "beach_gems" to "Big Wave Beach Piñata",
            "iceage_gems" to "Frostbite Caves Piñata",
            "lostcity_gems" to "Lost City Piñata",
            "eighties_gems" to "Neon Mixtape Tour Piñata",
            "dino_gems" to "Jurassic Marsh Piñata",
            "modern_gems" to "Modern Day Piñata",
            "fire" to "Fire Piñata",
            "cold" to "Cold Piñata",
            "electricity" to "Electric Piñata",
            "explosive" to "Explosive Piñata",
            "sun" to "Sun Piñata",
            "mushroom" to "Mushroom Piñata",
            "nuts" to "Nuts Piñata",
            "fruit" to "Fruit Piñata",
            "greens" to "Greens Piñata"
        )
        for ((pItem, pName) in worldPinatas) {
            for (cnt in listOf(1, 100, 10000)) {
                val pSku = "custom.Pinata.$pItem.$cnt"
                addStoreProduct(
                    sku = pSku,
                    name = "$pName x$cnt",
                    desc = "$cnt x $pName",
                    shortDesc = pName,
                    objectType = "pinata",
                    objectItem = pItem,
                    objectCount = cnt
                )
                scheduledPinatas.put(makeScheduleEntry("Pinatas", pSku))
            }
        }

        // 3. Бандлы открытия всех растений в 1 клик
        val bundlesList = listOf(
            Triple("custom.Bundle.Total", "Legendary Bundle (All 214 Plants)", "greenthumb"),
            Triple("custom.Bundle.AllPremium", "All Premium Plants Bundle", "kiwibeastbundle"),
            Triple("custom.Bundle.Common", "All World Plants Bundle", "whipsnip"),
            Triple("custom.Bundle.Mints", "All Power Mints Bundle", "dragonshoard")
        )
        for ((bSku, bTitle, bIcon) in bundlesList) {
            addStoreProduct(
                sku = bSku,
                name = bTitle,
                desc = bTitle,
                shortDesc = bTitle,
                objectType = "bundle",
                objectItem = bIcon,
                objectCount = 1,
                bundleProps = allPlantSkus
            )
            scheduledBundles.put(makeScheduleEntry("Bundles", bSku))
        }

        // 4. Все 342 Костюма
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

        // 5. Все 40 Игровых Апгрейдов
        for (upgId in Pvz2Database.GAME_UPGRADES) {
            val uSku = "custom.Upgrade.$upgId"
            addStoreProduct(
                sku = uSku,
                name = "Upgrade - $upgId",
                desc = "STORE_CATEGORY_DAILY_OFFERS",
                shortDesc = upgId,
                objectType = "gameupgrade",
                objectItem = upgId,
                objectCount = 1
            )
            scheduledUpgrades.put(makeScheduleEntry("Upgrades", uSku))
        }

        // 6. Положительные ресурсы (во вкладку Resources) + ОТРИЦАТЕЛЬНЫЕ РЕСУРСЫ ДЛЯ УДАЛЕНИЯ ВАЛЮТ (во вкладку DeleteItems / Delet)!
        val resourceDefs = listOf(
            Triple("Coin", "coin", "Coins" to "DeletCoin"),
            Triple("Gem", "gem", "Gems" to "DeletGem"),
            Triple("Mint", "mint", "Mint" to "DeletMint"),
            Triple("Ticket", "ticket", "Tickets" to "DeletTicket"),
            Triple("Fuel", "fuel", "Fuel" to "DeletFuel"),
            Triple("Sprout", "sprout", "Sprouts" to "DeletSprout")
        )
        val positiveAmounts = listOf(1, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)
        val negativeDeleteAmounts = listOf(-1000, -10000, -100000, -1000000, -10000000, -100000000, -1000000000)

        for ((prefix, objType, cats) in resourceDefs) {
            val (posCat, delCat) = cats
            // Начисление валюты
            for (amt in positiveAmounts) {
                val rSku = "custom.$prefix.$amt"
                addStoreProduct(
                    sku = rSku,
                    name = "+$amt $prefix",
                    desc = "+$amt $posCat!",
                    shortDesc = "+$amt $posCat",
                    objectType = objType,
                    objectItem = objType,
                    objectCount = amt
                )
                scheduledResources.put(makeScheduleEntry(posCat, rSku))
            }
            // Удаление (списание) лишней или ушедшей в оверфлоу валюты (точно как в products_2.json -> ### DELETE ###)!
            for (negAmt in negativeDeleteAmounts) {
                val absVal = kotlin.math.abs(negAmt)
                val delSku = "custom.Delete.$prefix.$absVal"
                addStoreProduct(
                    sku = delSku,
                    name = "Delete $negAmt $prefix",
                    desc = "Remove $negAmt $posCat",
                    shortDesc = "$negAmt $posCat",
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

        val entriesArr = JSONArray().apply {
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Bundles").put("ScheduledProducts", scheduledBundles))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Plants").put("ScheduledProducts", scheduledPlants))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds").put("ScheduledProducts", scheduledSeeds1))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds2").put("ScheduledProducts", scheduledSeeds2))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds3").put("ScheduledProducts", scheduledSeeds3))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Pinatas").put("ScheduledProducts", scheduledPinatas))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Costumes").put("ScheduledProducts", scheduledCostumes))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Upgrades").put("ScheduledProducts", scheduledUpgrades))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Resources").put("ScheduledProducts", scheduledResources))
            put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Delet").put("ScheduledProducts", scheduledDeleteItems))
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

    // ==================== 2. ИСПРАВЛЕННЫЙ МОД: МОМЕНТАЛЬНЫЕ РАСТЕНИЯ И ОТРИЦАТЕЛЬНАЯ ЦЕНА СОЛНЦА ====================
    // Поддерживает отрицательную цену (например, -500 солнца даёт +500 солнца при посадке!) и отрицательный кулдаун (-999 сек = мгновенный откат)!

    fun patchZeroSunAndInstantCooldown(
        cdnDir: String,
        useNegativeSunBonus: Boolean = true,
        log: (String) -> Unit
    ): Boolean {
        val targetSunValue = if (useNegativeSunBonus) -500 else 0
        // В движке PvZ 2 значение Cooldown = 0 сбрасывается на стандартное! А отрицательное (-999) даёт мгновенную перезарядку 0.0 сек!
        val targetCooldownValue = -999

        val (_, noBackup) = resolveNoBackupDir {}
        val allCdnDirs = listAllCdnDirs().ifEmpty { listOf(cdnDir) }.toMutableSet().apply { add(cdnDir) }

        var patchedFilesCount = 0

        // Патчим во ВСЕХ найденных папках CDN.* сразу, чтобы игра гарантированно подхватила нужный файл!
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
                // 1. Обработка блоков FloatStats в PlantLevels.rton: {"Name": "Cost", "Values": [...]}
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

                // 2. Обработка прямых свойств в PlantProperties.rton
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
