package com.layxtar.subwaysaver

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Полный снимок профиля Classic в оперативной памяти (для сохранения между вкладками и кнопки Шаг назад)
data class ClassicMemoryState(
    val walletVer: Int = 3,
    val wallet: JSONObject = JSONObject(),
    val upgradesVer: Int = 2,
    val upgrades: JSONObject = JSONObject(),
    val charsVer: Int = 3,
    val chars: JSONObject = JSONObject(),
    val boardsVer: Int = 2,
    val boards: JSONObject = JSONObject(),
    val statsVer: Int = 2,
    val stats: JSONObject = JSONObject(),
    val seasonHuntVer: Int = 2,
    val seasonHunt: JSONObject = JSONObject(),
    val challengesVer: Int = 2,
    val challenges: JSONObject = JSONObject(),
    val questsVer: Int = 1,
    val quests: JSONObject = JSONObject(),
    val cityTourVer: Int = 1,
    val cityTour: JSONObject = JSONObject(),
    val achievementsVer: Int = 1,
    val achievements: JSONObject = JSONObject(),
    val trophiesVer: Int = 1,
    val trophies: JSONObject = JSONObject(),
    val portraitsVer: Int = 1,
    val portraits: JSONObject = JSONObject(),
    val framesVer: Int = 1,
    val frames: JSONObject = JSONObject()
) {
    fun deepCopy(): ClassicMemoryState = ClassicMemoryState(
        walletVer, JSONObject(wallet.toString()),
        upgradesVer, JSONObject(upgrades.toString()),
        charsVer, JSONObject(chars.toString()),
        boardsVer, JSONObject(boards.toString()),
        statsVer, JSONObject(stats.toString()),
        seasonHuntVer, JSONObject(seasonHunt.toString()),
        challengesVer, JSONObject(challenges.toString()),
        questsVer, JSONObject(quests.toString()),
        cityTourVer, JSONObject(cityTour.toString()),
        achievementsVer, JSONObject(achievements.toString()),
        trophiesVer, JSONObject(trophies.toString()),
        portraitsVer, JSONObject(portraits.toString()),
        framesVer, JSONObject(frames.toString())
    )
}

object ClassicCryptoAndSyncManager {

    const val PKG_CLASSIC = "com.kiloo.subwaysurf"
    const val CLASSIC_PROFILE_DIR = "/storage/emulated/0/Android/data/$PKG_CLASSIC/files/profile"

    // Точный 32-байтовый ключ для СЕЙВОВ из твоего HTML-дешифратора
    private val SAVES_KEY_BYTES = byteArrayOf(
        57, 114, 107, 120, 67, 80, 108, 106,
        83, 77, 49, 71, 86, 81, 104, 87,
        119, 49, 114, 49, 114, 75, 79, 72,
        71, 99, 99, 98, 50, 105, 74, 53
    )

    private val ZERO_IV = ByteArray(16)

    private val DEFAULT_FILE_VERSIONS = mapOf(
        "wallet.json" to 3,
        "user_stats.json" to 2,
        "ad_placements.json" to 2,
        "season_hunt.json" to 2,
        "generic_challenges.json" to 2,
        "boards_inventory.json" to 2,
        "characters_inventory.json" to 3,
        "upgrades.json" to 2,
        "achievements.json" to 1,
        "quests.json" to 1,
        "city_tour.json" to 1,
        "missions.json" to 1,
        "trophies.json" to 1,
        "profile_portrait.json" to 1,
        "profile_frame.json" to 1
    )

