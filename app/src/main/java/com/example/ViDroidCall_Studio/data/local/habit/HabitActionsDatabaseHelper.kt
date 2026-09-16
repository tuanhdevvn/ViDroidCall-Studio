// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * SQLite riêng cho lệnh hay dùng — không dùng `command_history` (tối đa 10 câu STT).
 */
class HabitActionsDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_ACTIONS (
                $COLUMN_SLOT_KEY TEXT PRIMARY KEY NOT NULL,
                $COLUMN_INTENT TEXT NOT NULL,
                $COLUMN_LABEL TEXT NOT NULL,
                $COLUMN_ACTION_JSON TEXT NOT NULL,
                $COLUMN_LAST_USED INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SLOT_KEY TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_META")
        onCreate(db)
    }

    suspend fun upsertAction(record: HabitActionRecord, eventAtMs: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_SLOT_KEY, record.slotKey)
                put(COLUMN_INTENT, record.intent)
                put(COLUMN_LABEL, record.label)
                put(COLUMN_ACTION_JSON, record.actionJson)
                put(COLUMN_LAST_USED, record.lastUsedMs)
            }
            db.insertWithOnConflict(TABLE_ACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)

            val event = ContentValues().apply {
                put(COLUMN_SLOT_KEY, record.slotKey)
                put(COLUMN_TIMESTAMP, eventAtMs)
            }
            db.insert(TABLE_EVENTS, null, event)

            val purgeBefore = eventAtMs - HabitRules.PURGE_MS
            db.delete(TABLE_EVENTS, "$COLUMN_TIMESTAMP < ?", arrayOf(purgeBefore.toString()))
            db.execSQL(
                """
                DELETE FROM $TABLE_ACTIONS
                WHERE $COLUMN_SLOT_KEY NOT IN (
                    SELECT DISTINCT $COLUMN_SLOT_KEY FROM $TABLE_EVENTS
                    WHERE $COLUMN_TIMESTAMP >= ?
                )
                """.trimIndent(),
                arrayOf(purgeBefore.toString())
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        updateNotifier.tryEmit(Unit)
    }

    suspend fun loadRecords(): List<HabitActionRecord> = withContext(Dispatchers.IO) {
        val items = mutableListOf<HabitActionRecord>()
        readableDatabase.query(TABLE_ACTIONS, null, null, null, null, null, null).use { cursor ->
            val key = cursor.getColumnIndexOrThrow(COLUMN_SLOT_KEY)
            val intent = cursor.getColumnIndexOrThrow(COLUMN_INTENT)
            val label = cursor.getColumnIndexOrThrow(COLUMN_LABEL)
            val json = cursor.getColumnIndexOrThrow(COLUMN_ACTION_JSON)
            val lastUsed = cursor.getColumnIndexOrThrow(COLUMN_LAST_USED)
            while (cursor.moveToNext()) {
                items.add(
                    HabitActionRecord(
                        slotKey = cursor.getString(key),
                        intent = cursor.getString(intent),
                        label = cursor.getString(label),
                        actionJson = cursor.getString(json),
                        lastUsedMs = cursor.getLong(lastUsed)
                    )
                )
            }
        }
        items
    }

    suspend fun loadEvents(): List<HabitEvent> = withContext(Dispatchers.IO) {
        val items = mutableListOf<HabitEvent>()
        readableDatabase.query(TABLE_EVENTS, null, null, null, null, null, null).use { cursor ->
            val key = cursor.getColumnIndexOrThrow(COLUMN_SLOT_KEY)
            val ts = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            while (cursor.moveToNext()) {
                items.add(
                    HabitEvent(
                        slotKey = cursor.getString(key),
                        timestampMs = cursor.getLong(ts)
                    )
                )
            }
        }
        items
    }

    suspend fun loadSnapshot(): HabitSnapshot? = withContext(Dispatchers.IO) {
        val dayId = readMeta(META_SNAPSHOT_DAY) ?: return@withContext null
        val rawKeys = readMeta(META_SNAPSHOT_KEYS) ?: return@withContext null
        val keys = try {
            val array = JSONArray(rawKeys)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
        HabitSnapshot(dayId = dayId, slotKeys = keys)
    }

    suspend fun saveSnapshot(snapshot: HabitSnapshot) = withContext(Dispatchers.IO) {
        val keys = JSONArray()
        snapshot.slotKeys.forEach { keys.put(it) }
        writeMeta(META_SNAPSHOT_DAY, snapshot.dayId)
        writeMeta(META_SNAPSHOT_KEYS, keys.toString())
        updateNotifier.tryEmit(Unit)
    }

    fun quickActionsFlow(): Flow<Unit> = flow {
        updateNotifier.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

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
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
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

    companion object {
        private const val DATABASE_NAME = "vidroidcall_habit.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_ACTIONS = "habit_actions"
        const val TABLE_EVENTS = "habit_events"
        const val TABLE_META = "habit_meta"

        const val COLUMN_ID = "id"
        const val COLUMN_SLOT_KEY = "slot_key"
        const val COLUMN_INTENT = "intent"
        const val COLUMN_LABEL = "label"
        const val COLUMN_ACTION_JSON = "action_json"
        const val COLUMN_LAST_USED = "last_used"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_META_KEY = "meta_key"
        const val COLUMN_META_VALUE = "meta_value"

        const val META_SNAPSHOT_DAY = "snapshot_day"
        const val META_SNAPSHOT_KEYS = "snapshot_keys"

        private val updateNotifier = MutableSharedFlow<Unit>(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ).apply { tryEmit(Unit) }
    }
}
