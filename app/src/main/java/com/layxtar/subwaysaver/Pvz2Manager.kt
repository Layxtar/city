package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject

object Pvz2Manager {

    private val PACKAGES = listOf(
        Pvz2Database.PKG_PVZ2_ROW, // com.ea.game.pvz2_row
        Pvz2Database.PKG_PVZ2_NA   // com.ea.game.pvz2_na
    )

    // Автоматический поиск установленного пакета PvZ 2 и папки No_Backup
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

    // Автоматический поиск самой новой папки CDN.* (например, CDN.13.5 -> CDN.13.6 -> CDN.14.0)
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

    // Список всех папок CDN.*, если на устройстве осталось несколько версий
    fun listAllCdnDirs(): List<String> {
        val (_, noBackup) = resolveNoBackupDir {}
        val (_, out) = CryptoAndSyncManager.execSh("ls -1d '$noBackup'/CDN.* 2>/dev/null | sort -V -r")
        return out.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Чтение любого .rton или pp.dat файла и автоматическая конвертация в JSONObject
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

    // Конвертация JSONObject обратно в бинарный RTON и запись через Shizuku
    fun writeJsonAsRton(fullPath: String, json: JSONObject, log: (String) -> Unit): Boolean {
        val (pkg, _) = resolveNoBackupDir {}
        CryptoAndSyncManager.stopGameForSync(pkg, true, log)
        return try {
            val rtonBytes = RtonConverter.jsonToRton(json)
            val ok = CryptoAndSyncManager.writeFileViaShizuku(fullPath, rtonBytes, log)
            if (ok) log("✅ Записан RTON (${rtonBytes.size} байт): $fullPath")
            ok
        } catch (e: Exception) {
            log("❌ Ошибка упаковки RTON ($fullPath): ${e.message}")
            false
        }
    }

    // ==================== 1. РЕДАКТОР ПРОФИЛЯ ИГРОКА (pp.dat) ====================

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

    // Получает первый активный слот игрока внутри pp.dat (обычно массив "objects" -> GameData)
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
            // В pp.dat список открытых растений и их уровней хранится в "pl" / "plantLevels"
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

    // ==================== 2. МАГАЗИН «ВСЁ ПО 1 МОНЕТЕ» И КАСТОМНЫЕ СЕМЕНА ====================

    fun patchShopAllFor1CoinAndInjectSeeds(
        cdnDir: String,
        selectedPlantId: String,
        seedAmount: Int,
        unlockAllPlantsAndSkinsInShop: Boolean,
        injectCustomGodBundles: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val productsPath = "$cdnDir/products.rton"
        val marketLayoutPath = "$cdnDir/market_layout.rton"
        val marketSchedPath = "$cdnDir/market_schedule.rton"

        // 1. Патчим products.rton: переводим все товары на цену 1 монета (Coins = 1) + добавляем кастомные паки семян
        val productsJson = readRtonAsJson(productsPath, log)
        if (productsJson != null) {
            val objects = productsJson.optJSONArray("objects") ?: JSONArray()
            for (i in 0 until objects.length()) {
                val obj = objects.getJSONObject(i)
                val objData = obj.optJSONObject("objdata") ?: continue

                // Делаем любой товар за 1 монету!
                objData.put("PriceCoins", 1)
                objData.put("PriceGems", 0)
                objData.put("PriceMints", 0)
                objData.put("Price", 0.0)
                objData.put("IsFree", false)
                objData.put("CurrencyType", "Coins")

                // Если это пакет семян и выбрана инъекция семян — увеличиваем количество семян внутри пакета!
                if (seedAmount > 0 && objData.optString("ObjectType").contains("seed", ignoreCase = true)) {
                    objData.put("ObjectCount", seedAmount)
                }
            }

            // Добавляем в products.rton собственный кастомный товар: выбранное растение + нужное число семян (от 1 до 1 000 000) за 1 монету!
            val customSku = "editor_seed_${selectedPlantId}_$seedAmount"
            objects.put(JSONObject().apply {
                put("aliases", JSONArray().put(customSku))
                put("objclass", "ProductProps")
                put("objdata", JSONObject().apply {
                    put("Sku", customSku)
                    put("ObjectType", selectedPlantId)
                    put("ObjectCount", seedAmount)
                    put("PriceCoins", 1)
                    put("PriceGems", 0)
                    put("CurrencyType", "Coins")
                    put("IsAdPlacement", false)
                })
            })

            if (unlockAllPlantsAndSkinsInShop) {
                for ((plantId, _) in Pvz2Database.PLANTS) {
                    val plantUnlockSku = "editor_unlock_$plantId"
                    objects.put(JSONObject().apply {
                        put("aliases", JSONArray().put(plantUnlockSku))
                        put("objclass", "ProductProps")
                        put("objdata", JSONObject().apply {
                            put("Sku", plantUnlockSku)
                            put("ObjectType", plantId)
                            put("ObjectCount", maxOf(seedAmount, 250))
                            put("PriceCoins", 1)
                            put("PriceGems", 0)
                            put("CurrencyType", "Coins")
                        })
                    })
                }
            }

            productsJson.put("objects", objects)
            writeJsonAsRton(productsPath, productsJson, log)
        } else {
            log("⚠️ Файл $productsPath не найден в кэше!")
            return false
        }

        // 2. Патчим market_schedule.rton и market_layout.rton, чтобы все товары и наши новые слоты отображались на витрине за 1 монету
        listOf(marketSchedPath, marketLayoutPath).forEach { mPath ->
            val mJson = readRtonAsJson(mPath, log)
            if (mJson != null) {
                forcePricesTo1CoinRecursively(mJson, selectedPlantId, seedAmount, injectCustomGodBundles)
                writeJsonAsRton(mPath, mJson, log)
            }
        }

        log("🛒 Магазин PvZ 2 пропатчен: всё по 1 монете + пакет '$selectedPlantId' ($seedAmount семян)!")
        return true
    }

    private fun forcePricesTo1CoinRecursively(
        node: Any?,
        selectedPlantId: String,
        seedAmount: Int,
        injectCustomBundles: Boolean
    ) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (k.equals("SoftCost", ignoreCase = true) ||
                        k.equals("PriceCoins", ignoreCase = true) ||
                        k.equals("Cost", ignoreCase = true) ||
                        k.equals("Price", ignoreCase = true)
                    ) {
                        if (v is Number) node.put(k, 1)
                    }
                    if (k.equals("SoftCurrency", ignoreCase = true) || k.equals("CurrencyType", ignoreCase = true)) {
                        if (v is String) node.put(k, "coins")
                    }
                    if (k.equals("MaxPurchase", ignoreCase = true) || k.equals("PurchaseCap", ignoreCase = true)) {
                        if (v is Number) node.put(k, 9999)
                    }
                    if (injectCustomBundles && (k.equals("ItemType", ignoreCase = true) || k.equals("Product", ignoreCase = true))) {
                        // Если пользователь хочет заменить витрину выбранным растением
                        if (seedAmount > 1 && v is String && v.contains("seed", ignoreCase = true)) {
                            node.put("ItemCount", seedAmount)
                        }
                    }
                    forcePricesTo1CoinRecursively(v, selectedPlantId, seedAmount, injectCustomBundles)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    forcePricesTo1CoinRecursively(node.opt(i), selectedPlantId, seedAmount, injectCustomBundles)
                }
            }
        }
    }

    // ==================== 3. ВЗЛОМ ЛУТА ПИНЬЯТ (pinatacoreloot.rton) ====================

    fun patchPinataCoreLoot(cdnDir: String, multiplier: Int, minSeedsPerSlot: Int, log: (String) -> Unit): Boolean {
        val path = "$cdnDir/pinatacoreloot.rton"
        val json = readRtonAsJson(path, log) ?: run {
            log("❌ Файл $path не найден!")
            return false
        }
        boostPinataLootRecursively(json, multiplier, minSeedsPerSlot)
        val ok = writeJsonAsRton(path, json, log)
        if (ok) log("🪅 Лут всех пиньят увеличен в x$multiplier раз (минимум $minSeedsPerSlot семян в слоте)!")
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
                            if (!lowKey.contains("slot") && !lowKey.contains("weight")) {
                                val boosted = maxOf(v.toInt() * mult, minSeeds)
                                node.put(k, boosted)
                            } else if (lowKey.contains("slot")) {
                                node.put(k, maxOf(v.toInt(), 10))
                            }
                        }
                    } else {
                        boostPinataLootRecursively(v, mult, minSeeds)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    boostPinataLootRecursively(node.opt(i), mult, minSeeds)
                }
            }
        }
    }

    // ==================== 4. ГЕЙМПЛЕЙНЫЕ МОДЫ (0 СОЛНЦА, 0 КУЛДАУН, МЕГА-АРЕНА) ====================

    fun patchZeroSunAndInstantCooldown(cdnDir: String, log: (String) -> Unit): Boolean {
        val propsPath = "$cdnDir/PlantProperties.rton"
        val levelsPath = "$cdnDir/PlantLevels.rton"
        var anyPatched = false

        readRtonAsJson(propsPath, log)?.let { json ->
            zeroSunRecursively(json)
            if (writeJsonAsRton(propsPath, json, log)) anyPatched = true
        }

        readRtonAsJson(levelsPath, log)?.let { json ->
            zeroSunRecursively(json)
            if (writeJsonAsRton(levelsPath, json, log)) anyPatched = true
        }

        if (anyPatched) log("🌞 Мод активирован: Все растения стоят 0 солнца и перезаряжаются за 0 секунд!")
        return anyPatched
    }

    private fun zeroSunRecursively(node: Any?) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (k.equals("Cost", ignoreCase = true) || k.equals("SunCost", ignoreCase = true)) {
                        if (v is Number) node.put(k, 0)
                        if (v is JSONArray) {
                            for (idx in 0 until v.length()) v.put(idx, 0)
                        }
                    }
                    if (k.equals("PacketCooldown", ignoreCase = true) ||
                        k.equals("StartingCooldown", ignoreCase = true) ||
                        k.equals("Cooldown", ignoreCase = true)
                    ) {
                        if (v is Number) node.put(k, 0)
                        if (v is JSONArray) {
                            for (idx in 0 until v.length()) v.put(idx, 0)
                        }
                    }
                    zeroSunRecursively(v)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) zeroSunRecursively(node.opt(i))
            }
        }
    }

    // Взлом наград Арены (Joust) и Ежедневного Календаря: x100 ко всем наградам и закрытие сезона за 1 бой!
    fun patchArenaAndCalendarMegaRewards(cdnDir: String, log: (String) -> Unit): Boolean {
        val filesToBoost = listOf(
            "joust_crownrewards.rton",
            "joust_progressiverewards.rton",
            "joust_season_rewards.rton",
            "joust_tournamentrewards.rton",
            "calendar_schedule.rton",
            "dailyquestdata.rton"
        )
        var count = 0
        for (fName in filesToBoost) {
            val fullPath = "$cdnDir/$fName"
            val json = readRtonAsJson(fullPath, log) ?: continue
            boostRewardsAndLowerCrownsRecursively(json)
            if (writeJsonAsRton(fullPath, json, log)) count++
        }
        log("🏆 Пропатчено $count файлов Арены и Календаря: награды увеличены в x100 раз, требования корон снижены до 1!")
        return count > 0
    }

    private fun boostRewardsAndLowerCrownsRecursively(node: Any?) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    val low = k.lowercase()
                    if (v is Number) {
                        if (low.contains("crown") || low.contains("pointsneeded") || low.contains("required")) {
                            node.put(k, 1)
                        } else if (low.contains("amount") || low.contains("count") || low.contains("quantity") || low.contains("value")) {
                            node.put(k, maxOf(v.toInt() * 100, 1000))
                        }
                    } else {
                        boostRewardsAndLowerCrownsRecursively(v)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) boostRewardsAndLowerCrownsRecursively(node.opt(i))
            }
        }
    }

    // Откат любого пропатченного .rton конфига из резервной копии .bak_editor
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
        log("⏪ Все оригинальные .rton файлы в $cdnDir успешно восстановлены из резервных копий!")
    }
}