    fun decryptBase64Data(base64Data: String): String {
        val cleanB64 = base64Data.trim()
        val encryptedBytes = Base64.decode(cleanB64, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(SAVES_KEY_BYTES, "AES"), IvParameterSpec(ZERO_IV))
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun encryptToBase64Data(plainJsonString: String): String {
        val plainBytes = plainJsonString.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(SAVES_KEY_BYTES, "AES"), IvParameterSpec(ZERO_IV))
        val encryptedBytes = cipher.doFinal(plainBytes)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    fun readClassicJson(fileName: String, log: (String) -> Unit): Pair<Int, JSONObject> {
        val defaultVer = DEFAULT_FILE_VERSIONS[fileName] ?: 1
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        val rawBytes = CryptoAndSyncManager.readFileViaShizuku(path, log)

        if (rawBytes == null || rawBytes.isEmpty()) {
            return defaultVer to createDefaultStructureForFile(fileName)
        }

        return try {
            val fileText = String(rawBytes, Charsets.UTF_8).trim()
            val outerJson = JSONObject(fileText)
            val version = outerJson.optInt("version", defaultVer)

            if (outerJson.has("data") && outerJson.opt("data") is String) {
                val dataStr = outerJson.getString("data").trim()
                val decryptedStr = if (dataStr.startsWith("{")) dataStr else decryptBase64Data(dataStr)
                version to JSONObject(decryptedStr)
            } else {
                version to outerJson
            }
        } catch (e: Exception) {
            log("⚠️ Ошибка чтения $fileName: ${e.message}")
            defaultVer to createDefaultStructureForFile(fileName)
        }
    }

    fun writeClassicJson(fileName: String, version: Int, innerJson: JSONObject, log: (String) -> Unit): Boolean {
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        return try {
            innerJson.put("lastSaved", Instant.now().toString())
            val encryptedB64 = encryptToBase64Data(innerJson.toString())
            val wrapper = JSONObject().put("version", version).put("data", encryptedB64)
            val outBytes = wrapper.toString().toByteArray(Charsets.UTF_8)
            CryptoAndSyncManager.writeFileViaShizuku(path, outBytes, log)
        } catch (e: Exception) {
            log("❌ Ошибка шифрования $fileName: ${e.message}")
            false
        }
    }

    private fun createDefaultStructureForFile(fileName: String): JSONObject {
        return JSONObject().apply {
            put("lastSaved", Instant.now().toString())
            when (fileName) {
                "wallet.json" -> {
                    put("currencies", JSONObject())
                    put("lootboxQueue", JSONObject().put("unopenedLootboxes", JSONArray()))
                    put("currencyAllowedInRun", JSONObject().put("4", true).put("5", true))
                    put("lootBoxesOpened", JSONObject())
                    put("LootTableInjectedEntryOverrides", JSONObject())
                    put("ownedOnlyBuyOnceProducts", JSONArray())
                    put("productPurchaseTimeData", JSONObject())
                    put("crewCapClaimsPerSource", JSONObject())
                }
                "upgrades.json" -> {
                    put("patchVersion", 2)
                    put("scoreModifiers", JSONObject())
                    put("currencyPickupModifiers", JSONObject())
                    put("powerupLevels", JSONObject())
                }
                "characters_inventory.json" -> {
                    put("patchVersion", 3)
                    put("selected", JSONObject().put("character", "jake").put("outfit", "default"))
                    put("owned", JSONObject())
                }
                "boards_inventory.json" -> {
                    put("patchVersion", 2)
                    put("selected", "default")
                    put("owned", JSONObject())
                }
                "user_stats.json" -> {
                    put("highScoreCollection", JSONObject())
                    put("userStatCollection", JSONObject())
                }
            }
        }
    }

    // Загрузка всех файлов Classic в память за один проход
    fun loadFullClassicState(log: (String) -> Unit): ClassicMemoryState {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, false, log)
        val (wVer, w) = readClassicJson("wallet.json", log)
        val (uVer, u) = readClassicJson("upgrades.json", log)
        val (cVer, c) = readClassicJson("characters_inventory.json", log)
        val (bVer, b) = readClassicJson("boards_inventory.json", log)
        val (sVer, s) = readClassicJson("user_stats.json", log)
        val (shVer, sh) = readClassicJson("season_hunt.json", log)
        val (chVer, ch) = readClassicJson("generic_challenges.json", log)
        val (qVer, q) = readClassicJson("quests.json", log)
        val (ctVer, ct) = readClassicJson("city_tour.json", log)
        val (aVer, a) = readClassicJson("achievements.json", log)
        val (tVer, t) = readClassicJson("trophies.json", log)
        val (pVer, p) = readClassicJson("profile_portrait.json", log)
        val (fVer, f) = readClassicJson("profile_frame.json", log)

        return ClassicMemoryState(
            wVer, w, uVer, u, cVer, c, bVer, b, sVer, s,
            shVer, sh, chVer, ch, qVer, q, ctVer, ct, aVer, a, tVer, t, pVer, p, fVer, f
        )
    }

