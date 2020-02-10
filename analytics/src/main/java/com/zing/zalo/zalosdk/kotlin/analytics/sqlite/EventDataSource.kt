package com.zing.zalo.zalosdk.kotlin.analytics.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.COLUMN_ACTION
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.COLUMN_DATA
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.COLUMN_TIME
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.TABLE_EVENT
import com.zing.zalo.zalosdk.kotlin.analytics.model.Event
import com.zing.zalo.zalosdk.kotlin.core.helper.UtilsJSON
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class EventDataSource(var context: Context) {

    private val dbHelper: EventSQLiteHelper = EventSQLiteHelper(context)
    private var database: SQLiteDatabase = dbHelper.writableDatabase


    fun getListEvent(): List<Event> {
        var cursor: Cursor? = null
        val sessions = ArrayList<Event>()
        try {
            open()
            cursor =
                database.query(TABLE_EVENT, null, null, null, null, null, null)

            if (cursor != null) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val session = cursorToEvent(cursor) ?: continue
                    sessions.add(session)
                    cursor.moveToNext()
                }
            }
            return sessions

        } catch (e: Exception) {
            return sessions
        } finally {
            if (cursor != null && !cursor.isClosed) cursor.close()
            close()
        }
    }

    fun insertEvent(event: Event): Boolean {
        open()
        return try {
            val values = ContentValues()
            values.put(COLUMN_TIME, event.timestamp)
            values.put(COLUMN_ACTION, event.action)
            values.put(COLUMN_DATA, event.params.toString())
            val id = database.insertOrThrow(TABLE_EVENT, null, values)
            id != -1L
        } catch (ex: Exception) {
            Log.e("EventDataSource", "insertEvent - $ex")
            false
        } finally {
            close()
        }
    }

    fun insertEvent(event: List<Event>) {
        for (e in event) insertEvent(e)
    }

    fun deleteEvent(startTime: Long) {
        try {
            open()
            database.delete(
                TABLE_EVENT,
                "$COLUMN_TIME=?",
                arrayOf("" + startTime)
            )
        } catch (ex: Exception) {
            Log.e("EventDataSource", "deleteEvent - $ex")
        } finally {
            close()
        }
    }

    fun deleteEvent(event: List<Event>) {
        for (e in event) deleteEvent(e.timestamp)
    }

    fun clearEventsTable() {
        try {
            open()
            database.delete(TABLE_EVENT, null, null)
        } catch (ex: Exception) {
            Log.e("EventDataSource - clearEventsTable", "$ex")
        } finally {
            close()
        }
    }

    //#region private supportive method
    private fun open() {
        database = dbHelper.writableDatabase
    }

    private fun close() {
        dbHelper.close()
    }

    private fun cursorToEvent(cursor: Cursor): Event? {
        return try {
            val timestamp = java.lang.Long.parseLong(cursor.getString(0))
            val action = cursor.getString(1)
            val paramsJSONObject = JSONObject(cursor.getString(2))
            val params = UtilsJSON.jsonObjectToMap(paramsJSONObject)
            Event(action, params, timestamp)
        } catch (e: JSONException) {
            Log.w("cursorToEvent", e)
            null
        }
    }
    //#endregion
}