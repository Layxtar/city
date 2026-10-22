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

data class CitySurferState(
    val id: Long,
    val name: String,
    var isUnlocked: Boolean,
    var level: Int,
    var highScore: Long,
    var selectedSkin: Int,
    val availableSkins: List<Pair<Int, String>>
)

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

    const val DEFAULT_GAME_PROFILE_PATH = "/storage/emulated/0/Android/data/$PKG_CITY/files/enc/profile"
    private const val CHUNK_SIZE = 49152

    private var listenerRegistered = false
    var onPermissionGrantedCallback: ((Boolean) -> Unit)? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        onPermissionGrantedCallback?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
    }

    fun registerPermissionListener() {
        if (!listenerRegistered) {
            runCatching {
                Shizuku.addRequestPermissionResultListener(permissionListener)
                listenerRegistered = true
            }
        }
    }

    fun unregisterPermissionListener() {
        if (listenerRegistered) {
            runCatching {
                Shizuku.removeRequestPermissionResultListener(permissionListener)
                listenerRegistered = false
            }
        }
    }

    fun isServiceRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        isServiceRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermissionOrOpenShizuku(context: Context, log: (String) -> Unit) {
        if (isServiceRunning()) {
            Shizuku.requestPermission(42)
        } else {
            context.packageManager?.getLaunchIntentForPackage(PKG_SHIZUKU)?.let {
                context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
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

    fun resolveCityProfilePath(log: (String) -> Unit): String = DEFAULT_GAME_PROFILE_PATH

    fun readFileViaShizuku(path: String, log: (String) -> Unit): ByteArray? {
        val (code, result) = execSh("cat '$path' | base64 2>&1")
        if (code != 0 || result.isBlank()) return null
        return runCatching { Base64.decode(result.replace(Regex("\\s+"), ""), Base64.DEFAULT) }.getOrNull()
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
            execSh("sync")
            result.contains("DONE")
        } catch (_: Exception) {
            false
        } finally {
            execSh("rm -f '$tempB64' '$tempFile' 2>/dev/null")
        }
    }

    fun stopGameForSync(packageName: String, hardStop: Boolean = true, log: (String) -> Unit) {
        if (hardStop) execSh("am force-stop $packageName") else execSh("am kill $packageName")
    }

    fun launchGame(context: Context, packageName: String, log: (String) -> Unit) {
        val (code, _) = execSh("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        if (code != 0) {
            context.packageManager?.getLaunchIntentForPackage(packageName)?.let {
                context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun restoreBackup(path: String, log: (String) -> Unit): Boolean {
        stopGameForSync(PKG_CITY, true, log)
        val (_, out) = execSh("if [ -f '$path.bak_editor' ]; then cp '$path.bak_editor' '$path' && sync && echo 'RESTORED'; fi")
        return out.contains("RESTORED")
    }

    fun decryptCityProfile(rawBytes: ByteArray): Pair<JSONObject, JSONObject> {
        require(rawBytes.size >= 32)
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
        val container = JSONObject(String(decrypted, Charsets.UTF_8))
        val profile = JSONObject(container.getString("profile"))
        return container to profile
    }

    fun encryptCityProfile(originalRawBytes: ByteArray, containerJson: JSONObject, innerProfileJson: JSONObject): ByteArray {
        val iv = originalRawBytes.copyOfRange(0, 16)
        val key = originalRawBytes.copyOfRange(16, 32)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'").withZone(ZoneOffset.UTC)
        containerJson.put("lastUpdated", formatter.format(Instant.now().plusSeconds(300)))
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
            val encCounter = cipher.doFinal(counter)
            for (i in 0 until blockSize) {
                encrypted[offset + i] = (plainBytes[offset + i].toInt() xor encCounter[i].toInt()).toByte()
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

    // ==================== БЕРЕЖНЫЙ ПАРСИНГ И СОХРАНЕНИЕ СЁРФЕРОВ И ДОСОК CITY ====================
    // Читает как известные из GameDatabase.SURFERS, так и ВСЕ реальные объекты из твоего сохранения!
    fun parseCitySurfers(profile: JSONObject): List<CitySurferState> {
        val currentArray = profile.optJSONArray("surferProfiles") ?: JSONArray()
        val mapInSave = LinkedHashMap<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            mapInSave[obj.getLong("id")] = obj
        }

        val result = mutableListOf<CitySurferState>()
        val processedIds = mutableSetOf<Long>()

        // 1. Сначала проходим по базе GameDatabase.SURFERS
        for ((id, name) in GameDatabase.SURFERS) {
            processedIds.add(id)
            val obj = mapInSave[id]
            val skins = GameDatabase.getSkinsForSurfer(id, name)
            result.add(
                CitySurferState(
                    id = id,
                    name = name,
                    isUnlocked = obj?.optBoolean("isUnlocked", false) ?: false,
                    level = obj?.optInt("level", 1) ?: 1,
                    highScore = obj?.optLong("highScore", 0L) ?: 0L,
                    selectedSkin = obj?.optInt("selectedSkin", 0) ?: 0,
                    availableSkins = skins
                )
            )
        }

        // 2. Если в сохранении есть персонажи с другими ID (чтобы ни один открытый персонаж не потерялся!), добавляем их тоже
        for ((id, obj) in mapInSave) {
            if (processedIds.add(id)) {
                val label = "Сёрфер ID: $id"
                result.add(
                    CitySurferState(
                        id = id,
                        name = label,
                        isUnlocked = obj.optBoolean("isUnlocked", true),
                        level = obj.optInt("level", 1),
                        highScore = obj.optLong("highScore", 0L),
                        selectedSkin = obj.optInt("selectedSkin", 0),
                        availableSkins = GameDatabase.getSkinsForSurfer(id, label)
                    )
                )
            }
        }
        return result
    }

    // Обновляет surferProfiles на месте, НЕ удаляя никаких существующих полей из сохранения!
    fun applyCitySurfers(profile: JSONObject, states: List<CitySurferState>) {
        val currentArray = profile.optJSONArray("surferProfiles") ?: JSONArray()
        val existingObjects = LinkedHashMap<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            existingObjects[obj.getLong("id")] = obj
        }

        for (s in states) {
            val existing = existingObjects[s.id]
            if (existing != null) {
                existing.put("isUnlocked", s.isUnlocked)
                existing.put("level", s.level.coerceIn(1, 20))
                existing.put("highScore", s.highScore)
                existing.put("selectedSkin", s.selectedSkin)
                if (s.isUnlocked) existing.put("wasSeen", true)
            } else if (s.isUnlocked || s.highScore > 0 || s.level > 1) {
                existingObjects[s.id] = JSONObject().apply {
                    put("id", s.id)
                    put("level", s.level.coerceIn(1, 20))
                    put("isUnlocked", s.isUnlocked)
                    put("wasSeen", true)
                    put("highScore", s.highScore)
                    put("selectedSkin", s.selectedSkin)
                }
            }
        }

        val outArray = JSONArray()
        for ((_, obj) in existingObjects) outArray.put(obj)
        profile.put("surferProfiles", outArray)
    }

    fun parseCityBoards(profile: JSONObject): List<CityBoardState> {
        val currentArray = profile.optJSONArray("boardProfiles") ?: JSONArray()
        val mapInSave = LinkedHashMap<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            mapInSave[obj.getLong("id")] = obj
        }

        val result = mutableListOf<CityBoardState>()
        val processedIds = mutableSetOf<Long>()

        for ((id, name) in GameDatabase.BOARDS) {
            processedIds.add(id)
            val obj = mapInSave[id]
            result.add(
                CityBoardState(
                    id = id,
                    name = name,
                    isUnlocked = obj?.optBoolean("isUnlocked", false) ?: false,
                    level = obj?.optInt("level", 1) ?: 1
                )
            )
        }

        for ((id, obj) in mapInSave) {
            if (processedIds.add(id)) {
                result.add(
                    CityBoardState(
                        id = id,
                        name = "Доска ID: $id",
                        isUnlocked = obj.optBoolean("isUnlocked", true),
                        level = obj.optInt("level", 1)
                    )
                )
            }
        }
        return result
    }

    fun applyCityBoards(profile: JSONObject, states: List<CityBoardState>) {
        val currentArray = profile.optJSONArray("boardProfiles") ?: JSONArray()
        val existingObjects = LinkedHashMap<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            existingObjects[obj.getLong("id")] = obj
        }

        for (b in states) {
            val existing = existingObjects[b.id]
            if (existing != null) {
                existing.put("isUnlocked", b.isUnlocked)
                existing.put("level", b.level.coerceIn(1, 20))
                if (b.isUnlocked) existing.put("wasSeen", true)
            } else if (b.isUnlocked || b.level > 1) {
                existingObjects[b.id] = JSONObject().apply {
                    put("id", b.id)
                    put("level", b.level.coerceIn(1, 20))
                    put("isUnlocked", b.isUnlocked)
                    put("wasSeen", true)
                }
            }
        }

        val outArray = JSONArray()
        for ((_, obj) in existingObjects) outArray.put(obj)
        profile.put("boardProfiles", outArray)
    }

    fun unlockAndMaxAllSurfers(profile: JSONObject) {
        val currentArray = profile.optJSONArray("surferProfiles") ?: JSONArray()
        val existingObjects = LinkedHashMap<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            val id = obj.getLong("id")
            obj.put("level", 20)
            obj.put("isUnlocked", true)
            obj.put("wasSeen", true)
            existingObjects[id] = obj
        }
        for ((id, _) in GameDatabase.SURFERS) {
            if (!existingObjects.containsKey(id)) {
                existingObjects[id] = JSONObject().apply {
                    put("id", id)
                    put("level", 20)
                    put("isUnlocked", true)
                    put("wasSeen", true)
                    put("highScore", 0L)
                    put("selectedSkin", 0)
                }
            }
        }
        val newArray = JSONArray()
        for ((_, obj) in existingObjects) newArray.put(obj)
        profile.put("surferProfiles", newArray)
    }

    fun unlockAndMaxAllBoards(profile: JSONObject) {
        val currentArray = profile.optJSONArray("boardProfiles") ?: JSONArray()
        val existingObjects = LinkedHashMap<Long, JSONObject>()
        for (i in 0 until currentArray.length()) {
            val obj = currentArray.getJSONObject(i)
            val id = obj.getLong("id")
            obj.put("level", 20)
            obj.put("isUnlocked", true)
            obj.put("wasSeen", true)
            existingObjects[id] = obj
        }
        for ((id, _) in GameDatabase.BOARDS) {
            if (!existingObjects.containsKey(id)) {
                existingObjects[id] = JSONObject().put("id", id).put("level", 20).put("isUnlocked", true).put("wasSeen", true)
            }
        }
        val newArray = JSONArray()
        for ((_, obj) in existingObjects) newArray.put(obj)
        profile.put("boardProfiles", newArray)
    }

    // Бережная разблокировка всех скинов и аватаров (сохраняет все существующие в профиле ID скинов + добавляет из базы)
    fun unlockAllSkinsAndAvatars(profile: JSONObject) {
        val curSkins = profile.optJSONArray("surferSkinProfiles") ?: JSONArray()
        val skinMap = LinkedHashMap<Any, JSONObject>()
        for (i in 0 until curSkins.length()) {
            val obj = curSkins.getJSONObject(i)
            val idKey = obj.opt("id") ?: continue
            obj.put("isUnlocked", true)
            obj.put("wasSeen", true)
            skinMap[idKey] = obj
        }
        for (skinId in GameDatabase.ALL_SKINS) {
            if (!skinMap.containsKey(skinId)) {
                skinMap[skinId] = JSONObject().put("id", skinId).put("isUnlocked", true).put("wasSeen", true)
            }
        }
        val skinsOut = JSONArray()
        for ((_, obj) in skinMap) skinsOut.put(obj)
        profile.put("surferSkinProfiles", skinsOut)

        val curAvatars = profile.optJSONArray("avatarProfiles") ?: JSONArray()
        val avSet = mutableSetOf<Any>()
        for (i in 0 until curAvatars.length()) {
            curAvatars.getJSONObject(i).opt("id")?.let { avSet.add(it) }
        }
        for (avatarId in GameDatabase.ALL_AVATARS) {
            if (avSet.add(avatarId)) curAvatars.put(JSONObject().put("id", avatarId))
        }
        profile.put("avatarProfiles", curAvatars)
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
                    if (existingGoalIds.add(gId)) goals.put(JSONObject().put("id", gId).put("completed", true))
                }
            }
        }
        profile.put("goals", goals)
    }

    fun completeAllDailyAndWeeklyMissions(profile: JSONObject) {
        val missions = profile.optJSONArray("missionProgresses") ?: JSONArray()
        for (i in 0 until missions.length()) {
            val m = missions.getJSONObject(i)
            m.put("progress", maxOf(m.optInt("progress", 0), 100000))
            m.put("isComplete", true)
            m.put("isClaimed", false)
        }
        profile.put("missionProgresses", missions)
        profile.put("seasonPassPoints", maxOf(profile.optInt("seasonPassPoints", 0), 15000))
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
    }

    fun toggleSinglePurchase(profile: JSONObject, productId: String, unlocked: Boolean) {
        val purchaseHistory = profile.optJSONObject("purchaseHistory") ?: JSONObject()
        if (unlocked) {
            purchaseHistory.put(productId, JSONObject().put("PurchaseCount", 1).put("PurchaseDates", JSONArray().put(Instant.now().toString())))
            if (productId == "premium_pass") profile.put("seasonPassPurchased", true)
        } else {
            purchaseHistory.remove(productId)
        }
        profile.put("purchaseHistory", purchaseHistory)
    }

    fun configurePiggyBank(profile: JSONObject, piggyId: String, missionId: Long, progressAmount: Int, isFilled: Boolean, makeFreeToClaim: Boolean = true) {
        profile.put("piggyBank", JSONObject().apply {
            put("currentId", piggyId)
            put("lastSeenProgress", progressAmount)
            put("skippedCount", 0)
            put("isFilled", isFilled)
        })
        if (makeFreeToClaim) {
            val purchaseHistory = profile.optJSONObject("purchaseHistory") ?: JSONObject()
            purchaseHistory.put(piggyId, JSONObject().put("PurchaseCount", 1).put("PurchaseDates", JSONArray().put(Instant.now().toString())))
            profile.put("purchaseHistory", purchaseHistory)
        }
    }

    fun unlockAllEventTrialsAnd3StarMissions(profile: JSONObject) {
        val goals = profile.optJSONArray("goals") ?: JSONArray()
        val existing = mutableSetOf<String>()
        for (i in 0 until goals.length()) {
            goals.getJSONObject(i).put("completed", true)
            existing.add(goals.getJSONObject(i).optString("id"))
        }
        for ((campId, _) in GameDatabase.SCHEDULED_CAMPAIGNS) {
            for (stage in 1..7) {
                for (goal in 1..3) {
                    val gId = "${campId}_stage${stage}_goal$goal"
                    if (existing.add(gId)) goals.put(JSONObject().put("id", gId).put("completed", true))
                }
            }
        }
        profile.put("goals", goals)
        profile.put("timers", JSONObject())
    }

    fun hackDailyDealsAndAdRewards(profile: JSONObject) {
        val deals = JSONArray()
        val tags = longArrayOf(-1878560402L, -1509662453L, -1085419483L, -635142441L, -2008679869L, 326719861L)
        for (i in 0 until 6) {
            deals.put(JSONObject().apply {
                put("slotId", "daily_deal_${i + 1}")
                put("itemDatatag", tags[i])
                put("amount", 9999)
            })
        }
        profile.put("dailyDeal", JSONObject().put("dailyDeals", deals))
    }

    fun unlockSecretStarsAchievementsAndJackpots(profile: JSONObject) {
        val counters = profile.optJSONArray("counters") ?: JSONArray()
        upsertCounter(counters, "PityCounter", "mysterybox_regular", 9)
        upsertCounter(counters, "PityCounter", "shop.box.ad", 9)
        profile.put("counters", counters)
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
