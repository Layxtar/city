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

    // ==================== 1. УСТАНОВКА МАГАЗИНА СО СТАНДАРТНЫМИ НАБОРАМИ + ВСЕМИ РАСТЕНИЯМИ И СЕМЕНАМИ ====================

    fun installFullMegaShopAllPlantsSkinsAndSeeds(cdnDir: String, log: (String) -> Unit): Boolean {
        log("⏳ 1/5: Разрешаем типы plant, seedpacket, costume, bundle в magento_filter.rton...")
        val magentoJson = buildMagentoFilterJson()
        if (!writeJsonAsRton("$cdnDir/magento_filter.rton", magentoJson, log)) return false

        log("⏳ 2/5: Обновление макета вкладок (market_layout.rton) с сохранением стандартных категорий...")
        val layoutJson = buildMarketLayoutJson()
        if (!writeJsonAsRton("$cdnDir/market_layout.rton", layoutJson, log)) return false

        log("⏳ 3/5: Настройка таблиц наград семян в pinatacoreloot.rton...")
        patchPinataCoreLootForAllSeedPackets(cdnDir, log)

        log("⏳ 4/5: Обновление products.rton (сохранены стандартные наборы игры + добавлены 214 растений, 342 скина и семена)...")
        val (productsJson, scheduleJson) = buildProductsAndMarketScheduleJson(cdnDir, log)
        if (!writeJsonAsRton("$cdnDir/products.rton", productsJson, log)) return false

        log("⏳ 5/5: Запись расписания витрины (market_schedule.rton)...")
        if (!writeJsonAsRton("$cdnDir/market_schedule.rton", scheduleJson, log)) return false

        log("🎉 Магазин обновлён! Все стандартные наборы сохранены, семена имеют иконку своего растения и выдаются при покупке!")
        return true
    }

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
            // Обязательно разрешаем прямой тип seedpacket за монеты, чтобы семена выдавались с иконкой растения!
            put(JSONObject().put("Type", "seedpacket").put("WarnEnsureRealMoneyOnly", false).put("SubTypeFilters", plantSubTypes))
            put(JSONObject().put("Type", "plant").put("WarnEnsureRealMoneyOnly", false).put("SubTypeFilters", plantSubTypes))
            put(JSONObject().put("Type", "gameupgrade").put("WarnEnsureRealMoneyOnly", false).put("SubTypeFilters", upgradeSubTypes))
            put(JSONObject().put("Type", "costume").put("WarnEnsureRealMoneyOnly", false).put("SubTypeFilters", costumeSubTypes))
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

    private fun buildMarketLayoutJson(): JSONObject {
        val mainClusters = JSONArray().apply {
            put("Promotions"); put("Bundles"); put("Plants")
            put("Seeds"); put("Seeds2"); put("Seeds3")
            put("Pinatas"); put("Costumes"); put("Upgrades")
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
            put(JSONObject().put("Type", "Promoted Offers").put("Name", "[STORE_TAB_PROMOTED]").put("ImageDown", "IMAGE_UI_STORE_GEMSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_GEMSTAB_ACTIVE").put("Categories", JSONArray().put("Promoted Offers")))
            put(JSONObject().put("Type", "Resources").put("Name", "[STORE_TAB_RESOURCES]").put("ImageDown", "IMAGE_UI_STORE_COINSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_COINSTAB_ACTIVE").put("Categories", JSONArray().put("Coins").put("Gems").put("Tickets").put("Fuel").put("Sprouts").put("Mint")))
            put(JSONObject().put("Type", "Pinatas").put("Name", "[STORE_TAB_PINATAS]").put("ImageDown", "IMAGE_UI_STORE_PINATASTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PINATASTAB_ACTIVE").put("Categories", JSONArray().put("Pinatas")))
            put(JSONObject().put("Type", "Bundles").put("Name", "[STORE_TAB_BUNDLES]").put("ImageDown", "IMAGE_UI_STORE_BUNDLESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_BUNDLESTAB_ACTIVE").put("Categories", JSONArray().put("Bundles")))
            put(JSONObject().put("Type", "Plants").put("Name", "[STORE_TAB_PLANTS]").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Plants")))
            put(JSONObject().put("Type", "Costumes").put("Name", "Costumes").put("ImageDown", "IMAGE_UI_STORE_PLANTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_PLANTSTAB_ACTIVE").put("Categories", JSONArray().put("Costumes")))
            put(JSONObject().put("Type", "Seeds").put("Name", "Seeds 1").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds")))
            put(JSONObject().put("Type", "Seeds2").put("Name", "Seeds 2").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds2")))
            put(JSONObject().put("Type", "Seeds3").put("Name", "Seeds 3").put("ImageDown", "IMAGE_UI_STORE_SPROUTSTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_SPROUTSTAB_ACTIVE").put("Categories", JSONArray().put("Seeds3")))
            put(JSONObject().put("Type", "Upgrades").put("Name", "[STORE_TAB_UPGRADES]").put("ImageDown", "IMAGE_UI_STORE_UPGRADESTAB_DOWN").put("ImageActive", "IMAGE_UI_STORE_UPGRADESTAB_ACTIVE").put("Categories", JSONArray().put("Upgrades")))
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
            put(JSONObject().put("Type", "Promoted Offers").put("LabelText", "[STORE_CATEGORY_PROMOTED]").put("MaxCount", -1).put("PromotionSlots", JSONArray().put("Slot1").put("Slot2").put("Slot3").put("Slot4").put("Slot5").put("Slot6").put("Slot7")))
            put(JSONObject().put("Type", "Coins").put("LabelText", "[STORE_CATEGORY_COINS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_COIN_CARD"))
            put(JSONObject().put("Type", "Gems").put("LabelText", "[STORE_CATEGORY_GEMS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "Tickets").put("LabelText", "[STORE_CATEGORY_GAUNTLETS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "Fuel").put("LabelText", "[STORE_CATEGORY_FUEL]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Sprouts").put("LabelText", "[STORE_CATEGORY_SPROUTS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
            put(JSONObject().put("Type", "Mint").put("LabelText", "[STORE_CATEGORY_MINTS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PINATA_CARD"))
            put(JSONObject().put("Type", "Pinatas").put("LabelText", "[STORE_CATEGORY_PINATAS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "Bundles").put("LabelText", "[STORE_CATEGORY_BUNDLES]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_BUNDLE_CARD"))
            put(JSONObject().put("Type", "Plants").put("LabelText", "[STORE_CATEGORY_PLANTS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Costumes").put("LabelText", "[ALMANAC_COSTUME_DIALOG_TITLE]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            // Для семян ставим карточку растений (STORE_PLANT_CARD), чтобы они выглядели как карточки растений!
            put(JSONObject().put("Type", "Seeds").put("LabelText", "[STORE_CATEGORY_PLANTS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Seeds2").put("LabelText", "[STORE_CATEGORY_PLANTS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Seeds3").put("LabelText", "[STORE_CATEGORY_PLANTS]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Upgrades").put("LabelText", "[STORE_CATEGORY_UPGRADES]").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "Tutorial").put("LabelText", "Tutorial").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "Tutorial50MB").put("LabelText", "Tutorial50MB").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "QuickCoins").put("LabelText", "Coins").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_COIN_CARD"))
            put(JSONObject().put("Type", "QuickGems").put("LabelText", "Gems").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_GEM_CARD"))
            put(JSONObject().put("Type", "QuickGauntlets").put("LabelText", "Gauntlets").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_UPGRADE_CARD"))
            put(JSONObject().put("Type", "QuickFuel").put("LabelText", "Fuel").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_PLANT_CARD"))
            put(JSONObject().put("Type", "QuickSprouts").put("LabelText", "Sprouts").put("MaxCount", -1).put("Card", "IMAGE_UI_CARDS_STORE_STORE_SPROUT_CARD"))
        }

        val promoSlots = JSONArray().apply {
            for (slotIdx in 1..7) {
                put(JSONObject().put("Type", "Slot$slotIdx").put("GroupPriority", JSONArray().put("Bundles")).put("FallbackCategory", "Bundles"))
            }
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

    // Правильное добавление таблиц семян в pinatacoreloot.rton (RewardsTables — это JSONArray с полями Min и Max!)
    private fun patchPinataCoreLootForAllSeedPackets(cdnDir: String, log: (String) -> Unit) {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readRtonAsJson(path, log) ?: return
        val objects = json.optJSONArray("objects") ?: return

        var slotConfigsObj: JSONObject? = null
        var rewardsTablesArr: JSONArray? = null

        for (i in 0 until objects.length()) {
            val obj = objects.getJSONObject(i)
            val data = obj.optJSONObject("objdata") ?: continue
            if (data.has("SlotConfigs")) slotConfigsObj = data.optJSONObject("SlotConfigs")
            if (data.has("RewardsTables") && data.opt("RewardsTables") is JSONArray) {
                rewardsTablesArr = data.optJSONArray("RewardsTables")
            }
        }

        if (slotConfigsObj != null) {
            val existingTableIds = mutableSetOf<String>()
            if (rewardsTablesArr != null) {
                for (i in 0 until rewardsTablesArr.length()) {
                    existingTableIds.add(rewardsTablesArr.optJSONObject(i)?.optString("Id") ?: "")
                }
            }

            for (plantId in Pvz2Database.ALL_PLANT_IDS) {
                val slotKey = "custom_seed_${plantId}_1"
                val tableKey = "custom_table_${plantId}_1"

                slotConfigsObj.put(slotKey, JSONObject().apply {
                    put("BonusTable", "bonus_standard")
                    put("PercentBonusAward", 0)
                    put("PercentPinataAward", 1)
                    put("PinataType", "")
                    put("RewardsTable", tableKey)
                    put("Slots", JSONArray().put("PlantXP"))
                })

                if (rewardsTablesArr != null && existingTableIds.add(tableKey)) {
                    rewardsTablesArr.put(JSONObject().apply {
                        put("Id", tableKey)
                        put("Rewards", JSONArray().put(JSONObject().apply {
                            put("Max", 1)
                            put("Min", 1)
                            put("Plant", plantId)
                            put("Type", "PlantXP")
                            put("Weight", 100)
                        }))
                    })
                }
            }
        }

        writeJsonAsRton(path, json, log)
    }

    // Сохраняет все дефолтные наборы игры (переводя их в 1 монету) + добавляет 214 растений, 342 скина и рабочие семена с иконками растений!
    private fun buildProductsAndMarketScheduleJson(
        cdnDir: String,
        log: (String) -> Unit
    ): Pair<JSONObject, JSONObject> {
        val origProductsJson = readRtonAsJson("$cdnDir/products.rton", log)
        val origScheduleJson = readRtonAsJson("$cdnDir/market_schedule.rton", log)

        val productsArr = JSONArray()
        val addedSkus = mutableSetOf<String>()

        // 1. СНАЧАЛА БЕРЕЖНО СОХРАНЯЕМ ВСЕ ОРИГИНАЛЬНЫЕ ТОВАРЫ И НАБОРЫ ИГРЫ ИЗ PRODUCTS.RTON (с ценой 1 монета)
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
            descKey: String,
            shortDescKey: String,
            objectType: String,
            objectItem: String,
            objectCount: Int,
            artCenter: String = "",
            bundleProps: JSONArray? = null
        ) {
            if (!addedSkus.add(sku)) return
            val objData = JSONObject().apply {
                if (bundleProps != null) put("BundleProps", bundleProps)
                if (artCenter.isNotEmpty()) put("ArtCenter", artCenter)
                put("IsAdPlacement", false)
                put("IsFree", false)
                put("IsOneTimePurchase", false)
                // Используем официальные ключи локализации из LawnStrings (без текста MISSING!)
                put("LawnDescription", descKey)
                put("LawnShortDescription", shortDescKey)
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
                put("SoftCurrency", "coins")
                put("SoftCost", 1)
                put("MaxPurchase", -1)
                put("SoftCostPerPurchase", -1)
                put("Visuals", "RTID(0)")
                put("Sku", JSONArray().put(sku).put(sku))
            }
        }

        // 2. ДОБАВЛЯЕМ ВСЕ 214 РАСТЕНИЙ И ИХ НАБОРЫ СЕМЯН (С ИКОНКОЙ РАСТЕНИЯ И РАБОЧЕЙ ВЫДАЧЕЙ!)
        val allPlantSkusForBundle = JSONArray()
        Pvz2Database.ALL_PLANT_IDS.forEachIndexed { index, plantId ->
            val upperKey = plantId.uppercase()
            val plantSku = "custom.plant.$plantId"
            allPlantSkusForBundle.put(plantSku)

            addStoreProduct(
                sku = plantSku,
                name = "Plant - $plantId",
                descKey = upperKey,
                shortDescKey = upperKey,
                objectType = "plant",
                objectItem = plantId,
                objectCount = 1,
                artCenter = plantId
            )
            scheduledPlants.put(makeScheduleEntry("Plants", plantSku))

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

            // Создаем наборы семян (1, 100, 10000, 100000, 1000000) с нативным типом "seedpacket" и ArtCenter = plantId!
            // Благодаря этому у каждого пакета семян рисуется иконка самого растения, нет надписи MISSING, и семена сразу начисляются!
            for (amt in Pvz2Database.SEED_PACK_AMOUNTS) {
                val seedSku = "custom.seed.$plantId.$amt"
                addStoreProduct(
                    sku = seedSku,
                    name = "SeedPacket - $plantId x$amt",
                    descKey = upperKey,
                    shortDescKey = upperKey,
                    objectType = "seedpacket",
                    objectItem = plantId,
                    objectCount = amt,
                    artCenter = plantId
                )
                targetSeedArr.put(makeScheduleEntry(targetSeedCat, seedSku))
            }
        }

        // 3. Мега-Набор всех растений
        val totalBundleSku = "custom.bundle.all_plants"
        addStoreProduct(
            sku = totalBundleSku,
            name = "Bundle - All 214 Plants",
            descKey = "STORE_CATEGORY_BUNDLES",
            shortDescKey = "STORE_CATEGORY_PLANTS",
            objectType = "bundle",
            objectItem = "greenthumb",
            objectCount = 1,
            bundleProps = allPlantSkusForBundle
        )
        scheduledBundles.put(makeScheduleEntry("Bundles", totalBundleSku))

        // 4. Все 342 Костюма
        for (costumeId in 1..342) {
            val cSku = "custom.costume.$costumeId"
            addStoreProduct(
                sku = cSku,
                name = "Costume $costumeId",
                descKey = "ALMANAC_COSTUME_DIALOG_TITLE",
                shortDescKey = "ALMANAC_COSTUME_DIALOG_TITLE",
                objectType = "costume",
                objectItem = costumeId.toString(),
                objectCount = 1
            )
            scheduledCostumes.put(makeScheduleEntry("Costumes", cSku))
        }

        // 5. Все 40 Игровых Апгрейдов
        for (upgId in Pvz2Database.GAME_UPGRADES) {
            val uSku = "custom.upgrade.$upgId"
            addStoreProduct(
                sku = uSku,
                name = "Upgrade - $upgId",
                descKey = "STORE_CATEGORY_DAILY_OFFERS",
                shortDescKey = "STORE_TAB_UPGRADES",
                objectType = "gameupgrade",
                objectItem = upgId,
                objectCount = 1
            )
            scheduledUpgrades.put(makeScheduleEntry("Upgrades", uSku))
        }

        // 6. Пакеты ресурсов (Монеты, Алмазы, Мяты, Перчатки, Топливо, Ростки)
        val resourceDefs = listOf(
            Triple("coin", "Coins", "COIN_STORE_TEXT"),
            Triple("gem", "Gems", "PRODUCT_GEMS_1800_CONSUME_S"),
            Triple("mint", "Mint", "STORE_TAB_MINTS"),
            Triple("ticket", "Tickets", "STORE_TAB_GAUNTLETS"),
            Triple("fuel", "Fuel", "STORE_TAB_FUEL"),
            Triple("sprout", "Sprouts", "STORE_TAB_SPROUTS")
        )
        val resourceAmounts = listOf(1000, 10000, 100000, 1000000, 10000000)
        for ((objType, catName, locKey) in resourceDefs) {
            for (amt in resourceAmounts) {
                val rSku = "custom.resource.$objType.$amt"
                addStoreProduct(
                    sku = rSku,
                    name = "$catName x$amt",
                    descKey = locKey,
                    shortDescKey = locKey,
                    objectType = objType,
                    objectItem = objType,
                    objectCount = amt
                )
                scheduledResources.put(makeScheduleEntry(catName, rSku))
            }
        }

        val finalProductsJson = JSONObject().apply {
            put("version", 1)
            put("objects", productsArr)
        }

        // 7. СОХРАНЯЕМ ВСЕ ОРИГИНАЛЬНЫЕ НАБОРЫ И РАСПИСАНИЯ ИЗ MARKET_SCHEDULE.RTON (с ценой 1 монета) + ДОБАВЛЯЕМ НАШИ ВКЛАДКИ
        val entriesArr = JSONArray()
        val origSchedObjects = origScheduleJson?.optJSONArray("objects")
        if (origSchedObjects != null && origSchedObjects.length() > 0) {
            val existingEntries = origSchedObjects.getJSONObject(0)
                .optJSONObject("objdata")
                ?.optJSONArray("MarketScheduleEntries")
            if (existingEntries != null) {
                for (i in 0 until existingEntries.length()) {
                    val entry = existingEntries.getJSONObject(i)
                    // Делаем все стандартные предложения активными навсегда и за 1 монету
                    entry.put("StartDateTime", 0)
                    entry.put("EndDateTime", 0)
                    val schedProds = entry.optJSONArray("ScheduledProducts")
                    if (schedProds != null) {
                        for (j in 0 until schedProds.length()) {
                            val sp = schedProds.getJSONObject(j)
                            sp.put("SoftCurrency", "coins")
                            sp.put("SoftCost", 1)
                            sp.put("MaxPurchase", -1)
                            sp.put("SoftCostPerPurchase", -1)
                        }
                    }
                    entriesArr.put(entry)
                }
            }
        }

        // Дописываем наши полные вкладки
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Plants").put("ScheduledProducts", scheduledPlants))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds2").put("ScheduledProducts", scheduledSeeds2))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds3").put("ScheduledProducts", scheduledSeeds3))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Seeds").put("ScheduledProducts", scheduledSeeds1))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Costumes").put("ScheduledProducts", scheduledCostumes))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Bundles").put("ScheduledProducts", scheduledBundles))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Upgrades").put("ScheduledProducts", scheduledUpgrades))
        entriesArr.put(JSONObject().put("StartDateTime", 0).put("EndDateTime", 0).put("Name", "Resources").put("ScheduledProducts", scheduledResources))

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
