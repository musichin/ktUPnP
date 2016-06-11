package com.github.musichin.ktupnp.discovery

import rx.Observable
import rx.exceptions.Exceptions
import rx.subjects.PublishSubject
import rx.subjects.Subject
import java.io.IOException
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class Ssdp {
    companion object {
        const val PORT = 1900
        const val IPv4 = "239.255.255.250"
        const val IPv6 = "[FF02::C]"

        internal const val BUFFER_SIZE = 1024

        fun search(message: Message): Observable<Message> {
            val subject = PublishSubject.create<Message>()
            val thread = SearchThread(message, subject)

            return subject.doOnSubscribe {
                thread.start()
            }.doOnUnsubscribe({
                thread.cancel()
            }).share()
        }

        fun publish(message: Message): Observable<Message> {
            throw NotImplementedError("Not implemented yet")
        }

        fun notifications(): Observable<Message> {
            throw NotImplementedError("Not implemented yet")

//            val subject = ReplaySubject.create<Message>()
//            val thread = ReceiveThread(subject)
//
//            return subject.doOnSubscribe {
//                thread.start()
//            }.doOnUnsubscribe({
//                thread.cancel()
//            }).share()
        }
    }

    private class ReceiveThread(private val subject: Subject<Message, Message>,
                                private val socket: MulticastSocket = MulticastSocket(PORT)) : Thread() {
        private val canceled = AtomicBoolean(false)
        private val finished = AtomicBoolean(false)

        private fun DatagramPacket.message() = Message.fromBytes(this.data, this.offset, this.length)
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

            val responseDatagram = DatagramPacket(ByteArray(BUFFER_SIZE), BUFFER_SIZE);

            // communicate
            try {
                while (!socket.isClosed && !interrupted()) {
                    socket.receive(responseDatagram)
                    val responseMessage = responseDatagram.message()
                    if (responseMessage.type == Message.NOTIFY) {
                        subject.onNext(responseMessage)
                    }
                }
            } catch(e: SocketTimeoutException) {
                // socket normally timed out
            } catch(e: IOException) {
                if (!canceled.get()) {
                    throw e
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
            val message: Message,
            private val subject: Subject<Message, Message>,
            private val socket: MulticastSocket = MulticastSocket()) : Thread() {

        private val canceled = AtomicBoolean(false)
        private val finished = AtomicBoolean(false)

        private fun DatagramPacket.message() = Message.fromBytes(this.data, this.offset, this.length)
        override fun run() {
            handleException {
                receive()
                subject.onCompleted()
            }
            finished.set(true)
        }

        private fun send() {
            handleException {
                val addressV4 = InetAddress.getByName(IPv4)
                val addressV6 = InetAddress.getByName(IPv6)

                socket.joinGroup(addressV4)
                socket.joinGroup(addressV6)

                val messageV4 = message.builder().set("HOST", "$IPv4:$PORT").build().bytes()
                val messageV6 = message.builder().set("HOST", "$IPv6:$PORT").build().bytes()

                val requestDatagramV4 = DatagramPacket(messageV4, messageV4.size, addressV4, PORT)
                val requestDatagramV6 = DatagramPacket(messageV6, messageV6.size, addressV6, PORT)

                socket.send(requestDatagramV4)
                socket.send(requestDatagramV6)
            }
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
            val responseDatagram = DatagramPacket(ByteArray(BUFFER_SIZE), BUFFER_SIZE);
            // communicate
            try {
                while (!socket.isClosed && !interrupted()) {
                    socket.receive(responseDatagram)
                    val responseMessage = responseDatagram.message()
                    if (responseMessage.type == Message.OK && responseMessage.st == message.st) {
                        subject.onNext(responseMessage)
                    }
                }
            } catch(e: SocketTimeoutException) {
                // socket normally timed out
            } catch(e: IOException) {
                if (!canceled.get()) {
                    throw e
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
