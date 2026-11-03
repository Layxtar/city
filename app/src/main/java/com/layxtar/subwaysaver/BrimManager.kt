package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject

data class BrimScannedCatalog(
    val characterIds: List<String>,
    val gearIds: List<String>,
    val petIds: List<String>,
    val gachaBoxIds: List<String>,
    val straightUpgradeIds: List<String>,
    val achievementIds: List<String>
)

object BrimManager {

    // Автоматически находит точный путь к файлу user_profile (даже если user_profile — это папка с файлом внутри!)
    fun resolveProfileFilePath(log: (String) -> Unit): String {
        for (candidate in BrimDatabase.PROFILE_CANDIDATE_PATHS) {
            val (_, out) = CryptoAndSyncManager.execSh(
                "if [ -f '$candidate' ]; then echo 'IS_FILE'; elif [ -d '$candidate' ]; then ls -1 '$candidate' 2>/dev/null | head -n 1; fi"
            )
            val res = out.trim()
            if (res == "IS_FILE") {
                return candidate
            } else if (res.isNotEmpty()) {
                val innerFile = "$candidate/$res"
                if (CryptoAndSyncManager.fileExists(innerFile)) {
                    return innerFile
                }
            }
        }
        return "${BrimDatabase.BASE_FILES_DIR}/user_profile/user_profile.json"
    }

    fun resolveGameDataDir(): String? {
        for (dir in BrimDatabase.GAMEDATA_CANDIDATE_DIRS) {
            val (_, out) = CryptoAndSyncManager.execSh("if [ -d '$dir' ]; then echo 'FOUND'; fi")
            if (out.contains("FOUND")) return dir
        }
        // Поиск папки gamedata внутри files или cache
        val (_, findOut) = CryptoAndSyncManager.execSh(
            "find '/storage/emulated/0/Android/data/${BrimDatabase.PKG_BRIM}' -type d -name 'gamedata' 2>/dev/null | head -n 1"
        )
        val found = findOut.trim()
        return if (found.isNotEmpty()) found else null
    }

    fun loadUserProfile(log: (String) -> Unit): Pair<String, JSONObject>? {
        CryptoAndSyncManager.stopGameForSync(BrimDatabase.PKG_BRIM, false, log)
        val path = resolveProfileFilePath(log)
        val bytes = CryptoAndSyncManager.readFileViaShizuku(path, log) ?: return null
        return try {
            val json = JSONObject(String(bytes, Charsets.UTF_8).trim())
            log("✅ Профиль Blades of Brim прочитан: $path")
            path to json
        } catch (e: Exception) {
            log("❌ Ошибка разбора user_profile ($path): ${e.message}")
            null
        }
    }

    fun saveUserProfile(path: String, profileJson: JSONObject, log: (String) -> Unit): Boolean {
        CryptoAndSyncManager.stopGameForSync(BrimDatabase.PKG_BRIM, true, log)
        return try {
            val data = profileJson.toString().toByteArray(Charsets.UTF_8)
            val ok = CryptoAndSyncManager.writeFileViaShizuku(path, data, log)
            if (ok) log("✅ Профиль Blades of Brim успешно сохранён!")
            ok
        } catch (e: Exception) {
            log("❌ Ошибка сохранения профиля Brim: ${e.message}")
            false
        }
    }

    // ==================== АВТО-СКАНЕР ID ИЗ ПАПКИ TOWER/GAMEDATA ====================

