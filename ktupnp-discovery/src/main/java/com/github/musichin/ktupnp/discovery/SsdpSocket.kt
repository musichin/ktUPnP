package com.github.musichin.ktupnp.discovery

import java.io.Closeable
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

class SsdpSocket : Closeable {

    companion object {
        const val PORT = 1900
        val IPv4: InetAddress =
                InetAddress.getByAddress(byteArrayOf(-17, -1, -1, -6)) // "239.255.255.250"
        val IPv6: InetAddress =
                InetAddress.getByAddress(byteArrayOf(-1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12)) // "[FF02::C]"

        private const val DP_BUFFER_SIZE = 1024

        private fun DatagramPacket.message() = SsdpMessage.fromBytes(data, offset, length)
    }

    private val socket = MulticastSocket(PORT)
    private val dp = DatagramPacket(ByteArray(DP_BUFFER_SIZE), DP_BUFFER_SIZE)

    fun joinGroup(mcastaddr: InetAddress) {
        socket.joinGroup(mcastaddr)
    }

    fun receive(): SsdpPacket {
        socket.receive(dp)
        return SsdpPacket(dp.message(), dp.address, dp.port)
    }

    fun send(message: SsdpMessage, address: InetAddress, port: Int) {
        val messageBytes = message.bytes()
        val dp = DatagramPacket(messageBytes, messageBytes.size, address, port)
        socket.send(dp)
    }

    fun send(message: SsdpMessage, host: String, port: Int) {
        val address = InetAddress.getByName(host)
        send(message, address, port)
    }

    fun send(packet: SsdpPacket) {
        val (message, address, port) = packet
        val messageBytes = message.bytes()
        val dp = DatagramPacket(messageBytes, messageBytes.size, address, port)
        socket.send(dp)
    }

    override fun close() {
        socket.close()
    }

    val isClosed: Boolean
        get() {
            return socket.isClosed
        }
}
