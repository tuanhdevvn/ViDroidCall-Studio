package com.example.ViDroidCall_Studio.data.local.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
 * Quản lý cơ sở dữ liệu SQLite lưu trữ lịch sử câu lệnh ngoại tuyến (Giới hạn tối đa 10 câu lệnh mới nhất)
 */
class CommandHistoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val updateNotifier = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COMMAND_TEXT TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_TIME_FORMATTED TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    suspend fun insertCommand(
        commandText: String,
        category: String = "Hệ thống",
        status: String = "Thành công"
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val formattedTime = formatTimestamp(now)

        val values = ContentValues().apply {
            put(COLUMN_COMMAND_TEXT, commandText)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_STATUS, status)
            put(COLUMN_TIME_FORMATTED, formattedTime)
            put(COLUMN_TIMESTAMP, now)
        }

        val db = writableDatabase
        val id = db.insert(TABLE_HISTORY, null, values)

        // Tự động giữ tối đa 10 câu lệnh mới nhất, xóa sạch các câu lệnh cũ hơn
        try {
            db.execSQL("""
                DELETE FROM $TABLE_HISTORY 
                WHERE $COLUMN_ID NOT IN (
                    SELECT $COLUMN_ID FROM $TABLE_HISTORY 
                    ORDER BY $COLUMN_TIMESTAMP DESC 
                    LIMIT $MAX_HISTORY_ITEMS
                )
            """.trimIndent())
        } catch (e: Exception) {
            // ignore
        }

        updateNotifier.tryEmit(Unit)
        id
    }

    suspend fun getAllHistory(): List<CommandHistoryItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<CommandHistoryItem>()
        val cursor = readableDatabase.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC",
            "$MAX_HISTORY_ITEMS"
        )

        cursor.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(COLUMN_ID)
                val textIndex = it.getColumnIndexOrThrow(COLUMN_COMMAND_TEXT)
                val catIndex = it.getColumnIndexOrThrow(COLUMN_CATEGORY)
                val statusIndex = it.getColumnIndexOrThrow(COLUMN_STATUS)
                val timeIndex = it.getColumnIndexOrThrow(COLUMN_TIME_FORMATTED)
                val stampIndex = it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)

                do {
                    items.add(
                        CommandHistoryItem(
                            id = it.getLong(idIndex),
                            commandText = it.getString(textIndex),
                            category = it.getString(catIndex),
                            status = it.getString(statusIndex),
                            time = it.getString(timeIndex),
                            timestamp = it.getLong(stampIndex)
                        )
                    )
                } while (it.moveToNext())
            }
        }
        items
    }

    fun getAllHistoryFlow(): Flow<List<CommandHistoryItem>> = flow {
        updateNotifier.collect {
            emit(getAllHistory())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteById(id: Long): Int = withContext(Dispatchers.IO) {
        val rows = writableDatabase.delete(TABLE_HISTORY, "$COLUMN_ID = ?", arrayOf(id.toString()))
        updateNotifier.tryEmit(Unit)
        rows
    }

    suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        val rows = writableDatabase.delete(TABLE_HISTORY, null, null)
        updateNotifier.tryEmit(Unit)
        rows
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
        private const val DATABASE_NAME = "vidroidcall_history.db"
        private const val DATABASE_VERSION = 1
        const val MAX_HISTORY_ITEMS = 10

        const val TABLE_HISTORY = "command_history"
        const val COLUMN_ID = "id"
        const val COLUMN_COMMAND_TEXT = "command_text"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_STATUS = "status"
        const val COLUMN_TIME_FORMATTED = "time_formatted"
        const val COLUMN_TIMESTAMP = "timestamp"
    }
}
