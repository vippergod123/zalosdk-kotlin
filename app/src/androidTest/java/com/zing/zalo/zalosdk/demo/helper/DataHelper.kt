package com.zing.zalo.zalosdk.demo.helper

import com.zing.zalo.zalosdk.kotlin.analytics.model.Event

object DataHelper {
    fun mockEvent(): Event {
        val timeStamp = System.currentTimeMillis()
        val action = "action-$timeStamp"
        val params = mutableMapOf<String,String>()


        params["name"] = "datahelper-$timeStamp"
        params["age"] = timeStamp.toString()
        return Event(action,params,timeStamp)
    }
}