package com.zing.zalo.zalosdk.core.http

import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.log.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class HttpClientRequest(method: HttpMethod, mPath: String) : IHttpRequest,
    IMultipartHttpRequest {

    private var mParams: MutableMap<String, String> = mutableMapOf()
    private var mQueryParams: MutableMap<String, String> = mutableMapOf()
    private var mHeader: MutableMap<String, String> = mutableMapOf()
    private var fileName: String? = null
    private var fileKey: String? = null
    private var data: ByteArray? = null

    var method: HttpMethod = method
    var path: String = mPath

    companion object {
        private const val TWO_HYPHENS = "--"
        private const val BOUNDARY = "*****"
        private const val LINE_END = "\r\n"
    }

    init {
        this.addHeader("SDK-Source", "ZaloSDK-" + Constant.VERSION)
    }

    //#region IHttpRequest
    override fun addParameter(key: String, value: String) {
        mParams[key] = value
    }

    override fun addQueryStringParameter(key: String, value: String) {
        mQueryParams[key] = value
    }

    override fun addHeader(key: String, value: String) {
        mHeader[key] = value
    }


    override fun addFileParameter(key: String, fileName: String, data: ByteArray) {
        TODO("addFileParameter") //To change body of created functions use File | Settings | File Templates.
    }
    //#endregion

    //#region method
    fun getResponse(): InputStream? {
        try {
            when (method) {
                HttpMethod.POST -> {
                    val connection = URL(path).openConnection() as HttpURLConnection

                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Accept-Charset", "UTF-8")
                    connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded;charset=" + "UTF-8"
                    )
                    connection.doOutput = true // Triggers POST.

                    for (key in mHeader.keys) {
                        connection.setRequestProperty(key, mHeader[key])
                    }

                    val output = connection.outputStream
                    output.write(getParamsString().toByteArray(charset("UTF-8")))
                    output.flush()
                    output.close()
                    return connection.inputStream
                }
                HttpMethod.GET -> {//GET
                    val sb = StringBuilder(path)
                    val queryParam = getParamsString()

                    if (sb.toString().endsWith("?"))
                        sb.append(queryParam)
                    else {
                        sb.append("?")
                        sb.append(queryParam)
                    }

                    val connection: HttpURLConnection? = URL(sb.toString()).openConnection() as HttpURLConnection?
                    connection?.requestMethod = "GET"
                    for (key in mHeader.keys) {
                        connection?.setRequestProperty(key, mHeader[key])
                    }
                    return connection?.inputStream
                }
                else -> {
                    // Open a HTTP connection to the URL

                    //Todo: next task
                    val connection = URL(path).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.doOutput = true
                    connection.useCaches = false
                    connection.connectTimeout = 2 * 60 * 1000
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Connection", "Keep-Alive")
                    connection.setRequestProperty("ENCTYPE", "multipart/form-data")

                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$BOUNDARY")
                    connection.setRequestProperty("uploaded_file", fileName)

                    for (key in mHeader.keys) {
                        connection.setRequestProperty(key, mHeader[key])
                    }

                    val dos = DataOutputStream(connection.outputStream)
                    for (key in mParams.keys) {
                        dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
                        dos.writeBytes("Content-Disposition: form-data; name=\"$key\"$LINE_END")
                        dos.writeBytes("Content-Type: text/plain; charset=UTF-8$LINE_END")
                        dos.writeBytes(LINE_END)
                        dos.write(mParams[key]!!.toByteArray(charset("UTF-8")))
                        dos.writeBytes(LINE_END)
                    }

                    dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
                    dos.writeBytes("Content-Disposition: form-data; name=$fileKey; filename=$fileName$LINE_END")
                    dos.writeBytes(LINE_END)
                    dos.write(data)

                    dos.writeBytes(LINE_END)
                    dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END)
                    dos.flush()
                    dos.close()

                    val responseCode = connection.responseCode
                    Log.d("POST_MULTIPART $responseCode")
                    if (responseCode >= 300) {
                        val newLine = System.getProperty("line.separator")
                        val reader = BufferedReader(InputStreamReader(connection.errorStream))
                        val result = StringBuilder()
                        val line: String = reader.readLine()
                        var flag = false
                        while (line.isNotEmpty()) {
                            result.append(if (flag) newLine else "").append(line)
                            flag = true
                        }
                        Log.d(result.toString())
                        return null
                    }

                    return connection.inputStream
                }

            }
        } catch (ex: Exception) {
            Log.w(ex)
        }
        return null
    }

    //# endregion


    private fun getParamsString(): String {
        val sb = StringBuilder("")
        if (mQueryParams.isNotEmpty()) {
            for (key in mQueryParams.keys) {
                try {
                    sb.append(URLEncoder.encode(key, "UTF-8"))
                    sb.append("=")
                    sb.append(URLEncoder.encode(mQueryParams[key], "UTF-8"))
                    sb.append("&")
                } catch (e: Exception) {
                    Log.w(e)
                }

            }
        }
        return sb.toString()
    }


}
