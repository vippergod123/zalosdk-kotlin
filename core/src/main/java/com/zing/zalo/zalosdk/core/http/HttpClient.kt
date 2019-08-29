package com.zing.zalo.zalosdk.core.http

class HttpClient : IHttpClient {
    private lateinit var path: String
    private lateinit var method: HttpMethod

    override fun send(request: HttpClientRequest): HttpClientResponse {
        path = request.path
        method = request.method

        val response = HttpClientResponse()
        response.inputStream = request.getResponse()
        response.path = path
        return response
    }

//    fun getStatusCode(): Int {
//        val connection = URL(path).openConnection() as HttpURLConnection
//        return connection.responseCode
//    }
//
//    fun getText(): String? {
//        try {
//            val inputStream =
//
//            val bufferReader = BufferedReader(InputStreamReader(inputStream))
//
//            val sb = StringBuilder()
//            var line: String? = bufferReader.readLine()
//            while (line != null) {
//                sb.append(line)
//                line = bufferReader.readLine()
//            }
//            bufferReader.close()
//
//            Log.v("connect server RESPONSE getText() : $sb")
//            return sb.toString()
//        } catch (ex: Exception) {
//            Log.w(ex.toString())
//            return null
//        }
//    }

}