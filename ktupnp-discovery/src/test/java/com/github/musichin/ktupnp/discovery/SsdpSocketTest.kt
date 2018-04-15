package com.github.musichin.ktupnp.discovery

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SsdpSocketTest {
    companion object {
        val ST_TEST = "${SsdpSocketTest::class.java}"
    }

    @Test
    fun testClient() {
        SsdpSocket().use { socket ->
            var joined = false
            try {
                socket.joinGroup(SsdpSocket.IPv4)
                joined = true
            } catch (error: SocketException) {
            }
            try {
                socket.joinGroup(SsdpSocket.IPv6)
                joined = true
            } catch (error: SocketException) {
            }

            assertTrue(joined)

            val latch = CountDownLatch(1)
            Thread({
                while (latch.count > 0) {
                    val (message, address, port) = socket.receive()
                    println("$address:$port $message")
                    if (message.st == ST_TEST) latch.countDown()
                }
            }).start()


            val message = SsdpMessage.Builder()
                    .ok()
                    .st(ST_TEST)
                    .build()

            socket.send(message, SsdpSocket.IPv4, SsdpSocket.PORT)


            assertTrue(latch.await(10_000, TimeUnit.MILLISECONDS))
        }
    }
}
