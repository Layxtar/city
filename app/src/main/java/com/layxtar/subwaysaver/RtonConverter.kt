package com.layxtar.subwaysaver

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object RtonConverter {

    private const val MAGIC_RTON = "RTON"
    private const val MAGIC_DONE = "DONE"

    fun rtonToJson(bytes: ByteArray): JSONObject {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magicBytes = ByteArray(4)
        buf.get(magicBytes)
        val magic = String(magicBytes, Charsets.US_ASCII)
        require(magic == MAGIC_RTON) { "Неверный заголовок RTON: $magic" }

        val version = buf.int
        val r90List = ArrayList<String>()
        val r92List = ArrayList<String>()

        val rootObj = readObject(buf, r90List, r92List)
        rootObj.put("version", version)
        return rootObj
    }

    private fun readVarInt(buf: ByteBuffer): Long {
        var result = 0L
        var shift = 0
        while (buf.hasRemaining()) {
            val b = buf.get().toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
        }
        return result
    }

    private fun readZigZag(buf: ByteBuffer): Long {
        val raw = readVarInt(buf)
        return (raw ushr 1) xor -(raw and 1L)
    }

    private fun readAsciiString(buf: ByteBuffer): String {
        val len = readVarInt(buf).toInt()
        val arr = ByteArray(len)
        buf.get(arr)
        return String(arr, Charsets.UTF_8)
    }

    private fun readUtf8String(buf: ByteBuffer): String {
        val charCount = readVarInt(buf).toInt()
        val byteLen = readVarInt(buf).toInt()
        val arr = ByteArray(byteLen)
        buf.get(arr)
        return String(arr, Charsets.UTF_8)
    }

    private fun readRtid(buf: ByteBuffer, r90List: ArrayList<String>, r92List: ArrayList<String>): String {
        val subType = buf.get().toInt() and 0xFF
        return when (subType) {
            0x00 -> "RTID(0)"
            0x01 -> {
                val v1 = readVarInt(buf)
                val v2 = readVarInt(buf)
                val hex = buf.int
                val sheet = readValue(buf, r90List, r92List).toString()
                "RTID($v1.$v2.${String.format("%08x", hex)}@$sheet)"
            }
            0x02 -> {
                val sheet = readValue(buf, r90List, r92List).toString()
                val v1 = readVarInt(buf)
                val v2 = readVarInt(buf)
                val hex = buf.int
                "RTID($v1.$v2.${String.format("%08x", hex)}@$sheet)"
            }
            0x03 -> {
                val sheet = readValue(buf, r90List, r92List).toString()
                val alias = readValue(buf, r90List, r92List).toString()
                "RTID($alias@$sheet)"
            }
            else -> "RTID(0)"
        }
    }

    private fun readObject(buf: ByteBuffer, r90List: ArrayList<String>, r92List: ArrayList<String>): JSONObject {
        val obj = JSONObject()
        while (buf.hasRemaining()) {
            val posBefore = buf.position()
            if (buf.remaining() >= 4) {
                val peek = ByteArray(4)
                buf.get(peek)
                if (String(peek, Charsets.US_ASCII) == MAGIC_DONE) {
                    break
                }
                buf.position(posBefore)
            }

            val tag = buf.get().toInt() and 0xFF
            if (tag == 0xFF) break
            buf.position(posBefore)

            val key = readValue(buf, r90List, r92List)?.toString() ?: break
            val value = readValue(buf, r90List, r92List)
            obj.put(key, value ?: JSONObject.NULL)
        }
        return obj
    }

    private fun readArray(buf: ByteBuffer, r90List: ArrayList<String>, r92List: ArrayList<String>): JSONArray {
        val arr = JSONArray()
        val arrTag = buf.get().toInt() and 0xFF
        if (arrTag != 0xFD) return arr
        val count = readVarInt(buf).toInt()
        for (i in 0 until count) {
            val v = readValue(buf, r90List, r92List)
            arr.put(v ?: JSONObject.NULL)
        }
        val endTag = buf.get().toInt() and 0xFF
        require(endTag == 0xFE) { "Ожидался конец массива 0xFE, получен: $endTag" }
        return arr
    }

    private fun readValue(buf: ByteBuffer, r90List: ArrayList<String>, r92List: ArrayList<String>): Any? {
        val op = buf.get().toInt() and 0xFF
        return when (op) {
            0x00 -> false
            0x01 -> true
            0x02 -> JSONObject.NULL
            0x08 -> buf.get().toInt()
            0x09 -> 0
            0x0A -> buf.get().toInt() and 0xFF
            0x0B -> 0
            0x10 -> buf.short.toInt()
            0x11 -> 0
            0x12 -> buf.short.toInt() and 0xFFFF
            0x13 -> 0
            0x20 -> buf.int
            0x21 -> 0
            0x22 -> buf.float.toDouble()
            0x23 -> 0.0
            0x24 -> readVarInt(buf)
            0x25 -> readZigZag(buf)
            0x26 -> buf.int.toLong() and 0xFFFFFFFFL
            0x27 -> 0L
            0x28 -> readVarInt(buf)
            0x40 -> buf.long
            0x41 -> 0L
            0x42 -> buf.double
            0x43 -> 0.0
            0x44 -> readVarInt(buf)
            0x45 -> readZigZag(buf)
            0x46 -> buf.long
            0x47 -> 0L
            0x48 -> readVarInt(buf)
            0x81 -> readAsciiString(buf)
            0x82 -> readUtf8String(buf)
            0x83 -> readRtid(buf, r90List, r92List)
            0x84 -> "RTID(0)"
            0x85 -> readObject(buf, r90List, r92List)
            0x86 -> readArray(buf, r90List, r92List)
            0x90 -> {
                val s = readAsciiString(buf)
                r90List.add(s)
                s
            }
            0x91 -> {
                val idx = readVarInt(buf).toInt()
                r90List.getOrElse(idx) { "" }
            }
            0x92 -> {
                val s = readUtf8String(buf)
                r92List.add(s)
                s
            }
            0x93 -> {
                val idx = readVarInt(buf).toInt()
                r92List.getOrElse(idx) { "" }
            }
            else -> throw IllegalArgumentException("Неизвестный RTON опкод: 0x${op.toString(16)}")
        }
    }

    // ==================== ЗАПИСЬ JSON -> RTON ====================

    fun jsonToRton(json: JSONObject): ByteArray {
        val out = ByteArrayOutputStream(65536)
        out.write(MAGIC_RTON.toByteArray(Charsets.US_ASCII))

        val ver = json.optInt("version", 1)
        val verBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ver)
        out.write(verBuf.array())

        val r90Map = HashMap<String, Int>()
        val r92Map = HashMap<String, Int>()
        writeObjectContents(out, json, r90Map, r92Map, isRoot = true)

        out.write(MAGIC_DONE.toByteArray(Charsets.US_ASCII))
        return out.toByteArray()
    }

    private fun writeVarInt(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while ((v and -128L) != 0L) {
            out.write(((v.toInt() and 0x7F) or 0x80))
            v = v ushr 7
        }
        out.write(v.toInt() and 0x7F)
    }

    // Точная запись ASCII (0x90/0x91) и многобайтовых UTF-8 строк (0x92/0x93 и 0x82),
    // где для не-ASCII строк строго указывается и число символов, и число байт!
    private fun writeString(
        out: ByteArrayOutputStream,
        str: String,
        r90Map: HashMap<String, Int>,
        r92Map: HashMap<String, Int>
    ) {
        if (str.startsWith("RTID(") && str.endsWith(")")) {
            val inner = str.substring(5, str.length - 1)
            if (inner == "0") {
                out.write(0x84)
                return
            }
            if (inner.contains("@")) {
                val parts = inner.split("@", limit = 2)
                val left = parts[0]
                val sheet = parts[1]
                if (left.count { it == '.' } < 2) {
                    out.write(0x83)
                    out.write(0x03)
                    writePlainString(out, sheet, r90Map, r92Map)
                    writePlainString(out, left, r90Map, r92Map)
                    return
                }
            }
        }
        writePlainString(out, str, r90Map, r92Map)
    }

    private fun writePlainString(
        out: ByteArrayOutputStream,
        str: String,
        r90Map: HashMap<String, Int>,
        r92Map: HashMap<String, Int>
    ) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        val isPureAscii = bytes.size == str.length

        if (isPureAscii) {
            val existingIdx = r90Map[str]
            if (existingIdx != null) {
                out.write(0x91)
                writeVarInt(out, existingIdx.toLong())
            } else {
                r90Map[str] = r90Map.size
                out.write(0x90)
                writeVarInt(out, bytes.size.toLong())
                out.write(bytes)
            }
        } else {
            // Для строк с кириллицей и спецсимволами (где bytes.size != str.length)
            // движок PvZ 2 ТРЕБУЕТ опкод 0x82 (или 0x92/0x93), где сначала идёт длина в символах, а затем длина в байтах!
            val existingIdx = r92Map[str]
            if (existingIdx != null) {
                out.write(0x93)
                writeVarInt(out, existingIdx.toLong())
            } else {
                r92Map[str] = r92Map.size
                out.write(0x92)
                writeVarInt(out, str.length.toLong())
                writeVarInt(out, bytes.size.toLong())
                out.write(bytes)
            }
        }
    }

    private fun writeObjectContents(
        out: ByteArrayOutputStream,
        obj: JSONObject,
        r90Map: HashMap<String, Int>,
        r92Map: HashMap<String, Int>,
        isRoot: Boolean
    ) {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (isRoot && k == "version") continue
            val v = obj.opt(k)
            writeString(out, k, r90Map, r92Map)
            writeValue(out, v, r90Map, r92Map)
        }
        out.write(0xFF)
    }

    private fun writeValue(
        out: ByteArrayOutputStream,
        v: Any?,
        r90Map: HashMap<String, Int>,
        r92Map: HashMap<String, Int>
    ) {
        when (v) {
            null, JSONObject.NULL -> out.write(0x02)
            is Boolean -> out.write(if (v) 0x01 else 0x00)
            is Int -> {
                if (v == 0) {
                    out.write(0x21)
                } else {
                    out.write(0x20)
                    val b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v)
                    out.write(b.array())
                }
            }
            is Long -> {
                if (v == 0L) {
                    out.write(0x41)
                } else if (v in Int.MIN_VALUE..Int.MAX_VALUE) {
                    out.write(0x20)
                    val b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v.toInt())
                    out.write(b.array())
                } else {
                    out.write(0x40)
                    val b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v)
                    out.write(b.array())
                }
            }
            is Double -> {
                if (v == 0.0) {
                    out.write(0x23)
                } else {
                    out.write(0x22)
                    val b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v.toFloat())
                    out.write(b.array())
                }
            }
            is Float -> {
                if (v == 0f) {
                    out.write(0x23)
                } else {
                    out.write(0x22)
                    val b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v)
                    out.write(b.array())
                }
            }
            is String -> writeString(out, v, r90Map, r92Map)
            is JSONObject -> {
                out.write(0x85)
                writeObjectContents(out, v, r90Map, r92Map, isRoot = false)
            }
            is JSONArray -> {
                out.write(0x86)
                out.write(0xFD)
                writeVarInt(out, v.length().toLong())
                for (i in 0 until v.length()) {
                    writeValue(out, v.opt(i), r90Map, r92Map)
                }
                out.write(0xFE)
            }
            else -> writeString(out, v.toString(), r90Map, r92Map)
        }
    }
}