    // Сохранение всех изменённых файлов из памяти в игру
    fun saveFullClassicState(state: ClassicMemoryState, log: (String) -> Unit): Boolean {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        var ok = true
        ok = ok and writeClassicJson("wallet.json", state.walletVer, state.wallet, log)
        ok = ok and writeClassicJson("upgrades.json", state.upgradesVer, state.upgrades, log)
        ok = ok and writeClassicJson("characters_inventory.json", state.charsVer, state.chars, log)
        ok = ok and writeClassicJson("boards_inventory.json", state.boardsVer, state.boards, log)
        ok = ok and writeClassicJson("user_stats.json", state.statsVer, state.stats, log)
        if (state.seasonHunt.length() > 1) writeClassicJson("season_hunt.json", state.seasonHuntVer, state.seasonHunt, log)
        if (state.challenges.length() > 1) writeClassicJson("generic_challenges.json", state.challengesVer, state.challenges, log)
        if (state.quests.length() > 1) writeClassicJson("quests.json", state.questsVer, state.quests, log)
        if (state.cityTour.length() > 1) writeClassicJson("city_tour.json", state.cityTourVer, state.cityTour, log)
        if (state.achievements.length() > 1) writeClassicJson("achievements.json", state.achievementsVer, state.achievements, log)
        if (state.trophies.length() > 1) writeClassicJson("trophies.json", state.trophiesVer, state.trophies, log)
        if (state.portraits.length() > 1) writeClassicJson("profile_portrait.json", state.portraitsVer, state.portraits, log)
        if (state.frames.length() > 1) writeClassicJson("profile_frame.json", state.framesVer, state.frames, log)
        return ok
    }

    // ==================== ФУНКЦИИ МОДИФИКАЦИИ В ПАМЯТИ ====================

    // 1. Установка валюты (с поддержкой создания с нуля + авто-разблокировкой SkipAdTickets и PvpTickets)
    fun setCurrencyInMemory(wallet: JSONObject, id: String, amount: Long) {
        val currencies = wallet.optJSONObject("currencies") ?: JSONObject()

        fun writeSingleSlot(slotId: String, value: Long) {
            val existing = currencies.optJSONObject(slotId)
            if (existing != null) {
                existing.put("value", value)
                if (!existing.has("expirationType")) existing.put("expirationType", 0)
                if (!existing.has("contextType")) existing.put("contextType", 0)
                if (!existing.has("contextData")) existing.put("contextData", JSONObject())
            } else {
                currencies.put(slotId, JSONObject().apply {
                    put("value", value)
                    if (slotId == "6") {
                        put("expirationValue", 1793577599999L)
                        put("expirationType", 1)
                    } else {
                        put("expirationType", 0)
                    }
                    put("contextType", 0)
                    put("contextData", JSONObject())
                })
            }
        }

        writeSingleSlot(id, amount)

        // Если меняем SkipAdTickets ("11"), дублируем в "13" и обязательно добавляем покупку пакета билетов, иначе игра их сбрасывает!
        if (id == "11" && amount > 0) {
            writeSingleSlot("13", amount)
            addOwnedProductsToWallet(wallet, listOf(
                "welcomepack_skipAds_tickets",
                "skipAds_tickets_pack",
                "skipAds_tickets_pack_2",
                "skipAds_tickets_pack_3_unlimited",
                "no_ads_bundle"
            ))
        }

        // Если меняем Жетоны/Билеты гонок ("12"), дублируем в слот PvP-билетов ("14")
        if (id == "12" && amount > 0) {
            writeSingleSlot("14", amount)
        }

        wallet.put("currencies", currencies)
        wallet.put("currencyAllowedInRun", JSONObject().put("4", true).put("5", true))
    }

