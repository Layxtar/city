package com.layxtar.subwaysaver

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ClassicCryptoAndSyncManager {

    const val PKG_CLASSIC = "com.kiloo.subwaysurf"
    const val CLASSIC_PROFILE_DIR = "/storage/emulated/0/Android/data/$PKG_CLASSIC/files/profile"

    // Точный 32-байтовый ключ для СЕЙВОВ из твоего HTML-дешифратора!
    private val SAVES_KEY_BYTES = byteArrayOf(
        57, 114, 107, 120, 67, 80, 108, 106,
        83, 77, 49, 71, 86, 81, 104, 87,
        119, 49, 114, 49, 114, 75, 79, 72,
        71, 99, 99, 98, 50, 105, 74, 53
    )

    private val ZERO_IV = ByteArray(16)

    // Стандартные версии обёртки {"version": X, "data": "..."} для файлов из папки profile
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
        "missions.json" to 1,
        "trophies.json" to 1,
        "profile_portrait.json" to 1,
        "profile_frame.json" to 1
    )

    // Расшифровка строки Base64 точь-в-точь как в функции decryptData(base64Data) из HTML
    fun decryptBase64Data(base64Data: String): String {
        val cleanB64 = base64Data.trim()
        val encryptedBytes = Base64.decode(cleanB64, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(SAVES_KEY_BYTES, "AES"), IvParameterSpec(ZERO_IV))
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    // Шифрование JSON-строки обратно в чистый Base64 (AES-CBC с нулевым IV)
    fun encryptToBase64Data(plainJsonString: String): String {
        val plainBytes = plainJsonString.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(SAVES_KEY_BYTES, "AES"), IvParameterSpec(ZERO_IV))
        val encryptedBytes = cipher.doFinal(plainBytes)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    // Чтение файла: возвращает (версия_обёртки, расшифрованный_JSONObject)
    fun readClassicJson(fileName: String, log: (String) -> Unit): Pair<Int, JSONObject> {
        val defaultVer = DEFAULT_FILE_VERSIONS[fileName] ?: 1
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        val rawBytes = CryptoAndSyncManager.readFileViaShizuku(path, log)

        if (rawBytes == null || rawBytes.isEmpty()) {
            log("ℹ️ Файл $fileName не найден, будет создан с нуля (v$defaultVer)")
            return defaultVer to createDefaultStructureForFile(fileName)
        }

        return try {
            val fileText = String(rawBytes, Charsets.UTF_8).trim()
            val outerJson = JSONObject(fileText)
            val version = outerJson.optInt("version", defaultVer)

            if (outerJson.has("data") && outerJson.opt("data") is String) {
                val dataStr = outerJson.getString("data").trim()
                val decryptedStr = if (dataStr.startsWith("{")) {
                    dataStr
                } else {
                    decryptBase64Data(dataStr)
                }
                version to JSONObject(decryptedStr)
            } else {
                version to outerJson
            }
        } catch (e: Exception) {
            log("❌ Ошибка чтения $fileName: ${e.message}")
            defaultVer to createDefaultStructureForFile(fileName)
        }
    }

    // Запись файла: упаковывает в {"version":X,"data":"<Base64>"}
    fun writeClassicJson(fileName: String, version: Int, innerJson: JSONObject, log: (String) -> Unit): Boolean {
        val path = "$CLASSIC_PROFILE_DIR/$fileName"
        return try {
            innerJson.put("lastSaved", Instant.now().toString())
            val encryptedB64 = encryptToBase64Data(innerJson.toString())
            val wrapper = JSONObject()
                .put("version", version)
                .put("data", encryptedB64)

            val outBytes = wrapper.toString().toByteArray(Charsets.UTF_8)
            val ok = CryptoAndSyncManager.writeFileViaShizuku(path, outBytes, log)
            if (ok) log("✅ Успешно записан $fileName (v$version)")
            ok
        } catch (e: Exception) {
            log("❌ Ошибка шифрования $fileName: ${e.message}")
            false
        }
    }

    // Базовый шаблон на случай, если какого-то файла ещё нет в папке profile
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
            }
        }
    }

    // 1. Обновление wallet.json (создаёт любую отсутствующую валюту с нуля!)
    fun updateWalletAndInjectDonations(
        currencyMap: Map<String, Long>,
        unlockAllBuyOnce: Boolean,
        lootboxToQueue: String?,
        lootboxCount: Int,
        log: (String) -> Unit
    ) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (ver, wallet) = readClassicJson("wallet.json", log)
        val currencies = wallet.optJSONObject("currencies") ?: JSONObject()

        for ((id, amount) in currencyMap) {
            // Если строчки с этой валютой нет в wallet.json — создаём её с нуля!
            val existingObj = currencies.optJSONObject(id)
            if (existingObj != null) {
                existingObj.put("value", amount)
                if (!existingObj.has("expirationType")) existingObj.put("expirationType", 0)
                if (!existingObj.has("contextType")) existingObj.put("contextType", 0)
                if (!existingObj.has("contextData")) existingObj.put("contextData", JSONObject())
            } else {
                val newCurrencyEntry = JSONObject().apply {
                    put("value", amount)
                    if (id == "6") {
                        // Для сезонных монет (EventCoins, id = 6) ставим срок действия до конца сезона
                        put("expirationValue", 1793577599999L)
                        put("expirationType", 1)
                    } else {
                        put("expirationType", 0)
                    }
                    put("contextType", 0)
                    put("contextData", JSONObject())
                }
                currencies.put(id, newCurrencyEntry)
            }
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

        writeClassicJson("wallet.json", ver, wallet, log)
    }

    // 2. Двойные монеты, множители и бустеры 6 ур. -> upgrades.json
    fun unlockDoubleCoinsAndMaxUpgrades(log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (ver, upgrades) = readClassicJson("upgrades.json", log)
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
        pickupMods.put("permanent_score_multiplier", JSONObject().apply {
            put("value", JSONObject().put("id", "permanent_score_multiplier").put("type", 2).put("subType", 3).put("value", 5))
            put("expirationValue", 999999999999999L); put("expirationType", 1); put("contextType", 0); put("contextData", JSONObject())
        })
        pickupMods.put("token_multiplier_low", JSONObject().apply {
            put("value", JSONObject().put("id", "token_multiplier_low").put("type", 0).put("subType", 2).put("value", 2))
            put("expirationValue", 999999999999999L); put("expirationType", 1); put("contextType", 0); put("contextData", JSONObject())
        })
        upgrades.put("currencyPickupModifiers", pickupMods)

        upgrades.put("powerupLevels", JSONObject().apply {
            put("jetpack", 6); put("superSneakers", 6); put("magnet", 6); put("doubleScore", 6)
        })

        writeClassicJson("upgrades.json", ver, upgrades, log)
        log("🪙 upgrades.json: Двойные монеты, множитель и бустеры 6 ур. включены!")
    }

    // 3. Разблокировка сёрферов -> characters_inventory.json
    fun unlockClassicSurfers(specificSurferId: String? = null, selectSurferId: String? = null, log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (ver, inv) = readClassicJson("characters_inventory.json", log)
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

        writeClassicJson("characters_inventory.json", ver, inv, log)
    }

    // 4. Разблокировка досок -> boards_inventory.json
    fun unlockClassicBoards(specificBoardId: String? = null, selectBoardId: String? = null, log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)
        val (ver, inv) = readClassicJson("boards_inventory.json", log)
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

        writeClassicJson("boards_inventory.json", ver, inv, log)
    }

    // 5. Сезонная охота, Миссии, Трофеи и Ачивки -> season_hunt.json, missions.json, trophies.json, achievements.json
    fun maxSeasonHuntMissionsAndAchievements(log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)

        val (verSh, sh) = readClassicJson("season_hunt.json", log)
        sh.put("collectedTokens", 99999)
        val tiersArr = JSONArray()
        for (idx in 0..40) {
            val obj = JSONObject().put("mainRewardStatus", JSONArray().put(1))
            if (idx > 0) obj.put("tierIndex", idx)
            tiersArr.put(obj)
        }
        sh.put("rewardsThisSeason", tiersArr)
        sh.put("lastUnlockedTierSeen", 40)
        writeClassicJson("season_hunt.json", verSh, sh, log)

        val (verMis, mis) = readClassicJson("missions.json", log)
        mis.put("patchVersion", 1)
        mis.put("currentMissionSet", 31)
        mis.put("claimedMissionSet", 30)
        writeClassicJson("missions.json", verMis, mis, log)

        val (verTr, tr) = readClassicJson("trophies.json", log)
        val trArr = JSONArray()
        ClassicDatabase.ALL_TROPHIES.forEach { trArr.put(it) }
        tr.put("trophiesEarned", trArr)
        tr.put("trophyCompletions", JSONObject().put("TrophySet1", 4).put("TrophySet2", 4))
        writeClassicJson("trophies.json", verTr, tr, log)

        val (verAch, ach) = readClassicJson("achievements.json", log)
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
        writeClassicJson("achievements.json", verAch, ach, log)
    }

    // 6. Портреты и Рамки -> profile_portrait.json, profile_frame.json
    fun unlockAllPortraitsAndFrames(log: (String) -> Unit) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)

        val (verPort, portJson) = readClassicJson("profile_portrait.json", log)
        portJson.put("patchVersion", 1)
        if (!portJson.has("selected")) portJson.put("selected", "countcorvin_illustration_portrait")
        val ownedPortraits = portJson.optJSONObject("owned") ?: JSONObject()
        for (pId in ClassicDatabase.ALL_PORTRAITS) {
            ownedPortraits.put(pId, JSONObject().put("id", pId).put("isSeen", true))
        }
        portJson.put("owned", ownedPortraits)
        writeClassicJson("profile_portrait.json", verPort, portJson, log)

        val (verFrame, frameJson) = readClassicJson("profile_frame.json", log)
        frameJson.put("patchVersion", 1)
        if (!frameJson.has("selected")) frameJson.put("selected", "collection_revamp_15_frame")
        val ownedFrames = frameJson.optJSONObject("owned") ?: JSONObject()
        for (fId in ClassicDatabase.ALL_FRAMES) {
            ownedFrames.put(fId, JSONObject().put("id", fId).put("isSeen", true))
        }
        frameJson.put("owned", ownedFrames)
        writeClassicJson("profile_frame.json", verFrame, frameJson, log)
    }

    // 7. Выбор Трассы (Города) для забегов -> generic_challenges.json и user_stats.json
    fun setClassicEventCityAndUnlockStages(
        targetCityId: String,
        completeAllStages: Boolean,
        clearCooldowns: Boolean,
        log: (String) -> Unit
    ) {
        CryptoAndSyncManager.stopGameForSync(PKG_CLASSIC, true, log)

        val (verGc, gc) = readClassicJson("generic_challenges.json", log)
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
        writeClassicJson("generic_challenges.json", verGc, gc, log)

        if (targetCityId.isNotBlank()) {
            val (verSt, st) = readClassicJson("user_stats.json", log)
            val visited = st.optJSONObject("worldDestinationVisited") ?: JSONObject()
            visited.put(targetCityId, maxOf(visited.optInt(targetCityId, 0), 10))
            st.put("worldDestinationVisited", visited)
            writeClassicJson("user_stats.json", verSt, st, log)
        }

        log("🌍 Трасса '$targetCityId' успешно записана в generic_challenges.json!")
    }
}
