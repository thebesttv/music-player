package com.thebesttv.musicplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class FeishuNotifier(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun notify(webhookUrl: String, message: String) {
        if (webhookUrl.isBlank()) {
            return
        }

        val body = "{\"msg_type\":\"text\",\"content\":{\"text\":\"${message.replace("\"", "\\\"")}\"}}"
        val request = Request.Builder()
            .url(webhookUrl)
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { }
        }
    }
}