    fun addOwnedProductsToWallet(wallet: JSONObject, productIds: List<String>) {
        val ownedArr = wallet.optJSONArray("ownedOnlyBuyOnceProducts") ?: JSONArray()
        val existing = mutableSetOf<String>()
        for (i in 0 until ownedArr.length()) existing.add(ownedArr.getString(i))
        for (prodId in productIds) {
            if (existing.add(prodId)) ownedArr.put(prodId)
        }
        wallet.put("ownedOnlyBuyOnceProducts", ownedArr)
    }

    // 2. ТОЛЬКО Двойные Монеты (без прокачки способностей/бустеров!)
    fun setDoubleCoinsOnly(state: ClassicMemoryState, enabled: Boolean) {
        val pickupMods = state.upgrades.optJSONObject("currencyPickupModifiers") ?: JSONObject()
        if (enabled) {
            pickupMods.put("doubleCoins", JSONObject().apply {
                put("value", JSONObject().put("id", "doubleCoins").put("type", 0).put("subType", 0).put("value", 2))
                put("expirationType", 0); put("contextType", 0); put("contextData", JSONObject())
            })
            addOwnedProductsToWallet(state.wallet, listOf("coin_doubler"))
        } else {
            pickupMods.remove("doubleCoins")
        }
        state.upgrades.put("currencyPickupModifiers", pickupMods)
    }

    fun isDoubleCoinsEnabled(state: ClassicMemoryState): Boolean {
        return state.upgrades.optJSONObject("currencyPickupModifiers")?.has("doubleCoins") == true
    }

    // 3. Отдельная прокачка уровней способностей (Джетпак, Кроссовки, Магнит, x2 Звёзды) и множителя
    fun maxPowerupLevelsAndScoreMultiplier(state: ClassicMemoryState) {
        state.upgrades.put("patchVersion", 2)
        val scoreMods = state.upgrades.optJSONObject("scoreModifiers") ?: JSONObject()
        fun putScoreMod(id: String, value: Int) {
            scoreMods.put(id, JSONObject().apply {
                put("value", JSONObject().put("id", id).put("type", 2).put("subType", 3).put("value", value))
                put("expirationType", 0); put("contextType", 0); put("contextData", JSONObject())
            })
        }
        putScoreMod("permanent_score_multiplier", 5)
        putScoreMod("permanent_score_multiplier_3", 3)
        putScoreMod("permanent_score_multiplier_1", 1)
        state.upgrades.put("scoreModifiers", scoreMods)

        state.upgrades.put("powerupLevels", JSONObject().apply {
            put("jetpack", 6); put("superSneakers", 6); put("magnet", 6); put("doubleScore", 6)
        })
        addOwnedProductsToWallet(state.wallet, listOf("multiplier_pack"))
    }

    // 4. Редактор медалей Top Run (Бронза="103", Серебро="104", Золото="105", Алмаз="106", Чемпион="107")
    fun getTopRunMedals(state: ClassicMemoryState): Map<String, Int> {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        return mapOf(
            "103" to statsCol.optInt("103", 0), // Bronze
            "104" to statsCol.optInt("104", 0), // Silver
            "105" to statsCol.optInt("105", 0), // Gold
            "106" to statsCol.optInt("106", 0), // Diamond
            "107" to statsCol.optInt("107", 0)  // Champion
        )
    }

