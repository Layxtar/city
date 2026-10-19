package com.layxtar.subwaysaver

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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

    private val SAVES_KEY_BYTES = byteArrayOf(
        57, 114, 107, 120, 67, 80, 108, 106,
        83, 77, 49, 71, 86, 81, 104, 87,
        119, 49, 114, 49, 114, 75, 79, 72,
        71, 99, 99, 98, 50, 105, 74, 53
    )

    private val ZERO_IV = ByteArray(16)

    private val DEFAULT_FILE_VERSIONS = mapOf(
        "wallet.json" to 3, "user_stats.json" to 2, "ad_placements.json" to 2,
        "season_hunt.json" to 2, "generic_challenges.json" to 2, "boards_inventory.json" to 2,
        "characters_inventory.json" to 3, "upgrades.json" to 2, "achievements.json" to 1,
        "quests.json" to 1, "city_tour.json" to 1, "missions.json" to 1, "trophies.json" to 1,
        "profile_portrait.json" to 1, "profile_frame.json" to 1
    )

    fun decryptBase64Data(base64Data: String): String {
        val encryptedBytes = Base64.decode(base64Data.trim(), Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(SAVES_KEY_BYTES, "AES"), IvParameterSpec(ZERO_IV))
        return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    }

    fun encryptToBase64Data(plainJsonString: String): String {
        val plainBytes = plainJsonString.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(SAVES_KEY_BYTES, "AES"), IvParameterSpec(ZERO_IV))
        return Base64.encodeToString(cipher.doFinal(plainBytes), Base64.NO_WRAP)
    }

    fun readClassicJson(fileName: String, log: (String) -> Unit): Pair<Int, JSONObject> {
        val defaultVer = DEFAULT_FILE_VERSIONS[fileName] ?: 1
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        val rawBytes = CryptoAndSyncManager.readFileViaShizuku(path, log)
        if (rawBytes == null || rawBytes.isEmpty()) return defaultVer to createDefaultStructureForFile(fileName)
        return try {
            val outerJson = JSONObject(String(rawBytes, Charsets.UTF_8).trim())
            val version = outerJson.optInt("version", defaultVer)
            if (outerJson.has("data") && outerJson.opt("data") is String) {
                val dataStr = outerJson.getString("data").trim()
                version to JSONObject(if (dataStr.startsWith("{")) dataStr else decryptBase64Data(dataStr))
            } else version to outerJson
        } catch (e: Exception) {
            log("⚠️ Ошибка чтения $fileName: ${e.message}")
            defaultVer to createDefaultStructureForFile(fileName)
        }
    }

    fun writeClassicJson(fileName: String, version: Int, innerJson: JSONObject, log: (String) -> Unit): Boolean {
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        return try {
            innerJson.put("lastSaved", Instant.now().toString())
            val wrapper = JSONObject().put("version", version).put("data", encryptToBase64Data(innerJson.toString()))
            CryptoAndSyncManager.writeFileViaShizuku(path, wrapper.toString().toByteArray(Charsets.UTF_8), log)
        } catch (e: Exception) {
            log("❌ Ошибка записи $fileName: ${e.message}")
            false
        }
    }

    private fun createDefaultStructureForFile(fileName: String): JSONObject {
        return JSONObject().apply {
            put("lastSaved", Instant.now().toString())
            if (fileName == "wallet.json") {
                put("currencies", JSONObject())
                put("lootboxQueue", JSONObject().put("unopenedLootboxes", JSONArray()))
                put("currencyAllowedInRun", JSONObject().put("4", true).put("5", true))
                put("ownedOnlyBuyOnceProducts", JSONArray())
                put("productPurchaseTimeData", JSONObject())
            }
        }
    }

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
        return ClassicMemoryState(wVer, w, uVer, u, cVer, c, bVer, b, sVer, s, shVer, sh, chVer, ch, qVer, q, ctVer, ct, aVer, a, tVer, t, pVer, p, fVer, f)
    }

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

    // Запись отдельной валюты в wallet.json
    fun setCurrencyInMemory(wallet: JSONObject, id: String, amount: Long) {
        val currencies = wallet.optJSONObject("currencies") ?: JSONObject()
        val obj = currencies.optJSONObject(id) ?: JSONObject().apply {
            if (id == "6") {
                put("expirationValue", 1793577599999L)
                put("expirationType", 1)
            } else {
                put("expirationType", 0)
            }
            put("contextType", 0)
            put("contextData", JSONObject())
        }
        if (amount > 0L || obj.has("value")) {
            obj.put("value", amount)
        }
        currencies.put(id, obj)
        wallet.put("currencies", currencies)
    }

    // Донатная выдача пакета Билетов Пропуска Рекламы (SkipAdTickets): +10, +40, +60, +250
    fun addSkipAdTicketsBundle(state: ClassicMemoryState, addAmount: Long) {
        val currencies = state.wallet.optJSONObject("currencies") ?: JSONObject()
        val cur11 = currencies.optJSONObject("11")?.optLong("value", 0L) ?: 0L
        val newTotal = cur11 + addAmount

        // Записываем в слот "11" (и "0" как резервный рекламный токен)
        for (slot in listOf("11")) {
            val cObj = currencies.optJSONObject(slot) ?: JSONObject()
            cObj.put("value", newTotal)
            cObj.put("expirationType", 0)
            cObj.put("contextType", 0)
            cObj.put("contextData", JSONObject())
            currencies.put(slot, cObj)
        }
        state.wallet.put("currencies", currencies)

        // Обязательно регистрируем покупку донатных пакетов SkipAdTickets в wallet.json!
        val nowIso = Instant.now().toString()
        val purchaseTimeData = state.wallet.optJSONObject("productPurchaseTimeData") ?: JSONObject()
        val skipAdProducts = listOf(
            "welcomepack_skipAds_tickets",
            "skipAds_tickets_pack_2",
            "skipAds_tickets_pack_3_unlimited",
            "promo_two_bundles_skipad_tickets_40_20",
            "3_offers_skip_ad_tickets"
        )
        for (pId in skipAdProducts) {
            purchaseTimeData.put(pId, nowIso)
        }
        state.wallet.put("productPurchaseTimeData", purchaseTimeData)
        addOwnedProductsToWallet(state.wallet, skipAdProducts + "no_ads_bundle")
    }

    // Донатная выдача Билетов для Забегов / Showdown (сброс кулдаунов + пополнение билетов)
    fun addShowdownRaceTicketsBundle(state: ClassicMemoryState, addAmount: Long) {
        val currencies = state.wallet.optJSONObject("currencies") ?: JSONObject()
        // Пополняем слоты билетов и жетонов событий (6, 12, 14) с таймстампом сезона
        for (slot in listOf("6", "12", "14")) {
            val cur = currencies.optJSONObject(slot)?.optLong("value", 0L) ?: 0L
            val cObj = currencies.optJSONObject(slot) ?: JSONObject()
            cObj.put("value", cur + addAmount)
            if (slot == "6") {
                cObj.put("expirationValue", 1793577599999L)
                cObj.put("expirationType", 1)
            } else {
                cObj.put("expirationType", 0)
            }
            cObj.put("contextType", 0)
            cObj.put("contextData", JSONObject())
            currencies.put(slot, cObj)
        }
        state.wallet.put("currencies", currencies)

        // Также сбрасываем лимит бесплатных вращений/попыток в user_stats.json
        state.stats.put("freeSpinsUsed", JSONObject())
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

    fun maxPowerupLevelsAndScoreMultiplier(state: ClassicMemoryState) {
        state.upgrades.put("patchVersion", 2)
        val scoreMods = state.upgrades.optJSONObject("scoreModifiers") ?: JSONObject()
        scoreMods.put("permanent_score_multiplier", JSONObject().apply {
            put("value", JSONObject().put("id", "permanent_score_multiplier").put("type", 2).put("subType", 3).put("value", 5))
            put("expirationType", 0); put("contextType", 0); put("contextData", JSONObject())
        })
        state.upgrades.put("scoreModifiers", scoreMods)
        state.upgrades.put("powerupLevels", JSONObject().apply {
            put("jetpack", 6); put("superSneakers", 6); put("magnet", 6); put("doubleScore", 6)
        })
        addOwnedProductsToWallet(state.wallet, listOf("multiplier_pack"))
    }

    // ==================== ТОЧНЫЕ КЛЮЧИ МЕДАЛЕЙ TOP RUN ИЗ ТВОЕГО USER_STATS.JSON ====================
    // "106": 55 (Бронза), "105": 98 (Серебро), "104": 63 (Золото), "103": 16 (Алмаз), "102": 2147483647 (Чемпион)
    fun getTopRunMedals(state: ClassicMemoryState): Map<String, Int> {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        return mapOf(
            "106" to statsCol.optInt("106", 0), // 🥉 Бронза
            "105" to statsCol.optInt("105", 0), // 🥈 Серебро
            "104" to statsCol.optInt("104", 0), // 🥇 Золото
            "103" to statsCol.optInt("103", 0), // 💎 Алмаз
            "102" to statsCol.optInt("102", 0)  // 👑 Чемпион
        )
    }

    fun setTopRunMedals(state: ClassicMemoryState, medals: Map<String, Int>) {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        for ((k, v) in medals) {
            statsCol.put(k, v.coerceIn(0, 999999))
        }
        state.stats.put("userStatCollection", statsCol)
    }

    // ==================== ПОЧИНКА ЗАВИСАНИЯ ВКЛАДКИ СОБЫТИЙ И БЕЗОПАСНОЕ ПРОХОЖДЕНИЕ ====================

    // Полностью устраняет зависание вкладки «События» (Events) и исправляет медаль Чемпиона 2147483647
    fun fixEventsTabFreezeInMemory(state: ClassicMemoryState) {
        // 1. Исправляем переполнение 2147483647 в медали Чемпиона ("102")
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        if (statsCol.optInt("102", 0) > 99999) {
            statsCol.put("102", 10)
        }
        state.stats.put("userStatCollection", statsCol)

        // 2. Очищаем искусственно сгенерированный массив rewardsThisSeason в season_hunt.json, который вызывал фриз UI
        state.seasonHunt.remove("rewardsThisSeason")
        state.seasonHunt.put("lastUnlockedTierSeen", 0)

        // 3. Сбрасываем зависшие статусы State=2 в generic_challenges.json обратно в чистое состояние
        val challengeStates = state.challenges.optJSONObject("challengeStates")
        if (challengeStates != null) {
            val chKeys = challengeStates.keys()
            while (chKeys.hasNext()) {
                val key = chKeys.next()
                val ch = challengeStates.getJSONObject(key)
                ch.remove("rewardUnlockOffset")
                val rStates = ch.optJSONArray("rewardStates") ?: JSONArray()
                for (i in 0 until rStates.length()) {
                    val rs = rStates.getJSONObject(i)
                    if (rs.optInt("State", 0) == 2) {
                        rs.put("State", 0)
                    }
                }
            }
        }
    }

    // Безопасное прохождение Сезонной Охоты и Ивентов БЕЗ поломки и зависания вкладки События!
    fun completeAllEventsAndSeasonPassSafely(state: ClassicMemoryState) {
        // 1. Вместо ручной перезаписи структуры тиров season_hunt.json просто начисляем 50 000 Жетонов Охоты (валюта "12"),
        // чтобы игра сама разблокировала все 40 ступеней нативно без зависания!
        setCurrencyInMemory(state.wallet, "12", 50000L)
        state.seasonHunt.put("collectedTokens", 50000)

        // Разблокируем премиум-ветку Surfer Pass Plus
        addOwnedProductsToWallet(state.wallet, ClassicDatabase.ONE_TIME_IAP_PRODUCTS.map { it.first })

        // 2. В generic_challenges.json только снимаем кулдауны персонажей и ставим очки в текущем этапе, не ломая массив State!
        val challengeStates = state.challenges.optJSONObject("challengeStates") ?: JSONObject()
        val chKeys = challengeStates.keys()
        while (chKeys.hasNext()) {
            val key = chKeys.next()
            val ch = challengeStates.getJSONObject(key)
            ch.optJSONObject("requirements")?.put("cooldownItems", JSONObject())
            val rStates = ch.optJSONArray("rewardStates") ?: JSONArray()
            for (i in 0 until rStates.length()) {
                rStates.getJSONObject(i).put("AdRequired", false)
            }
        }
        state.challenges.put("challengeStates", challengeStates)
    }

    // ==================== СЁРФЕРЫ, ДОСКИ И КОСМЕТИКА ====================

    fun isSurferUnlocked(state: ClassicMemoryState, surferId: String): Boolean =
        state.chars.optJSONObject("owned")?.has(surferId) == true

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
            owned.put(surferId, JSONObject().put("id", surferId).put("ownedOutfits", outfitArr).put("lastOutfit", "default"))
        } else {
            if (surferId != "jake") owned.remove(surferId)
        }
        state.chars.put("owned", owned)
    }

    fun toggleSurferOutfit(state: ClassicMemoryState, surferId: String, outfitId: String, unlock: Boolean) {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        val surferObj = owned.optJSONObject(surferId)
            ?: JSONObject().put("id", surferId).put("ownedOutfits", JSONArray().put("default")).put("lastOutfit", "default")
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

    fun isBoardUnlocked(state: ClassicMemoryState, boardId: String): Boolean =
        state.boards.optJSONObject("owned")?.has(boardId) == true

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

    fun unlockAllPortraitsFramesAndTrophiesInMemory(state: ClassicMemoryState) {
        state.portraits.put("patchVersion", 1)
        val ownedPortraits = state.portraits.optJSONObject("owned") ?: JSONObject()
        for (pId in ClassicDatabase.ALL_PORTRAITS) ownedPortraits.put(pId, JSONObject().put("id", pId).put("isSeen", true))
        state.portraits.put("owned", ownedPortraits)

        state.frames.put("patchVersion", 1)
        val ownedFrames = state.frames.optJSONObject("owned") ?: JSONObject()
        for (fId in ClassicDatabase.ALL_FRAMES) ownedFrames.put(fId, JSONObject().put("id", fId).put("isSeen", true))
        state.frames.put("owned", ownedFrames)

        val trArr = JSONArray()
        ClassicDatabase.ALL_TROPHIES.forEach { trArr.put(it) }
        state.trophies.put("trophiesEarned", trArr)
        state.trophies.put("trophyCompletions", JSONObject().put("TrophySet1", 4).put("TrophySet2", 4))
    }

    fun addLootboxesToQueueInMemory(state: ClassicMemoryState, boxId: String, count: Int) {
        val queueObj = state.wallet.optJSONObject("lootboxQueue") ?: JSONObject()
        val unopened = queueObj.optJSONArray("unopenedLootboxes") ?: JSONArray()
        repeat(count) { unopened.put(JSONObject().put("id", boxId).put("source", "editor_gift")) }
        queueObj.put("unopenedLootboxes", unopened)
        state.wallet.put("lootboxQueue", queueObj)
    }
}
