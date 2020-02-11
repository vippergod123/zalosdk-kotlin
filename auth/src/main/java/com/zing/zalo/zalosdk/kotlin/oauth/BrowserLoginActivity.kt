package com.zing.zalo.zalosdk.kotlin.oauth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import org.json.JSONException
import org.json.JSONObject

class BrowserLoginActivity : Activity() {

    private lateinit var zaloSDK: ZaloSDK
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        zaloSDK = ZaloSDK(this)
        if (handleBrowserCallback()) {
            finish()
        }
    }

    private fun handleBrowserCallback(): Boolean {
        val data = intent.data
        if (data == null || data.query == null) return false

        val scheme = data.scheme
        if (scheme == null || !scheme.startsWith("zalo-" + AppInfo.getAppId(this))) {
            return false
        }

        val intent = Intent()
        val sError = data.getQueryParameter("error")

        if (sError != null && Integer.parseInt(sError) != 0) {
            intent.putExtra("error", Integer.parseInt(data.getQueryParameter("error")!!))
            intent.putExtra("data", "{}")
        } else if (data.getQueryParameter("code") != null) {
            intent.putExtra("uid", java.lang.Long.parseLong(data.getQueryParameter("uid")!!))
            intent.putExtra("code", data.getQueryParameter("code"))

            val extra = JSONObject()
            val extraData = JSONObject()
            try {
                extraData.put("display_name", data.getQueryParameter("display_name"))
                extra.put("data", extraData)
            } catch (ignored: JSONException) {
            }

            intent.putExtra("data", extra.toString())
        }

        zaloSDK.onActivityResult(
            this,
            Constant.ZALO_AUTHENTICATE_REQUEST_CODE,
            0,
            intent
        )
        return true
    }
}