    fun setTopRunMedals(state: ClassicMemoryState, medals: Map<String, Int>) {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        for ((k, v) in medals) {
            statsCol.put(k, v)
        }
        state.stats.put("userStatCollection", statsCol)

        // Также обновляем достижение Top Run (achievement_21) в achievements.json
        val achEntries = state.achievements.optJSONObject("achievementEntries")
        val topRunAch = achEntries?.optJSONObject("achievement_21")
        if (topRunAch != null) {
            val totalGoldOrHigher = (medals["105"] ?: 0) + (medals["106"] ?: 0) + (medals["107"] ?: 0)
            topRunAch.put("progress", maxOf(topRunAch.optInt("progress", 0), totalGoldOrHigher))
            topRunAch.put("highestProgress", maxOf(topRunAch.optInt("highestProgress", 0), totalGoldOrHigher))
        }
    }

    // 5. Проверка и переключение статуса «Открыто / Закрыто» для каждого сёрфера и костюма
    fun isSurferUnlocked(state: ClassicMemoryState, surferId: String): Boolean {
        return state.chars.optJSONObject("owned")?.has(surferId) == true
    }

    fun getOwnedOutfitsForSurfer(state: ClassicMemoryState, surferId: String): Set<String> {
        val arr = state.chars.optJSONObject("owned")?.optJSONObject(surferId)?.optJSONArray("ownedOutfits") ?: return emptySet()
        val res = mutableSetOf<String>()
        for (i in 0 until arr.length()) res.add(arr.getString(i))
        return res
    }

    fun toggleSurferUnlock(state: ClassicMemoryState, surferId: String, unlock: Boolean, allOutfits: List<String>) {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        if (unlock) {
            val outfitArr = JSONArray()
            allOutfits.forEach { outfitArr.put(it) }
            owned.put(surferId, JSONObject().apply {
                put("id", surferId)
                put("ownedOutfits", outfitArr)
                put("lastOutfit", "default")
            })
        } else {
            if (surferId != "jake") owned.remove(surferId)
        }
        state.chars.put("owned", owned)
    }

    fun toggleSurferOutfit(state: ClassicMemoryState, surferId: String, outfitId: String, unlock: Boolean) {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        val surferObj = owned.optJSONObject(surferId) ?: JSONObject().apply {
            put("id", surferId)
            put("ownedOutfits", JSONArray().put("default"))
            put("lastOutfit", "default")
        }
        val currentSet = getOwnedOutfitsForSurfer(state, surferId).toMutableSet()
        currentSet.add("default")
        if (unlock) currentSet.add(outfitId) else if (outfitId != "default") currentSet.remove(outfitId)

        val newArr = JSONArray()
        currentSet.forEach { newArr.put(it) }
        surferObj.put("ownedOutfits", newArr)
        if (unlock) surferObj.put("lastOutfit", outfitId)
        owned.put(surferId, surferObj)
        state.chars.put("owned", owned)
    }

    fun selectSurfer(state: ClassicMemoryState, surferId: String, outfitId: String = "default") {
        state.chars.put("selected", JSONObject().put("character", surferId).put("outfit", outfitId))
    }

    fun unlockAllClassicSurfersInMemory(state: ClassicMemoryState) {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        for ((id, pair) in ClassicDatabase.CLASSIC_SURFERS) {
            val outfitArr = JSONArray()
            pair.second.forEach { outfitArr.put(it) }
            owned.put(id, JSONObject().put("id", id).put("ownedOutfits", outfitArr).put("lastOutfit", "default"))
        }
        state.chars.put("owned", owned)
    }

    // 6. Проверка и переключение статуса «Открыто / Закрыто» для каждой доски и её способностей
    fun isBoardUnlocked(state: ClassicMemoryState, boardId: String): Boolean {
        return state.boards.optJSONObject("owned")?.has(boardId) == true
    }

    fun getOwnedUpgradesForBoard(state: ClassicMemoryState, boardId: String): Set<String> {
        val upgObj = state.boards.optJSONObject("owned")?.optJSONObject(boardId)?.optJSONObject("ownedUpgrades") ?: return emptySet()
        return upgObj.keys().asSequence().filter { upgObj.optBoolean(it, false) }.toSet()
    }

