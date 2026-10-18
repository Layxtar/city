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

    // Жестко фиксируем правильный путь, который ты указал
    private val CITY_PATHS = listOf(
        "/storage/emulated/0/Android/data/$PKG_CITY/files/enc",
        "/storage/emulated/0/Android/data/com.sybogames.subway.surfers.game/files/enc/profile",
        "/storage/emulated/0/Android/data/$PKG_CITY/files/profile"
    )

    const val CLASSIC_WALLET_PATH = "/storage/emulated/0/Android/data/$PKG_CLASSIC/files/profile/wallet.json"

    fun checkShizukuPermission(log: (String) -> Unit): Boolean {
        if (!Shizuku.pingBinder()) {
            log("❌ Shizuku не запущен или нет связи с сервером")
            return false
        }
        val hasPerm = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            log("✅ Разрешение Shizuku получено")
            return true
        } else {
            log("⚠️ Запрос разрешения Shizuku...")
            try {
                Shizuku.requestPermission(1001)
            } catch (e: Exception) {
                log("❌ Ошибка запроса разрешения: ${e.message}")
            }
            return false
        }
    }

    fun execSh(command: String, log: (String) -> Unit): Pair<Int, String> {
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
            val errOutput = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            if (errOutput.isNotEmpty()) {
                log("⚙️ [stderr]: $errOutput")
            }
            code to output
        } catch (e: Exception) {
            log("⚠️ Ошибка рефлексии: ${e.message}. Пробуем Runtime...")
            try {
                val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val out = p.inputStream.bufferedReader().readText()
                p.waitFor() to out
            } catch (ex: Exception) {
                log("❌ Ошибка shell: ${ex.message}")
                -1 to (ex.message ?: "Unknown error")
            }
        }
    }

    fun findCitySavePath(log: (String) -> Unit): String? {
        log("🔍 Поиск файла сохранения Subway Surfers City...")
        for (basePath in CITY_PATHS) {
            log("📁 Проверка папки: $basePath")
            val cmd = "if [ -f '$basePath' ]; then echo 'FILE'; elif [ -d '$basePath' ]; then ls -1 '$basePath' 2>/dev/null; fi"
            val (code, out) = execSh(cmd, log)
            log("⚙️ Результат ls (код $code): '${out.trim()}'")

            if (code == 0 && out.trim() == "FILE") {
                log("🎯 Найден файл по пути: $basePath")
                return basePath
            }
            // Если внутри папки есть файлы (например, сам сейв или папка profile)
            val files = out.trim().split("\n")
            for (f in files) {
                if (f.isNotEmpty()) {
                    val fullPath = if (basePath.endsWith("enc") || basePath.endsWith("profile")) "$basePath/$f" else basePath
                    // Проверим, файл ли это
                    val (c2, o2) = execSh("if [ -f '$fullPath' ]; then echo 'YES'; fi", log)
                    if (c2 == 0 && o2.trim() == "YES") {
                        log("🎯 Найден точный файл сохранения: $fullPath")
                        return fullPath
                    }
                }
            }
        }
        // Если автопоиск не сработал, возвращаем прямой проверенный путь
        val fallback = "/storage/emulated/0/Android/data/$PKG_CITY/files/enc"
        log("⚠️ Используем принудительный базовый путь: $fallback")
        return fallback
    }

    fun pullSaveFile(remotePath: String, localTempFile: File, packageName: String, log: (String) -> Unit): Boolean {
        log("🛑 Остановка игры ($packageName)...")
        execSh("am force-stop $packageName", log)

        log("📥 Чтение файла через base64: $remotePath")
        val (code, out) = execSh("base64 < '$remotePath'", log)
        log("📥 Результат base64 read: код $code, длина ответа: ${out.length}")

        if (code == 0 && out.trim().isNotEmpty()) {
            try {
                val cleanBase64 = out.replace(Regex("\\s+"), "")
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                localTempFile.writeBytes(bytes)
                log("📦 Успешно! Записано во временный файл: ${bytes.size} байт")
                return bytes.size > 32
            } catch (e: Exception) {
                log("❌ Ошибка декодирования base64: ${e.message}")
                return false
            }
        } else {
            log("❌ Не удалось прочитать файл. Проверь, запущен ли уровень в игре.")
            return false
        }
    }

    fun pushAndSyncSaveFile(localTempFile: File, remotePath: String, packageName: String, log: (String) -> Unit): Boolean {
        log("🛑 Остановка игры перед записью...")
        execSh("am force-stop $packageName", log)

        log("📑 Создание бэкапа (.bak_editor)...")
        execSh("cp '$remotePath' '$remotePath.bak_editor'", log)

        log("💾 Запись измененного файла через base64...")
        try {
            val bytes = localTempFile.readBytes()
            val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            
            val (code, out) = execSh("echo '$base64Data' | base64 -d > '$remotePath'", log)
            log("💾 Результат записи: код $code, вывод: $out")

            val parentDir = File(remotePath).parent
            execSh("rm -f '$parentDir'/*.tmp '$parentDir'/*.temp", log)

            return code == 0
        } catch (e: Exception) {
            log("❌ Ошибка записи: ${e.message}")
            return false
        }
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
                put(-1836944478L); put(1900660162L); put(2129411796L); put(-496667767L)
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
