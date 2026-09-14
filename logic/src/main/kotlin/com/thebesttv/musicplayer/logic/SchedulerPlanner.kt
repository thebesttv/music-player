package com.thebesttv.musicplayer.logic

import kotlin.math.max
import kotlin.random.Random

data class ScheduleConfig(
    val windowMinutes: Int,
    val playMinutes: Int
)

data class PlannedPlayback(
    val delayMillis: Long,
    val seekMillis: Long
)

object SchedulerPlanner {
    fun plan(
        config: ScheduleConfig,
        trackDurationMillis: Long,
        random: Random = Random.Default
    ): PlannedPlayback {
        val playMillis = config.playMinutes.coerceAtLeast(1) * 60_000L
        val windowMillis = max(config.windowMinutes, config.playMinutes).toLong() * 60_000L

        val maxDelay = (windowMillis - playMillis).coerceAtLeast(0L)
        val delayMillis = if (maxDelay == 0L) 0L else random.nextLong(maxDelay + 1)

        val boundedDuration = trackDurationMillis.coerceAtLeast(0L)
        val maxSeek = (boundedDuration - playMillis).coerceAtLeast(0L)
        val seekMillis = if (maxSeek == 0L) 0L else random.nextLong(maxSeek + 1)

        return PlannedPlayback(delayMillis = delayMillis, seekMillis = seekMillis)
    }
}