    fun toggleBoardUnlock(state: ClassicMemoryState, boardId: String, unlock: Boolean, allUpgrades: List<String>) {
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        if (unlock) {
            val upgObj = JSONObject()
            allUpgrades.forEach { upgObj.put(it, true) }
            owned.put(boardId, JSONObject().put("id", boardId).put("ownedUpgrades", upgObj))
        } else {
            if (boardId != "default") owned.remove(boardId)
        }
        state.boards.put("owned", owned)
    }

    fun toggleBoardUpgrade(state: ClassicMemoryState, boardId: String, upgradeId: String, unlock: Boolean) {
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        val boardObj = owned.optJSONObject(boardId) ?: JSONObject().put("id", boardId).put("ownedUpgrades", JSONObject())
        val upgObj = boardObj.optJSONObject("ownedUpgrades") ?: JSONObject()
        if (unlock) upgObj.put(upgradeId, true) else upgObj.remove(upgradeId)
        boardObj.put("ownedUpgrades", upgObj)
        owned.put(boardId, boardObj)
        state.boards.put("owned", owned)
    }

    fun selectBoard(state: ClassicMemoryState, boardId: String) {
        state.boards.put("selected", boardId)
    }

    fun unlockAllClassicBoardsInMemory(state: ClassicMemoryState) {
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        for ((id, pair) in ClassicDatabase.CLASSIC_BOARDS) {
            val upgObj = JSONObject()
            pair.second.forEach { upgObj.put(it, true) }
            owned.put(id, JSONObject().put("id", id).put("ownedUpgrades", upgObj))
        }
        state.boards.put("owned", owned)
    }

    // 7. Прохождение ВСЕХ Ивентов Classic, где дают персонажей, доски, ключи и награды (Season Hunt + Challenges + Quests + Gauntlet)
    fun completeAllEventsAndSeasonPassInMemory(state: ClassicMemoryState) {
        // А) Сезонная Охота / Surfer Pass (season_hunt.json) — открываем все 40 ступеней для сбора
        state.seasonHunt.put("collectedTokens", 99999)
        val tiersArr = JSONArray()
        for (idx in 0..40) {
            // Статус 1 = Разблокировано (кнопка «Забрать» в игре)
            val obj = JSONObject().put("mainRewardStatus", JSONArray().put(1))
            if (idx > 0) obj.put("tierIndex", idx)
            tiersArr.put(obj)
        }
        state.seasonHunt.put("rewardsThisSeason", tiersArr)
        state.seasonHunt.put("lastUnlockedTierSeen", 40)

        // Разблокируем премиум-ветку Surfer Pass Plus в wallet.json
        addOwnedProductsToWallet(state.wallet, ClassicDatabase.ONE_TIME_IAP_PRODUCTS.map { it.first })

        // Б) Ивентовые забеги (generic_challenges.json): Trick or Treat, Floor Is Lava, Tag Time Attack, Marathon, Subway Showdown
        val challengeStates = state.challenges.optJSONObject("challengeStates") ?: JSONObject()
        val chKeys = challengeStates.keys()
        while (chKeys.hasNext()) {
            val key = chKeys.next()
            val ch = challengeStates.getJSONObject(key)
            // Ставим рекорд выше финальной планки (3 000 000 очков)
            val rStates = ch.optJSONArray("rewardStates") ?: JSONArray()
            var maxReqScore = 50000
            for (i in 0 until rStates.length()) {
                val rs = rStates.getJSONObject(i)
                val req = rs.optInt("RequiredScore", 0)
                if (req > maxReqScore) maxReqScore = req
                // Если награда ещё не собрана (State != 10), ставим State = 2 (Готово к сбору) и убираем рекламу!
                if (rs.optInt("State", 0) != 10) {
                    rs.put("State", 2)
                }
                rs.put("AdRequired", false)
            }
            ch.put("currentScore", maxReqScore + 1000)
            ch.put("highScore", maxReqScore + 1000)
            ch.put("lastSeenScore", maxReqScore + 1000)
            ch.put("lastSubmittedScore", maxReqScore + 1000)
            // Снимаем таймеры ожидания сёрферов и 24/48/72/96-часовые замки этапов
            ch.put("rewardUnlockOffset", JSONArray().put(0).put(0).put(0).put(0))
            ch.optJSONObject("requirements")?.put("cooldownItems", JSONObject())
        }
        state.challenges.put("challengeStates", challengeStates)

        // В) Квесты сезона (quests.json) — начисляем 5000 очков и открываем все 12 наград
        state.quests.put("progressPoints", 5000)
        state.quests.put("lastSeenProgressPoints", 5000)
        val questRewards = state.quests.optJSONArray("rewards") ?: JSONArray()
        for (i in 0 until questRewards.length()) {
            val qr = questRewards.getJSONObject(i)
            if (qr.optInt("state", 0) != 10) {
                qr.put("state", 1) // Готово к сбору
            }
        }
        state.quests.put("rewards", questRewards)

        // Г) Испытание Gauntlet (city_tour.json)
        val gauntlet = state.cityTour.optJSONObject("activeGauntletInstance")
        if (gauntlet != null) {
            val gTiers = gauntlet.optJSONArray("tiers") ?: JSONArray()
            for (i in 0 until gTiers.length()) {
                gTiers.getJSONObject(i).put("goal", 0)
            }
        }
    }

