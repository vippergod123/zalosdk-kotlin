package com.zing.zalo.zalosdk.core.http

import android.text.TextUtils
import com.zing.zalo.zalosdk.core.log.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class HttpClientResponse : IHttpResponse {

    var path: String? = null
    var inputStream: InputStream? = null
    override fun getStatusCode(): Int {
        if (TextUtils.isEmpty(path)) return 404
        val connection = URL(path).openConnection() as HttpURLConnection
        return connection.responseCode
    }

    override fun getText(): String? {
        if (inputStream == null)
            return null
        try {

            val bufferReader = BufferedReader(InputStreamReader(inputStream))

            val sb = StringBuilder()
            var line: String? = bufferReader.readLine()
            while (line != null) {
                sb.append(line)
                line = bufferReader.readLine()
            }
            bufferReader.close()

            Log.v("connect server RESPONSE getText() : $sb")
            return sb.toString()
        } catch (ex: Exception) {
            Log.w(ex.toString())
            return null
        }
    }


}