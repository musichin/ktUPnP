package com.github.musichin.ktupnp.discovery

import org.junit.Test

import rx.observers.TestSubscriber
import java.util.concurrent.TimeUnit

import org.fest.assertions.api.Assertions.assertThat
import org.junit.Ignore

class SsdpTest {
    companion object {
        val ST_ROOTDEVICE = "upnp:rootdevice"
        val MESSAGE = Message.Builder().default(Message.SEARCH).st(ST_ROOTDEVICE).build()
    }

    @Test
    @Ignore
    fun listen() {
        val subscriber = TestSubscriber<Message>();
        Ssdp.notifications().subscribe(subscriber)

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)
        assertThat(subscriber.onNextEvents).isNotEmpty
    }

    @Test
    @Ignore
    fun publish() {
        val subscriber = TestSubscriber<Message>();
        Ssdp.publish(MESSAGE).subscribe(subscriber)
    }

    @Test
    fun search() {
        val subscriber = TestSubscriber<Message>();
        Ssdp.search(MESSAGE).subscribe(subscriber)

        subscriber.awaitTerminalEvent(5, TimeUnit.SECONDS)
        subscriber.assertNoErrors()
//        assertThat(subscriber.onNextEvents).isNotEmpty
    }
}