    fun unlockAllPortraitsFramesAndTrophiesInMemory(state: ClassicMemoryState) {
        state.portraits.put("patchVersion", 1)
        val ownedPortraits = state.portraits.optJSONObject("owned") ?: JSONObject()
        for (pId in ClassicDatabase.ALL_PORTRAITS) {
            ownedPortraits.put(pId, JSONObject().put("id", pId).put("isSeen", true))
        }
        state.portraits.put("owned", ownedPortraits)

        state.frames.put("patchVersion", 1)
        val ownedFrames = state.frames.optJSONObject("owned") ?: JSONObject()
        for (fId in ClassicDatabase.ALL_FRAMES) {
            ownedFrames.put(fId, JSONObject().put("id", fId).put("isSeen", true))
        }
        state.frames.put("owned", ownedFrames)

        val trArr = JSONArray()
        ClassicDatabase.ALL_TROPHIES.forEach { trArr.put(it) }
        state.trophies.put("trophiesEarned", trArr)
        state.trophies.put("trophyCompletions", JSONObject().put("TrophySet1", 4).put("TrophySet2", 4))

        val entries = state.achievements.optJSONObject("achievementEntries") ?: JSONObject()
        val keys = entries.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val entry = entries.getJSONObject(k)
            val goals = entry.optJSONArray("goals") ?: continue
            val maxGoal = goals.optInt(goals.length() - 1, 100)
            entry.put("currentTier", maxOf(0, goals.length() - 1))
            entry.put("progress", maxGoal)
            entry.put("highestProgress", maxGoal)
            val claimArr = JSONArray()
            for (i in 0 until goals.length()) claimArr.put(true)
            entry.put("claimState", claimArr)
            entry.put("markAsViewed", true)
        }
        state.achievements.put("achievementEntries", entries)
    }

    fun addLootboxesToQueueInMemory(state: ClassicMemoryState, boxId: String, count: Int) {
        val queueObj = state.wallet.optJSONObject("lootboxQueue") ?: JSONObject()
        val unopened = queueObj.optJSONArray("unopenedLootboxes") ?: JSONArray()
        repeat(count) {
            unopened.put(JSONObject().put("id", boxId).put("source", "editor_gift"))
        }
        queueObj.put("unopenedLootboxes", unopened)
        state.wallet.put("lootboxQueue", queueObj)
    }
}
