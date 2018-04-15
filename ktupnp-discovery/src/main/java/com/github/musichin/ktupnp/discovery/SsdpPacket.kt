package com.github.musichin.ktupnp.discovery

import java.net.InetAddress

data class SsdpPacket(
        val message: SsdpMessage,
        val address: InetAddress,
        val port: Int
)