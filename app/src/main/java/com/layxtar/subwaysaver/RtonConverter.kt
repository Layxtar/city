package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object RtonConverter {

    private val MAGIC_RTON = byteArrayOf(82, 84, 79, 78) // "RTON"
    private val MAGIC_DONE = byteArrayOf(68, 79, 78, 69) // "DONE"

    // ==================== RTON -> JSON ====================

    fun rtonToJson(bytes: ByteArray): JSONObject {
        require(bytes.size >= 12) { "RTON слишком мал (${bytes.size} байт)" }
        for (i in 0..3) {
            require(bytes[i] == MAGIC_RTON[i]) { "Отсутствует заголовок RTON" }
        }
        val reader = RtonReader(bytes)
        reader.pos = 4
        val version = reader.u32()
        require(version == 1L) { "Неподдерживаемая версия RTON: $version" }
        return reader.readRootObject()
    }

    private class RtonReader(private val bytes: ByteArray) {
        var pos = 0
        private val cacheStr = ArrayList<String>()
        private val cacheUtf = ArrayList<String>()

        fun peek(): Int = bytes[pos].toInt() and 0xFF
        fun u8(): Int = bytes[pos++].toInt() and 0xFF
        fun i8(): Int = bytes[pos++].toInt()

        fun i16(): Int {
            val v = ByteBuffer.wrap(bytes, pos, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            pos += 2
            return v
        }

        fun u16(): Int = i16() and 0xFFFF

        fun i32(): Int {
            val v = ByteBuffer.wrap(bytes, pos, 4).order(ByteOrder.LITTLE_ENDIAN).int
            pos += 4
            return v
        }

        fun u32(): Long = i32().toLong() and 0xFFFFFFFFL

        fun i64(): Long {
            val v = ByteBuffer.wrap(bytes, pos, 8).order(ByteOrder.LITTLE_ENDIAN).long
            pos += 8
            return v
        }

        fun f32(): Double {
            val v = ByteBuffer.wrap(bytes, pos, 4).order(ByteOrder.LITTLE_ENDIAN).float
            pos += 4
            return v.toString().toDouble()
        }

        fun f64(): Double {
            val v = ByteBuffer.wrap(bytes, pos, 8).order(ByteOrder.LITTLE_ENDIAN).double
            pos += 8
            return v
        }

        fun bytesOf(len: Int): ByteArray {
            val sub = bytes.copyOfRange(pos, pos + len)
            pos += len
            return sub
        }

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val b = u8()
                result = result or ((b and 0x7F).toLong() shl shift)
                if ((b and 0x80) == 0) break
                shift += 7
            }
            return result
        }

        fun zigzag(): Long {
            val v = varint()
            return (v ushr 1) xor -(v and 1L)
        }

        fun readStr(): String {
            val len = varint().toInt()
            val raw = bytesOf(len)
            return String(raw, Charsets.UTF_8)
        }

        fun readUtf(): String {
            varint() // длина в символах
            val byteLen = varint().toInt()
            return String(bytesOf(byteLen), Charsets.UTF_8)
        }

        fun readRtid(): String {
            return when (val subType = u8()) {
                0 -> "RTID(0)"
                1 -> readRtidUid("")
                2 -> readRtidUid(readUtf())
                3 -> {
                    val sheet = readUtf()
                    val alias = readUtf()
                    "RTID($alias@$sheet)"
                }
                else -> throw IllegalArgumentException("Неизвестный RTID тип: 0x${subType.toString(16)}")
            }
        }

        private fun readRtidUid(sheet: String): String {
            val a = varint()
            val b = varint()
            val c = u32().toString(16).padStart(8, '0')
            return "RTID($b.$a.$c@$sheet)"
        }

        fun readBinary(): String {
            u8()
            val s = readStr()
            val n = varint()
            return "\$BINARY(\"$s\", $n)"
        }

        fun readRootObject(): JSONObject {
            pos = 8
            return readObject()
        }

        fun readObject(): JSONObject {
            val obj = JSONObject()
            if (peek() == 0xFF) {
                pos++
                return obj
            }
            while (peek() != 0xFF) {
                val key = readValue().toString()
                val value = readValue()
                obj.put(key, value)
            }
            pos++ // пропускаем 0xFF
            return obj
        }

        fun readArray(): JSONArray {
            val startTag = u8()
            require(startTag == 0xFD) { "Ожидался маркер начала массива 0xFD" }
            val count = varint().toInt()
            val arr = JSONArray()
            for (i in 0 until count) {
                arr.put(readValue())
            }
            val endTag = u8()
            require(endTag == 0xFE) { "Ожидался маркер конца массива 0xFE" }
            return arr
        }

        fun readValue(): Any {
            return when (val tag = u8()) {
                0x00 -> false
                0x01 -> true
                0x02 -> JSONObject.NULL
                0x08 -> i8()
                0x09, 0x0B, 0x11, 0x13, 0x21, 0x27, 0x41, 0x47 -> 0
                0x0A -> u8()
                0x10 -> i16()
                0x12 -> u16()
                0x20 -> i32()
                0x22 -> f32()
                0x23, 0x43 -> 0.0
                0x24, 0x28, 0x44, 0x48 -> varint()
                0x25, 0x29, 0x45, 0x49 -> zigzag()
                0x26 -> u32()
                0x40, 0x46 -> i64()
                0x42 -> f64()
                0x81 -> readStr()
                0x82 -> readUtf()
                0x83 -> readRtid()
                0x84 -> "RTID(0)"
                0x85 -> readObject()
                0x86 -> readArray()
                0x87 -> readBinary()
                0x90 -> {
                    val s = readStr()
                    cacheStr.add(s)
                    s
                }
                0x91 -> cacheStr[varint().toInt()]
                0x92 -> {
                    val s = readUtf()
                    cacheUtf.add(s)
                    s
                }
                0x93 -> cacheUtf[varint().toInt()]
                0xBC -> u8() != 0
                else -> throw IllegalArgumentException("Неизвестный тег RTON: 0x${tag.toString(16)} на позиции ${pos - 1}")
            }
        }
    }

    // ==================== JSON -> RTON ====================

    fun jsonToRton(root: JSONObject): ByteArray {
        val writer = RtonWriter()
        return writer.writeRoot(root)
    }

    private class RtonWriter {
        private val out = ByteArrayOutputStream(8192)
        private val cacheStr = HashMap<String, Int>()
        private val cacheUtf = HashMap<String, Int>()

        private val rtidUidRegex = Regex("^RTID\\((\\d+)\\.(\\d+)\\.([0-9a-fA-F]{8})@(.*)\\)$")
        private val rtidAliasRegex = Regex("^RTID\\(([^@]*)@(.*)\\)$")
        private val binaryRegex = Regex("^\\\$BINARY\\(\"(.*)\", (\\d+)\\)$")

        fun u8(vararg values: Int) {
            for (v in values) out.write(v and 0xFF)
        }

        fun i32(v: Int) {
            val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
            out.write(buf)
        }

        fun u32(v: Long) = i32((v and 0xFFFFFFFFL).toInt())

        fun i64(v: Long) {
            val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v).array()
            out.write(buf)
        }

        fun f32(v: Float) {
            val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v).array()
            out.write(buf)
        }

        fun f64(v: Double) {
            val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(v).array()
            out.write(buf)
        }

        fun varint(value: Long) {
            var v = value
            do {
                var b = (v and 0x7FL).toInt()
                v = v ushr 7
                if (v != 0L) b = b or 0x80
                out.write(b)
            } while (v != 0L)
        }

        fun zigzag(value: Long) {
            val z = if (value >= 0) value * 2 else (-value * 2) - 1
            varint(z)
        }

        fun writeRoot(root: JSONObject): ByteArray {
            out.write(MAGIC_RTON)
            i32(1) // версия 1
            writeEntries(root)
            out.write(MAGIC_DONE)
            return out.toByteArray()
        }

        private fun writeEntries(obj: JSONObject) {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                writeStringCached(k)
                writeValue(obj.opt(k))
            }
            u8(0xFF)
        }

        private fun writeArray(arr: JSONArray) {
            u8(0x86, 0xFD)
            varint(arr.length().toLong())
            for (i in 0 until arr.length()) {
                writeValue(arr.opt(i))
            }
            u8(0xFE)
        }

        private fun writeUtfRaw(s: String) {
            val utfBytes = s.toByteArray(Charsets.UTF_8)
            val charCount = s.codePointCount(0, s.length).toLong()
            varint(charCount)
            varint(utfBytes.size.toLong())
            out.write(utfBytes)
        }

        private fun writeStringCached(s: String) {
            val utfBytes = s.toByteArray(Charsets.UTF_8)
            val isAscii = utfBytes.size == s.length
            if (isAscii) {
                val idx = cacheStr[s]
                if (idx != null) {
                    u8(0x91)
                    varint(idx.toLong())
                } else {
                    cacheStr[s] = cacheStr.size
                    u8(0x90)
                    varint(utfBytes.size.toLong())
                    out.write(utfBytes)
                }
            } else {
                val idx = cacheUtf[s]
                if (idx != null) {
                    u8(0x93)
                    varint(idx.toLong())
                } else {
                    cacheUtf[s] = cacheUtf.size
                    u8(0x92)
                    writeUtfRaw(s)
                }
            }
        }

        private fun writeStringValue(s: String) {
            if (s == "RTID(0)") {
                u8(0x84)
                return
            }
            if (s == "RTID()") {
                u8(0x83, 0x00)
                return
            }
            rtidUidRegex.matchEntire(s)?.let { m ->
                val b = m.groupValues[1].toLong()
                val a = m.groupValues[2].toLong()
                val hex = m.groupValues[3].toLong(16)
                val sheet = m.groupValues[4]
                if (sheet.isEmpty()) {
                    u8(0x83, 0x01)
                } else {
                    u8(0x83, 0x02)
                    writeUtfRaw(sheet)
                }
                varint(a)
                varint(b)
                u32(hex)
                return
            }
            rtidAliasRegex.matchEntire(s)?.let { m ->
                val alias = m.groupValues[1]
                val sheet = m.groupValues[2]
                u8(0x83, 0x03)
                writeUtfRaw(sheet)
                writeUtfRaw(alias)
                return
            }
            binaryRegex.matchEntire(s)?.let { m ->
                val binBytes = m.groupValues[1].toByteArray(Charsets.UTF_8)
                val num = m.groupValues[2].toLong()
                u8(0x87, 0x00)
                varint(binBytes.size.toLong())
                out.write(binBytes)
                varint(num)
                return
            }
            writeStringCached(s)
        }

        private fun writeIntGame(v: Long) {
            val limit21 = 1L shl 21
            val limit49 = 1L shl 49
            when {
                v == 0L -> u8(0x21)
                v in 1 until limit21 -> {
                    u8(0x24)
                    varint(v)
                }
                v < 0 && v >= -(limit21 / 2) -> {
                    u8(0x25)
                    zigzag(v)
                }
                v in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() -> {
                    u8(0x20)
                    i32(v.toInt())
                }
                v in (Int.MAX_VALUE.toLong() + 1)..0xFFFFFFFFL -> {
                    u8(0x26)
                    u32(v)
                }
                v in 1 until limit49 -> {
                    u8(0x44)
                    varint(v)
                }
                v < 0 && v >= -(limit49 / 2) -> {
                    u8(0x45)
                    zigzag(v)
                }
                else -> {
                    u8(0x40)
                    i64(v)
                }
            }
        }

        private fun writeFloat(d: Double) {
            if (d == 0.0) {
                u8(0x23)
                return
            }
            val f = d.toFloat()
            if (abs(f.toDouble() - d) < 1e-6) {
                u8(0x22)
                f32(f)
            } else {
                u8(0x42)
                f64(d)
            }
        }

        fun writeValue(v: Any?) {
            when (v) {
                null, JSONObject.NULL -> u8(0x84)
                is Boolean -> u8(if (v) 0x01 else 0x00)
                is Int -> writeIntGame(v.toLong())
                is Long -> writeIntGame(v)
                is Float -> writeFloat(v.toDouble())
                is Double -> writeFloat(v)
                is Number -> writeIntGame(v.toLong())
                is String -> writeStringValue(v)
                is JSONArray -> writeArray(v)
                is JSONObject -> {
                    u8(0x85)
                    writeEntries(v)
                }
                else -> writeStringValue(v.toString())
            }
        }
    }
}
