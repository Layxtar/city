package com.layxtar.subwaysaver

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class ActiveChallengeInfo(
    val id: String,
    val title: String,
    val currentScore: Long,
    val maxScoreRequired: Long,
    val targetCity: String
)

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
            put("patchVersion", 1)
            when (fileName) {
                "wallet.json" -> {
                    put("currencies", JSONObject())
                    put("lootboxQueue", JSONObject().put("unopenedLootboxes", JSONArray()))
                    put("currencyAllowedInRun", JSONObject().put("4", true).put("5", true))
                    put("ownedOnlyBuyOnceProducts", JSONArray())
                }
                "characters_inventory.json" -> {
                    put("patchVersion", 2)
                    put("owned", JSONObject().put("jake", JSONObject().apply {
                        put("id", "jake")
                        put("ownedOutfits", JSONArray().put("default"))
                        put("lastOutfit", "default")
                        put("isSeen", true)
                    }))
                    put("selected", JSONObject().put("character", "jake").put("outfit", "default"))
                }
                "boards_inventory.json" -> {
                    put("patchVersion", 1)
                    put("owned", JSONObject().put("default", JSONObject().apply {
                        put("id", "default")
                        put("ownedUpgrades", JSONObject())
                        put("isSeen", true)
                    }))
                    put("selected", "default")
                }
                "profile_portrait.json" -> {
                    put("owned", JSONObject().put("jake_portrait", JSONObject().apply {
                        put("id", "jake_portrait")
                        put("isSeen", true)
                    }))
                    put("selected", "jake_portrait")
                }
                "profile_frame.json" -> {
                    put("owned", JSONObject().put("default_frame", JSONObject().apply {
                        put("id", "default_frame")
                        put("isSeen", true)
                    }))
                    put("selected", "default_frame")
                }
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
        if (state.seasonHunt.length() > 0) ok = ok and writeClassicJson("season_hunt.json", state.seasonHuntVer, state.seasonHunt, log)
        if (state.challenges.length() > 0) ok = ok and writeClassicJson("generic_challenges.json", state.challengesVer, state.challenges, log)
        if (state.quests.length() > 0) ok = ok and writeClassicJson("quests.json", state.questsVer, state.quests, log)
        if (state.cityTour.length() > 0) ok = ok and writeClassicJson("city_tour.json", state.cityTourVer, state.cityTour, log)
        if (state.achievements.length() > 0) ok = ok and writeClassicJson("achievements.json", state.achievementsVer, state.achievements, log)
        if (state.trophies.length() > 0) ok = ok and writeClassicJson("trophies.json", state.trophiesVer, state.trophies, log)
        if (state.portraits.length() > 0) ok = ok and writeClassicJson("profile_portrait.json", state.portraitsVer, state.portraits, log)
        if (state.frames.length() > 0) ok = ok and writeClassicJson("profile_frame.json", state.framesVer, state.frames, log)
        return ok
    }

    fun setCurrencyInMemory(wallet: JSONObject, id: String, amount: Long) {
        val currencies = wallet.optJSONObject("currencies") ?: JSONObject()
        val obj = currencies.optJSONObject(id) ?: JSONObject().apply {
            put("expirationType", 0)
            put("contextType", 0)
            put("contextData", JSONObject())
        }
        obj.put("value", amount)
        currencies.put(id, obj)
        wallet.put("currencies", currencies)
    }

    // Починенная функция выдачи токенов персонажей (Jake, Tricky, Fresh, Yutani, Spike) + их авто-открытие
    fun unlockAllTokenCharactersAndMaxTokens(state: ClassicMemoryState) {
        val tokenIds = listOf("7", "8", "9", "10", "11")
        for (tId in tokenIds) {
            setCurrencyInMemory(state.wallet, tId, 999L)
        }
        val tokenChars = listOf("jake", "tricky", "fresh", "yutani", "spike")
        for (charId in tokenChars) {
            val outfits = ClassicDatabase.CLASSIC_SURFERS.find { it.first == charId }?.second?.second ?: listOf("default")
            toggleSurferUnlock(state, charId, true, outfits)
        }
    }

    fun setDoubleCoinsOnly(state: ClassicMemoryState, enabled: Boolean) {
        val pickupMods = state.upgrades.optJSONObject("currencyPickupModifiers") ?: JSONObject()
        if (enabled) {
            pickupMods.put("doubleCoins", JSONObject().apply {
                put("value", JSONObject().put("id", "doubleCoins").put("type", 0).put("subType", 0).put("value", 2))
                put("expirationType", 0); put("contextType", 0); put("contextData", JSONObject())
            })
            val ownedArr = state.wallet.optJSONArray("ownedOnlyBuyOnceProducts") ?: JSONArray()
            var hasDoubler = false
            for (i in 0 until ownedArr.length()) if (ownedArr.getString(i) == "coin_doubler") hasDoubler = true
            if (!hasDoubler) ownedArr.put("coin_doubler")
            state.wallet.put("ownedOnlyBuyOnceProducts", ownedArr)
        } else {
            pickupMods.remove("doubleCoins")
        }
        state.upgrades.put("currencyPickupModifiers", pickupMods)
    }

    fun isDoubleCoinsEnabled(state: ClassicMemoryState): Boolean =
        state.upgrades.optJSONObject("currencyPickupModifiers")?.has("doubleCoins") == true

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
    }

    fun getTopRunMedals(state: ClassicMemoryState): Map<String, Int> {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        return mapOf(
            "106" to statsCol.optInt("106", 0),
            "105" to statsCol.optInt("105", 0),
            "104" to statsCol.optInt("104", 0),
            "103" to statsCol.optInt("103", 0),
            "102" to statsCol.optInt("102", 0)
        )
    }

    fun setTopRunMedals(state: ClassicMemoryState, medals: Map<String, Int>) {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        for ((k, v) in medals) {
            statsCol.put(k, v.coerceIn(0, 999999))
        }
        state.stats.put("userStatCollection", statsCol)
    }

    fun fixEventsTabFreezeInMemory(state: ClassicMemoryState) {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        if (statsCol.optInt("102", 0) > 99999) {
            statsCol.put("102", 10)
        }
        state.stats.put("userStatCollection", statsCol)
        state.seasonHunt.remove("rewardsThisSeason")
        state.seasonHunt.put("lastUnlockedTierSeen", 0)

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
                    if (rs.optInt("State", 0) == 2) rs.put("State", 0)
                }
            }
        }
    }

    fun getActiveChallenges(state: ClassicMemoryState): List<ActiveChallengeInfo> {
        val list = mutableListOf<ActiveChallengeInfo>()
        val challengeStates = state.challenges.optJSONObject("challengeStates") ?: return list
        val keys = challengeStates.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val ch = challengeStates.optJSONObject(key) ?: continue

            val curScore = ch.optLong("currentScore", 0L)
            val city = ch.optString("targetCity", "default")

            var maxReq = 0L
            val rStates = ch.optJSONArray("rewardStates")
            if (rStates != null) {
                for (i in 0 until rStates.length()) {
                    val req = rStates.getJSONObject(i).optLong("RequiredScore", 0L)
                    if (req > maxReq) maxReq = req
                }
            }

            val friendlyTitle = when {
                key.contains("marathon", ignoreCase = true) -> "🏃 Марафон (Marathon)"
                key.contains("pvp", ignoreCase = true) || key.contains("showdown", ignoreCase = true) -> "🏁 PvP-Забег (Subway Showdown)"
                key.contains("lava", ignoreCase = true) -> "🌋 Пол — это лава (Floor Is Lava)"
                key.contains("tag", ignoreCase = true) || key.contains("time", ignoreCase = true) -> "⏱️ Забег на время (Tag Time Attack)"
                key.contains("mystery", ignoreCase = true) -> "🎁 Таинственные препятствия"
                else -> "🏆 Событие: $key"
            }

            list.add(ActiveChallengeInfo(key, friendlyTitle, curScore, maxReq, city))
        }
        return list
    }

    fun setChallengeScoreCustom(
        state: ClassicMemoryState,
        challengeId: String,
        targetScore: Long,
        unlockStagesReward: Boolean
    ) {
        val challengeStates = state.challenges.optJSONObject("challengeStates") ?: return
        val ch = challengeStates.optJSONObject(challengeId) ?: return

        ch.put("currentScore", targetScore)
        ch.put("highScore", maxOf(ch.optLong("highScore", 0L), targetScore))
        ch.put("lastSeenScore", targetScore)
        ch.put("lastSubmittedScore", targetScore)
        ch.optJSONObject("requirements")?.put("cooldownItems", JSONObject())

        if (unlockStagesReward) {
            val rStates = ch.optJSONArray("rewardStates")
            if (rStates != null) {
                for (i in 0 until rStates.length()) {
                    rStates.getJSONObject(i).put("AdRequired", false)
                }
            }
        }
    }

    // ==================== ДОСТИЖЕНИЯ ====================

    fun unlockAllAchievementsInMemory(state: ClassicMemoryState) {
        state.achievements.put("patchVersion", 1)
        val achObj = state.achievements.optJSONObject("achievements") ?: JSONObject()
        for (info in ClassicDatabase.ACHIEVEMENTS_LIST) {
            achObj.put(info.id, JSONObject().apply {
                put("tier", info.maxTier)
                put("progress", 999999)
                put("isClaimed", false)
            })
        }
        state.achievements.put("achievements", achObj)
    }

    // ==================== РАМКИ И АВАТАРКИ (ТОЧНЫЕ ID ИЗ DECRYPTED_PLAYERPROFILE.JSON) ====================

    fun isPortraitUnlocked(state: ClassicMemoryState, portraitId: String): Boolean =
        state.portraits.optJSONObject("owned")?.has(portraitId) == true

    fun togglePortraitUnlock(state: ClassicMemoryState, portraitId: String, unlock: Boolean) {
        state.portraits.put("patchVersion", 1)
        val owned = state.portraits.optJSONObject("owned") ?: JSONObject()
        if (unlock) {
            owned.put(portraitId, JSONObject().put("id", portraitId).put("isSeen", true))
        } else {
            if (portraitId != "jake_portrait") owned.remove(portraitId)
        }
        state.portraits.put("owned", owned)
    }

    fun selectPortrait(state: ClassicMemoryState, portraitId: String) {
        state.portraits.put("patchVersion", 1)
        togglePortraitUnlock(state, portraitId, true)
        state.portraits.put("selected", portraitId)
    }

    fun unlockAllPortraitsInMemory(state: ClassicMemoryState) {
        state.portraits.put("patchVersion", 1)
        val owned = state.portraits.optJSONObject("owned") ?: JSONObject()
        for ((id, _) in ClassicDatabase.ALL_PORTRAITS) {
            owned.put(id, JSONObject().put("id", id).put("isSeen", true))
        }
        state.portraits.put("owned", owned)
    }

    fun isFrameUnlocked(state: ClassicMemoryState, frameId: String): Boolean =
        state.frames.optJSONObject("owned")?.has(frameId) == true

    fun toggleFrameUnlock(state: ClassicMemoryState, frameId: String, unlock: Boolean) {
        state.frames.put("patchVersion", 1)
        val owned = state.frames.optJSONObject("owned") ?: JSONObject()
        if (unlock) {
            owned.put(frameId, JSONObject().put("id", frameId).put("isSeen", true))
        } else {
            if (frameId != "default_frame") owned.remove(frameId)
        }
        state.frames.put("owned", owned)
    }

    fun selectFrame(state: ClassicMemoryState, frameId: String) {
        state.frames.put("patchVersion", 1)
        toggleFrameUnlock(state, frameId, true)
        state.frames.put("selected", frameId)
    }

    fun unlockAllFramesInMemory(state: ClassicMemoryState) {
        state.frames.put("patchVersion", 1)
        val owned = state.frames.optJSONObject("owned") ?: JSONObject()
        for ((id, _) in ClassicDatabase.ALL_FRAMES) {
            owned.put(id, JSONObject().put("id", id).put("isSeen", true))
        }
        state.frames.put("owned", owned)
    }

    // ==================== СЁРФЕРЫ И ИХ ПОД-СКИНЫ (КОСТЮМЫ) ====================

    fun isSurferUnlocked(state: ClassicMemoryState, surferId: String): Boolean =
        state.chars.optJSONObject("owned")?.has(surferId) == true

    fun getOwnedOutfitsForSurfer(state: ClassicMemoryState, surferId: String): Set<String> {
        val arr = state.chars.optJSONObject("owned")?.optJSONObject(surferId)?.optJSONArray("ownedOutfits") ?: return emptySet()
        val res = mutableSetOf<String>()
        for (i in 0 until arr.length()) res.add(arr.getString(i))
        return res
    }

    fun toggleSurferUnlock(state: ClassicMemoryState, surferId: String, unlock: Boolean, allOutfits: List<String>) {
        state.chars.put("patchVersion", 2)
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        if (unlock) {
            val outfitArr = JSONArray().put("default")
            allOutfits.forEach { if (it != "default") outfitArr.put(it) }
            owned.put(surferId, JSONObject().apply {
                put("id", surferId)
                put("ownedOutfits", outfitArr)
                put("lastOutfit", "default")
                put("isSeen", true)
            })
        } else {
            if (surferId != "jake") owned.remove(surferId)
        }
        state.chars.put("owned", owned)
    }

    fun toggleSurferOutfit(state: ClassicMemoryState, surferId: String, outfitId: String, unlock: Boolean) {
        state.chars.put("patchVersion", 2)
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        val surferObj = owned.optJSONObject(surferId) ?: JSONObject().apply {
            put("id", surferId)
            put("ownedOutfits", JSONArray().put("default"))
            put("lastOutfit", "default")
            put("isSeen", true)
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
        state.chars.put("patchVersion", 2)
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        if (!owned.has(surferId)) {
            owned.put(surferId, JSONObject().apply {
                put("id", surferId)
                put("ownedOutfits", JSONArray().put("default"))
                put("lastOutfit", "default")
                put("isSeen", true)
            })
            state.chars.put("owned", owned)
        }
        state.chars.put("selected", JSONObject().put("character", surferId).put("outfit", outfitId))
    }

    fun unlockAllClassicSurfersInMemory(state: ClassicMemoryState) {
        state.chars.put("patchVersion", 2)
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        for ((id, pair) in ClassicDatabase.CLASSIC_SURFERS) {
            val outfitArr = JSONArray().put("default")
            pair.second.forEach { if (it != "default") outfitArr.put(it) }
            owned.put(id, JSONObject().apply {
                put("id", id)
                put("ownedOutfits", outfitArr)
                put("lastOutfit", "default")
                put("isSeen", true)
            })
        }
        state.chars.put("owned", owned)
    }

    // ==================== ХОВЕРБОРДЫ И АПГРЕЙДЫ ====================

    fun isBoardUnlocked(state: ClassicMemoryState, boardId: String): Boolean =
        state.boards.optJSONObject("owned")?.has(boardId) == true

    fun getOwnedUpgradesForBoard(state: ClassicMemoryState, boardId: String): Set<String> {
        val upgObj = state.boards.optJSONObject("owned")?.optJSONObject(boardId)?.optJSONObject("ownedUpgrades") ?: return emptySet()
        return upgObj.keys().asSequence().filter { upgObj.optBoolean(it, false) }.toSet()
    }

    fun toggleBoardUnlock(state: ClassicMemoryState, boardId: String, unlock: Boolean, allUpgrades: List<String>) {
        state.boards.put("patchVersion", 1)
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        if (unlock) {
            val upgObj = JSONObject()
            allUpgrades.forEach { upgObj.put(it, true) }
            owned.put(boardId, JSONObject().apply {
                put("id", boardId)
                put("ownedUpgrades", upgObj)
                put("isSeen", true)
            })
        } else {
            if (boardId != "default") owned.remove(boardId)
        }
        state.boards.put("owned", owned)
    }

    fun toggleBoardUpgrade(state: ClassicMemoryState, boardId: String, upgradeId: String, unlock: Boolean) {
        state.boards.put("patchVersion", 1)
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        val boardObj = owned.optJSONObject(boardId) ?: JSONObject().apply {
            put("id", boardId)
            put("ownedUpgrades", JSONObject())
            put("isSeen", true)
        }
        val upgObj = boardObj.optJSONObject("ownedUpgrades") ?: JSONObject()
        if (unlock) upgObj.put(upgradeId, true) else upgObj.remove(upgradeId)
        boardObj.put("ownedUpgrades", upgObj)
        owned.put(boardId, boardObj)
        state.boards.put("owned", owned)
    }

    fun selectBoard(state: ClassicMemoryState, boardId: String) {
        state.boards.put("patchVersion", 1)
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        if (!owned.has(boardId)) {
            owned.put(boardId, JSONObject().put("id", boardId).put("ownedUpgrades", JSONObject()).put("isSeen", true))
            state.boards.put("owned", owned)
        }
        state.boards.put("selected", boardId)
    }

    fun unlockAllClassicBoardsInMemory(state: ClassicMemoryState) {
        state.boards.put("patchVersion", 1)
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        for ((id, pair) in ClassicDatabase.CLASSIC_BOARDS) {
            val upgObj = JSONObject()
            pair.second.forEach { upgObj.put(it, true) }
            owned.put(id, JSONObject().apply {
                put("id", id)
                put("ownedUpgrades", upgObj)
                put("isSeen", true)
            })
        }
        state.boards.put("owned", owned)
    }
}
