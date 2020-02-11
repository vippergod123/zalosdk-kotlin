package com.zing.zalo.zalosdk.kotlin.openapi

import org.json.JSONObject

interface ZaloOpenApiCallback {
    fun onResult(data: JSONObject?)
}

interface ZaloPluginCallback {
    fun onResult(isSuccess: Boolean, error_code: Int, message: String?, jsonData: String?)
}