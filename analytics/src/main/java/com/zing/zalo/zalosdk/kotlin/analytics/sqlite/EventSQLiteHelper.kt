package com.zing.zalo.zalosdk.kotlin.analytics.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.zing.zalo.zalosdk.kotlin.analytics.Constant
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.COLUMN_ACTION
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.COLUMN_DATA
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.COLUMN_TIME
import com.zing.zalo.zalosdk.kotlin.analytics.EventDatabase.TABLE_EVENT
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.log.Log

class EventSQLiteHelper(
    context: Context
) : SQLiteOpenHelper(
    context,
    "${Utils.getCurrentProcessName(context)}.${Constant.eventDatabase.NAME}",
    null,
    Constant.eventDatabase.VERSION
) {

    companion object {
        private val DATABASE_CREATE_QUERY = ("create table "
                + TABLE_EVENT + "("
                + COLUMN_TIME + " integer not null, "
                + COLUMN_ACTION + " text not null, "
                + COLUMN_DATA + " text not null);")
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(DATABASE_CREATE_QUERY)
        Log.d("EventSQLiteHelper", "onCreate table - $TABLE_EVENT")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_EVENT")
        onCreate(db)
        Log.d("EventSQLiteHelper", "onUpgrade table - $TABLE_EVENT")
    }


}

