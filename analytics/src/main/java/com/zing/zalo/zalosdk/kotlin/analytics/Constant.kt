package com.zing.zalo.zalosdk.kotlin.analytics

import androidx.annotation.Keep
import com.zing.zalo.zalosdk.kotlin.core.Constant

@Keep
object Constant {
    val core = Constant
    val eventDatabase = EventDatabase

    const val DEFAULT_MAX_EVENTS_STORED = 100
}


@Keep
object EventDatabase {
    const val NAME = "session.db"
    const val VERSION = 1
    const val TABLE_EVENT = "events"
    const val COLUMN_TIME = "time"
    const val COLUMN_ACTION = "action"
    const val COLUMN_DATA = "data"
}