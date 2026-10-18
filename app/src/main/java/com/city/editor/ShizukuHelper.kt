package com.city.editor

import android.content.pm.PackageManager
import android.util.Base64
import rikka.shizuku.Shizuku

object ShizukuHelper {

    // Пакеты и пути строго через /storage/emulated/0/
    const val CITY_PKG = "com.sybogames.subway.surfers.game"
    const val CITY_SAVE_DIR = "/storage/emulated/0/Android/data/$CITY_PKG/files/save/0"
    const val CITY_SAVE_PATH = "$CITY_SAVE_DIR/save.dat"
    const val CITY_MANIFEST_PATH = "$CITY_SAVE_DIR/manifest.json"

    const val CLASSIC_PKG = "com.kiloo.subwaysurf"
    const val CLASSIC_PROFILE_DIR = "/storage/emulated/0/Android/data/$CLASSIC_PKG/files/profile"

    fun checkPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                Shizuku.requestPermission(1001)
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun exec(cmd: String): String {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        val out = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        process.waitFor()
        if (out.isBlank() && err.isNotBlank()) {
            throw Exception("Shell Error: $err")
        }
        return out.trim()
    }

    fun readFileBytes(remotePath: String): ByteArray {
        val tmpPath = "/data/local/tmp/ce_read_tmp.bin"
        val b64 = exec(
            "cp '$remotePath' $tmpPath && chmod 666 $tmpPath && base64 -w 0 $tmpPath && rm -f $tmpPath"
        ).replace("\\s".toRegex(), "")
        if (b64.isEmpty()) throw Exception("Файл пуст или не найден: $remotePath")
        return Base64.decode(b64, Base64.DEFAULT)
    }

    fun writeFileBytes(remotePath: String, bytes: ByteArray, pkgToStop: String? = null) {
        if (pkgToStop != null) {
            exec("am force-stop $pkgToStop")
        }
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val tmpB64 = "/data/local/tmp/ce_write_tmp.b64"
        val tmpBin = "/data/local/tmp/ce_write_tmp.bin"

        exec("rm -f $tmpB64 $tmpBin")
        b64.chunked(48000).forEach { chunk ->
            exec("printf '%s' '$chunk' >> $tmpB64")
        }
        exec("base64 -d $tmpB64 > $tmpBin && cp -f $tmpBin '$remotePath' && chmod 666 '$remotePath' && rm -f $tmpB64 $tmpBin && sync")
    }

    /**
     * Ищет реальное имя файла в папке профиля Classic (например, achievements.json или achievements_2.json)
     */
    fun resolveClassicFilePath(baseName: String): String {
        val candidates = listOf(
            "$CLASSIC_PROFILE_DIR/$baseName.json",
            "$CLASSIC_PROFILE_DIR/${baseName}_2.json",
            "$CLASSIC_PROFILE_DIR/decrypted_$baseName.json",
            "$CLASSIC_PROFILE_DIR/decrypted_${baseName}_2.json"
        )
        for (path in candidates) {
            val check = try {
                exec("[ -f '$path' ] && echo 'FOUND' || echo 'NO'")
            } catch (_: Exception) { "NO" }
            if (check.contains("FOUND")) return path
        }
        // По умолчанию возвращаем стандартный путь
        return "$CLASSIC_PROFILE_DIR/$baseName.json"
    }

    fun listClassicProfileFiles(): List<String> {
        return try {
            val out = exec("ls -1 '$CLASSIC_PROFILE_DIR'")
            out.lines().map { it.trim() }.filter { it.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchGame(pkg: String) {
        try {
            exec("monkey -p $pkg -c android.intent.category.LAUNCHER 1")
        } catch (_: Exception) {}
    }
}
