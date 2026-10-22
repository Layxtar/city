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
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        val rawBytes = CryptoAndSyncManager.readFileViaShizuku(path, log)
        if (rawBytes == null || rawBytes.isEmpty()) return 1 to JSONObject()
        return try {
            val outerJson = JSONObject(String(rawBytes, Charsets.UTF_8).trim())
            val version = outerJson.optInt("version", 1)
            if (outerJson.has("data") && outerJson.opt("data") is String) {
                val dataStr = outerJson.getString("data").trim()
                version to JSONObject(if (dataStr.startsWith("{")) dataStr else decryptBase64Data(dataStr))
            } else version to outerJson
        } catch (_: Exception) {
            1 to JSONObject()
        }
    }

    fun writeClassicJson(fileName: String, version: Int, innerJson: JSONObject, log: (String) -> Unit): Boolean {
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        return try {
            innerJson.put("lastSaved", Instant.now().toString())
            val wrapper = JSONObject().put("version", version).put("data", encryptToBase64Data(innerJson.toString()))
            CryptoAndSyncManager.writeFileViaShizuku(path, wrapper.toString().toByteArray(Charsets.UTF_8), log)
        } catch (_: Exception) {
            false
        }
    }

    fun loadFullClassicState(log: (String) -> Unit): ClassicMemoryState {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, false, log)
        return ClassicMemoryState(
            walletVer = readClassicJson("wallet.json", log).first, wallet = readClassicJson("wallet.json", log).second,
            upgradesVer = readClassicJson("upgrades.json", log).first, upgrades = readClassicJson("upgrades.json", log).second,
            charsVer = readClassicJson("characters_inventory.json", log).first, chars = readClassicJson("characters_inventory.json", log).second,
            boardsVer = readClassicJson("boards_inventory.json", log).first, boards = readClassicJson("boards_inventory.json", log).second,
            statsVer = readClassicJson("user_stats.json", log).first, stats = readClassicJson("user_stats.json", log).second,
            seasonHuntVer = readClassicJson("season_hunt.json", log).first, seasonHunt = readClassicJson("season_hunt.json", log).second,
            challengesVer = readClassicJson("generic_challenges.json", log).first, challenges = readClassicJson("generic_challenges.json", log).second,
            questsVer = readClassicJson("quests.json", log).first, quests = readClassicJson("quests.json", log).second,
            cityTourVer = readClassicJson("city_tour.json", log).first, cityTour = readClassicJson("city_tour.json", log).second,
            achievementsVer = readClassicJson("achievements.json", log).first, achievements = readClassicJson("achievements.json", log).second,
            trophiesVer = readClassicJson("trophies.json", log).first, trophies = readClassicJson("trophies.json", log).second,
            portraitsVer = readClassicJson("profile_portrait.json", log).first, portraits = readClassicJson("profile_portrait.json", log).second,
            framesVer = readClassicJson("profile_frame.json", log).first, frames = readClassicJson("profile_frame.json", log).second
        )
    }

    fun saveFullClassicState(state: ClassicMemoryState, log: (String) -> Unit): Boolean {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        var ok = true
        ok = ok and writeClassicJson("wallet.json", state.walletVer, state.wallet, log)
        ok = ok and writeClassicJson("upgrades.json", state.upgradesVer, state.upgrades, log)
        ok = ok and writeClassicJson("characters_inventory.json", state.charsVer, state.chars, log)
        ok = ok and writeClassicJson("boards_inventory.json", state.boardsVer, state.boards, log)
        ok = ok and writeClassicJson("user_stats.json", state.statsVer, state.stats, log)
        if (state.seasonHunt.length() > 0) writeClassicJson("season_hunt.json", state.seasonHuntVer, state.seasonHunt, log)
        if (state.challenges.length() > 0) writeClassicJson("generic_challenges.json", state.challengesVer, state.challenges, log)
        if (state.achievements.length() > 0) writeClassicJson("achievements.json", state.achievementsVer, state.achievements, log)
        if (state.trophies.length() > 0) writeClassicJson("trophies.json", state.trophiesVer, state.trophies, log)
        if (state.portraits.length() > 0) writeClassicJson("profile_portrait.json", state.portraitsVer, state.portraits, log)
        if (state.frames.length() > 0) writeClassicJson("profile_frame.json", state.framesVer, state.frames, log)
        return ok
    }

    fun setCurrencyInMemory(wallet: JSONObject, id: String, amount: Long) {
        val currencies = wallet.optJSONObject("currencies") ?: JSONObject()
        val obj = currencies.optJSONObject(id) ?: JSONObject().apply {
            put("expirationType", if (id == "6") 1 else 0)
            if (id == "6") put("expirationValue", 1793577599999L)
            put("contextType", 0)
            put("contextData", JSONObject())
        }
        obj.put("value", amount)
        currencies.put(id, obj)
        wallet.put("currencies", currencies)
    }

    fun addSkipAdTicketsBundle(state: ClassicMemoryState, addAmount: Long) {
        val currencies = state.wallet.optJSONObject("currencies") ?: JSONObject()
        val cur = currencies.optJSONObject("11")?.optLong("value", 0L) ?: 0L
        val newTotal = cur + addAmount

        val cObj = currencies.optJSONObject("11") ?: JSONObject()
        cObj.put("value", newTotal)
        cObj.put("expirationType", 0); cObj.put("contextType", 0); cObj.put("contextData", JSONObject())
        currencies.put("11", cObj)
        state.wallet.put("currencies", currencies)
    }

    fun addShowdownRaceTicketsBundle(state: ClassicMemoryState, addAmount: Long) {
        val currencies = state.wallet.optJSONObject("currencies") ?: JSONObject()
        for (slot in listOf("6", "12")) {
            val cur = currencies.optJSONObject(slot)?.optLong("value", 0L) ?: 0L
            val cObj = currencies.optJSONObject(slot) ?: JSONObject()
            cObj.put("value", cur + addAmount)
            cObj.put("expirationType", if (slot == "6") 1 else 0)
            if (slot == "6") cObj.put("expirationValue", 1793577599999L)
            cObj.put("contextType", 0); cObj.put("contextData", JSONObject())
            currencies.put(slot, cObj)
        }
        state.wallet.put("currencies", currencies)
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
        for ((k, v) in medals) statsCol.put(k, v.coerceIn(0, 999999))
        state.stats.put("userStatCollection", statsCol)
    }

    fun fixEventsTabFreezeInMemory(state: ClassicMemoryState) {
        val statsCol = state.stats.optJSONObject("userStatCollection") ?: JSONObject()
        if (statsCol.optInt("102", 0) > 99999) statsCol.put("102", 10)
        state.stats.put("userStatCollection", statsCol)
        state.seasonHunt.remove("rewardsThisSeason")
        state.seasonHunt.put("lastUnlockedTierSeen", 0)
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
                key.contains("marathon", true) -> "🏃 Марафон (Marathon)"
                key.contains("pvp", true) || key.contains("showdown", true) -> "🏁 PvP-Забег (Showdown)"
                key.contains("lava", true) -> "🌋 Пол — это лава"
                else -> "🏆 Режим: $key"
            }
            list.add(ActiveChallengeInfo(key, friendlyTitle, curScore, maxReq, city))
        }
        return list
    }

    fun setChallengeScoreCustom(state: ClassicMemoryState, challengeId: String, targetScore: Long, unlockStagesReward: Boolean) {
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
                for (i in 0 until rStates.length()) rStates.getJSONObject(i).put("AdRequired", false)
            }
        }
    }

    fun unlockAllAchievementsCompletely(state: ClassicMemoryState) {
        val ach = state.achievements
        ach.put("patchVersion", 1)
        val entries = ach.optJSONObject("achievementEntries") ?: JSONObject()

        val keys = entries.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val entry = entries.getJSONObject(k)
            val goals = entry.optJSONArray("goals")
            val maxTier = if (goals != null && goals.length() > 0) goals.length() - 1 else 3
            val maxProgress = if (goals != null && goals.length() > 0) goals.optInt(goals.length() - 1, 1000) else 1000
            entry.put("currentTier", maxTier)
            entry.put("progress", maxProgress)
            entry.put("highestProgress", maxProgress)
            val claimArr = JSONArray()
            for (i in 0..maxTier) claimArr.put(true)
            entry.put("claimState", claimArr)
            entry.put("markAsViewed", true)
        }

        fun inject(id: String, targetVal: Int) {
            val obj = entries.optJSONObject(id) ?: JSONObject().apply {
                put("id", id)
                put("goals", JSONArray().put(1).put(5).put(targetVal))
            }
            obj.put("currentTier", 2)
            obj.put("progress", targetVal)
            obj.put("highestProgress", targetVal)
            obj.put("claimState", JSONArray().put(true).put(true).put(true))
            obj.put("markAsViewed", true)
            entries.put(id, obj)
        }

        inject("achievement_social_friends", 50)
        inject("achievement_brawl_stars_crew", 10)
        inject("achievement_facebook_connect", 1)
        inject("achievement_top_run_diamond", 100)

        ach.put("achievementEntries", entries)

        val trArr = JSONArray()
        ClassicDatabase.ALL_TROPHIES.forEach { trArr.put(it) }
        state.trophies.put("trophiesEarned", trArr)
        state.trophies.put("trophyCompletions", JSONObject().put("TrophySet1", 4).put("TrophySet2", 4))
    }

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

    // Методы подсчета 557 и 573 для интерфейса Classic
    fun getSurfersAndOutfitsCount(state: ClassicMemoryState): Pair<Int, Int> {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        var unlockedTotal = 0
        val keys = owned.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val obj = owned.optJSONObject(k)
            val outfits = obj?.optJSONArray("ownedOutfits")
            unlockedTotal += if (outfits != null && outfits.length() > 0) outfits.length() else 1
        }
        return unlockedTotal to maxOf(557, unlockedTotal)
    }

    fun getBoardsAndUpgradesCount(state: ClassicMemoryState): Pair<Int, Int> {
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        var unlockedTotal = 0
        val keys = owned.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val obj = owned.optJSONObject(k)
            val upgs = obj?.optJSONObject("ownedUpgrades")
            val upgCount = upgs?.keys()?.asSequence()?.count { upgs.optBoolean(it, false) } ?: 0
            unlockedTotal += (1 + upgCount)
        }
        return unlockedTotal to maxOf(573, unlockedTotal)
    }

    fun getMergedSurfersList(state: ClassicMemoryState): List<Pair<String, Pair<String, List<String>>>> {
        val result = mutableListOf<Pair<String, Pair<String, List<String>>>>()
        val seenIds = mutableSetOf<String>()
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()

        for ((id, pair) in ClassicDatabase.CLASSIC_SURFERS) {
            seenIds.add(id)
            val existingOutfits = getOwnedOutfitsForSurfer(state, id)
            val mergedOutfits = (pair.second + existingOutfits).distinct()
            result.add(id to (pair.first to mergedOutfits))
        }

        val keys = owned.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            if (seenIds.add(id)) {
                val outfits = getOwnedOutfitsForSurfer(state, id).toList().ifEmpty { listOf("default") }
                val prettyName = id.replaceFirstChar { it.uppercase() }
                result.add(id to ("$prettyName ($id)" to outfits))
            }
        }
        return result
    }

    fun getMergedBoardsList(state: ClassicMemoryState): List<Pair<String, Pair<String, List<String>>>> {
        val result = mutableListOf<Pair<String, Pair<String, List<String>>>>()
        val seenIds = mutableSetOf<String>()
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()

        for ((id, pair) in ClassicDatabase.CLASSIC_BOARDS) {
            seenIds.add(id)
            val existingUpgs = getOwnedUpgradesForBoard(state, id)
            val mergedUpgs = (pair.second + existingUpgs).distinct()
            result.add(id to (pair.first to mergedUpgs))
        }

        val keys = owned.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            if (seenIds.add(id)) {
                val upgs = getOwnedUpgradesForBoard(state, id).toList()
                val prettyName = id.replaceFirstChar { it.uppercase() }
                result.add(id to ("$prettyName ($id)" to upgs))
            }
        }
        return result
    }
}
