package com.thebesttv.musicplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.thebesttv.musicplayer.logic.ScheduleConfig
import com.thebesttv.musicplayer.logic.SchedulerPlanner
import kotlinx.coroutines.delay
import kotlin.random.Random

class PlaybackWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uriText = inputData.getString(KEY_URI) ?: return Result.failure()
        val uri = Uri.parse(uriText)
        val webhook = inputData.getString(KEY_WEBHOOK).orEmpty()
        val windowMinutes = inputData.getInt(KEY_WINDOW_MINUTES, 60)
        val playMinutes = inputData.getInt(KEY_PLAY_MINUTES, 20).coerceAtLeast(1)

        setForeground(createForegroundInfo())

        val duration = extractDuration(uri)
        val plan = SchedulerPlanner.plan(
            config = ScheduleConfig(windowMinutes = windowMinutes, playMinutes = playMinutes),
            trackDurationMillis = duration,
            random = Random.Default
        )

        val player = MediaPlayer()
        return try {
            player.setDataSource(applicationContext, uri)
            player.prepare()
            player.seekTo(plan.seekMillis.toInt())
            player.start()
            FeishuNotifier().notify(webhook, "Music playback started: $uri")
            delay(playMinutes * 60_000L)
            player.stop()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            player.release()
        }
    }

    private fun extractDuration(uri: Uri): Long {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(applicationContext, uri)
            val value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            value?.toLongOrNull() ?: 0L
        }.getOrDefault(0L)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "playback_worker"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "Playback", NotificationManager.IMPORTANCE_LOW)
        )

        val notification: Notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Music Player")
            .setContentText("Playing scheduled music")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        return ForegroundInfo(1001, notification)
    }

    companion object {
        const val KEY_URI = "uri"
        const val KEY_WEBHOOK = "webhook"
        const val KEY_WINDOW_MINUTES = "window_minutes"
        const val KEY_PLAY_MINUTES = "play_minutes"
    }
}
