package com.layxtar.subwaysaver

import android.util.Base64
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

    // 32-байтовый ключ AES-256 для классического Subway Surfers
    private val CLASSIC_AES_KEY = byteArrayOf(
        57, 116, 107, 119, 86, 80, 102, 106,
        86, 114, 52, 52, 54, 82, 104, 51,
        119, 109, 117, 48, 115, 73, 74, 71,
        89, 98, 99, 102, 53, 57, 48, 53
    )

    private val ZERO_IV = ByteArray(16)

    // Умный подбор смещения заголовка (20 байт SHA-1, 24 байта Length+SHA1, 8 байт и т.д.)
    private fun decryptBinaryPayload(payload: ByteArray): JSONObject {
        val candidateOffsets = listOf(20, 24, 8, 0, 4, 16, 28, 32)
        for (offset in candidateOffsets) {
            if (payload.size <= offset) continue
            val remaining = payload.size - offset
            if (remaining % 16 != 0) continue

            try {
                val cipherBytes = payload.copyOfRange(offset, payload.size)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(CLASSIC_AES_KEY, "AES"), IvParameterSpec(ZERO_IV))
                val decryptedBytes = cipher.doFinal(cipherBytes)
                val text = String(decryptedBytes, Charsets.UTF_8).trim()
                if (text.startsWith("{") && text.endsWith("}")) {
                    return JSONObject(text)
                }
            } catch (_: Exception) {
                // Пробуем следующее смещение
            }
        }

        // Если ни одно стандартное смещение с PKCS5Padding не подошло, пробуем NoPadding и ищем '{'
        for (offset in 0 until minOf(36, payload.size)) {
            val remaining = (payload.size - offset) / 16 * 16
            if (remaining <= 0) continue
            try {
                val cipherBytes = payload.copyOfRange(offset, offset + remaining)
                val cipher = Cipher.getInstance("AES/CBC/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(CLASSIC_AES_KEY, "AES"), IvParameterSpec(ZERO_IV))
                val dec = cipher.doFinal(cipherBytes)
                // Убираем PKCS7 паддинг вручную, если он есть
                val pad = dec.last().toInt() and 0xFF
                val validLen = if (pad in 1..16) dec.size - pad else dec.size
                if (validLen > 2) {
                    val str = String(dec, 0, validLen, Charsets.UTF_8).trim()
                    val firstBrace = str.indexOf('{')
                    val lastBrace = str.lastIndexOf('}')
                    if (firstBrace != -1 && lastBrace > firstBrace) {
                        return JSONObject(str.substring(firstBrace, lastBrace + 1))
                    }
                }
            } catch (_: Exception) { }
        }

        throw IllegalArgumentException("Не удалось подобрать смещение AES-CBC (размер payload: ${payload.size} байт)")
    }

    // Определяет, какой заголовок использовался в оригинальном payload (20 байт SHA-1 или 24 байта Length+SHA-1)
    private fun encryptBinaryPayloadMatchingOriginal(originalPayload: ByteArray?, json: JSONObject): ByteArray {
        val plainBytes = json.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(CLASSIC_AES_KEY, "AES"), IvParameterSpec(ZERO_IV))
        val encryptedBytes = cipher.doFinal(plainBytes)

        // Внутри {"version":3,"data":"..."} игра использует 20-байтовый SHA-1 от ОТКРЫТЫХ или ЗАШИФРОВАННЫХ данных,
        // но при этом размер 376 байт = 20 байт SHA-1 + 356 (либо 8 байт + 368).
        // Проверим, какое смещение было у оригинала:
        val origOffset = if (originalPayload != null) {
            listOf(20, 24, 8, 0, 4, 16).firstOrNull { off ->
                originalPayload.size > off && (originalPayload.size - off) % 16 == 0 && runCatching {
                    val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    c.init(Cipher.DECRYPT_MODE, SecretKeySpec(CLASSIC_AES_KEY, "AES"), IvParameterSpec(ZERO_IV))
                    val s = String(c.doFinal(originalPayload.copyOfRange(off, originalPayload.size)), Charsets.UTF_8).trim()
                    s.startsWith("{")
                }.getOrDefault(false)
            } ?: 20
        } else 20

        val sha1 = MessageDigest.getInstance("SHA-1").digest(encryptedBytes)

        return when (origOffset) {
            20 -> {
                // 20 байт SHA-1 + зашифрованные данные (стандарт для поля "data" в wallet.json!)
                ByteBuffer.allocate(20 + encryptedBytes.size)
                    .put(sha1)
                    .put(encryptedBytes)
                    .array()
            }
            24 -> {
                // 4 байта длина (20) + 20 байт SHA-1 + зашифрованные данные
                val headerLen = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(20).array()
                ByteBuffer.allocate(24 + encryptedBytes.size)
                    .put(headerLen)
                    .put(sha1)
                    .put(encryptedBytes)
                    .array()
            }
            8 -> {
                // Копируем первые 8 байт заголовка из оригинала (если они были) + зашифрованные данные
                val prefix = originalPayload?.copyOfRange(0, 8) ?: ByteArray(8)
                ByteBuffer.allocate(8 + encryptedBytes.size)
                    .put(prefix)
                    .put(encryptedBytes)
                    .array()
            }
            0 -> encryptedBytes
            else -> {
                ByteBuffer.allocate(20 + encryptedBytes.size)
                    .put(sha1)
                    .put(encryptedBytes)
                    .array()
            }
        }
    }

    // Универсальная расшифровка: читает {"version":3,"data":"Base64..."} из wallet.json[span_1](start_span)[span_1](end_span)
    fun decryptClassicFile(rawBytes: ByteArray): JSONObject {
        val textAttempt = runCatching { String(rawBytes, Charsets.UTF_8).trim() }.getOrNull()
        if (textAttempt != null && textAttempt.startsWith("{")) {
            val outer = JSONObject(textAttempt)
            if (outer.has("data")) {
                val dataObj = outer.opt("data")
                if (dataObj is JSONObject) return dataObj
                if (dataObj is String) {
                    val dataStr = dataObj.trim()
                    if (dataStr.startsWith("{")) {
                        return JSONObject(dataStr)
                    }
                    val decodedPayload = Base64.decode(dataStr, Base64.DEFAULT)
                    return decryptBinaryPayload(decodedPayload)
                }
            }
            return outer
        }
        return decryptBinaryPayload(rawBytes)
    }

    // Универсальное шифрование: упаковывает обратно в {"version":3,"data":"Base64..."}[span_2](start_span)[span_2](end_span)
    fun encryptClassicFile(originalBytes: ByteArray?, json: JSONObject): ByteArray {
        json.put("lastSaved", Instant.now().toString())

        val textAttempt = originalBytes?.let { runCatching { String(it, Charsets.UTF_8).trim() }.getOrNull() }
        if (textAttempt != null && textAttempt.startsWith("{")) {
            val outer = JSONObject(textAttempt)
            if (outer.has("data")) {
                val oldDataStr = outer.optString("data", "").trim()
                val origPayload = if (oldDataStr.isNotEmpty() && !oldDataStr.startsWith("{")) {
                    runCatching { Base64.decode(oldDataStr, Base64.DEFAULT) }.getOrNull()
                } else null

                val encryptedPayload = encryptBinaryPayloadMatchingOriginal(origPayload, json)
                val b64 = Base64.encodeToString(encryptedPayload, Base64.NO_WRAP)
                outer.put("data", b64)
                return outer.toString().toByteArray(Charsets.UTF_8)
            }
        }

        val encryptedPayload = encryptBinaryPayloadMatchingOriginal(originalBytes, json)
        val b64 = Base64.encodeToString(encryptedPayload, Base64.NO_WRAP)
        val wrapper = JSONObject().put("version", 3).put("data", b64)
        return wrapper.toString().toByteArray(Charsets.UTF_8)
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
            if (!cObj.has("expirationType")) cObj.put("expirationType", 0)
            if (!cObj.has("contextType")) cObj.put("contextType", 0)
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
        putScoreMod("permanent_score_multiplier_collection_revamp_1a", 1)
        putScoreMod("permanent_score_multiplier_collection_revamp_1b", 1)
        putScoreMod("permanent_score_multiplier_collection_revamp_1c", 1)
        putScoreMod("permanent_score_multiplier_collection_revamp_1d", 1)
        upgrades.put("scoreModifiers", scoreMods)

        val pickupMods = upgrades.optJSONObject("currencyPickupModifiers") ?: JSONObject()
        pickupMods.put("doubleCoins", JSONObject().apply {
            put("value", JSONObject().put("id", "doubleCoins").put("type", 0).put("subType", 0).put("value", 2))
            put("expirationType", 0); put("contextType", 0); put("contextData", JSONObject())
        })
        pickupMods.put("token_multiplier_high", JSONObject().apply {
            put("value", JSONObject().put("id", "token_multiplier_high").put("type", 0).put("subType", 2).put("value", 4))
            put("expirationValue", 999999999999999L); put("expirationType", 1); put("contextType", 0); put("contextData", JSONObject())
        })
        upgrades.put("currencyPickupModifiers", pickupMods)

        upgrades.put("powerupLevels", JSONObject().apply {
            put("jetpack", 6); put("superSneakers", 6); put("magnet", 6); put("doubleScore", 6)
        })

        writeClassicJson("upgrades.json", raw, upgrades, log)
        log("🪙 Двойные монеты, Умножитель жетонов x4 и все бустеры 6 ур. активированы!")
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

        log("🏆 Сезонная охота (40 тиров), 30 сетов миссий, все трофеи и ачивки разблокированы!")
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

        log("🖼️ Все 114 портретов и 56 рамок профиля успешно открыты!")
    }

    fun setClassicEventCityAndUnlockStages(
        targetCityId: String,
        completeAllStages: Boolean,
        clearCooldowns: Boolean,
        log: (String) -> Unit
    ) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)

        val (rawGc, gc) = readClassicJson("generic_challenges.json", log)
        val states = gc.optJSONObject("challengeStates") ?: JSONObject()
        val keys = states.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val ch = states.getJSONObject(key)
            if (targetCityId.isNotBlank() && ch.has("targetCity")) {
                ch.put("targetCity", targetCityId)
                val reqs = ch.optJSONObject("requirements")
                val progData = reqs?.optJSONObject("progress")?.optJSONArray("data")
                if (progData != null) {
                    for (i in 0 until progData.length()) {
                        val d = progData.getJSONObject(i)
                        if (d.optString("type") == "City") d.put("value", targetCityId)
                    }
                }
            }
            if (clearCooldowns) {
                ch.optJSONObject("requirements")?.put("cooldownItems", JSONObject())
            }
            if (completeAllStages) {
                ch.put("currentScore", 5000000)
                ch.put("highScore", 5000000)
                ch.put("lastSeenScore", 5000000)
                val rStates = ch.optJSONArray("rewardStates") ?: JSONArray()
                for (i in 0 until rStates.length()) {
                    val rs = rStates.getJSONObject(i)
                    if (rs.optInt("State", 0) == 0) rs.put("State", 1)
                    rs.put("AdRequired", false)
                }
            }
        }
        gc.put("challengeStates", states)
        writeClassicJson("generic_challenges.json", rawGc, gc, log)

        if (targetCityId.isNotBlank()) {
            val (rawSt, st) = readClassicJson("user_stats.json", log)
            val visited = st.optJSONObject("worldDestinationVisited") ?: JSONObject()
            visited.put(targetCityId, maxOf(visited.optInt(targetCityId, 0), 10))
            st.put("worldDestinationVisited", visited)
            writeClassicJson("user_stats.json", rawSt, st, log)
        }

        log("🌍 Трасса '$targetCityId' установлена во все режимы забегов Classic!")
    }
}
