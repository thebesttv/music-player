package com.thebesttv.musicplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.thebesttv.musicplayer.logic.ScheduleConfig
import com.thebesttv.musicplayer.logic.SchedulerPlanner
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val prefs by lazy {
        getSharedPreferences("music_player", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            MaterialTheme {
                SchedulerScreen(
                    initialUri = prefs.getString(KEY_URI, "").orEmpty(),
                    initialWebhook = prefs.getString(KEY_WEBHOOK, "").orEmpty(),
                    onSave = { uri, webhook ->
                        prefs.edit()
                            .putString(KEY_URI, uri)
                            .putString(KEY_WEBHOOK, webhook)
                            .apply()
                    },
                    onSchedule = { uri, webhook, windowMinutes, playMinutes ->
                        schedulePlayback(uri, webhook, windowMinutes, playMinutes)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) {
            return
        }

        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        prefs.edit().putString(KEY_URI, uri.toString()).apply()
    }

    private fun schedulePlayback(uri: String, webhook: String, windowMinutes: Int, playMinutes: Int) {
        if (uri.isBlank()) {
            Toast.makeText(this, "请先分享一个音频文件", Toast.LENGTH_SHORT).show()
            return
        }

        val data = Data.Builder()
            .putString(PlaybackWorker.KEY_URI, uri)
            .putString(PlaybackWorker.KEY_WEBHOOK, webhook)
            .putInt(PlaybackWorker.KEY_WINDOW_MINUTES, windowMinutes)
            .putInt(PlaybackWorker.KEY_PLAY_MINUTES, playMinutes)
            .build()

        val request = OneTimeWorkRequestBuilder<PlaybackWorker>()
            .setInputData(data)
            .setInitialDelay(
                SchedulerPlanner.plan(
                    config = ScheduleConfig(windowMinutes = windowMinutes, playMinutes = playMinutes),
                    trackDurationMillis = 0L
                ).delayMillis,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueue(request)
        Toast.makeText(this, "已创建播放任务", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val KEY_WEBHOOK = "webhook"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulerScreen(
    initialUri: String,
    initialWebhook: String,
    onSave: (String, String) -> Unit,
    onSchedule: (String, String, Int, Int) -> Unit
) {
    var uri by remember { mutableStateOf(initialUri) }
    var webhook by remember { mutableStateOf(initialWebhook) }
    var windowMinutes by remember { mutableIntStateOf(60) }
    var playMinutes by remember { mutableIntStateOf(20) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "通过分享导入音频后，可配置随机播放调度", style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = uri,
                onValueChange = { uri = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("音频 URI") }
            )

            OutlinedTextField(
                value = webhook,
                onValueChange = { webhook = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("飞书 Webhook (可选)") }
            )

            NumberField(
                label = "调度窗口(分钟)",
                value = windowMinutes,
                onValueChange = { windowMinutes = it }
            )

            NumberField(
                label = "播放时长(分钟)",
                value = playMinutes,
                onValueChange = { playMinutes = it }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSave(uri, webhook) }) {
                    Text("保存配置")
                }
                Button(onClick = {
                    onSave(uri, webhook)
                    onSchedule(uri, webhook, windowMinutes, playMinutes)
                }) {
                    Text("开始调度")
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> onValueChange(text.toIntOrNull() ?: value) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