    private fun extractKeysFromGameDataFile(gameDataDir: String?, fileName: String): List<String> {
        if (gameDataDir == null) return emptyList()
        val path = "$gameDataDir/$fileName"
        val bytes = CryptoAndSyncManager.readFileViaShizuku(path) {} ?: return emptyList()
        return try {
            val text = String(bytes, Charsets.UTF_8).trim()
            val result = mutableSetOf<String>()
            if (text.startsWith("{")) {
                val root = JSONObject(text)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = root.opt(k)
                    if (v is JSONObject && !k.startsWith("_") && k != "version" && k != "schema") {
                        val subKeys = v.keys()
                        var addedSub = false
                        while (subKeys.hasNext()) {
                            val sk = subKeys.next()
                            if (v.opt(sk) is JSONObject) {
                                result.add(sk)
                                addedSub = true
                            }
                        }
                        if (!addedSub) result.add(k)
                    } else if (v is JSONArray) {
                        for (i in 0 until v.length()) {
                            val item = v.optJSONObject(i) ?: continue
                            val id = item.optString("id", item.optString("Id", ""))
                            if (id.isNotEmpty()) result.add(id)
                        }
                    }
                }
            } else if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val id = item.optString("id", item.optString("Id", ""))
                    if (id.isNotEmpty()) result.add(id)
                }
            }
            result.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun scanCatalogFromDevice(profileJson: JSONObject?): BrimScannedCatalog {
        val gDir = resolveGameDataDir()

        val chars = (BrimDatabase.DEFAULT_CHARACTERS.map { it.first } +
                extractKeysFromGameDataFile(gDir, "characters.json") +
                (profileJson?.optJSONObject("characterInventory")?.keys()?.asSequence()?.toList() ?: emptyList()))
            .distinct()

        // Базовые ID снаряжения и оружия из профиля + из gear.json
        val defaultGear = listOf(
            "32", "2300", "2301", "2302",
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "33", "34", "35"
        )
        val gear = (defaultGear +
                extractKeysFromGameDataFile(gDir, "gear.json") +
                (profileJson?.optJSONObject("gearInventory")?.keys()?.asSequence()?.toList() ?: emptyList()))
            .distinct()

        val defaultPets = (1..25).map { it.toString() } + (100..120).map { it.toString() }
        val pets = (extractKeysFromGameDataFile(gDir, "pets.json").ifEmpty { defaultPets } +
                (profileJson?.optJSONObject("petInventory")?.keys()?.asSequence()?.toList() ?: emptyList()))
            .distinct()

        val defaultBoxes = listOf("1", "2", "3", "4", "5", "common_box", "rare_box", "epic_box", "gold_box", "magic_box")
        val boxes = (extractKeysFromGameDataFile(gDir, "gachaboxes.json").ifEmpty { defaultBoxes } +
                (profileJson?.optJSONObject("gachaBoxInventory")?.keys()?.asSequence()?.toList() ?: emptyList()))
            .distinct()

        val upgrades = (BrimDatabase.DEFAULT_STRAIGHT_UPGRADES.map { it.first } +
                extractKeysFromGameDataFile(gDir, "straightupgrades.json") +
                extractKeysFromGameDataFile(gDir, "boosts.json") +
                (profileJson?.optJSONObject("straightUpgrades")?.keys()?.asSequence()?.toList() ?: emptyList()))
            .distinct()

        val achievements = (BrimDatabase.DEFAULT_ACHIEVEMENT_IDS +
                extractKeysFromGameDataFile(gDir, "achievements.json") +
                (profileJson?.optJSONObject("achievements")?.keys()?.asSequence()?.toList() ?: emptyList()))
            .distinct()

        return BrimScannedCatalog(chars, gear, pets, boxes, upgrades, achievements)
    }

    // ==================== ФУНКЦИИ МОДИФИКАЦИИ USER_PROFILE.JSON ====================

    fun readCurrencies(profile: JSONObject): Map<String, Long> {
        val inv = profile.optJSONObject("currencyInventory") ?: JSONObject()
        val map = mutableMapOf<String, Long>()
        for ((id, _) in BrimDatabase.CURRENCIES) {
            map[id] = inv.optLong(id, 0L)
        }
        return map
    }

    fun writeCurrenciesAndStats(
        profile: JSONObject,
        currencies: Map<String, Long>,
        highscore: Long,
        playerLevel: Int
    ) {
        val inv = profile.optJSONObject("currencyInventory") ?: JSONObject()
        for ((id, amount) in currencies) {
            inv.put(id, amount)
        }
        profile.put("currencyInventory", inv)
        profile.put("highscore", highscore)
        profile.put("level", playerLevel.coerceAtLeast(1))
    }

    // Открывает всех героев, сохраняя выбранные шлем/броню/поножи
    fun unlockAllCharacters(profile: JSONObject, catalog: BrimScannedCatalog) {
        val charInv = profile.optJSONObject("characterInventory") ?: JSONObject()
        for (charId in catalog.characterIds) {
            if (!charInv.has(charId)) {
                charInv.put(charId, JSONObject().apply {
                    put("selectedHead", "2301")
                    put("selectedChest", "2300")
                    put("selectedLegs", "2302")
                })
            }
        }
        profile.put("characterInventory", charInv)
        profile.put("characterCount", charInv.length())
    }

    // Открывает и прокачивает всё оружие и броню в gearInventory
    fun unlockAndMaxAllGear(profile: JSONObject, catalog: BrimScannedCatalog, targetUpgradeLevel: Int = 10) {
        val gearInv = profile.optJSONObject("gearInventory") ?: JSONObject()
        for (gearId in catalog.gearIds) {
            gearInv.put(gearId, JSONObject().put("upgradeLevel", targetUpgradeLevel))
        }
        profile.put("gearInventory", gearInv)
        profile.put("gearCount", gearInv.length())
        profile.put("weaponCount", maxOf(profile.optInt("weaponCount", 1), gearInv.length() / 2))
    }

    // Открывает и прокачивает всех питомцев в petInventory
    fun unlockAndMaxAllPets(profile: JSONObject, catalog: BrimScannedCatalog, targetUpgradeLevel: Int = 10) {
        val petInv = profile.optJSONObject("petInventory") ?: JSONObject()
        for (petId in catalog.petIds) {
            petInv.put(petId, JSONObject().put("upgradeLevel", targetUpgradeLevel))
        }
        profile.put("petInventory", petInv)
        profile.put("petsCount", petInv.length())
    }

    // Прокачивает все прямые улучшения и бусты (straightUpgrades)
    fun maxAllStraightUpgrades(profile: JSONObject, catalog: BrimScannedCatalog, targetLevel: Int = 10) {
        val su = profile.optJSONObject("straightUpgrades") ?: JSONObject()
        for (upgId in catalog.straightUpgradeIds) {
            su.put(upgId, JSONObject().put("level", targetLevel))
        }
        profile.put("straightUpgrades", su)
    }

    // Добавляет сундуки в gachaBoxInventory
    fun addGachaBoxesToInventory(profile: JSONObject, catalog: BrimScannedCatalog, countPerType: Int = 50) {
        val boxInv = profile.optJSONObject("gachaBoxInventory") ?: JSONObject()
        for (boxId in catalog.gachaBoxIds) {
            val current = boxInv.opt(boxId)
            if (current is JSONObject) {
                current.put("count", countPerType)
                current.put("amount", countPerType)
            } else {
                boxInv.put(boxId, countPerType)
            }
        }
        profile.put("gachaBoxInventory", boxInv)
    }

    // Выполняет все достижения на 3 звезды, открывает все фичи и убирает туториалы
    fun completeAllAchievementsAndFeatures(profile: JSONObject, catalog: BrimScannedCatalog) {
        val ach = profile.optJSONObject("achievements") ?: JSONObject()
        for (achId in catalog.achievementIds) {
            ach.put(achId, JSONObject().apply {
                put("progress", 999999)
                put("previousMax", 999999)
                put("stars", 3)
            })
        }
        profile.put("achievements", ach)

        // Включаем все биты открытых фич и закрываем все биты обучения
        val features = profile.optJSONObject("features") ?: JSONObject()
        val stateLen = features.optString("state", "").length.coerceAtLeast(68)
        val tutLen = features.optString("tutorials", "").length.coerceAtLeast(68)
        features.put("state", "1".repeat(stateLen))
        features.put("tutorials", "1".repeat(tutLen))
        profile.put("features", features)

        // Завершаем активные цели текущей главы
        val chapters = profile.optJSONObject("chapters")
        val objectives = chapters?.optJSONObject("activeChapterObjectives")
        if (objectives != null) {
            val keys = objectives.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                objectives.optJSONObject(k)?.apply {
                    put("progress", 99999)
                    put("state", 2)
                }
            }
        }

        // Выполняем активные миссии
        val missions = profile.optJSONObject("missions")
        val activeMissions = missions?.optJSONArray("activeMissions")
        if (activeMissions != null) {
            for (i in 0 until activeMissions.length()) {
                activeMissions.optJSONObject(i)?.put("progress", 99999)
            }
        }
    }

    // ==================== ПАТЧЕР ФАЙЛОВ TOWER/GAMEDATA (ЦЕНЫ 0 МОНЕТ И БЕСПЛАТНОЕ ВОЗРОЖДЕНИЕ) ====================

    fun patchGameDataFreePricesAndRevive(log: (String) -> Unit): Boolean {
        val gDir = resolveGameDataDir()
        if (gDir == null) {
            log("⚠️ Папка tower/gamedata не найдена в кэше игры.")
            return false
        }
        CryptoAndSyncManager.stopGameForSync(BrimDatabase.PKG_BRIM, true, log)
        var patchedFiles = 0

        val targetFiles = listOf("pricelist.json", "revive.json", "economy.json", "straightupgrades.json")
        for (fName in targetFiles) {
            val fullPath = "$gDir/$fName"
            val bytes = CryptoAndSyncManager.readFileViaShizuku(fullPath) {} ?: continue
            try {
                val text = String(bytes, Charsets.UTF_8).trim()
                if (text.startsWith("{")) {
                    val json = JSONObject(text)
                    zeroPricesRecursively(json)
                    if (CryptoAndSyncManager.writeFileViaShizuku(fullPath, json.toString().toByteArray(Charsets.UTF_8), log)) {
                        patchedFiles++
                    }
                }
            } catch (_: Exception) { }
        }
        if (patchedFiles > 0) {
            log("⚡ Пропатчено $patchedFiles файлов в tower/gamedata (все цены и возрождения = 0)!")
        } else {
            log("⚠️ Файлы tower/gamedata сжаты или ещё не распакованы игрой.")
        }
        return patchedFiles > 0
    }

    private fun zeroPricesRecursively(node: Any?) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                for (k in keys) {
                    val v = node.opt(k)
                    val low = k.lowercase()
                    if (v is Number && (low.contains("price") || low.contains("cost") || low.contains("fee"))) {
                        node.put(k, 0)
                    } else {
                        zeroPricesRecursively(v)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) zeroPricesRecursively(node.opt(i))
            }
        }
    }
}
