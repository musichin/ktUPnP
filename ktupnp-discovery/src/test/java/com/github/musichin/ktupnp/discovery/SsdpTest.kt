package com.github.musichin.ktupnp.discovery

import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpTest {
    companion object {
        val ST_TEST = "${SsdpTest::class.java}"
    }

    @Test
    fun testNotifyAndNotifications() {
        val message = SsdpMessage.Builder()
                .type(SsdpMessage.NOTIFY_TYPE)
                .st(ST_TEST)
                .build()

        val notificationsSubscriber = TestSubscriber<SsdpMessage>()
        Ssdp.notifications()
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .filter { it.st == message.st }
                .subscribe(notificationsSubscriber)

        val notifySubscriber = TestSubscriber<Unit>()
        Ssdp.notify(message)
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .toFlowable<Unit>()
                .subscribe(notifySubscriber)

        Thread.sleep(1_000)
        notifySubscriber.dispose()
        notificationsSubscriber.dispose()

        notifySubscriber.assertNoErrors()
        notificationsSubscriber.assertNoErrors()

        notifySubscriber.assertComplete()
        notificationsSubscriber.assertNotComplete()

        val values = notificationsSubscriber.values()
        assertTrue(values.isNotEmpty())
        values.forEach {
            assertEquals(it.st, message.st)
        }
    }

    @Test
    fun testSearchAndPublish() {
        val message = SsdpMessage.Builder()
                .ok()
                .st(ST_TEST)
                .build()

        val publishSubscriber = TestSubscriber<Unit>()
        Ssdp.publish(message)
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .toFlowable<Unit>()
                .subscribe(publishSubscriber)

        val searchSubscriber = TestSubscriber<SsdpMessage>()
        Ssdp.search(ST_TEST)
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(searchSubscriber)

        Thread.sleep(1_000)
        publishSubscriber.dispose()
        searchSubscriber.dispose()

        publishSubscriber.assertNoErrors()
        searchSubscriber.assertNoErrors()

        publishSubscriber.assertNotComplete()
        searchSubscriber.assertNotComplete()

        val values = searchSubscriber.values()
        assertTrue(values.isNotEmpty())
        values.forEach {
            assertEquals(it.st, message.st)
        }
    }
}
