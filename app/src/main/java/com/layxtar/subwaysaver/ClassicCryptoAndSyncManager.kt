package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ClassicCryptoAndSyncManager {

    const val PKG_CLASSIC = "com.kiloo.subwaysurf"
    const val CLASSIC_PROFILE_DIR = "/storage/emulated/0/Android/data/$PKG_CLASSIC/files/profile"

    private val CLASSIC_AES_KEY = byteArrayOf(
        57, 116, 107, 119, 86, 80, 102, 106,
        86, 114, 52, 52, 54, 82, 104, 51,
        119, 109, 117, 48, 115, 73, 74, 71,
        89, 98, 99, 102, 53, 57, 48, 53
    )

    private val ZERO_IV = ByteArray(16)

    fun decryptClassicFile(rawBytes: ByteArray): JSONObject {
        val textAttempt = runCatching { String(rawBytes, Charsets.UTF_8).trim() }.getOrNull()
        if (textAttempt != null && textAttempt.startsWith("{")) {
            return try {
                val outer = JSONObject(textAttempt)
                if (outer.has("data") && outer.opt("data") is String) {
                    val innerStr = outer.getString("data")
                    if (innerStr.startsWith("{")) JSONObject(innerStr) else outer
                } else {
                    outer
                }
            } catch (_: Exception) {
                JSONObject()
            }
        }

        return try {
            if (rawBytes.size <= 24) return JSONObject()
            val cipherBytes = rawBytes.copyOfRange(24, rawBytes.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(CLASSIC_AES_KEY, "AES"), IvParameterSpec(ZERO_IV))
            val decryptedBytes = cipher.doFinal(cipherBytes)
            JSONObject(String(decryptedBytes, Charsets.UTF_8))
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun encryptClassicFile(originalBytes: ByteArray?, json: JSONObject): ByteArray {
        json.put("lastSaved", Instant.now().toString())

        val textAttempt = originalBytes?.let { runCatching { String(it, Charsets.UTF_8).trim() }.getOrNull() }
        if (textAttempt != null && textAttempt.startsWith("{")) {
            val outer = JSONObject(textAttempt)
            if (outer.has("data") && outer.opt("data") is String) {
                outer.put("data", json.toString())
                return outer.toString().toByteArray(Charsets.UTF_8)
            }
            return json.toString(2).toByteArray(Charsets.UTF_8)
        }

        val plainBytes = json.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(CLASSIC_AES_KEY, "AES"), IvParameterSpec(ZERO_IV))
        val encryptedBytes = cipher.doFinal(plainBytes)

        val sha1 = MessageDigest.getInstance("SHA-1").digest(encryptedBytes)
        val headerLen = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(20).array()

        return ByteBuffer.allocate(4 + 20 + encryptedBytes.size)
            .put(headerLen)
            .put(sha1)
            .put(encryptedBytes)
            .array()
    }

    fun readClassicJson(fileName: String, log: (String) -> Unit): Pair<ByteArray?, JSONObject> {
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        val raw = CryptoAndSyncManager.readFileViaShizuku(path, log)
        if (raw != null) {
            return try {
                raw to decryptClassicFile(raw)
            } catch (e: Exception) {
                log("⚠️ Ошибка дешифровки $fileName: ${e.message}")
                raw to JSONObject()
            }
        }
        return null to JSONObject()
    }

    fun writeClassicJson(fileName: String, originalRaw: ByteArray?, json: JSONObject, log: (String) -> Unit): Boolean {
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        val outBytes = encryptClassicFile(originalRaw, json)
        return CryptoAndSyncManager.writeFileViaShizuku(path, outBytes, log)
    }

    fun updateWalletAndInjectDonations(
        currencyMap: Map<String, Long>,
        unlockAllBuyOnce: Boolean,
        lootboxToQueue: String?,
        lootboxCount: Int,
        log: (String) -> Unit
    ) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (raw, wallet) = readClassicJson("wallet.json", log)
        val currencies = wallet.optJSONObject("currencies") ?: JSONObject()

        for ((id, amount) in currencyMap) {
            val cObj = currencies.optJSONObject(id) ?: JSONObject()
            cObj.put("value", amount)
            cObj.put("expirationType", 0)
            cObj.put("contextType", 0)
            if (!cObj.has("contextData")) cObj.put("contextData", JSONObject())
            currencies.put(id, cObj)
        }
        wallet.put("currencies", currencies)
        wallet.put("currencyAllowedInRun", JSONObject().put("4", true).put("5", true))

        if (unlockAllBuyOnce) {
            val ownedArr = wallet.optJSONArray("ownedOnlyBuyOnceProducts") ?: JSONArray()
            val existing = mutableSetOf<String>()
            for (i in 0 until ownedArr.length()) existing.add(ownedArr.getString(i))
            for ((prodId, _) in ClassicDatabase.ONE_TIME_IAP_PRODUCTS) {
                if (existing.add(prodId)) ownedArr.put(prodId)
            }
            wallet.put("ownedOnlyBuyOnceProducts", ownedArr)
        }

        if (lootboxToQueue != null && lootboxCount > 0) {
            val queueObj = wallet.optJSONObject("lootboxQueue") ?: JSONObject()
            val unopened = queueObj.optJSONArray("unopenedLootboxes") ?: JSONArray()
            repeat(lootboxCount) {
                unopened.put(JSONObject().put("id", lootboxToQueue).put("source", "editor_gift"))
            }
            queueObj.put("unopenedLootboxes", unopened)
            wallet.put("lootboxQueue", queueObj)
        }

        writeClassicJson("wallet.json", raw, wallet, log)
    }

    fun unlockDoubleCoinsAndMaxUpgrades(log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (raw, upgrades) = readClassicJson("upgrades.json", log)
        upgrades.put("patchVersion", 2)

        val scoreMods = upgrades.optJSONObject("scoreModifiers") ?: JSONObject()
        fun putScoreMod(id: String, value: Int) {
            scoreMods.put(id, JSONObject().apply {
                put("value", JSONObject().put("id", id).put("type", 2).put("subType", 3).put("value", value))
                put("expirationType", 0); put("contextType", 0); put("contextData", JSONObject())
            })
        }
        putScoreMod("permanent_score_multiplier", 5)
        putScoreMod("permanent_score_multiplier_3", 3)
        putScoreMod("permanent_score_multiplier_1", 1)
        upgrades.put("scoreModifiers", scoreMods)

        val pickupMods = upgrades.optJSONObject("currencyPickupModifiers") ?: JSONObject()
        pickupMods.put("doubleCoins", JSONObject().apply {
            put("value", JSONObject().put("id", "doubleCoins").put("type", 0).put("subType", 0).put("value", 2))
            put("expirationType", 0); put("contextType", 0); put("contextData", JSONObject())
        })
        upgrades.put("currencyPickupModifiers", pickupMods)

        upgrades.put("powerupLevels", JSONObject().apply {
            put("jetpack", 6); put("superSneakers", 6); put("magnet", 6); put("doubleScore", 6)
        })

        writeClassicJson("upgrades.json", raw, upgrades, log)
        log("🪙 Двойные монеты и все бустеры 6 ур. активированы!")
    }

    fun unlockClassicSurfers(specificSurferId: String? = null, selectSurferId: String? = null, log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (raw, inv) = readClassicJson("characters_inventory.json", log)
        inv.put("patchVersion", 3)
        val owned = inv.optJSONObject("owned") ?: JSONObject()

        val listToProcess = if (specificSurferId != null) {
            ClassicDatabase.CLASSIC_SURFERS.filter { it.first == specificSurferId }
        } else {
            ClassicDatabase.CLASSIC_SURFERS
        }

        for ((id, pair) in listToProcess) {
            val outfits = pair.second
            val existing = owned.optJSONObject(id)
            val outfitArr = JSONArray()
            val merged = mutableSetOf<String>()
            if (existing != null) {
                val oldArr = existing.optJSONArray("ownedOutfits") ?: JSONArray()
                for (i in 0 until oldArr.length()) merged.add(oldArr.getString(i))
            }
            for (o in outfits) merged.add(o)
            for (o in merged) outfitArr.put(o)

            owned.put(id, JSONObject().apply {
                put("id", id)
                put("ownedOutfits", outfitArr)
                put("lastOutfit", "default")
            })
        }
        inv.put("owned", owned)
        if (selectSurferId != null) {
            inv.put("selected", JSONObject().put("character", selectSurferId).put("outfit", "default"))
        }

        writeClassicJson("characters_inventory.json", raw, inv, log)
        log("✅ Сёрферы Classic успешно обновлены!")
    }

    fun unlockClassicBoards(specificBoardId: String? = null, selectBoardId: String? = null, log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (raw, inv) = readClassicJson("boards_inventory.json", log)
        inv.put("patchVersion", 2)
        val owned = inv.optJSONObject("owned") ?: JSONObject()

        val listToProcess = if (specificBoardId != null) {
            ClassicDatabase.CLASSIC_BOARDS.filter { it.first == specificBoardId }
        } else {
            ClassicDatabase.CLASSIC_BOARDS
        }

        for ((id, pair) in listToProcess) {
            val upgrades = pair.second
            val upgObj = owned.optJSONObject(id)?.optJSONObject("ownedUpgrades") ?: JSONObject()
            for (u in upgrades) upgObj.put(u, true)
            owned.put(id, JSONObject().put("id", id).put("ownedUpgrades", upgObj))
        }
        inv.put("owned", owned)
        if (selectBoardId != null) inv.put("selected", selectBoardId)

        writeClassicJson("boards_inventory.json", raw, inv, log)
        log("✅ Ховерборды Classic успешно обновлены!")
    }

    fun maxSeasonHuntMissionsAndAchievements(log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)

        val (rawSh, sh) = readClassicJson("season_hunt.json", log)
        sh.put("collectedTokens", 99999)
        val tiersArr = JSONArray()
        for (idx in 0..40) {
            val obj = JSONObject().put("mainRewardStatus", JSONArray().put(1))
            if (idx > 0) obj.put("tierIndex", idx)
            tiersArr.put(obj)
        }
        sh.put("rewardsThisSeason", tiersArr)
        sh.put("lastUnlockedTierSeen", 40)
        writeClassicJson("season_hunt.json", rawSh, sh, log)

        val (rawMis, mis) = readClassicJson("missions.json", log)
        mis.put("patchVersion", 1)
        mis.put("currentMissionSet", 31)
        mis.put("claimedMissionSet", 30)
        writeClassicJson("missions.json", rawMis, mis, log)

        val (rawTr, tr) = readClassicJson("trophies.json", log)
        val trArr = JSONArray()
        ClassicDatabase.ALL_TROPHIES.forEach { trArr.put(it) }
        tr.put("trophiesEarned", trArr)
        tr.put("trophyCompletions", JSONObject().put("TrophySet1", 4).put("TrophySet2", 4))
        writeClassicJson("trophies.json", rawTr, tr, log)

        val (rawAch, ach) = readClassicJson("achievements.json", log)
        val entries = ach.optJSONObject("achievementEntries") ?: JSONObject()
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
        ach.put("achievementEntries", entries)
        writeClassicJson("achievements.json", rawAch, ach, log)

        log("🏆 Сезонная охота, миссии, трофеи и ачивки разблокированы!")
    }

    fun unlockAllPortraitsAndFrames(log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)

        val (rawPort, portJson) = readClassicJson("profile_portrait.json", log)
        portJson.put("patchVersion", 1)
        if (!portJson.has("selected")) portJson.put("selected", "countcorvin_illustration_portrait")
        val ownedPortraits = portJson.optJSONObject("owned") ?: JSONObject()
        for (pId in ClassicDatabase.ALL_PORTRAITS) {
            ownedPortraits.put(pId, JSONObject().put("id", pId).put("isSeen", true))
        }
        portJson.put("owned", ownedPortraits)
        writeClassicJson("profile_portrait.json", rawPort, portJson, log)

        val (rawFrame, frameJson) = readClassicJson("profile_frame.json", log)
        frameJson.put("patchVersion", 1)
        if (!frameJson.has("selected")) frameJson.put("selected", "collection_revamp_15_frame")
        val ownedFrames = frameJson.optJSONObject("owned") ?: JSONObject()
        for (fId in ClassicDatabase.ALL_FRAMES) {
            ownedFrames.put(fId, JSONObject().put("id", fId).put("isSeen", true))
        }
        frameJson.put("owned", ownedFrames)
        writeClassicJson("profile_frame.json", rawFrame, frameJson, log)

        log("🖼️ Портреты и рамки успешно открыты!")
    }
}
