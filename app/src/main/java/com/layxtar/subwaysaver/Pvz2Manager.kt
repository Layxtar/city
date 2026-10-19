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
            if (ok) log("✅ Записан RTON (${rtonBytes.size} байт): $fullPath")
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

    // ==================== ПРИНУДИТЕЛЬНЫЙ ПАТЧ МАГАЗИНА И ВВЕДЕНИЕ ВСЕХ ТОВАРОВ НА ВИТРИНУ ====================

    fun patchShopAllFor1CoinAndInjectSeeds(
        cdnDir: String,
        selectedPlantId: String,
        seedAmount: Int,
        unlockAllPlantsAndSkinsInShop: Boolean,
        log: (String) -> Unit
    ): Boolean {
        val productsPath = "$cdnDir/products.rton"
        val marketLayoutPath = "$cdnDir/market_layout.rton"
        val marketSchedPath = "$cdnDir/market_schedule.rton"

        // 1. Патчим products.rton: переводим всё на 1 монету + генерируем товары для абсолютно каждого растения и скина
        val productsJson = readRtonAsJson(productsPath, log)
        if (productsJson != null) {
            val objects = productsJson.optJSONArray("objects") ?: JSONArray()
            val allSkus = mutableListOf<String>()

            for (i in 0 until objects.length()) {
                val obj = objects.getJSONObject(i)
                val objData = obj.optJSONObject("objdata") ?: continue

                objData.put("PriceCoins", 1)
                objData.put("PriceGems", 0)
                objData.put("PriceMints", 0)
                objData.put("Price", 0.0)
                objData.put("IsFree", false)
                objData.put("CurrencyType", "Coins")

                val sku = objData.optString("Sku")
                if (sku.isNotEmpty()) allSkus.add(sku)
            }

            // Добавляем кастомный товар с выбранным количеством семян (от 1 до 1 000 000)
            val customSku = "editor_seed_${selectedPlantId}_$seedAmount"
            allSkus.add(customSku)
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

            // Если включено — создаем товары для вообще всех растений из базы данных
            if (unlockAllPlantsAndSkinsInShop) {
                for ((plantId, _) in Pvz2Database.PLANTS) {
                    val plantSku = "editor_unlock_$plantId"
                    allSkus.add(plantSku)
                    objects.put(JSONObject().apply {
                        put("aliases", JSONArray().put(plantSku))
                        put("objclass", "ProductProps")
                        put("objdata", JSONObject().apply {
                            put("Sku", plantSku)
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

            // 2. ПРИНУДИТЕЛЬНО ПРОПИСЫВАЕМ ВСЕ SKUs НА ГЛАВНУЮ ВИТРИНУ (market_layout.rton)
            val layoutJson = readRtonAsJson(marketLayoutPath, log)
            if (layoutJson != null) {
                forceInjectSkusToLayoutAndSchedule(layoutJson, allSkus)
                writeJsonAsRton(marketLayoutPath, layoutJson, log)
            }

            // 3. ПРИНУДИТЕЛЬНО АКТИВИРОВАТЬ РАСПИСАНИЕ МАГАЗИНА (market_schedule.rton)
            val schedJson = readRtonAsJson(marketSchedPath, log)
            if (schedJson != null) {
                forceInjectSkusToLayoutAndSchedule(schedJson, allSkus)
                writeJsonAsRton(marketSchedPath, schedJson, log)
            }
        } else {
            log("⚠️ Файл products.rton не найден в кэше!")
            return false
        }

        log("🛒 Магазин PvZ 2 полностью принудительно обновлен: всё по 1 монете + семена ($seedAmount шт.) на витрине!")
        return true
    }

    private fun forceInjectSkusToLayoutAndSchedule(node: Any?, allSkus: List<String>) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    if (k.equals("SoftCost", ignoreCase = true) || k.equals("PriceCoins", ignoreCase = true) || k.equals("Cost", ignoreCase = true)) {
                        if (v is Number) node.put(k, 1)
                    }
                    if (k.equals("Products", ignoreCase = true) || k.equals("Items", ignoreCase = true) || k.equals("Offers", ignoreCase = true)) {
                        if (v is JSONArray) {
                            // Внедряем абсолютно все SKU в массивы витрины, чтобы игра обязана была их показать
                            for (sku in allSkus) {
                                var exists = false
                                for (i in 0 until v.length()) {
                                    if (v.optString(i) == sku || v.optJSONObject(i)?.optString("Sku") == sku) {
                                        exists = true
                                        break
                                    }
                                }
                                if (!exists) {
                                    v.put(sku)
                                }
                            }
                        }
                    }
                    forceInjectSkusToLayoutAndSchedule(v, allSkus)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    forceInjectSkusToLayoutAndSchedule(node.opt(i), allSkus)
                }
            }
        }
    }

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
                            if (!lowKey.contains("slot") && !lowKey.contains("weight")) {
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
        if (anyPatched) log("🌞 Мод активирован: Все растения стоят 0 солнца и перезаряжаются мгновенно!")
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
                        if (v is JSONArray) { for (i in 0 until v.length()) v.put(i, 0) }
                    }
                    if (k.equals("PacketCooldown", ignoreCase = true) || k.equals("Cooldown", ignoreCase = true)) {
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
        log("🏆 Пропатчено файлов Арены: $count (награды x100, требования снижены)")
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
