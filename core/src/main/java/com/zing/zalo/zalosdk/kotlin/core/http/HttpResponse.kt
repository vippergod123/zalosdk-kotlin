package com.zing.zalo.zalosdk.kotlin.core.http

import com.zing.zalo.zalosdk.kotlin.core.log.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class HttpResponse(var request: IHttpRequest) : IHttpResponse {
    var responseStream: InputStream? = null
    var responseCode: Int = 0

    override fun getStatusCode(): Int {
        return responseCode
    }

    override fun getText(): String? {
        if (responseStream == null)
            return null

        try {
            val bufferReader = BufferedReader(InputStreamReader(responseStream!!))

            val sb = StringBuilder()
            var line: String? = bufferReader.readLine()
            while (line != null) {
                sb.append(line)
                line = bufferReader.readLine()
            }
            bufferReader.close()

            Log.d(  "HttpResponse","$sb")
            return sb.toString()
        } catch (ex: Exception) {
            Log.e("HttpResponse",ex)
            return null
        }
    }

    override fun getJSON(): JSONObject? {
        val text = getText() ?: return null
        try {
            return JSONObject(text)
        } catch (ex: JSONException) {
            Log.w("getJSON", ex)
        }

         return null;
    }
}