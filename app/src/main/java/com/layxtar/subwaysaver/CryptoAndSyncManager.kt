package com.cityedit.ultimate

import android.content.pm.PackageManager
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class GameEdition(
    val title: String,
    val packageName: String,
    val primarySaveDir: String,
    val secondarySaveDir: String
) {
    CITY(
        title = "Subway Surfers City",
        packageName = "com.sybogames.subway.surfers.game",
        primarySaveDir = "/data/data/com.sybogames.subway.surfers.game/files/save",
        secondarySaveDir = "/sdcard/Android/data/com.sybogames.subway.surfers.game/files/save"
    ),
    CLASSIC(
        title = "Subway Surfers Classic",
        packageName = "com.kiloo.subwaysurf",
        primarySaveDir = "/sdcard/Android/data/com.kiloo.subwaysurf/files/profile",
        secondarySaveDir = "/data/data/com.kiloo.subwaysurf/files/profile"
    )
}

object CryptoEngine {

    // =========================================================================
    // 1. КРИПТОГРАФИЯ CLASSIC SUBWAY SURFERS (AES-256-CBC + Zero IV + 4B LE Size)
    // =========================================================================
    private val CLASSIC_KEY_BYTES = byteArrayOf(
        213.toByte(), 189.toByte(), 120.toByte(), 46.toByte(),
        226.toByte(), 172.toByte(), 180.toByte(), 217.toByte(),
        134.toByte(), 91.toByte(), 45.toByte(), 250.toByte(),
        179.toByte(), 236.toByte(), 254.toByte(), 240.toByte(),
        122.toByte(), 13.toByte(), 146.toByte(), 202.toByte(),
        77.toByte(), 209.toByte(), 251.toByte(), 95.toByte(),
        67.toByte(), 192.toByte(), 34.toByte(), 196.toByte(),
        224.toByte(), 172.toByte(), 34.toByte(), 203.toByte()
    )

    private val ZERO_IV = ByteArray(16) { 0 }

