package com.city.editor

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    // Ключ для Subway Surfers City (AES-128 ECB)
    private val CITY_AES_KEY = "8472619503827461".toByteArray(Charsets.UTF_8)

    // 32-байтовый ключ для Subway Surfers Classic (AES-256 CBC)
    // Стандартный ключ шифрования профилей SYBO (SHA-256 от секретной соли профиля)
    private val CLASSIC_AES_KEY = byteArrayOf(
        0x39.toByte(), 0x76.toByte(), 0x67.toByte(), 0x5A.toByte(),
        0x6B.toByte(), 0x49.toByte(), 0x6E.toByte(), 0x71.toByte(),
        0x38.toByte(), 0x41.toByte(), 0x33.toByte(), 0x79.toByte(),
        0x50.toByte(), 0x56.toByte(), 0x2B.toByte(), 0x68.toByte(),
        0x64.toByte(), 0x35.toByte(), 0x59.toByte(), 0x74.toByte(),
        0x4B.toByte(), 0x32.toByte(), 0x4E.toByte(), 0x58.toByte(),
        0x4F.toByte(), 0x37.toByte(), 0x7A.toByte(), 0x61.toByte(),
        0x63.toByte(), 0x66.toByte(), 0x4D.toByte(), 0x30.toByte()
    )

    // Позволяет переопределить ключ Classic из UI при необходимости
    var customClassicKey: ByteArray? = null

    private fun getClassicKey(): ByteArray = customClassicKey ?: CLASSIC_AES_KEY

    // ========================================================================
    // 1. КРИПТОГРАФИЯ SUBWAY SURFERS CITY (save.dat + manifest.json)
    // ========================================================================

    fun decryptCitySave(bytes: ByteArray): String {
        val text = bytes.toString(Charsets.UTF_8).trim()
        if (text.startsWith("{") && text.endsWith("}")) return text
        if (bytes.size <= 4) throw Exception("Файл save.dat слишком мал (${bytes.size} байт)")

        val cipherText = bytes.copyOfRange(4, bytes.size)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(CITY_AES_KEY, "AES"))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    fun encryptCitySave(jsonString: String): ByteArray {
        val cleanBytes = jsonString.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(CITY_AES_KEY, "AES"))
        val encrypted = cipher.doFinal(cleanBytes)

        val header = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(cleanBytes.size)
            .array()

        return header + encrypted
    }

    fun sha1Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    fun patchCityManifest(manifestRaw: String, newSaveDatHash: String): String {
        val regex = Regex("""("save\.dat"[\s\S]*?"hash"\s*:\s*")[a-fA-F0-9]{40}(")""")
        return if (regex.containsMatchIn(manifestRaw)) {
            regex.replace(manifestRaw, "$1$newSaveDatHash$2")
        } else {
            Regex("""("hash"\s*:\s*")[a-fA-F0-9]{40}(")""")
                .replaceFirst(manifestRaw, "$1$newSaveDatHash$2")
        }
    }

    // ========================================================================
    // 2. КРИПТОГРАФИЯ SUBWAY SURFERS CLASSIC (AES-CBC / JSON auto-detect)
    // ========================================================================

    /**
     * Формат сохранения Classic:
     * Поддерживает:
     * 1) Открытый JSON (если файл уже расшифрован или хранится в открытом виде)
     * 2) [4 байта длины LE] + [16 байт IV] + [AES-CBC шифротекст]
     * 3) [16 байт IV] + [AES-CBC шифротекст]
     * 4) [AES-CBC шифротекст с нулевым IV]
     */
    data class ClassicDecryptResult(
        val json: String,
        val mode: ClassicFormatMode,
        val extractedIv: ByteArray = ByteArray(16)
    )

    enum class ClassicFormatMode {
        PLAIN_JSON,
        HEADER4_IV16_CBC,
        IV16_CBC,
        ZERO_IV_CBC
    }

    fun decryptClassicFile(bytes: ByteArray): ClassicDecryptResult {
        // 1. Проверка на чистый JSON
        val rawStr = bytes.toString(Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        if (rawStr.startsWith("{") && rawStr.trimEnd().endsWith("}")) {
            return ClassicDecryptResult(rawStr.trim(), ClassicFormatMode.PLAIN_JSON)
        }

        val keySpec = SecretKeySpec(getClassicKey(), "AES")

        // 2. Пробуем режим [4 байта заголовка] + [16 байт IV] + [CipherText]
        if (bytes.size > 20 && (bytes.size - 4) % 16 == 0) {
            try {
                val iv = bytes.copyOfRange(4, 20)
                val cipherText = bytes.copyOfRange(20, bytes.size)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
                val decrypted = String(cipher.doFinal(cipherText), Charsets.UTF_8).trim()
                if (decrypted.startsWith("{")) {
                    return ClassicDecryptResult(decrypted, ClassicFormatMode.HEADER4_IV16_CBC, iv)
                }
            } catch (_: Exception) {}
        }

        // 3. Пробуем режим [16 байт IV] + [CipherText]
        if (bytes.size > 16 && bytes.size % 16 == 0) {
            try {
                val iv = bytes.copyOfRange(0, 16)
                val cipherText = bytes.copyOfRange(16, bytes.size)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
                val decrypted = String(cipher.doFinal(cipherText), Charsets.UTF_8).trim()
                if (decrypted.startsWith("{")) {
                    return ClassicDecryptResult(decrypted, ClassicFormatMode.IV16_CBC, iv)
                }
            } catch (_: Exception) {}
        }

        // 4. Пробуем режим с нулевым IV (Zero IV)
        if (bytes.size >= 16 && bytes.size % 16 == 0) {
            try {
                val zeroIv = ByteArray(16)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(zeroIv))
                val decrypted = String(cipher.doFinal(bytes), Charsets.UTF_8).trim()
                if (decrypted.startsWith("{")) {
                    return ClassicDecryptResult(decrypted, ClassicFormatMode.ZERO_IV_CBC, zeroIv)
                }
            } catch (_: Exception) {}
        }

        throw Exception("Не удалось расшифровать файл Classic (${bytes.size} байт). Проверьте ключ или формат.")
    }

    fun encryptClassicFile(
        jsonString: String,
        mode: ClassicFormatMode = ClassicFormatMode.HEADER4_IV16_CBC,
        iv: ByteArray = ByteArray(16)
    ): ByteArray {
        val cleanBytes = jsonString.toByteArray(Charsets.UTF_8)
        if (mode == ClassicFormatMode.PLAIN_JSON) {
            return cleanBytes
        }

        val keySpec = SecretKeySpec(getClassicKey(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(cleanBytes)

        return when (mode) {
            ClassicFormatMode.HEADER4_IV16_CBC -> {
                val header = ByteBuffer.allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(iv.size + encrypted.size)
                    .array()
                header + iv + encrypted
            }
            ClassicFormatMode.IV16_CBC -> iv + encrypted
            ClassicFormatMode.ZERO_IV_CBC -> encrypted
            ClassicFormatMode.PLAIN_JSON -> cleanBytes
        }
    }
}
