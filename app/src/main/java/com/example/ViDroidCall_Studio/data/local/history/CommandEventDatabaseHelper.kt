// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.ViDroidCall_Studio.data.local.habit.HabitQuickAction
import com.example.ViDroidCall_Studio.data.local.habit.HabitQuickActionSelector
import com.example.ViDroidCall_Studio.data.local.habit.HabitRules
import com.example.ViDroidCall_Studio.feature.history.model.CommandHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Một SQLite cho lịch sử (10 câu mới nhất) và câu lệnh nhanh (top 5 theo slot_key).
 * Giữ log 24 giờ; dọn 1 lần/ngày.
 */
class CommandEventDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COMMAND_TEXT TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_TIME_FORMATTED TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SLOT_KEY TEXT,
                $COLUMN_INTENT TEXT,
                $COLUMN_LABEL TEXT,
                $COLUMN_ACTION_JSON TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX idx_command_events_ts ON $TABLE_EVENTS($COLUMN_TIMESTAMP DESC)"
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_META (
                $COLUMN_META_KEY TEXT PRIMARY KEY NOT NULL,
                $COLUMN_META_VALUE TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_META")
        onCreate(db)
    }

    suspend fun insertHistory(
        commandText: String,
        category: String = "Hệ thống",
        status: String = "Thành công",
        nowMs: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        purgeIfNewDay(nowMs)
        val values = ContentValues().apply {
            put(COLUMN_COMMAND_TEXT, commandText)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_STATUS, status)
            put(COLUMN_TIME_FORMATTED, formatTimestamp(nowMs))
            put(COLUMN_TIMESTAMP, nowMs)
        }
        val id = writableDatabase.insert(TABLE_EVENTS, null, values)
        notifyChanged()
        id
    }

    /**
     * Gắn NativeAction vào câu STT mới nhất chưa có slot, hoặc thêm dòng nếu không có.
     */
    suspend fun attachOrInsertAction(
        slotKey: String,
        intent: String,
        label: String,
        actionJson: String,
        category: String,
        nowMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        purgeIfNewDay(nowMs)
        val db = writableDatabase
        val latestOpenId = db.query(
            TABLE_EVENTS,
            arrayOf(COLUMN_ID),
            "$COLUMN_SLOT_KEY IS NULL OR $COLUMN_SLOT_KEY = ''",
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC, $COLUMN_ID DESC",
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }

        if (latestOpenId != null) {
            val values = ContentValues().apply {
                put(COLUMN_SLOT_KEY, slotKey)
                put(COLUMN_INTENT, intent)
                put(COLUMN_LABEL, label)
                put(COLUMN_ACTION_JSON, actionJson)
            }
            db.update(TABLE_EVENTS, values, "$COLUMN_ID = ?", arrayOf(latestOpenId.toString()))
        } else {
            val values = ContentValues().apply {
                put(COLUMN_COMMAND_TEXT, label)
                put(COLUMN_CATEGORY, category)
                put(COLUMN_STATUS, "Thành công")
                put(COLUMN_TIME_FORMATTED, formatTimestamp(nowMs))
                put(COLUMN_TIMESTAMP, nowMs)
                put(COLUMN_SLOT_KEY, slotKey)
                put(COLUMN_INTENT, intent)
                put(COLUMN_LABEL, label)
                put(COLUMN_ACTION_JSON, actionJson)
            }
            db.insert(TABLE_EVENTS, null, values)
        }
        notifyChanged()
    }

    suspend fun getRecentHistory(
        nowMs: Long = System.currentTimeMillis()
    ): List<CommandHistoryItem> = withContext(Dispatchers.IO) {
        purgeIfNewDay(nowMs)
        val items = mutableListOf<CommandHistoryItem>()
        readableDatabase.query(
            TABLE_EVENTS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC, $COLUMN_ID DESC",
            "${HabitRules.MAX_HISTORY_ITEMS}"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val textIndex = cursor.getColumnIndexOrThrow(COLUMN_COMMAND_TEXT)
            val catIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)
            val statusIndex = cursor.getColumnIndexOrThrow(COLUMN_STATUS)
            val timeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIME_FORMATTED)
            val stampIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            while (cursor.moveToNext()) {
                items.add(
                    CommandHistoryItem(
                        id = cursor.getLong(idIndex),
                        commandText = cursor.getString(textIndex),
                        category = cursor.getString(catIndex),
                        status = cursor.getString(statusIndex),
                        time = cursor.getString(timeIndex),
                        timestamp = cursor.getLong(stampIndex)
                    )
                )
            }
        }
        items
    }

    fun historyFlow(): Flow<List<CommandHistoryItem>> = flow {
        updateNotifier.collect { emit(getRecentHistory()) }
    }.flowOn(Dispatchers.IO)

    suspend fun getQuickActions(
        nowMs: Long = System.currentTimeMillis()
    ): List<HabitQuickAction> = withContext(Dispatchers.IO) {
        purgeIfNewDay(nowMs)
        val windowStart = nowMs - HabitRules.WINDOW_MS
        val events = mutableListOf<HabitQuickActionSelector.ActionEvent>()
        readableDatabase.query(
            TABLE_EVENTS,
            arrayOf(COLUMN_SLOT_KEY, COLUMN_INTENT, COLUMN_LABEL, COLUMN_ACTION_JSON, COLUMN_TIMESTAMP),
            "$COLUMN_SLOT_KEY IS NOT NULL AND $COLUMN_SLOT_KEY != '' AND $COLUMN_TIMESTAMP >= ?",
            arrayOf(windowStart.toString()),
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        ).use { cursor ->
            val key = cursor.getColumnIndexOrThrow(COLUMN_SLOT_KEY)
            val intent = cursor.getColumnIndexOrThrow(COLUMN_INTENT)
            val label = cursor.getColumnIndexOrThrow(COLUMN_LABEL)
            val json = cursor.getColumnIndexOrThrow(COLUMN_ACTION_JSON)
            val ts = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            while (cursor.moveToNext()) {
                events.add(
                    HabitQuickActionSelector.ActionEvent(
                        slotKey = cursor.getString(key),
                        intent = cursor.getString(intent).orEmpty(),
                        label = cursor.getString(label).orEmpty(),
                        actionJson = cursor.getString(json).orEmpty(),
                        timestampMs = cursor.getLong(ts)
                    )
                )
            }
        }
        HabitQuickActionSelector.topQuickActions(events, HabitRules.MAX_QUICK_ACTIONS)
    }

    fun quickActionsFlow(): Flow<List<HabitQuickAction>> = flow {
        updateNotifier.collect { emit(getQuickActions()) }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteById(id: Long): Int = withContext(Dispatchers.IO) {
        val rows = writableDatabase.delete(TABLE_EVENTS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        notifyChanged()
        rows
    }

    suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        val rows = writableDatabase.delete(TABLE_EVENTS, null, null)
        notifyChanged()
        rows
    }

    internal fun purgeIfNewDay(nowMs: Long) {
        val today = dayId(nowMs)
        val last = readMeta(META_LAST_PURGE_DAY)
        if (last == today) return
        val cutoff = nowMs - HabitRules.WINDOW_MS
        writableDatabase.delete(
            TABLE_EVENTS,
            "$COLUMN_TIMESTAMP < ?",
            arrayOf(cutoff.toString())
        )
        writeMeta(META_LAST_PURGE_DAY, today)
    }

    private fun readMeta(key: String): String? {
        readableDatabase.query(
            TABLE_META,
            arrayOf(COLUMN_META_VALUE),
            "$COLUMN_META_KEY = ?",
            arrayOf(key),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun writeMeta(key: String, value: String) {
        val values = ContentValues().apply {
            put(COLUMN_META_KEY, key)
            put(COLUMN_META_VALUE, value)
        }
        writableDatabase.insertWithOnConflict(TABLE_META, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun notifyChanged() {
        updateNotifier.tryEmit(Unit)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = System.currentTimeMillis()
        val diffHours = (now - timestamp) / (1000 * 60 * 60)
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return when {
            diffHours < 24 -> "Hôm nay " + timeFormatter.format(date)
            diffHours < 48 -> "Hôm qua " + timeFormatter.format(date)
            else -> dateFormatter.format(date) + " " + timeFormatter.format(date)
        }
    }

    companion object {
        private const val DATABASE_NAME = "vidroidcall_commands.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_EVENTS = "command_events"
        const val TABLE_META = "command_meta"

        const val COLUMN_ID = "id"
        const val COLUMN_COMMAND_TEXT = "command_text"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_STATUS = "status"
        const val COLUMN_TIME_FORMATTED = "time_formatted"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SLOT_KEY = "slot_key"
        const val COLUMN_INTENT = "intent"
        const val COLUMN_LABEL = "label"
        const val COLUMN_ACTION_JSON = "action_json"
        const val COLUMN_META_KEY = "meta_key"
        const val COLUMN_META_VALUE = "meta_value"
        const val META_LAST_PURGE_DAY = "last_purge_day"

        private val LEGACY_DATABASES = listOf("vidroidcall_history.db", "vidroidcall_habit.db")

        private val updateNotifier = MutableSharedFlow<Unit>(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ).apply { tryEmit(Unit) }

        @Volatile
        private var instance: CommandEventDatabaseHelper? = null

        fun get(context: Context): CommandEventDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: run {
                    LEGACY_DATABASES.forEach { name ->
                        context.applicationContext.deleteDatabase(name)
                    }
                    CommandEventDatabaseHelper(context.applicationContext).also { instance = it }
                }
            }
        }

        fun dayId(nowMs: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
        }
    }
}