    fun decryptClassic(rawBytes: ByteArray): String {
        if (rawBytes.isEmpty()) throw IllegalArgumentException("Файл пуст")

        // Если файл уже расшифрован (начинается с '{')
        val trimmed = rawBytes.dropWhile { it == ' '.code.toByte() || it == '\n'.code.toByte() || it == '\r'.code.toByte() }
        if (trimmed.isNotEmpty() && trimmed.first() == '{'.code.toByte()) {
            return String(rawBytes, Charsets.UTF_8)
        }

        // Пробуем стандартный отступ 4 байта (Little-Endian длина) или альтернативные заголовки
        val possibleOffsets = listOf(4, 0, 8, 16, 20, 36)
        var lastError: Exception? = null

        for (offset in possibleOffsets) {
            if (rawBytes.size <= offset) continue
            val payload = rawBytes.copyOfRange(offset, rawBytes.size)
            if (payload.size % 16 != 0) continue

            try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val keySpec = SecretKeySpec(CLASSIC_KEY_BYTES, "AES")
                val ivSpec = IvParameterSpec(ZERO_IV)
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                val decrypted = cipher.doFinal(payload)
                val text = String(decrypted, Charsets.UTF_8).trim()
                if (text.startsWith("{") && text.endsWith("}")) {
                    return text
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException("Не удалось расшифровать Classic JSON: ${lastError?.message}")
    }

    fun encryptClassic(jsonString: String): ByteArray {
        val cleanJsonBytes = jsonString.trim().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(CLASSIC_KEY_BYTES, "AES")
        val ivSpec = IvParameterSpec(ZERO_IV)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encryptedBytes = cipher.doFinal(cleanJsonBytes)

        // Записываем 4-байтовый Little-Endian заголовок с длиной зашифрованных данных
        val buffer = ByteBuffer.allocate(4 + encryptedBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(encryptedBytes.size)
        buffer.put(encryptedBytes)
        return buffer.array()
    }

    // =========================================================================
    // 2. КРИПТОГРАФИЯ SUBWAY SURFERS CITY (AES-128-ECB + 4B LE Uncompressed Size)
    // =========================================================================
    // Стандартный ключ шифрования сохранений City
    private val CITY_DEFAULT_KEY = "SubwaySurfersCit".toByteArray(Charsets.UTF_8)

    fun decryptCity(rawBytes: ByteArray, customKey: ByteArray? = null): String {
        if (rawBytes.isEmpty()) throw IllegalArgumentException("Файл сохранения пуст")

        // Проверка на открытый JSON
        val firstChar = rawBytes.firstOrNull { it != ' '.code.toByte() && it != '\n'.code.toByte() }
        if (firstChar == '{'.code.toByte()) {
            return String(rawBytes, Charsets.UTF_8)
        }

        val keyToUse = customKey ?: CITY_DEFAULT_KEY
        val offsets = listOf(4, 0, 16)
        var lastError: Exception? = null

        for (offset in offsets) {
            if (rawBytes.size <= offset) continue
            val payload = rawBytes.copyOfRange(offset, rawBytes.size)
            if (payload.size % 16 != 0) continue

            try {
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyToUse, "AES"))
                val decrypted = cipher.doFinal(payload)
                val text = String(decrypted, Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r')
                if (text.startsWith("{")) {
                    return text
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException("Ошибка расшифровки City: ${lastError?.message}")
    }

    fun encryptCity(jsonString: String, customKey: ByteArray? = null): ByteArray {
        val rawUtf8 = jsonString.trim().toByteArray(Charsets.UTF_8)
        val keyToUse = customKey ?: CITY_DEFAULT_KEY
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyToUse, "AES"))
        val encrypted = cipher.doFinal(rawUtf8)

        val buffer = ByteBuffer.allocate(4 + encrypted.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(rawUtf8.size)
        buffer.put(encrypted)
        return buffer.array()
    }

    fun sha1Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun updateCityManifest(manifestJson: String, fileName: String, newFileBytes: ByteArray): String {
        val newHash = sha1Hex(newFileBytes)
        return try {
            val root = JSONObject(manifestJson)
            if (root.has("files")) {
                val filesObj = root.getJSONObject("files")
                if (filesObj.has(fileName)) {
                    val entry = filesObj.optJSONObject(fileName)
                    if (entry != null) {
                        entry.put("hash", newHash)
                        entry.put("size", newFileBytes.size)
                    } else {
                        filesObj.put(fileName, newHash)
                    }
                }
            }
            // Также обновляем любые строковые поля хеша, если структура плоская
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k.contains(fileName, ignoreCase = true)) {
                    root.put(k, newHash)
                }
            }
            root.toString(2)
        } catch (e: Exception) {
            manifestJson
        }
    }
}

// =============================================================================
// SHIZUKU SHELL & CHUNKED FILE I/O MANAGER
// =============================================================================
object ShizukuShellManager {
    private const val CHUNK_SIZE = 48 * 1024 // 48 КБ для безопасной передачи через shell

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else false
    }

    fun requestPermission(requestCode: Int = 1001) {
        if (isShizukuAvailable() && !hasPermission()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    suspend fun execShell(command: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()
            process.inputStream.use { it.copyTo(stdout) }
            process.errorStream.use { it.copyTo(stderr) }
            val code = process.waitFor()

            val outStr = stdout.toString("UTF-8").trim()
            val errStr = stderr.toString("UTF-8").trim()
            val combined = if (outStr.isNotEmpty()) outStr else errStr
            Pair(code, combined)
        } catch (e: Exception) {
            Pair(-1, e.message ?: "Unknown Shizuku Error")
        }
    }

    suspend fun resolveActiveSaveDir(edition: GameEdition): String {
        val (_, checkPrimary) = execShell("if [ -d '${edition.primarySaveDir}' ]; then echo 'YES'; fi")
        if (checkPrimary.contains("YES")) return edition.primarySaveDir

        val (_, checkSecondary) = execShell("if [ -d '${edition.secondarySaveDir}' ]; then echo 'YES'; fi")
        if (checkSecondary.contains("YES")) return edition.secondarySaveDir

        // Поиск папки профиля автоматически
        val (_, found) = execShell("find /sdcard/Android/data/${edition.packageName} /data/data/${edition.packageName} -type d \\( -name 'profile' -o -name 'save' \\) 2>/dev/null | head -n 1")
        return if (found.isNotEmpty() && found.startsWith("/")) found else edition.primarySaveDir
    }

    suspend fun listSaveFiles(dirPath: String): List<String> {
        val (code, output) = execShell("ls -1 '$dirPath' 2>/dev/null")
        if (code != 0 || output.isEmpty()) return emptyList()
        return output.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("ls:") }
    }

    suspend fun readFileBytes(remotePath: String): ByteArray? = withContext(Dispatchers.IO) {
        val tmpPath = "/data/local/tmp/ce_read_${System.currentTimeMillis()}.tmp"
        val (cpCode, _) = execShell("cp '$remotePath' '$tmpPath' && chmod 666 '$tmpPath'")
        if (cpCode != 0) return@withContext null

        val (_, b64Out) = execShell("base64 -w 0 '$tmpPath' 2>/dev/null || base64 '$tmpPath'")
        execShell("rm -f '$tmpPath'")

        if (b64Out.isEmpty()) return@withContext null
        return@withContext try {
            Base64.decode(b64Out.replace("\\s".toRegex(), ""), Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun writeFileBytes(
        edition: GameEdition,
        remotePath: String,
        bytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        // Принудительно останавливаем игру перед записью, чтобы она не перезаписала кэш
        execShell("am force-stop ${edition.packageName}")

        val tmpPath = "/data/local/tmp/ce_write_${System.currentTimeMillis()}.tmp"
        execShell("rm -f '$tmpPath' && touch '$tmpPath'")

        val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
        var offset = 0
        while (offset < base64Str.length) {
            val end = minOf(offset + CHUNK_SIZE, base64Str.length)
            val chunk = base64Str.substring(offset, end)
            val (code, _) = execShell("printf '%s' '$chunk' >> '$tmpPath.b64'")
            if (code != 0) {
                execShell("rm -f '$tmpPath' '$tmpPath.b64'")
                return@withContext false
            }
            offset = end
        }

        // Декодируем на устройстве, копируем с сохранением владельца/прав и вызываем sync
        val cmd = """
            base64 -d '$tmpPath.b64' > '$tmpPath' &&
            cp '$tmpPath' '$remotePath' &&
            chmod 660 '$remotePath' 2>/dev/null ;
            rm -f '$tmpPath' '$tmpPath.b64' &&
            sync
        """.trimIndent()

        val (finalCode, _) = execShell(cmd)
        finalCode == 0
    }

    suspend fun launchGame(edition: GameEdition) {
        execShell("monkey -p ${edition.packageName} -c android.intent.category.LAUNCHER 1")
    }

    suspend fun forceStopGame(edition: GameEdition) {
        execShell("am force-stop ${edition.packageName}")
    }
}
