package com.github.musichin.ktupnp.discovery

import rx.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

class Ssdp private constructor() {
    companion object {
        const val PORT = 1900
        const val IPv4 = "239.255.255.250"
        const val IPv6 = "[FF02::C]"

        internal const val BUFFER_SIZE = 1024

        /**
         * Performs search for device with given ST and other default parameter values
         */
        fun search(st: String) = search(SsdpMessage.Builder().default(SsdpMessage.SEARCH_TYPE).st(st).build())

        /**
         * Sends a search request and returns responding messages.
         * This method has no timeout and should be unsubscribed after a timeout.
         */
        fun search(message: SsdpMessage): Observable<SsdpMessage> {
            if (message.type != SsdpMessage.SEARCH_TYPE) {
                throw IllegalArgumentException("Type ${message.type} is not supported for search.")
            }

            return Observable.create({ e ->
                SearchSource(message).let {
                    e.setSubscription(it)
                    it.start(e)
                }
            }, Emitter.BackpressureMode.BUFFER)
        }

        /**
         * Listens for all notification messages.
         */
        fun notifications(): Observable<SsdpMessage> {
            return Observable.create({ e ->
                NotificationSource().let {
                    e.setSubscription(it)
                    it.start(e)
                }
            }, Emitter.BackpressureMode.BUFFER)
        }

        fun publish(replay: (SsdpMessage) -> SsdpMessage?): Completable {
            return Completable.fromEmitter { e ->
                PublishSource(replay).let {
                    e.setSubscription(it)
                    it.start(e)
                }
            }
        }

        fun publish(message: SsdpMessage, vararg messages: SsdpMessage): Completable {
            val allMessages = listOf(message, *messages)
            return publish { req ->
                allMessages.find { req.st == it.st }
            }
        }

        fun notify(message: SsdpMessage, vararg messages: SsdpMessage): Completable {
            return Completable.fromEmitter { e ->
                NotifySource(message, *messages).let {
                    e.setSubscription(it)
                    it.start(e)
                }
            }
        }
    }

    private class NotifySource(vararg val messages: SsdpMessage) : AbstractSource<CompletableEmitter>(MulticastSocket()) {
        override fun onError(emitter: CompletableEmitter, cause: Throwable) = emitter.onError(cause)

        override fun onStart(emitter: CompletableEmitter) {
            messages.forEach { message ->
                send(message, IPv4, PORT)
                send(message, IPv6, PORT)
            }

            unsubscribe()
            emitter.onCompleted()
        }
    }

    private class PublishSource(
            val generate: (SsdpMessage) -> SsdpMessage?
    ) : AbstractSource<CompletableEmitter>(MulticastSocket(PORT)) {
        override fun onError(emitter: CompletableEmitter, cause: Throwable) = emitter.onError(cause)

        override fun onStart(emitter: CompletableEmitter) {
            socket.joinGroup(InetAddress.getByName(IPv4))
            socket.joinGroup(InetAddress.getByName(IPv6))

            receive { message, address, port -> onMessage(message, address, port) }
        }

        fun onMessage(message: SsdpMessage, address: InetAddress, port: Int) {
            if (message.type != SsdpMessage.SEARCH_TYPE) return

            val response = generate(message) ?: return

            if (response.type != SsdpMessage.OK_TYPE) {
                throw IllegalArgumentException("Type ${response.type} is not supported for response.")
            }

            val responseBytes = response.bytes()
            val responseDp = DatagramPacket(responseBytes, responseBytes.size, address, port)
            socket.send(responseDp)
        }

    }

    private class SearchSource(val message: SsdpMessage) : AbstractSource<Emitter<SsdpMessage>>(MulticastSocket()) {
        override fun onError(emitter: Emitter<SsdpMessage>, cause: Throwable) = emitter.onError(cause)

        override fun onStart(emitter: Emitter<SsdpMessage>) {
            send(message, IPv4, PORT)
            send(message, IPv6, PORT)

            receive { message, _, _ ->
                if (message.type == SsdpMessage.OK_TYPE && message.st == message.st) {
                    emitter.onNext(message)
                }
            }
        }
    }

    private class NotificationSource : AbstractSource<Emitter<SsdpMessage>>(MulticastSocket(PORT)) {
        override fun onError(emitter: Emitter<SsdpMessage>, cause: Throwable) = emitter.onError(cause)

        override fun onStart(emitter: Emitter<SsdpMessage>) {
            socket.joinGroup(InetAddress.getByName(IPv4))
            socket.joinGroup(InetAddress.getByName(IPv6))

            receive { message, _, _ ->
                if (message.type == SsdpMessage.NOTIFY_TYPE) {
                    emitter.onNext(message)
                }
            }
        }
    }

    private abstract class AbstractSource<in T>(
            val socket: MulticastSocket
    ) : Subscription {
        override fun isUnsubscribed(): Boolean = socket.isClosed

        override fun unsubscribe() {
            if (!isUnsubscribed) socket.close()
        }

        fun start(emitter: T) {
            try {
                onStart(emitter)
            } catch (cause: IOException) {
                if (!isUnsubscribed) {
                    onError(emitter, cause)
                }
            } finally {
                try {
                    unsubscribe()
                } catch (ignore: Exception) {
                }
            }
        }

        abstract protected fun onStart(emitter: T)

        abstract protected fun onError(emitter: T, cause: Throwable)

        protected fun receive(onMessage: (SsdpMessage, InetAddress, Int) -> Unit) {
            val dp = DatagramPacket(ByteArray(BUFFER_SIZE), BUFFER_SIZE)

            while (!socket.isClosed) {
                socket.receive(dp)
                onMessage(dp.message(), dp.address, dp.port)
            }
        }

        protected fun send(message: SsdpMessage, address: InetAddress, port: Int) {
            val messageBytes = message.bytes()
            val dp = DatagramPacket(messageBytes, messageBytes.size, address, port)
            socket.send(dp)
        }

        protected fun send(message: SsdpMessage, host: String, port: Int) {
            val address = InetAddress.getByName(host)
            send(message.builder().host("$host:$port").build(), address, port)
        }

        private fun DatagramPacket.message() = SsdpMessage.fromBytes(this.data, this.offset, this.length)
    }
}
