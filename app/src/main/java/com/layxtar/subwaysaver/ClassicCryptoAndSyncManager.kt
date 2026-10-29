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
    const val CLASSIC_FILES_DIR = "/storage/emulated/0/Android/data/$PKG_CLASSIC/files"

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
            when (fileName) {
                "wallet.json" -> {
                    put("currencies", JSONObject())
                    put("lootboxQueue", JSONObject().put("unopenedLootboxes", JSONArray()))
                    put("currencyAllowedInRun", JSONObject().put("4", true).put("5", true))
                    put("ownedOnlyBuyOnceProducts", JSONArray())
                }
                "characters_inventory.json" -> {
                    put("selected", JSONObject().put("character", "jake").put("outfit", "default"))
                    put("owned", JSONObject())
                }
                "boards_inventory.json" -> {
                    put("selected", "default")
                    put("owned", JSONObject())
                }
                "profile_portrait.json" -> {
                    put("patchVersion", 1)
                    put("selected", "jake_portrait")
                    put("owned", JSONObject())
                }
                "profile_frame.json" -> {
                    put("patchVersion", 1)
                    put("selected", "default_frame")
                    put("owned", JSONObject())
                }
            }
        }
    }

    // Автоматически подтягивает существующие в сохранении или кэше игры кастомные ID персонажей/досок/портретов
    private fun syncExistingSaveIdsToDatabase(state: ClassicMemoryState) {
        state.chars.optJSONObject("owned")?.keys()?.forEach { charId ->
            ClassicDatabase.registerDiscoveredSurfer(charId)
        }
        state.boards.optJSONObject("owned")?.keys()?.forEach { boardId ->
            ClassicDatabase.registerDiscoveredBoard(boardId)
        }
        state.portraits.optJSONObject("owned")?.keys()?.forEach { pId ->
            ClassicDatabase.registerDiscoveredPortrait(pId)
        }
        state.frames.optJSONObject("owned")?.keys()?.forEach { fId ->
            ClassicDatabase.registerDiscoveredFrame(fId)
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

        val state = ClassicMemoryState(
            wVer, w, uVer, u, cVer, c, bVer, b, sVer, s,
            shVer, sh, chVer, ch, qVer, q, ctVer, ct, aVer, a, tVer, t, pVer, p, fVer, f
        )
        syncExistingSaveIdsToDatabase(state)
        return state
    }

    // ИСПРАВЛЕНО: Все ключевые файлы (включая портреты, рамки, достижения, персонажей и доски) теперь записываются ВСЕГДА!
    fun saveFullClassicState(state: ClassicMemoryState, log: (String) -> Unit): Boolean {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        var ok = true
        ok = ok and writeClassicJson("wallet.json", state.walletVer, state.wallet, log)
        ok = ok and writeClassicJson("upgrades.json", state.upgradesVer, state.upgrades, log)
        ok = ok and writeClassicJson("characters_inventory.json", state.charsVer, state.chars, log)
        ok = ok and writeClassicJson("boards_inventory.json", state.boardsVer, state.boards, log)
        ok = ok and writeClassicJson("user_stats.json", state.statsVer, state.stats, log)
        ok = ok and writeClassicJson("profile_portrait.json", state.portraitsVer, state.portraits, log)
        ok = ok and writeClassicJson("profile_frame.json", state.framesVer, state.frames, log)
        ok = ok and writeClassicJson("achievements.json", state.achievementsVer, state.achievements, log)
        ok = ok and writeClassicJson("trophies.json", state.trophiesVer, state.trophies, log)

        if (state.seasonHunt.length() > 1) writeClassicJson("season_hunt.json", state.seasonHuntVer, state.seasonHunt, log)
        if (state.challenges.length() > 1) writeClassicJson("generic_challenges.json", state.challengesVer, state.challenges, log)
        if (state.quests.length() > 1) writeClassicJson("quests.json", state.questsVer, state.quests, log)
        if (state.cityTour.length() > 1) writeClassicJson("city_tour.json", state.cityTourVer, state.cityTour, log)
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

    // Медали Top Run: 106=Бронза, 105=Серебро, 104=Золото, 103=Алмаз, 102=Чемпион
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
                key.contains("pvp", ignoreCase = true) || key.contains("showdown", ignoreCase = true) -> "🏁 Соревнование (Showdown)"
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

    // ==================== ИСПРАВЛЕННАЯ РАЗБЛОКИРОВКА ДОСТИЖЕНИЙ И ТРОФЕЕВ ====================

    fun unlockAllAchievementsInMemory(state: ClassicMemoryState) {
        state.achievements.put("patchVersion", 1)
        val achEntries = state.achievements.optJSONObject("achievementEntries")
            ?: state.achievements.optJSONObject("achievements")
            ?: JSONObject()

        for (info in ClassicDatabase.ACHIEVEMENTS_LIST) {
            val entryObj = JSONObject().apply {
                put("id", info.id)
                put("progress", 999999)
                put("tier", info.maxTier)
                put("currentTier", info.maxTier)
                put("unclaimedTiers", JSONArray())
                put("isCompleted", true)
            }
            achEntries.put(info.id, entryObj)
        }
        state.achievements.put("achievementEntries", achEntries)
        state.achievements.put("achievements", achEntries)

        // Открываем все трофеи в trophies.json
        state.trophies.put("patchVersion", 1)
        val ownedTrophies = state.trophies.optJSONObject("ownedTrophies")
            ?: state.trophies.optJSONObject("owned")
            ?: JSONObject()
        for (tId in ClassicDatabase.ALL_TROPHIES) {
            ownedTrophies.put(tId, JSONObject().put("id", tId).put("seen", true))
        }
        state.trophies.put("ownedTrophies", ownedTrophies)
        state.trophies.put("owned", ownedTrophies)
    }

    // ==================== ИСПРАВЛЕННЫЕ АВАТАРКИ И РАМКИ (РАБОТАЮТ В ЛЮБОЙ ВЕРСИИ CLASSIC!) ====================

    fun isPortraitUnlocked(state: ClassicMemoryState, portraitId: String): Boolean {
        val owned = state.portraits.optJSONObject("owned")
            ?: state.portraits.optJSONObject("ownedPortraits")
        return owned?.has(portraitId) == true
    }

    fun togglePortraitUnlock(state: ClassicMemoryState, portraitId: String, unlock: Boolean) {
        state.portraits.put("patchVersion", 1)
        val owned = state.portraits.optJSONObject("owned") ?: JSONObject()
        val ownedPortraits = state.portraits.optJSONObject("ownedPortraits") ?: JSONObject()

        if (unlock) {
            val itemObj = JSONObject().apply {
                put("id", portraitId)
                put("seen", true)
                put("isSeen", true)
                put("isNew", false)
            }
            owned.put(portraitId, itemObj)
            ownedPortraits.put(portraitId, JSONObject(itemObj.toString()))
        } else {
            owned.remove(portraitId)
            ownedPortraits.remove(portraitId)
        }
        state.portraits.put("owned", owned)
        state.portraits.put("ownedPortraits", ownedPortraits)
    }

    fun selectPortrait(state: ClassicMemoryState, portraitId: String) {
        state.portraits.put("selected", portraitId)
        state.portraits.put("selectedPortrait", portraitId)
    }

    fun unlockAllPortraitsInMemory(state: ClassicMemoryState) {
        for ((pId, _) in ClassicDatabase.ALL_PORTRAITS) {
            togglePortraitUnlock(state, pId, true)
        }
    }

    fun isFrameUnlocked(state: ClassicMemoryState, frameId: String): Boolean {
        val owned = state.frames.optJSONObject("owned")
            ?: state.frames.optJSONObject("ownedFrames")
        return owned?.has(frameId) == true
    }

    fun toggleFrameUnlock(state: ClassicMemoryState, frameId: String, unlock: Boolean) {
        state.frames.put("patchVersion", 1)
        val owned = state.frames.optJSONObject("owned") ?: JSONObject()
        val ownedFrames = state.frames.optJSONObject("ownedFrames") ?: JSONObject()

        if (unlock) {
            val itemObj = JSONObject().apply {
                put("id", frameId)
                put("seen", true)
                put("isSeen", true)
                put("isNew", false)
            }
            owned.put(frameId, itemObj)
            ownedFrames.put(frameId, JSONObject(itemObj.toString()))
        } else {
            owned.remove(frameId)
            ownedFrames.remove(frameId)
        }
        state.frames.put("owned", owned)
        state.frames.put("ownedFrames", ownedFrames)
    }

    fun selectFrame(state: ClassicMemoryState, frameId: String) {
        state.frames.put("selected", frameId)
        state.frames.put("selectedFrame", frameId)
    }

    fun unlockAllFramesInMemory(state: ClassicMemoryState) {
        for ((fId, _) in ClassicDatabase.ALL_FRAMES) {
            toggleFrameUnlock(state, fId, true)
        }
    }

    // ==================== ИСПРАВЛЕННАЯ РАЗБЛОКИРОВКА СЁРФЕРОВ И ПОДСКИНОВ (characters_inventory.json) ====================
    // Движок Classic проверяет ownedOutfits как JSONObject {"default": {"id": "default", "value": true, "seen": true}}
    // Либо как JSONArray — мы определяем нативный формат прямо по твоему jake в characters_inventory.json!

    private fun buildSurferEntryJson(
        existingCharTemplate: JSONObject?,
        surferId: String,
        outfits: Collection<String>,
        lastOutfit: String = "default"
    ): JSONObject {
        val usesObjectMapForOutfits = existingCharTemplate?.opt("ownedOutfits") is JSONObject
        val ownedOutfitsValue: Any = if (usesObjectMapForOutfits) {
            JSONObject().apply {
                for (oId in outfits) {
                    put(oId, JSONObject().put("id", oId).put("value", true).put("seen", true))
                }
            }
        } else {
            // В версии 3 characters_inventory.json ownedOutfits — это массив строк ИЛИ словарь, но обязательно с полем seen
            JSONArray().apply {
                for (oId in outfits) put(oId)
            }
        }

        return JSONObject().apply {
            put("id", surferId)
            put("lastOutfit", lastOutfit)
            put("selectedOutfit", lastOutfit)
            put("seen", true)
            put("isNew", false)
            put("ownedOutfits", ownedOutfitsValue)
        }
    }

    fun isSurferUnlocked(state: ClassicMemoryState, surferId: String): Boolean =
        state.chars.optJSONObject("owned")?.has(surferId) == true

    fun getOwnedOutfitsForSurfer(state: ClassicMemoryState, surferId: String): Set<String> {
        val surferObj = state.chars.optJSONObject("owned")?.optJSONObject(surferId) ?: return emptySet()
        val rawOutfits = surferObj.opt("ownedOutfits") ?: return setOf("default")
        val res = mutableSetOf<String>()
        when (rawOutfits) {
            is JSONArray -> {
                for (i in 0 until rawOutfits.length()) {
                    val elem = rawOutfits.get(i)
                    if (elem is String) res.add(elem)
                    else if (elem is JSONObject) res.add(elem.optString("id", "default"))
                }
            }
            is JSONObject -> {
                val keys = rawOutfits.keys()
                while (keys.hasNext()) res.add(keys.next())
            }
        }
        if (res.isEmpty()) res.add("default")
        return res
    }

    fun toggleSurferUnlock(state: ClassicMemoryState, surferId: String, unlock: Boolean, allOutfits: List<String>) {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        val jakeTemplate = owned.optJSONObject("jake")
        if (unlock) {
            val listWithDefault = (listOf("default") + allOutfits).distinct()
            owned.put(surferId, buildSurferEntryJson(jakeTemplate, surferId, listWithDefault, "default"))
        } else {
            if (surferId != "jake") owned.remove(surferId)
        }
        state.chars.put("owned", owned)
    }

    fun toggleSurferOutfit(state: ClassicMemoryState, surferId: String, outfitId: String, unlock: Boolean) {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        val jakeTemplate = owned.optJSONObject("jake")
        val currentSet = getOwnedOutfitsForSurfer(state, surferId).toMutableSet()
        currentSet.add("default")
        if (unlock) currentSet.add(outfitId) else if (outfitId != "default") currentSet.remove(outfitId)

        val lastOut = if (unlock) outfitId else "default"
        owned.put(surferId, buildSurferEntryJson(jakeTemplate, surferId, currentSet, lastOut))
        state.chars.put("owned", owned)
    }

    fun selectSurfer(state: ClassicMemoryState, surferId: String, outfitId: String = "default") {
        state.chars.put("selected", JSONObject().put("character", surferId).put("outfit", outfitId))
        state.chars.put("selectedCharacter", surferId)
        state.chars.put("selectedOutfit", outfitId)
    }

    fun unlockAllClassicSurfersInMemory(state: ClassicMemoryState) {
        val owned = state.chars.optJSONObject("owned") ?: JSONObject()
        val jakeTemplate = owned.optJSONObject("jake")
        for ((id, pair) in ClassicDatabase.CLASSIC_SURFERS) {
            val listWithDefault = (listOf("default") + pair.second).distinct()
            owned.put(id, buildSurferEntryJson(jakeTemplate, id, listWithDefault, "default"))
        }
        state.chars.put("owned", owned)
    }

    // ==================== ИСПРАВЛЕННАЯ РАЗБЛОКИРОВКА ДОСОК И СПОСОБНОСТЕЙ (boards_inventory.json) ====================

    private fun buildBoardEntryJson(
        existingBoardTemplate: JSONObject?,
        boardId: String,
        upgrades: Collection<String>
    ): JSONObject {
        val usesArrayForUpgrades = existingBoardTemplate?.opt("ownedUpgrades") is JSONArray
        val ownedUpgradesValue: Any = if (usesArrayForUpgrades) {
            JSONArray().apply {
                put("default")
                for (u in upgrades) if (u != "default") put(u)
            }
        } else {
            JSONObject().apply {
                put("default", true)
                for (u in upgrades) put(u, true)
            }
        }

        val activeUpgradesArr = JSONArray().apply {
            for (u in upgrades) if (u != "default") put(u)
        }

        return JSONObject().apply {
            put("id", boardId)
            put("seen", true)
            put("isNew", false)
            put("ownedUpgrades", ownedUpgradesValue)
            put("activeUpgrades", activeUpgradesArr)
            put("selectedUpgrades", activeUpgradesArr)
        }
    }

    fun isBoardUnlocked(state: ClassicMemoryState, boardId: String): Boolean =
        state.boards.optJSONObject("owned")?.has(boardId) == true

    fun getOwnedUpgradesForBoard(state: ClassicMemoryState, boardId: String): Set<String> {
        val boardObj = state.boards.optJSONObject("owned")?.optJSONObject(boardId) ?: return emptySet()
        val rawUpg = boardObj.opt("ownedUpgrades") ?: return emptySet()
        val res = mutableSetOf<String>()
        when (rawUpg) {
            is JSONObject -> {
                val keys = rawUpg.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k != "default" && rawUpg.optBoolean(k, true)) res.add(k)
                }
            }
            is JSONArray -> {
                for (i in 0 until rawUpg.length()) {
                    val s = rawUpg.optString(i)
                    if (s.isNotEmpty() && s != "default") res.add(s)
                }
            }
        }
        return res
    }

    fun toggleBoardUnlock(state: ClassicMemoryState, boardId: String, unlock: Boolean, allUpgrades: List<String>) {
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        val defaultTemplate = owned.optJSONObject("default")
        if (unlock) {
            owned.put(boardId, buildBoardEntryJson(defaultTemplate, boardId, allUpgrades))
        } else {
            if (boardId != "default") owned.remove(boardId)
        }
        state.boards.put("owned", owned)
    }

    fun toggleBoardUpgrade(state: ClassicMemoryState, boardId: String, upgradeId: String, unlock: Boolean) {
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        val defaultTemplate = owned.optJSONObject("default")
        val currentSet = getOwnedUpgradesForBoard(state, boardId).toMutableSet()
        if (unlock) currentSet.add(upgradeId) else currentSet.remove(upgradeId)
        owned.put(boardId, buildBoardEntryJson(defaultTemplate, boardId, currentSet))
        state.boards.put("owned", owned)
    }

    fun selectBoard(state: ClassicMemoryState, boardId: String) {
        state.boards.put("selected", boardId)
        state.boards.put("selectedBoard", boardId)
    }

    fun unlockAllClassicBoardsInMemory(state: ClassicMemoryState) {
        val owned = state.boards.optJSONObject("owned") ?: JSONObject()
        val defaultTemplate = owned.optJSONObject("default")
        for ((id, pair) in ClassicDatabase.CLASSIC_BOARDS) {
            owned.put(id, buildBoardEntryJson(defaultTemplate, id, pair.second))
        }
        state.boards.put("owned", owned)
    }

    fun addLootboxesToQueueInMemory(state: ClassicMemoryState, boxId: String, count: Int) {
        val queueObj = state.wallet.optJSONObject("lootboxQueue") ?: JSONObject()
        val unopened = queueObj.optJSONArray("unopenedLootboxes") ?: JSONArray()
        repeat(count) { unopened.put(JSONObject().put("id", boxId).put("source", "editor_gift")) }
        queueObj.put("unopenedLootboxes", unopened)
        state.wallet.put("lootboxQueue", queueObj)
    }
}
