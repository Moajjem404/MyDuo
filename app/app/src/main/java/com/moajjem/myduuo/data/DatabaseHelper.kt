package com.moajjem.myduuo.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class HistoryItem(val app: String, val time: Long)

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "myduo.db"
        const val DATABASE_VERSION = 1

        const val TABLE_PARTNER_STATE = "partner_state"
        const val COLUMN_SENDER = "sender"
        const val COLUMN_APP = "app"
        const val COLUMN_TIME = "time" // Unix timestamp in seconds

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PARTNER_STATE (
                $COLUMN_SENDER TEXT PRIMARY KEY,
                $COLUMN_APP TEXT NOT NULL,
                $COLUMN_TIME INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PARTNER_STATE")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS partner_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                app TEXT NOT NULL,
                time INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    fun savePartnerHistory(app: String, time: Long) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("app", app)
                put("time", time)
            }
            db.insert("partner_history", null, values)
            // Cap at 10 items
            db.execSQL("DELETE FROM partner_history WHERE id NOT IN (SELECT id FROM partner_history ORDER BY id DESC LIMIT 10)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPartnerHistory(): List<HistoryItem> {
        val list = ArrayList<HistoryItem>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                "partner_history",
                arrayOf("app", "time"),
                null, null, null, null, "id DESC"
            )
            while (cursor.moveToNext()) {
                val app = cursor.getString(0)
                val time = cursor.getLong(1)
                list.add(HistoryItem(app, time))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun savePartnerState(sender: String, app: String, time: Long) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_SENDER, sender)
                put(COLUMN_APP, app)
                put(COLUMN_TIME, time)
            }
            db.insertWithOnConflict(TABLE_PARTNER_STATE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLatestPartnerState(localSenderId: String): PartnerState? {
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_PARTNER_STATE,
                arrayOf(COLUMN_SENDER, COLUMN_APP, COLUMN_TIME),
                "$COLUMN_SENDER != ?",
                arrayOf(localSenderId),
                null, null, null
            )
            
            var state: PartnerState? = null
            if (cursor.moveToFirst()) {
                val sender = cursor.getString(0)
                val app = cursor.getString(1)
                val time = cursor.getLong(2)
                state = PartnerState(sender, app, time)
            }
            cursor.close()
            return state
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

data class PartnerState(
    val sender: String,
    val app: String,
    val time: Long // Unix timestamp in seconds
)
