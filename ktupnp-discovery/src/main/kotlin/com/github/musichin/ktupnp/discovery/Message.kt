package com.github.musichin.ktupnp.discovery;

import java.util.*

class Message private constructor(val type: String, val keyValues: Map<String, String>) {
    class Builder() {
        constructor(message: Message) : this() {
            type = message.type
            keyValues.putAll(message.keyValues)
        }

        var type: String? = null
        var keyValues: HashMap<String, String> = HashMap()

        fun startingLine(startingLine: String): Builder {
            this.type = startingLine
            return this
        }

        fun default(type: String): Builder {
            return when (type) {
                SEARCH -> search().man("\"ssdp:discover\"").mx(5)
                NOTIFY -> notify().cacheControl("max-age=1800")
                OK -> ok().cacheControl("max-age=1800")
                else -> throw IllegalArgumentException("Unsupported type $type")
            }
        }

        fun notify() = startingLine(NOTIFY)

        fun ok() = startingLine(OK)

        fun search() = startingLine(SEARCH)

        fun st(st: String) = set(ST, st)

        fun mx(mx: Any) = set(MX, mx)

        fun man(man: String) = set(MAN, man)

        fun cacheControl(man: String) = set(CACHE_CONTROL, man)

        fun build() = Message(type!!, keyValues)

        fun set(key: String, value: Any): Builder {
            keyValues.put(key, value.toString())
            return this
        }
    }

    companion object {
        const val HTTP = "HTTP/1.1"
        const val OK = "${HTTP} 200 OK"
        const val SEARCH = "M-SEARCH * ${HTTP}"
        const val NOTIFY = "NOTIFY * ${HTTP}"
        const val ST = "ST"
        const val MX = "MX"
        const val MAN = "MAN"
        const val USN = "USN"
        const val SERVER = "SERVER"
        const val LOCATION = "LOCATION"
        const val NT = "NT"
        const val CACHE_CONTROL = "CACHE-CONTROL"

        private const val LINE_BREAK = "\r\n"
        private const val COLON = ":"
        private const val COLON_WITH_SPACE = "${COLON} "

        fun fromBytes(bytes: ByteArray): Message {
            return fromBytes(bytes, 0, bytes.size)
        }

        fun fromBytes(bytes: ByteArray, offset: Int, length: Int): Message {
            val builder = Builder()
            val str = String(bytes, offset, length)
            val strArray = str.split(LINE_BREAK)

            if (strArray.size > 0) {
                builder.type = strArray[0]

                var keyValue: String
                var keyValueArray: List<String>
                var key: String
                var value: String
                for (i in 1..strArray.size - 1) {
                    keyValue = strArray[i]
                    if (!keyValue.isEmpty()) {
                        keyValueArray = keyValue.split(COLON, limit = 2)
                        key = keyValueArray[0].trim()
                        value = if (keyValueArray.size > 1) keyValueArray[1].trim() else ""
                        builder.set(key, value)
                    }
                }
            }

            return builder.build()
        }
    }

    fun get(key: String) = keyValues[key]

    val server = get(SERVER)

    val location = get(LOCATION)

    val usn = get(USN)

    val nt = get(NT)

    val mx = get(MX)

    val man = get(MAN)

    val st = get(ST)


    fun builder(): Builder {
        return Builder(this)
    }

    fun bytes(): ByteArray {
        return toString().toByteArray()
    }

    override fun toString(): String {
        val sb = StringBuilder(256)
        sb.append(type)
        sb.append(LINE_BREAK)
        for ((key, value) in keyValues) {
            sb.append(key).append(COLON_WITH_SPACE).append(value).append(LINE_BREAK)
        }
        sb.append(LINE_BREAK)

        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Message) {
            return keyValues == other.keyValues && type == other.type
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var h = 31
        h = h * 17 + type.hashCode()
        h = h * 17 + keyValues.hashCode()
        return h
    }
}