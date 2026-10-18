package com.layxtar.subwaysaver

import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoAndSyncManager {

    const val PKG_CITY = "com.sybogames.subway.surfers.game"
    const val PKG_CLASSIC = "com.kiloo.subwaysurf"

    private val CITY_PATHS = listOf(
        "/storage/emulated/0/Android/data/$PKG_CITY/files/enc/profile",
        "/storage/emulated/0/Android/data/$PKG_CITY/files/enc",
        "/storage/emulated/0/Android/data/$PKG_CITY/files/profile"
    )

    const val CLASSIC_WALLET_PATH = "/storage/emulated/0/Android/data/$PKG_CLASSIC/files/profile/wallet.json"
    const val CLASSIC_UPGRADES_PATH = "/storage/emulated/0/Android/data/$PKG_CLASSIC/files/profile/upgrades.json"

    fun checkShizukuPermission(): Boolean {
        if (!Shizuku.pingBinder()) return false
        return if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            Shizuku.requestPermission(1001)
            false
        }
    }

    // Безопасный запуск через рефлексию Shizuku с исправленными типами классов
    fun execSh(command: String): Pair<Int, String> {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText()
            val code = process.waitFor()
            code to output
        } catch (e: Exception) {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val out = p.inputStream.bufferedReader().readText()
                p.waitFor() to out
            } catch (ex: Exception) {
                -1 to (ex.message ?: "Unknown error")
            }
        }
    }

    fun findCitySavePath(): String? {
        for (path in CITY_PATHS) {
            val (code, out) = execSh("if [ -f '$path' ]; then echo 'FILE'; elif [ -d '$path' ]; then ls -1 '$path' | head -n 1; fi")
            if (code == 0 && out.trim() == "FILE") return path
            if (code == 0 && out.trim().isNotEmpty()) return "$path/${out.trim()}"
        }
        return null
    }

    fun pullSaveFile(remotePath: String, localTempFile: File, packageName: String): Boolean {
        execSh("am force-stop $packageName")
        val (code, _) = execSh("cat '$remotePath' > '${localTempFile.absolutePath}'")
        return code == 0 && localTempFile.exists() && localTempFile.length() > 32
    }

    fun pushAndSyncSaveFile(localTempFile: File, remotePath: String, packageName: String): Boolean {
        execSh("am force-stop $packageName")
        execSh("cp '$remotePath' '$remotePath.bak_editor'")
        val (code, _) = execSh("cat '${localTempFile.absolutePath}' > '$remotePath'")
        val parentDir = File(remotePath).parent
        execSh("rm -f '$parentDir'/*.tmp '$parentDir'/*.temp")
        return code == 0
    }

    fun restoreBackup(remotePath: String, packageName: String): Boolean {
        execSh("am force-stop $packageName")
        val (code, _) = execSh("if [ -f '$remotePath.bak_editor' ]; then cat '$remotePath.bak_editor' > '$remotePath'; else exit 1; fi")
        return code == 0
    }

    fun decryptCityProfile(rawBytes: ByteArray): Pair<JSONObject, JSONObject> {
        require(rawBytes.size >= 32) { "Файл слишком мал (< 32 байт)" }
        val iv = rawBytes.copyOfRange(0, 16)
        val key = rawBytes.copyOfRange(16, 32)
        val encrypted = rawBytes.copyOfRange(32, rawBytes.size)

        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))

        val counter = iv.clone()
        val decrypted = ByteArray(encrypted.size)

        for (offset in encrypted.indices step 16) {
            val blockSize = minOf(16, encrypted.size - offset)
            val encryptedCounter = cipher.doFinal(counter)
            for (i in 0 until blockSize) {
                decrypted[offset + i] = (encrypted[offset + i].toInt() xor encryptedCounter[i].toInt()).toByte()
            }
            incrementCounter(counter)
        }

        val containerJson = JSONObject(String(decrypted, Charsets.UTF_8))
        val innerProfileJson = JSONObject(containerJson.getString("profile"))
        return containerJson to innerProfileJson
    }

    fun encryptCityProfile(originalRawBytes: ByteArray, containerJson: JSONObject, innerProfileJson: JSONObject): ByteArray {
        val iv = originalRawBytes.copyOfRange(0, 16)
        val key = originalRawBytes.copyOfRange(16, 32)

        val futureTime = Instant.now().plusSeconds(300)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'").withZone(ZoneOffset.UTC)
        containerJson.put("lastUpdated", formatter.format(futureTime))
        containerJson.put("version", 8)
        containerJson.put("profile", innerProfileJson.toString())
        containerJson.put("hash", JSONObject.NULL)

        innerProfileJson.put("cloudsave", JSONObject().put("askedToLinkAccount", false))
        val flags = innerProfileJson.optJSONObject("flags") ?: JSONObject()
        flags.put("CloudSave_AskedToLinkAccount", true)
        flags.put("noadsbutton", true)
        innerProfileJson.put("flags", flags)

        val plainBytes = containerJson.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))

        val counter = iv.clone()
        val encrypted = ByteArray(plainBytes.size)

        for (offset in plainBytes.indices step 16) {
            val blockSize = minOf(16, plainBytes.size - offset)
            val encryptedCounter = cipher.doFinal(counter)
            for (i in 0 until blockSize) {
                encrypted[offset + i] = (plainBytes[offset + i].toInt() xor encryptedCounter[i].toInt()).toByte()
            }
            incrementCounter(counter)
        }

        return ByteBuffer.allocate(32 + encrypted.size)
            .put(iv)
            .put(key)
            .put(encrypted)
            .array()
    }

    private fun incrementCounter(counter: ByteArray) {
        var carry = 1
        for (i in 15 downTo 8) {
            val s = (counter[i].toInt() and 0xFF) + carry
            counter[i] = (s and 0xFF).toByte()
            carry = s ushr 8
            if (carry == 0) break
        }
    }

    fun unlockAndMaxAllSurfers(profile: JSONObject, customHighScore: Long = 12500000L) {
        val currentArray = profile.optJSONArray("surferProfiles") ?: JSONArray()
        val existingSkins = mutableMapOf<Long, Int>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            existingSkins[obj.getLong("id")] = obj.optInt("selectedSkin", 0)
        }

        val newArray = JSONArray()
        for ((id, _) in GameDatabase.SURFERS) {
            newArray.put(JSONObject().apply {
                put("id", id)
                put("level", 20)
                put("isUnlocked", true)
                put("wasSeen", true)
                put("highScore", customHighScore)
                put("selectedSkin", existingSkins[id] ?: 0)
            })
        }
        profile.put("surferProfiles", newArray)

        val counters = profile.optJSONArray("counters") ?: JSONArray()
        for ((id, _) in GameDatabase.SURFERS) {
            upsertCounter(counters, "LastUnlockedTierCounter", "DataTag($id)", 40)
            upsertCounter(counters, "ScoreLadderUnlockTierCounter", "DataTag($id)", 40)
        }
        profile.put("counters", counters)
    }

    fun unlockAndMaxAllBoards(profile: JSONObject) {
        val newArray = JSONArray()
        for ((id, _) in GameDatabase.BOARDS) {
            newArray.put(JSONObject().apply {
                put("id", id)
                put("level", 20)
                put("isUnlocked", true)
                put("wasSeen", true)
            })
        }
        profile.put("boardProfiles", newArray)
    }

    fun unlockAllSkinsAndAvatars(profile: JSONObject) {
        val skinsArray = JSONArray()
        for (skinId in GameDatabase.ALL_SKINS) {
            skinsArray.put(JSONObject().apply {
                put("id", skinId)
                put("isUnlocked", true)
                put("wasSeen", true)
            })
        }
        profile.put("surferSkinProfiles", skinsArray)

        val avatarsArray = JSONArray()
        for (avatarId in GameDatabase.ALL_AVATARS) {
            avatarsArray.put(JSONObject().put("id", avatarId))
        }
        profile.put("avatarProfiles", avatarsArray)
    }

    fun completeAllDistrictsAndStages(profile: JSONObject) {
        val distProf = JSONArray()
        val campDistProf = JSONArray()
        for (dId in GameDatabase.DISTRICTS) {
            distProf.put(JSONObject().put("id", dId).put("isUnlocked", true).put("wasSeen", true))
            campDistProf.put(JSONObject().put("id", dId).put("isUnlocked", true))
        }
        for (num in 1..6) {
            distProf.put(JSONObject().put("id", num).put("isUnlocked", true).put("wasSeen", true))
        }
        profile.put("districtProfiles", distProf)
        profile.put("campaignDistrictProfiles", campDistProf)

        val goals = profile.optJSONArray("goals") ?: JSONArray()
        val existingGoalIds = mutableSetOf<String>()
        for (i in 0 until goals.length()) {
            val g = goals.getJSONObject(i)
            g.put("completed", true)
            existingGoalIds.add(g.getString("id"))
        }

        val stageCounts = mapOf(1 to 32, 2 to 36, 3 to 36, 4 to 36, 5 to 36, 6 to 20)
        for ((districtNum, maxStage) in stageCounts) {
            for (stage in 1..maxStage) {
                for (goal in 1..3) {
                    val gId = "district${districtNum}_stage${stage}_goal$goal"
                    if (existingGoalIds.add(gId)) {
                        goals.put(JSONObject().put("id", gId).put("completed", true))
                    }
                }
            }
        }
        profile.put("goals", goals)
    }

    fun injectAllPaidPassesAndDonations(profile: JSONObject) {
        val purchaseHistory = profile.optJSONObject("purchaseHistory") ?: JSONObject()
        val nowIso = Instant.now().toString()

        for (productId in GameDatabase.ALL_IAP_PURCHASES) {
            purchaseHistory.put(productId, JSONObject().apply {
                put("PurchaseCount", 1)
                put("PurchaseDates", JSONArray().put(nowIso))
            })
        }
        profile.put("purchaseHistory", purchaseHistory)
        profile.put("seasonPassPurchased", true)
        profile.put("hasSeenPremiumPassActivation", true)
        profile.put("lifetimeMaxPurchasePrice", 99.99)

        val counters = profile.optJSONArray("counters") ?: JSONArray()
        for (notifId in GameDatabase.HOME_OFFER_NOTIFICATION_IDS) {
            upsertCounter(counters, "HomeOfferNotificationViewCounter", notifId, 999)
        }
        profile.put("counters", counters)
    }

    fun configurePiggyBank(profile: JSONObject, piggyId: String, missionId: Long, progressAmount: Int, isFilled: Boolean) {
        profile.put("piggyBank", JSONObject().apply {
            put("currentId", piggyId)
            put("lastSeenProgress", progressAmount)
            put("skippedCount", 0)
            put("lastSentAnalyticsProgress", 10)
            put("isFilled", isFilled)
        })

        val missions = profile.optJSONArray("missionProgresses") ?: JSONArray()
        var found = false
        for (i in 0 until missions.length()) {
            val m = missions.getJSONObject(i)
            if (m.optString("featureType") == "PiggyBank" || m.optLong("id") == missionId) {
                m.put("id", missionId)
                m.put("progress", progressAmount)
                m.put("isComplete", isFilled)
                m.put("isClaimed", false)
                m.put("featureType", "PiggyBank")
                found = true
            }
        }
        if (!found) {
            missions.put(JSONObject().apply {
                put("id", missionId)
                put("progress", progressAmount)
                put("isComplete", isFilled)
                put("isClaimed", false)
                put("creationUnixTimestamp", 0)
                put("creationUnixTimestampSeconds", Instant.now().epochSecond)
                put("featureType", "PiggyBank")
            })
        }
        profile.put("missionProgresses", missions)
    }

    fun activateScheduledCampaign(profile: JSONObject, campaignId: String) {
        profile.put("activeScheduledCampaignProfile", JSONObject().apply {
            put("activeScheduledCampaignId", campaignId)
            put("playableSurfers", JSONArray().apply {
                put(-1836944478L)
                put(1900660162L)
                put(2129411796L)
                put(-496667767L)
            })
        })

        val goals = profile.optJSONArray("goals") ?: JSONArray()
        for (stage in 1..7) {
            for (goal in 1..3) {
                goals.put(JSONObject().put("id", "${campaignId}_stage${stage}_goal$goal").put("completed", true))
            }
        }
        profile.put("goals", goals)
    }

    fun activateAndCompleteQuest(profile: JSONObject, questId: String, missionId: Long) {
        val quests = profile.optJSONArray("quest") ?: JSONArray()
        quests.put(JSONObject().apply {
            put("QuestID", questId)
            put("AnalyticID", "${questId}_editor_unlock")
            put("Amount", 50000)
            put("TimerID", -2010162359)
            put("SavableItems", JSONArray())
            put("PendingProgress", 50000)
        })
        profile.put("quest", quests)

        val missions = profile.optJSONArray("missionProgresses") ?: JSONArray()
        missions.put(JSONObject().apply {
            put("id", missionId)
            put("progress", 50000)
            put("isComplete", true)
            put("isClaimed", false)
            put("creationUnixTimestamp", 0)
            put("creationUnixTimestampSeconds", Instant.now().epochSecond)
            put("featureType", "Quest")
        })
        profile.put("missionProgresses", missions)
    }

    fun hackDailyDealsAndAdRewards(profile: JSONObject, dealRewardAmount: Int = 9999) {
        val customTags = longArrayOf(-1878560402L, -1509662453L, -1085419483L, -635142441L, -2008679869L, 326719861L)
        val dealsArray = JSONArray()
        for (i in 0 until 6) {
            dealsArray.put(JSONObject().apply {
                put("slotId", "daily_deal_${i + 1}")
                put("slotSeed", 10 + i)
                put("itemDatatag", customTags[i])
                put("amount", dealRewardAmount)
            })
        }
        profile.put("dailyDeal", JSONObject().put("dailyDeals", dealsArray))

        val adRewards = JSONArray()
        val adTags = longArrayOf(-635142441L, -2008679869L, 326719861L, 1669706535L, -633437229L, -1085419483L, -1878560402L, 722065917L, -1509662453L)
        for (tag in adTags) {
            adRewards.put(JSONObject().apply {
                put("RewardType", tag)
                put("RewardValue", 99999)
                put("Claimed", false)
            })
        }
        profile.put("dailyAdRewards", adRewards)
    }

    fun unlockSecretStarsAndAchievements(profile: JSONObject) {
        val counters = profile.optJSONArray("counters") ?: JSONArray()
        for (starTag in GameDatabase.SECRET_STAR_TAGS) {
            upsertCounter(counters, "SecretStarCounter", "DataTag($starTag)", 10)
        }
        upsertCounter(counters, "PityCounter", "mysterybox_regular", 9)
        upsertCounter(counters, "PityCounter", "shop.box.ad", 9)
        upsertCounter(counters, "PityCounter", "shop.box.ad.board", 9)
        upsertCounter(counters, "PityCounter", "shop.box.ad.repeatable", 9)
        profile.put("counters", counters)

        val missions = profile.optJSONArray("missionProgresses") ?: JSONArray()
        for ((achId, target) in GameDatabase.ACHIEVEMENT_MISSIONS) {
            var exists = false
            for (i in 0 until missions.length()) {
                val m = missions.getJSONObject(i)
                if (m.optLong("id") == achId) {
                    m.put("progress", target)
                    m.put("isComplete", true)
                    m.put("isClaimed", false)
                    exists = true
                }
            }
            if (!exists) {
                missions.put(JSONObject().apply {
                    put("id", achId)
                    put("progress", target)
                    put("isComplete", true)
                    put("isClaimed", false)
                    put("creationUnixTimestamp", 0)
                    put("creationUnixTimestampSeconds", Instant.now().epochSecond)
                    put("featureType", "Achievement")
                })
            }
        }
        profile.put("missionProgresses", missions)
    }

    fun patchGameConfigsViaShizuku(cacheDir: File): String {
        val (_, findOut) = execSh("find /storage/emulated/0/Android/data/$PKG_CITY -name 'manifest.json' 2>/dev/null | head -n 1")
        val manifestPath = findOut.trim()
        if (manifestPath.isEmpty()) {
            return "Локальный кэш manifest.json не найден. Все фичи применены в профиль!"
        }
        val configDir = File(manifestPath).parent
        val tempManifest = File(cacheDir, "manifest_tmp.json")
        execSh("cat '$manifestPath' > '${tempManifest.absolutePath}'")
        if (!tempManifest.exists()) return "Не удалось прочитать manifest.json"

        val manifestJson = JSONObject(tempManifest.readText())
        val objects = manifestJson.optJSONObject("objects") ?: return "Неверный формат manifest.json"

        val ticketJson = """{"maxTicket":999,"regenerationCooldownInSeconds":1,"ticketCostPerPlay":0}"""
        patchSingleConfig(cacheDir, configDir, objects, "campaignTicketingConfig", "campaignTicketingConfig.json", ticketJson)

        val noAdsJson = """{"configurations":[{"countryTier":3,"minimumPlayerLevelToDisplayInterstitial":999,"rewardedVideoAdCooldown":0,"maximumCapacity":0,"coolDownBetweenInterstitials":999999,"minimumRunNumber":99999,"skipInterstitialIfRVInLastRun":true,"percentageFirstInterstitial":0,"percentageAfterFirstInterstitial":0}]}"""
        patchSingleConfig(cacheDir, configDir, objects, "interstitials", "interstitials.json", noAdsJson)

        tempManifest.writeText(manifestJson.toString())
        execSh("cat '${tempManifest.absolutePath}' > '$manifestPath'")
        return "✅ Конфиги пропатчены, SHA-1 ключи в manifest.json обновлены!"
    }

    private fun patchSingleConfig(cacheDir: File, configDir: String, objects: JSONObject, keyName: String, fileName: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val sha1 = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
        val tmpFile = File(cacheDir, fileName)
        tmpFile.writeBytes(bytes)
        execSh("cat '${tmpFile.absolutePath}' > '$configDir/$fileName'")
        objects.optJSONObject(keyName)?.put("key", sha1)
    }

    private fun upsertCounter(counters: JSONArray, type: String, id: String, count: Int) {
        for (i in 0 until counters.length()) {
            val c = counters.getJSONObject(i)
            if (c.optString("CounterType") == type && c.optString("Id") == id) {
                c.put("Count", count)
                return
            }
        }
        counters.put(JSONObject().apply {
            put("CounterType", type)
            put("Id", id)
            put("Count", count)
        })
    }
}
