package com.github.musichin.ktupnp.discovery

import org.junit.Test

import rx.observers.TestSubscriber
import java.util.concurrent.TimeUnit

import org.fest.assertions.api.Assertions.assertThat
import org.junit.Ignore

class SsdpTest {
    companion object {
        val ST_ROOTDEVICE = "upnp:rootdevice"
    }

    @Test
    fun listen() {
        val subscriber = TestSubscriber<SsdpMessage>();
        Ssdp.notifications().subscribe(subscriber)

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)
//        assertThat(subscriber.onNextEvents).isNotEmpty
    }

    @Test
    fun search() {
        val subscriber = TestSubscriber<SsdpMessage>();
        Ssdp.search(ST_ROOTDEVICE).subscribe(subscriber)

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)
        subscriber.assertNoErrors()
//        assertThat(subscriber.onNextEvents).isNotEmpty
    }
}