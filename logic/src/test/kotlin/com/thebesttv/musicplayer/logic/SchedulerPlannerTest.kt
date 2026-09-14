package com.thebesttv.musicplayer.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchedulerPlannerTest {
    @Test
    fun `delay and seek stay within bounds`() {
        repeat(200) {
            val result = SchedulerPlanner.plan(
                config = ScheduleConfig(windowMinutes = 60, playMinutes = 20),
                trackDurationMillis = 90 * 60_000L,
                random = Random(it)
            )

            assertTrue(result.delayMillis in 0L..(40 * 60_000L))
            assertTrue(result.seekMillis in 0L..(70 * 60_000L))
        }
    }

    @Test
    fun `play duration longer than window clamps delay to zero`() {
        val result = SchedulerPlanner.plan(
            config = ScheduleConfig(windowMinutes = 10, playMinutes = 20),
            trackDurationMillis = 25 * 60_000L,
            random = Random(1)
        )

        assertEquals(0L, result.delayMillis)
    }

    @Test
    fun `short track clamps seek to zero`() {
        val result = SchedulerPlanner.plan(
            config = ScheduleConfig(windowMinutes = 60, playMinutes = 20),
            trackDurationMillis = 10 * 60_000L,
            random = Random(2)
        )

        assertEquals(0L, result.seekMillis)
    }
}
