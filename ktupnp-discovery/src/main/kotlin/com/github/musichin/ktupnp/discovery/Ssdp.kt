package com.github.musichin.ktupnp.discovery

import rx.Observable
import rx.exceptions.Exceptions
import rx.subjects.PublishSubject
import rx.subjects.Subject
import java.io.IOException
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class Ssdp private constructor() {
    companion object {
        const val PORT = 1900
        const val IPv4 = "239.255.255.250"
        const val IPv6 = "[FF02::C]"

        internal const val BUFFER_SIZE = 1024

        /**
         * Sends a search request and returns responding messages.
         * This method has no timeout and should be unsubscribed after a timeout.
         */
        fun search(message: SsdpMessage): Observable<SsdpMessage> {
            if (message.type != SsdpMessage.SEARCH_TYPE) {
                throw IllegalArgumentException("Type ${message.type} is not supported for search.")
            }

            val subject = PublishSubject.create<SsdpMessage>()
            val thread = SearchThread(message, subject)

            return subject.doOnSubscribe {
                thread.start()
            }.doOnUnsubscribe({
                thread.cancel()
            }).share()
        }

        /**
         * Listens for all notification messages.
         */
        fun notifications(): Observable<SsdpMessage> {
            val subject = PublishSubject.create<SsdpMessage>()
            val thread = ReceiveThread(subject)

            return subject.doOnSubscribe {
                thread.start()
            }.doOnUnsubscribe({
                thread.cancel()
            }).share()
        }
    }

    private open class SocketThread(val socket: MulticastSocket = MulticastSocket()) : Thread() {
        protected val canceled = AtomicBoolean(false)
        protected val finished = AtomicBoolean(false)

        protected fun DatagramPacket.message() = SsdpMessage.fromBytes(this.data, this.offset, this.length)


        protected fun receive(socket: DatagramSocket, action: (SsdpMessage) -> Unit) {
            val dp = DatagramPacket(ByteArray(BUFFER_SIZE), BUFFER_SIZE);

            try {
                while (!socket.isClosed && !interrupted()) {
                    socket.receive(dp)
                    action(dp.message())
                }
            } catch(e: SocketTimeoutException) {
                // socket normally timed out
            } catch(e: IOException) {
                if (!canceled.get()) {
                    throw e
                }
            }
        }
    }

    private class ReceiveThread(
            private val subject: Subject<SsdpMessage, SsdpMessage>
    ) : SocketThread(MulticastSocket(PORT)) {
        override fun run() {
            try {
                sendAndReceive()
                subject.onCompleted()
            } catch(e: IOException) {
                subject.onError(e)
            } catch(e: IllegalArgumentException) {
                subject.onError(e)
            }
            finished.set(true)
        }

        @Throws(IOException::class)
        private fun sendAndReceive() {
            socket.joinGroup(InetAddress.getByName(IPv4))
            socket.joinGroup(InetAddress.getByName(IPv6))

            // communicate
            receive(socket) {
                if (it.type == SsdpMessage.NOTIFY_TYPE) {
                    subject.onNext(it)
                }
            }
        }

        fun cancel() {
            canceled.set(true)
            interrupt()
            socket.close()
        }

        override fun start() {
            if (!isAlive && !isInterrupted && !canceled.get() && !finished.get()) {
                super.start()
            }
        }
    }

    private class SearchThread(
            val message: SsdpMessage,
            private val subject: Subject<SsdpMessage, SsdpMessage>) : SocketThread() {

        override fun run() {
            handleException {
                receive()
                subject.onCompleted()
            }
            finished.set(true)
        }

        private fun send() {
            handleException {
                send(IPv4)
                send(IPv6)
            }
        }

        private fun send(host: String) {
            val address = InetAddress.getByName(host)
            val message = message.builder().set("HOST", "$host:$PORT").build().bytes()
            val dp = DatagramPacket(message, message.size, address, PORT)

            socket.send(dp)
        }

        private fun handleException(action: () -> Unit) {
            try {
                action()
            } catch(e: Throwable) {
                Exceptions.throwIfFatal(e)
                subject.onError(e)
            }
        }

        private fun receive() {
            receive(socket) {
                if (it.type == SsdpMessage.OK_TYPE && it.st == message.st) {
                    subject.onNext(it)
                }
            }
        }

        fun cancel() {
            canceled.set(true)
            interrupt()
            socket.close()
        }

        override fun start() {
            if (!isAlive && !isInterrupted && !canceled.get() && !finished.get()) {
                super.start()
                send()
            }
        }
    }
}
