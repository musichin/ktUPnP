package com.github.musichin.ktupnp.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers

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
                .toObservable<Unit>()
                .subscribe(notifySubscriber)

        Thread.sleep(1_000)
        notifySubscriber.unsubscribe()
        notificationsSubscriber.unsubscribe()

        notifySubscriber.assertNoErrors()
        notificationsSubscriber.assertNoErrors()

        notifySubscriber.assertCompleted()
        notificationsSubscriber.assertNotCompleted()

        val values = notificationsSubscriber.onNextEvents
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
                .toObservable<Unit>()
                .subscribe(publishSubscriber)

        val searchSubscriber = TestSubscriber<SsdpMessage>()
        Ssdp.search(ST_TEST)
                .unsubscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(searchSubscriber)

        Thread.sleep(1_000)
        publishSubscriber.unsubscribe()
        searchSubscriber.unsubscribe()

        publishSubscriber.assertNoErrors()
        searchSubscriber.assertNoErrors()

        publishSubscriber.assertNotCompleted()
        searchSubscriber.assertNotCompleted()

        val values = searchSubscriber.onNextEvents
        assertTrue(values.isNotEmpty())
        values.forEach {
            assertEquals(it.st, message.st)
        }
    }
}
