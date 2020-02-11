package com.zing.zalo.zalosdk.kotlin.core.http

import com.zing.zalo.zalosdk.kotlin.core.log.Log
import java.net.HttpURLConnection
import java.net.URL

class HttpClient(private val baseUrl: String) : IHttpClient {

    override fun send(request: IHttpRequest): HttpResponse {
        val response = HttpResponse(request)

        try {
            val url = URL(request.getUrl(baseUrl))
            val conn = url.openConnection() as HttpURLConnection
            val headers = request.getHeaders()
            for (key in headers.keys) {
                conn.setRequestProperty(key, headers[key])
            }

            when (request.getMethod()) {
                HttpMethod.POST-> {
                    val postRequest = request as IPostHttpRequest
                    conn.requestMethod = "POST"
                    conn.doOutput = true // Triggers POST.

                    val output = conn.outputStream
                    postRequest.encodeBody(output)
                    output.flush()
                    output.close()
                }
                HttpMethod.GET -> {
                    conn.requestMethod = "GET"
                }
            }

            response.responseCode = conn.responseCode
            response.responseStream = conn.inputStream
        } catch (ex: Exception) {
            Log.e("HttpClient", ex)
        }

        return response;
    }

}