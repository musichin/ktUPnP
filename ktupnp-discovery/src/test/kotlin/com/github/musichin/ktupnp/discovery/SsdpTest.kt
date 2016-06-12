package com.github.musichin.ktupnp.discovery

import org.junit.Test

import rx.observers.TestSubscriber
import java.util.concurrent.TimeUnit

import org.fest.assertions.api.Assertions.assertThat
import org.junit.Ignore

class SsdpTest {
    companion object {
        val ST_ROOTDEVICE = "upnp:rootdevice"
        val MESSAGE = SsdpMessage.Builder().default(SsdpMessage.SEARCH_TYPE).st(ST_ROOTDEVICE).build()
    }

    @Test
    fun listen() {
        val subscriber = TestSubscriber<SsdpMessage>();
        Ssdp.notifications().subscribe(subscriber)

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)
//        assertThat(subscriber.onNextEvents).isNotEmpty
    }

    @Test
    @Ignore
    fun publish() {
        val subscriber = TestSubscriber<SsdpMessage>();
        Ssdp.publish(MESSAGE).subscribe(subscriber)
    }

    @Test
    fun search() {
        val subscriber = TestSubscriber<SsdpMessage>();
        Ssdp.search(MESSAGE).subscribe(subscriber)

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)
        subscriber.assertNoErrors()
//        assertThat(subscriber.onNextEvents).isNotEmpty
    }
}