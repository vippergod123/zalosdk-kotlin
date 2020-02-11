package com.zing.zalo.zalosdk.kotlin.analytics

import com.zing.zalo.zalosdk.kotlin.analytics.model.Event

interface IEventTracker {
    fun addEvent(action: String, params: Map<String, String>, timestamp: Long)
    fun addEvent(event: Event)
    fun dispatchEvent()
    fun dispatchEventImmediate(event: Event?)
}

interface EventTrackerListener {
    fun dispatchComplete() {}
}