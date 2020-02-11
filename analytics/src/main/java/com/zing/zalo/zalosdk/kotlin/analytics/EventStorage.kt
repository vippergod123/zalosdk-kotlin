package com.zing.zalo.zalosdk.kotlin.analytics

import android.content.Context
import com.zing.zalo.zalosdk.kotlin.analytics.model.Event
import com.zing.zalo.zalosdk.kotlin.analytics.sqlite.EventDataSource
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.log.Log

class EventStorage(context: Context) : Storage(context) {


    private var eventDataSource = EventDataSource(context)

    companion object {
        var events: MutableList<Event> = mutableListOf()
    }

    fun addEvent(e: Event) {
        if (events.size >= Constant.DEFAULT_MAX_EVENTS_STORED) {
            Log.d(
                "EventStorage",
                "addEvent: exceed max number of events" +
                        " ${events.size} >" +
                        "${Constant.DEFAULT_MAX_EVENTS_STORED}"
            )
            return
        }
        events.add(e)
        eventDataSource.insertEvent(e)
    }


    fun loadEventsFromDevice(): List<Event> {
        events = eventDataSource.getListEvent() as MutableList<Event>
        return events
    }

    fun storeEventsToDevice() {
        //Clear All data -> add new data
        eventDataSource.clearEventsTable()
        eventDataSource.insertEvent(events)
        Log.d("storeEventsToDevice", "store events to device complete")
    }

    fun clearEventStorage() {
        events.clear()
        eventDataSource.clearEventsTable()
    }
}