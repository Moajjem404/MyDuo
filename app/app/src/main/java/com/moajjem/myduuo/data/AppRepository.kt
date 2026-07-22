package com.moajjem.myduuo.data

import android.content.Context
import com.moajjem.myduuo.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository private constructor(context: Context) {

    private val securityManager = SecurityManager.getInstance(context)
    private val databaseHelper = DatabaseHelper.getInstance(context)

    private val _partnerState = MutableStateFlow<PartnerState?>(null)
    val partnerState: StateFlow<PartnerState?> = _partnerState.asStateFlow()

    init {
        val localSenderId = securityManager.getSenderId() ?: ""
        _partnerState.value = databaseHelper.getLatestPartnerState(localSenderId)
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getBotToken(): String? = securityManager.getBotToken()
    fun saveBotToken(token: String) = securityManager.saveBotToken(token)

    fun getGroupId(): String? = securityManager.getGroupId()
    fun saveGroupId(groupId: String) = securityManager.saveGroupId(groupId)

    fun getPartnerName(): String = securityManager.getPartnerName()
    fun savePartnerName(name: String) = securityManager.savePartnerName(name)

    fun getSenderId(): String? = securityManager.getSenderId()
    fun saveSenderId(senderId: String) = securityManager.saveSenderId(senderId)

    fun getGender(): String? = securityManager.getGender()
    fun saveGender(gender: String) = securityManager.saveGender(gender)

    fun getTelegramOffset(): Long = securityManager.getTelegramOffset()
    fun saveTelegramOffset(offset: Long) = securityManager.saveTelegramOffset(offset)

    fun isSetupComplete(): Boolean = securityManager.isSetupComplete()

    fun clearAllData() {
        securityManager.clearAll()
        try {
            val db = databaseHelper.writableDatabase
            databaseHelper.onUpgrade(db, DatabaseHelper.DATABASE_VERSION, DatabaseHelper.DATABASE_VERSION)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _partnerState.value = null
    }

    fun updatePartnerState(sender: String, app: String, time: Long) {
        databaseHelper.savePartnerState(sender, app, time)
        val mySenderId = getSenderId() ?: ""
        if (sender != mySenderId) {
            databaseHelper.savePartnerHistory(app, time)
        }
        _partnerState.value = PartnerState(sender, app, time)
    }
}
