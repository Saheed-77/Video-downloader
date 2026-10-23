package com.saheed.videodownloader.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class UpdateSchedulerTest {

    private val now = 1_800_000_000_000L
    private fun daysAgo(days: Long) = now - TimeUnit.DAYS.toMillis(days)

    @Test
    fun sixDaysOldIsNotDue() = assertFalse(EngineUpdater.isUpdateDue(daysAgo(6), now))

    @Test
    fun eightDaysOldIsDue() = assertTrue(EngineUpdater.isUpdateDue(daysAgo(8), now))

    @Test
    fun exactlySevenDaysIsNotYetDue() = assertFalse(EngineUpdater.isUpdateDue(daysAgo(7), now))

    @Test
    fun neverUpdatedIsDue() = assertTrue(EngineUpdater.isUpdateDue(null, now))

    @Test
    fun justUpdatedIsNotDue() = assertFalse(EngineUpdater.isUpdateDue(now, now))
}
