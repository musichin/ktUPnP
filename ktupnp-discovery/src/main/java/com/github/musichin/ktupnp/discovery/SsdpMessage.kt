package com.github.musichin.ktupnp.discovery;

import java.util.*

data class SsdpMessage(val type: String, val keyValues: Map<String, String>) {
    class Builder() {
        constructor(message: SsdpMessage) : this() {
            type = message.type
            keyValues.putAll(message.keyValues)
        }

        var type: String? = null
        var keyValues: HashMap<String, String> = HashMap()

        fun type(startingLine: String): Builder {
            this.type = startingLine
            return this
        }

        fun default(type: String): Builder {
            return when (type) {
                SEARCH_TYPE -> search().man("\"ssdp:discover\"").mx(5)
                NOTIFY_TYPE -> notify().cacheControl("max-age=1800")
                OK_TYPE -> ok().cacheControl("max-age=1800")
                else -> throw IllegalArgumentException("Unsupported type $type")
            }
        }

        fun notify() = type(NOTIFY_TYPE)

        fun ok() = type(OK_TYPE)

        fun search() = type(SEARCH_TYPE)

        fun st(st: String?) = set(ST, st)

        fun mx(mx: Any?) = set(MX, mx)

        fun host(host: Any?) = set(HOST, host)

        fun location(location: Any?) = set(LOCATION, location)

        fun man(man: String?) = set(MAN, man)

        fun cacheControl(man: String?) = set(CACHE_CONTROL, man)

        fun build() = SsdpMessage(type!!, keyValues)

        fun set(key: String, value: Any?): Builder {
            if (value != null) {
                keyValues.put(key, value.toString())
            } else {
                keyValues.remove(key)
            }
            return this
        }
    }

    companion object {
        const val HTTP = "HTTP/1.1"
        const val OK_TYPE = "${HTTP} 200 OK"
        const val SEARCH_TYPE = "M-SEARCH * ${HTTP}"
        const val NOTIFY_TYPE = "NOTIFY * ${HTTP}"
        const val ST = "ST"
        const val HOST = "HOST"
        const val MX = "MX"
        const val MAN = "MAN"
        const val USN = "USN"
        const val SERVER = "SERVER"
        const val LOCATION = "LOCATION"
        const val NT = "NT"
        const val CACHE_CONTROL = "CACHE-CONTROL"

        private const val LINE_BREAK = "\r\n"
        private const val COLON = ":"
        private const val COLON_WITH_SPACE = "$COLON "

        fun fromBytes(bytes: ByteArray): SsdpMessage {
            return fromBytes(bytes, 0, bytes.size)
        }

        fun fromBytes(bytes: ByteArray, offset: Int, length: Int): SsdpMessage {
            val builder = Builder()
            val str = String(bytes, offset, length)
            val strArray = str.split(LINE_BREAK)

            if (strArray.isNotEmpty()) {
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
                        value = keyValueArray.getOrElse(1) { "" }.trim()
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

    val host = get(HOST)

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
}