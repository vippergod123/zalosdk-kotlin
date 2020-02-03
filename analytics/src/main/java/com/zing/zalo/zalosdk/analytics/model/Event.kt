package com.zing.zalo.zalosdk.analytics.model

data class Event(var action: String, var params: Map<String, String>, var timestamp: Long)