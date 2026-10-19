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
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// Модель состояния отдельного сёрфера в Subway Surfers City
data class CitySurferState(
    val id: Long,
    val name: String,
    var isUnlocked: Boolean,
    var level: Int,
    var highScore: Long,
    var tierRank: Int,
    var selectedSkin: Int
)

// Модель состояния отдельной доски в Subway Surfers City
data class CityBoardState(
    val id: Long,
    val name: String,
    var isUnlocked: Boolean,
    var level: Int
)

object CryptoAndSyncManager {

    const val PKG_CITY = "com.sybogames.subway.surfers.game"
    const val PKG_CLASSIC = "com.kiloo.subwaysurf"
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
                log("⚠️ Запрос разрешения у Shizuku...")
                Shizuku.requestPermission(42)
            } else {
                log("⚠️ Открываем приложение Shizuku...")
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
            if (errOutput.isNotBlank()) log?.invoke("⚙️ [stderr]: ${errOutput.trim()}")
            code to (output + errOutput)
        } catch (e: Exception) {
            log?.invoke("⚠️ Ошибка Shizuku shell: ${e.message}")
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
        val (code, result) = execSh("cat '$path' | base64 2>&1")
        if (code != 0 || result.isBlank()) return null
        return try {
            val cleanBase64 = result.replace("\n", "").replace("\r", "").replace(Regex("\\s+"), "").trim()
            val data = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (data.isNotEmpty()) data else null
        } catch (e: Exception) {
            log("❌ Ошибка чтения файла: ${e.message}")
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
            execSh("base64 -d '$tempB64' > '$tempFile'")
            val (_, result) = execSh("cp '$tempFile' '$path' 2>&1 && echo 'DONE'")
            if (!result.contains("DONE")) return false

            File(path).parent?.let { parentDir ->
                execSh("rm -f '$parentDir'/*.tmp '$parentDir'/*.temp 2>/dev/null")
            }
            execSh("sync")
            true
        } catch (e: Exception) {
            log("❌ Ошибка записи: ${e.message}")
            false
        } finally {
            try { execSh("rm -f '$tempB64' '$tempFile' 2>/dev/null") } catch (_: Exception) { }
        }
    }

    fun stopGameForSync(packageName: String = PKG_CITY, hardStop: Boolean = true, log: (String) -> Unit) {
        if (hardStop) execSh("am force-stop $packageName") else execSh("am kill $packageName")
    }

    fun launchGame(context: Context, packageName: String = PKG_CITY, log: (String) -> Unit) {
        log("🚀 Запускаем игру...")
        val (code, _) = execSh("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        if (code != 0) {
            val intent = context.packageManager?.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    fun restoreBackup(path: String, log: (String) -> Unit): Boolean {
        stopGameForSync(PKG_CITY, true, log)
        val (_, out) = execSh("if [ -f '$path.bak_editor' ]; then cp '$path.bak_editor' '$path' && sync && echo 'RESTORED'; fi")
        return out.contains("RESTORED")
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

    // ==================== ИНДИВИДУАЛЬНЫЙ РЕДАКТОР СЁРФЕРОВ И ДОСОК CITY ====================

    fun parseCitySurfers(profile: JSONObject): List<CitySurferState> {
        val currentArray = profile.optJSONArray("surferProfiles") ?: JSONArray()
        val map = mutableMapOf<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            map[obj.getLong("id")] = obj
        }

        val counters = profile.optJSONArray("counters") ?: JSONArray()
        val tierMap = mutableMapOf<Long, Int>()
        for (i in 0 until counters.length()) {
            val c = counters.getJSONObject(i)
            if (c.optString("CounterType") == "ScoreLadderUnlockTierCounter") {
                val idStr = c.optString("Id").removePrefix("DataTag(").removeSuffix(")")
                idStr.toLongOrNull()?.let { tierMap[it] = c.optInt("Count", 0) }
            }
        }

        return GameDatabase.SURFERS.map { (id, name) ->
            val obj = map[id]
            CitySurferState(
                id = id,
                name = name,
                isUnlocked = obj?.optBoolean("isUnlocked", false) ?: false,
                level = obj?.optInt("level", 1) ?: 1,
                highScore = obj?.optLong("highScore", 0L) ?: 0L,
                tierRank = tierMap[id] ?: 0,
                selectedSkin = obj?.optInt("selectedSkin", 0) ?: 0
            )
        }
    }

    fun applyCitySurfers(profile: JSONObject, states: List<CitySurferState>) {
        val newArray = JSONArray()
        val counters = profile.optJSONArray("counters") ?: JSONArray()
        for (s in states) {
            if (s.isUnlocked || s.highScore > 0 || s.level > 1) {
                newArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("level", s.level.coerceIn(1, 20))
                    put("isUnlocked", s.isUnlocked)
                    put("wasSeen", true)
                    put("highScore", s.highScore)
                    put("selectedSkin", s.selectedSkin)
                })
            }
            if (s.tierRank > 0) {
                upsertCounter(counters, "LastUnlockedTierCounter", "DataTag(${s.id})", s.tierRank.coerceIn(0, 40))
                upsertCounter(counters, "ScoreLadderUnlockTierCounter", "DataTag(${s.id})", s.tierRank.coerceIn(0, 40))
            }
        }
        profile.put("surferProfiles", newArray)
        profile.put("counters", counters)
    }

    fun parseCityBoards(profile: JSONObject): List<CityBoardState> {
        val currentArray = profile.optJSONArray("boardProfiles") ?: JSONArray()
        val map = mutableMapOf<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            map[obj.getLong("id")] = obj
        }
        return GameDatabase.BOARDS.map { (id, name) ->
            val obj = map[id]
            CityBoardState(
                id = id,
                name = name,
                isUnlocked = obj?.optBoolean("isUnlocked", false) ?: false,
                level = obj?.optInt("level", 1) ?: 1
            )
        }
    }

    fun applyCityBoards(profile: JSONObject, states: List<CityBoardState>) {
        val newArray = JSONArray()
        for (b in states) {
            if (b.isUnlocked || b.level > 1) {
                newArray.put(JSONObject().apply {
                    put("id", b.id)
                    put("level", b.level.coerceIn(1, 20))
                    put("isUnlocked", b.isUnlocked)
                    put("wasSeen", true)
                })
            }
        }
        profile.put("boardProfiles", newArray)
    }

    fun unlockAndMaxAllSurfers(profile: JSONObject) {
        val currentArray = profile.optJSONArray("surferProfiles") ?: JSONArray()
        val existingSkins = mutableMapOf<Long, Int>()
        val existingScores = mutableMapOf<Long, Long>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            val sId = obj.getLong("id")
            existingSkins[sId] = obj.optInt("selectedSkin", 0)
            existingScores[sId] = obj.optLong("highScore", 0L)
        }
        val newArray = JSONArray()
        for ((id, _) in GameDatabase.SURFERS) {
            newArray.put(JSONObject().apply {
                put("id", id)
                put("level", 20)
                put("isUnlocked", true)
                put("wasSeen", true)
                put("highScore", existingScores[id] ?: 0L)
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

    // Выполнение всех активных Ежедневных и Еженедельных заданий в Subway Surfers City
    fun completeAllDailyAndWeeklyMissions(profile: JSONObject) {
        val missions = profile.optJSONArray("missionProgresses") ?: JSONArray()
        for (i in 0 until missions.length()) {
            val m = missions.getJSONObject(i)
            val curProg = m.optInt("progress", 0)
            m.put("progress", maxOf(curProg, 100000))
            m.put("isComplete", true)
            m.put("isClaimed", false)
        }
        profile.put("missionProgresses", missions)

        // Также начисляем очки сезонного пропуска за задания
        val curSeasonPts = profile.optInt("seasonPassPoints", 0)
        profile.put("seasonPassPoints", maxOf(curSeasonPts, 15000))
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
        })

        if (makeFreeToClaim) {
            val purchaseHistory = profile.optJSONObject("purchaseHistory") ?: JSONObject()
            val nowIso = Instant.now().toString()
            for ((pId, _) in GameDatabase.PIGGY_BANKS) {
                purchaseHistory.put(pId, JSONObject().put("PurchaseCount", 1).put("PurchaseDates", JSONArray().put(nowIso)))
            }
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

    fun unlockAllEventTrialsAnd3StarMissions(profile: JSONObject) {
        val goals = profile.optJSONArray("goals") ?: JSONArray()
        val existingGoalIds = mutableSetOf<String>()
        for (i in 0 until goals.length()) {
            val g = goals.getJSONObject(i)
            g.put("completed", true)
            existingGoalIds.add(g.optString("id"))
        }

        for ((campId, _) in GameDatabase.SCHEDULED_CAMPAIGNS) {
            for (stage in 1..7) {
                for (goal in 1..3) {
                    val gId = "${campId}_stage${stage}_goal$goal"
                    if (existingGoalIds.add(gId)) {
                        goals.put(JSONObject().put("id", gId).put("completed", true))
                    }
                }
            }
        }

        val trialPrefixes = listOf("trial", "daily_trial", "event_trial", "weekly_trial")
        for (prefix in trialPrefixes) {
            for (stage in 1..5) {
                for (goal in 1..3) {
                    val gId = "${prefix}_stage${stage}_goal$goal"
                    if (existingGoalIds.add(gId)) {
                        goals.put(JSONObject().put("id", gId).put("completed", true))
                    }
                }
            }
        }
        profile.put("goals", goals)

        val activeCamp = profile.optJSONObject("activeScheduledCampaignProfile") ?: JSONObject()
        if (!activeCamp.has("activeScheduledCampaignId")) {
            activeCamp.put("activeScheduledCampaignId", GameDatabase.SCHEDULED_CAMPAIGNS.first().first)
        }
        activeCamp.put("playableSurfers", JSONArray().apply {
            for ((sId, _) in GameDatabase.SURFERS) put(sId)
        })
        profile.put("activeScheduledCampaignProfile", activeCamp)

        val trials = profile.optJSONArray("dailyTrials")
        if (trials != null) {
            for (i in 0 until trials.length()) {
                trials.getJSONObject(i).apply {
                    put("attempts", 0)
                    put("claimedGoalRewards", 0)
                }
            }
        }

        profile.put("timers", JSONObject())
        profile.optJSONObject("onlinePlayerProfile")?.put("renameableAt", "0001-01-01T00:00:00")

        val ftue = profile.optJSONObject("ftue") ?: JSONObject()
        for (step in GameDatabase.FTUE_STEPS) {
            ftue.put(step, true)
        }
        profile.put("ftue", ftue)
    }

    fun hackDailyDealsAndAdRewards(profile: JSONObject, dealRewardAmount: Int = 9999, customTag: Long? = null) {
        val defaultTags = longArrayOf(-1878560402L, -1509662453L, -1085419483L, -635142441L, -2008679869L, 326719861L)
        val dealsArray = JSONArray()
        for (i in 0 until 6) {
            dealsArray.put(JSONObject().apply {
                put("slotId", "daily_deal_${i + 1}")
                put("slotSeed", 10 + i)
                put("itemDatatag", customTag ?: defaultTags[i])
                put("amount", dealRewardAmount)
            })
        }
        profile.put("dailyDeal", JSONObject().put("dailyDeals", dealsArray))

        val adRewards = JSONArray()
        val adTags = longArrayOf(-635142441L, -2008679869L, 326719861L, 1669706535L, -633437229L, -1085419483L, -1878560402L, 722065917L, -1509662453L)
        for (tag in adTags) {
            adRewards.put(JSONObject().apply {
                put("RewardType", customTag ?: tag)
                put("RewardValue", dealRewardAmount)
                put("Claimed", false)
            })
        }
        profile.put("dailyAdRewards", adRewards)
    }

    fun unlockSecretStarsAchievementsAndJackpots(profile: JSONObject) {
        val counters = profile.optJSONArray("counters") ?: JSONArray()
        for (starTag in GameDatabase.SECRET_STAR_TAGS) {
            upsertCounter(counters, "SecretStarCounter", "DataTag($starTag)", 10)
        }
        upsertCounter(counters, "PityCounter", "mysterybox_regular", 9)
        upsertCounter(counters, "PityCounter", "shop.box.ad", 9)
        upsertCounter(counters, "PityCounter", "shop.box.ad.board", 9)
        upsertCounter(counters, "PityCounter", "shop.box.ad.repeatable", 9)
        upsertCounter(counters, "DailyDealCounter", "daily_deal", 0)
        profile.put("counters", counters)

        val missions = profile.optJSONArray("missionProgresses") ?: JSONArray()
        for ((achId, target) in GameDatabase.ACHIEVEMENT_MISSIONS) {
            var exists = false
            for (i in 0 until missions.length()) {
                val m = missions.getJSONObject(i)
                if (m.optLong("id") == achId) {
                    m.put("progress", target); m.put("isComplete", true); m.put("isClaimed", false)
                    exists = true
                }
            }
            if (!exists) {
                missions.put(JSONObject().apply {
                    put("id", achId); put("progress", target)
                    put("isComplete", true); put("isClaimed", false)
                    put("creationUnixTimestamp", 0)
                    put("creationUnixTimestampSeconds", Instant.now().epochSecond)
                    put("featureType", "Achievement")
                })
            }
        }
        profile.put("missionProgresses", missions)
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
