package com.layxtar.subwaysaver

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoAndSyncManager {

    const val PKG_CITY = "com.sybogames.subway.surfers.game"
    const val PKG_SHIZUKU = "moe.shizuku.privileged.api"

    const val DEFAULT_GAME_PROFILE_PATH =
        "/storage/emulated/0/Android/data/$PKG_CITY/files/enc/profile"

    private val FALLBACK_CITY_PATHS = listOf(
        "/storage/emulated/0/Android/data/$PKG_CITY/files/enc/profile",
        "/storage/emulated/0/Android/data/$PKG_CITY/files/enc",
        "/storage/emulated/0/Android/data/$PKG_CITY/files/profile"
    )

    const val DEFAULT_SAVE_DIR = "/storage/emulated/0/Download/CityEdit"
    private const val CHUNK_SIZE = 49152

    private var listenerRegistered = false
    var onPermissionGrantedCallback: ((Boolean) -> Unit)? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        onPermissionGrantedCallback?.invoke(granted)
    }

    fun registerPermissionListener() {
        if (listenerRegistered) return
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            listenerRegistered = true
        } catch (_: Exception) { }
    }

    fun unregisterPermissionListener() {
        if (!listenerRegistered) return
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            listenerRegistered = false
        } catch (_: Exception) { }
    }

    fun isServiceRunning(): Boolean = try { Shizuku.pingBinder() } catch (_: Exception) { false }

    fun hasPermission(): Boolean = try {
        isServiceRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun requestPermissionOrOpenShizuku(context: Context, log: (String) -> Unit) {
        try {
            if (isServiceRunning()) {
                Shizuku.requestPermission(42)
            } else {
                val intent = context.packageManager?.getLaunchIntentForPackage(PKG_SHIZUKU)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    log("❌ Приложение Shizuku не найдено!")
                }
            }
        } catch (e: Exception) {
            log("❌ Ошибка запроса Shizuku: ${e.message}")
        }
    }

    fun execSh(command: String, log: ((String) -> Unit)? = null): Pair<Int, String> {
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
            code to (output + errOutput)
        } catch (e: Exception) {
            -1 to (e.message ?: "Error")
        }
    }

    fun fileExists(path: String): Boolean {
        val (_, out) = execSh("test -f '$path' && echo 'EXISTS' || echo 'NOTFOUND'")
        return out.contains("EXISTS")
    }

    fun resolveCityProfilePath(log: (String) -> Unit): String {
        if (fileExists(DEFAULT_GAME_PROFILE_PATH)) return DEFAULT_GAME_PROFILE_PATH
        for (path in FALLBACK_CITY_PATHS) {
            val (code, out) = execSh("if [ -f '$path' ]; then echo 'FILE'; elif [ -d '$path' ]; then ls -1 '$path' 2>/dev/null | head -n 1; fi")
            if (code == 0 && out.trim() == "FILE") return path
            if (code == 0 && out.trim().isNotEmpty()) {
                val candidate = "$path/${out.trim()}"
                if (fileExists(candidate)) return candidate
            }
        }
        return DEFAULT_GAME_PROFILE_PATH
    }

    fun readFileViaShizuku(path: String, log: (String) -> Unit): ByteArray? {
        val (code, result) = execSh("cat '$path' | base64 2>&1", log)
        if (code != 0 || result.isBlank()) return null
        return try {
            val cleanBase64 = result.replace("\n", "").replace("\r", "").replace(Regex("\\s+"), "").trim()
            val data = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (data.isNotEmpty()) data else null
        } catch (_: Exception) {
            null
        }
    }

    fun writeFileViaShizuku(path: String, data: ByteArray, log: (String) -> Unit): Boolean {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        val tempB64 = "/data/local/tmp/ce_b64_$uuid"
        val tempFile = "/data/local/tmp/ce_w_$uuid"

        return try {
            execSh("cp '$path' '$path.bak_editor' 2>/dev/null")
            val base64 = Base64.encodeToString(data, Base64.NO_WRAP)
            var offset = 0
            while (offset < base64.length) {
                val len = minOf(CHUNK_SIZE, base64.length - offset)
                val chunk = base64.substring(offset, offset + len)
                val op = if (offset == 0) ">" else ">>"
                execSh("printf '%s' '$chunk' $op '$tempB64'")
                offset += CHUNK_SIZE
            }
            execSh("base64 -d '$tempB64' > '$tempFile'", log)
            val (_, result) = execSh("cp '$tempFile' '$path' 2>&1 && echo 'DONE'", log)
            if (!result.contains("DONE")) return false

            File(path).parent?.let { parentDir ->
                execSh("rm -f '$parentDir'/*.tmp '$parentDir'/*.temp 2>/dev/null")
            }
            execSh("sync")
            true
        } catch (_: Exception) {
            false
        } finally {
            try { execSh("rm -f '$tempB64' '$tempFile' 2>/dev/null") } catch (_: Exception) { }
        }
    }

    fun stopGameForSync(packageName: String = PKG_CITY, hardStop: Boolean = true, log: (String) -> Unit) {
        if (hardStop) execSh("am force-stop $packageName") else execSh("am kill $packageName")
    }

    fun launchGame(context: Context, packageName: String = PKG_CITY, log: (String) -> Unit) {
        val (code, _) = execSh("monkey -p $packageName -c android.intent.category.LAUNCHER 1", log)
        if (code != 0) {
            val intent = context.packageManager?.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    fun decryptCityProfile(rawBytes: ByteArray): Pair<JSONObject, JSONObject> {
        require(rawBytes.size >= 32) { "Файл слишком мал" }
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

        return ByteBuffer.allocate(32 + encrypted.size).put(iv).put(key).put(encrypted).array()
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

    fun unlockAndMaxAllSurfers(profile: JSONObject, customHighScore: Long = 25000000L) {
        val currentArray = profile.optJSONArray("surferProfiles") ?: JSONArray()
        val existingSkins = mutableMapOf<Long, Int>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            existingSkins[obj.getLong("id")] = obj.optInt("selectedSkin", 0)
        }
        val newArray = JSONArray()
        for ((id, _) in GameDatabase.SURFERS) {
            newArray.put(JSONObject().apply {
                put("id", id); put("level", 20); put("isUnlocked", true); put("wasSeen", true)
                put("highScore", customHighScore); put("selectedSkin", existingSkins[id] ?: 0)
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
            newArray.put(JSONObject().put("id", id).put("level", 20).put("isUnlocked", true).put("wasSeen", true))
        }
        profile.put("boardProfiles", newArray)
    }

    fun unlockAllSkinsAndAvatars(profile: JSONObject) {
        val skinsArray = JSONArray()
        for (skinId in GameDatabase.ALL_SKINS) {
            skinsArray.put(JSONObject().put("id", skinId).put("isUnlocked", true).put("wasSeen", true))
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
            purchaseHistory.put(productId, JSONObject().put("PurchaseCount", 1).put("PurchaseDates", JSONArray().put(nowIso)))
        }
        for ((piggyId, _) in GameDatabase.PIGGY_BANKS) {
            purchaseHistory.put(piggyId, JSONObject().put("PurchaseCount", 1).put("PurchaseDates", JSONArray().put(nowIso)))
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

    fun toggleSinglePurchase(profile: JSONObject, productId: String, unlocked: Boolean) {
        val purchaseHistory = profile.optJSONObject("purchaseHistory") ?: JSONObject()
        if (unlocked) {
            val nowIso = Instant.now().toString()
            purchaseHistory.put(productId, JSONObject().put("PurchaseCount", 1).put("PurchaseDates", JSONArray().put(nowIso)))
            if (productId == "premium_pass") {
                profile.put("seasonPassPurchased", true)
                profile.put("hasSeenPremiumPassActivation", true)
            }
        } else {
            purchaseHistory.remove(productId)
        }
        profile.put("purchaseHistory", purchaseHistory)
    }

    fun configurePiggyBank(
        profile: JSONObject,
        piggyId: String,
        missionId: Long,
        progressAmount: Int,
        isFilled: Boolean,
        makeFreeToClaim: Boolean = true
    ) {
        profile.put("piggyBank", JSONObject().apply {
            put("currentId", piggyId)
            put("lastSeenProgress", progressAmount)
            put("skippedCount", 0)
            put("lastSentAnalyticsProgress", 10)
            put("isFilled", isFilled)
            put("isFree", makeFreeToClaim)
            put("canClaimForFree", makeFreeToClaim)
        })

        if (makeFreeToClaim) {
            val purchaseHistory = profile.optJSONObject("purchaseHistory") ?: JSONObject()
            val nowIso = Instant.now().toString()
            purchaseHistory.put(piggyId, JSONObject().put("PurchaseCount", 1).put("PurchaseDates", JSONArray().put(nowIso)))
            profile.put("purchaseHistory", purchaseHistory)
        }

        val missions = profile.optJSONArray("missionProgresses") ?: JSONArray()
        var found = false
        for (i in 0 until missions.length()) {
            val m = missions.getJSONObject(i)
            if (m.optString("featureType") == "PiggyBank" || m.optLong("id") == missionId) {
                m.put("id", missionId); m.put("progress", progressAmount)
                m.put("isComplete", isFilled); m.put("isClaimed", false)
                m.put("featureType", "PiggyBank")
                found = true
            }
        }
        if (!found) {
            missions.put(JSONObject().apply {
                put("id", missionId); put("progress", progressAmount)
                put("isComplete", isFilled); put("isClaimed", false)
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
                for ((sId, _) in GameDatabase.SURFERS) put(sId)
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

    fun upsertCounter(counters: JSONArray, type: String, id: String, count: Int) {
        for (i in 0 until counters.length()) {
            val c = counters.getJSONObject(i)
            if (c.optString("CounterType") == type && c.optString("Id") == id) {
                c.put("Count", count)
                return
            }
        }
        counters.put(JSONObject().put("CounterType", type).put("Id", id).put("Count", count))
    }
}
