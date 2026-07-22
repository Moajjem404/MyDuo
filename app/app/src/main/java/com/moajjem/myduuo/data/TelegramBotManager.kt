package com.moajjem.myduuo.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TelegramBotManager {
    private const val TAG = "TelegramBotManager"

    private val client = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Validates a Telegram Bot Token by calling the /getMe endpoint.
     */
    fun validateToken(token: String): Boolean {
        val url = "https://api.telegram.org/bot$token/getMe"
        val request = Request.Builder().url(url).get().build()
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating token: ${e.message}")
            false
        }
    }

    /**
     * Sends a raw JSON text message to the group.
     * Returns the sent message's ID, or -1 if failed.
     */
    fun sendMessage(token: String, groupId: String, text: String): Long {
        val url = "https://api.telegram.org/bot$token/sendMessage"
        val json = JSONObject().apply {
            put("chat_id", groupId)
            put("text", text)
        }

        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (bodyString != null) {
                        val jsonObj = JSONObject(bodyString)
                        if (jsonObj.optBoolean("ok", false)) {
                            val resultObj = jsonObj.optJSONObject("result")
                            return resultObj?.optLong("message_id", -1L) ?: -1L
                        }
                    }
                }
                -1L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            -1L
        }
    }

    /**
     * Pins a specific message in a chat.
     */
    fun pinChatMessage(token: String, chatId: String, messageId: Long): Boolean {
        val url = "https://api.telegram.org/bot$token/pinChatMessage"
        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("message_id", messageId)
            put("disable_notification", true)
        }

        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pinning message: ${e.message}")
            false
        }
    }

    /**
     * Fetches the latest pinned message from a chat.
     */
    fun getLatestPinnedMessage(token: String, chatId: String): TelegramPinnedMessage? {
        val url = "https://api.telegram.org/bot$token/getChat?chat_id=$chatId"
        val request = Request.Builder().url(url).get().build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return null
                    val jsonResponse = JSONObject(bodyString)
                    if (jsonResponse.optBoolean("ok", false)) {
                        val resultObj = jsonResponse.optJSONObject("result")
                        val pinnedMsgObj = resultObj?.optJSONObject("pinned_message")
                        if (pinnedMsgObj != null) {
                            val text = pinnedMsgObj.optString("text", "")
                            val date = pinnedMsgObj.optLong("date", 0L)
                            val messageId = pinnedMsgObj.optLong("message_id", -1L)
                            return TelegramPinnedMessage(messageId, text, date)
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pinned message: ${e.message}")
            null
        }
    }

    /**
     * Fetches new updates from the Telegram bot.
     */
    fun getUpdates(token: String, offset: Long): List<TelegramGroupMessage> {
        val url = "https://api.telegram.org/bot$token/getUpdates"
        val json = JSONObject().apply {
            put("offset", offset)
            put("timeout", 20) // Use 20s timeout for battery-friendly long polling
        }
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        val messages = mutableListOf<TelegramGroupMessage>()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return emptyList()
                    val jsonResponse = JSONObject(bodyString)
                    if (jsonResponse.optBoolean("ok", false)) {
                        val resultArray = jsonResponse.optJSONArray("result") ?: JSONArray()
                        for (i in 0 until resultArray.length()) {
                            val updateObj = resultArray.getJSONObject(i)
                            val updateId = updateObj.getLong("update_id")
                            
                            val messageObj = updateObj.optJSONObject("message")
                            val channelPostObj = updateObj.optJSONObject("channel_post")
                            val editedMessageObj = updateObj.optJSONObject("edited_message")
                            val editedChannelPostObj = updateObj.optJSONObject("edited_channel_post")
                            
                            val activeMsgObj = messageObj ?: channelPostObj ?: editedMessageObj ?: editedChannelPostObj
                            
                            if (activeMsgObj != null) {
                                val text = activeMsgObj.optString("text", "")
                                val date = activeMsgObj.optLong("date", activeMsgObj.optLong("edit_date", 0L))
                                messages.add(TelegramGroupMessage(updateId, text, date))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting updates: ${e.message}")
        }
        return messages
    }
}

data class TelegramGroupMessage(
    val updateId: Long,
    val text: String,
    val date: Long // Unix timestamp in seconds
)

data class TelegramPinnedMessage(
    val messageId: Long,
    val text: String,
    val date: Long // Unix timestamp in seconds
)
