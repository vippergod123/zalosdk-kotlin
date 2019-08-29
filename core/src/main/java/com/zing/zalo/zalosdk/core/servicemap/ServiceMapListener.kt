package com.zing.zalo.zalosdk.core.servicemap

import org.json.JSONObject

interface ServiceMapListener
{
     fun receiveJSONObject(dataObject: JSONObject?)
}