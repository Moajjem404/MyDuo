package com.moajjem.myduuo.security

import android.content.Context
import android.content.SharedPreferences

class SecurityManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "myduo_preferences"
        private const val KEY_BOT_TOKEN = "key_bot_token"
        private const val KEY_GROUP_ID = "key_group_id"
        private const val KEY_PARTNER_NAME = "key_partner_name"
        private const val KEY_SENDER_ID = "key_sender_id"
        private const val KEY_GENDER = "key_gender"
        private const val KEY_TELEGRAM_OFFSET = "key_telegram_offset"

        @Volatile
        private var instance: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return instance ?: synchronized(this) {
                instance ?: SecurityManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveBotToken(token: String) {
        val encrypted = KeystoreHelper.encrypt(token)
        prefs.edit().putString(KEY_BOT_TOKEN, encrypted).apply()
    }

    fun getBotToken(): String? {
        val encrypted = prefs.getString(KEY_BOT_TOKEN, null) ?: return null
        val decrypted = KeystoreHelper.decrypt(encrypted)
        return if (decrypted.isEmpty()) null else decrypted
    }

    fun saveGroupId(groupId: String) {
        prefs.edit().putString(KEY_GROUP_ID, groupId).apply()
    }

    fun getGroupId(): String? {
        return prefs.getString(KEY_GROUP_ID, null)
    }

    fun savePartnerName(name: String) {
        prefs.edit().putString(KEY_PARTNER_NAME, name).apply()
    }

    fun getPartnerName(): String {
        return prefs.getString(KEY_PARTNER_NAME, "") ?: ""
    }

    fun saveSenderId(senderId: String) {
        prefs.edit().putString(KEY_SENDER_ID, senderId).apply()
    }

    fun getSenderId(): String? {
        return prefs.getString(KEY_SENDER_ID, null)
    }

    fun saveGender(gender: String) {
        prefs.edit().putString(KEY_GENDER, gender).apply()
    }

    fun getGender(): String? {
        return prefs.getString(KEY_GENDER, null)
    }

    fun saveTelegramOffset(offset: Long) {
        prefs.edit().putLong(KEY_TELEGRAM_OFFSET, offset).apply()
    }

    fun getTelegramOffset(): Long {
        return prefs.getLong(KEY_TELEGRAM_OFFSET, 0L)
    }

    fun isSetupComplete(): Boolean {
        return !getBotToken().isNullOrEmpty() &&
                !getGroupId().isNullOrEmpty() &&
                !getPartnerName().isNullOrEmpty() &&
                !getSenderId().isNullOrEmpty()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
