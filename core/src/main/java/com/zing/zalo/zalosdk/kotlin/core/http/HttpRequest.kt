package com.zing.zalo.zalosdk.kotlin.core.http

import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.URLEncoder

abstract class BaseHttpRequest(val path: String) : IHttpRequest {
    var mQueryParams: MutableMap<String, String> = mutableMapOf()
    var mHeader: MutableMap<String, String> = mutableMapOf()

    init {
        //default header
        this.addHeader("SDK-Source", "ZaloSDK-" + Constant.VERSION)
        this.addHeader("Accept-Charset", "UTF-8")
    }

    override fun addQueryStringParameter(key: String, value: String) {
        mQueryParams[key] = value
    }

    override fun addQueryStringParameters(params: Map<String, String>) {
        mQueryParams.putAll(params)
    }

    override fun addHeader(key: String, value: String) {
        mHeader[key] = value
    }

    override fun getUrl(baseUrl: String): String {
        val sb = StringBuilder(baseUrl)

        if (baseUrl.isNotBlank() && !baseUrl.endsWith("/") && !path.startsWith("/")) {
            sb.append("/")
        }

        sb.append(path)

        if (mQueryParams.isEmpty()) {
            return sb.toString()
        }

        if (path.contains("?")) {
            sb.append("&")
        } else {
            sb.append("?")
        }

        for (key in mQueryParams.keys) {
            try {
                sb.append(URLEncoder.encode(key, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(mQueryParams[key], "UTF-8"))
                    .append("&")
            } catch (e: Exception) {
                Log.w("getUrl", e)
            }
        }

        if (sb.isNotEmpty() && sb.last() == '&') {
            sb.deleteCharAt(sb.length - 1)
        }

        return sb.toString()
    }

    override fun getHeaders(): Map<String, String> {
        return mHeader
    }
}

class HttpGetRequest(path: String) : BaseHttpRequest(path) {
    override fun getMethod(): HttpMethod {
        return HttpMethod.GET
    }
}

abstract class BasePostHttpRequest(path: String) : BaseHttpRequest(path), IPostHttpRequest {
    var params: MutableMap<String, String> = mutableMapOf()

    override fun getMethod(): HttpMethod {
        return HttpMethod.POST
    }

    override fun addParameter(key: String, value: String) {
        params[key] = value
    }

    override fun addParameters(params: Map<String, String>) {
        this.params.putAll(params)
    }
}


class HttpUrlEncodedRequest(path: String) : BasePostHttpRequest(path) {
    init {
        this.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
    }

    override fun encodeBody(stream: OutputStream) {
        val sb = StringBuilder()
        for (key in params.keys) {
            try {
                sb.append(URLEncoder.encode(key, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(params[key], "UTF-8"))
                    .append("&")
            } catch (e: Exception) {
                Log.w("HttpUrlEncodedRequest - encodeBody()", e)
            }
        }

        if (sb.isNotEmpty() && sb.last() == '&') {
            sb.deleteCharAt(sb.length - 1)
        }

        stream.write(sb.toString().toByteArray(charset("UTF-8")))
    }
}


class HttpMultipartRequest(path: String) : BasePostHttpRequest(path), IMultipartHttpRequest {
    private var fileKey: String? = null
    private var fileName: String? = null
    private var fileData: ByteArray? = null

    companion object {
        const val TWO_HYPHENS = "--"
        const val BOUNDARY = "*****"
        const val LINE_END = "\r\n"
    }

    init {
        this.addHeader("Connection", "Keep-Alive")
        this.addHeader("ENCTYPE", "multipart/form-data")
        this.addHeader("Content-Type", "multipart/form-data;boundary=$BOUNDARY")
    }

    override fun encodeBody(stream: OutputStream) {
        val dos = DataOutputStream(stream)
        for (key in params.keys) {
            val value = params[key] ?: continue

            dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
            dos.writeBytes("Content-Disposition: form-data; name=\"$key\"$LINE_END")
            dos.writeBytes("Content-Type: text/plain; charset=UTF-8${LINE_END}")
            dos.writeBytes(LINE_END)
            dos.write(value.toByteArray(charset("UTF-8")))
            dos.writeBytes(LINE_END)
        }

        if (fileData != null && fileKey != null) {
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
            dos.writeBytes("Content-Disposition: form-data; name=$fileKey; filename=$fileName${LINE_END}")
            dos.writeBytes(LINE_END)
            dos.write(fileData!!)
        }

        dos.writeBytes(LINE_END)
        dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END)
        dos.flush()
    }

    override fun setFileParameter(key: String, name: String, data: ByteArray) {
        fileKey = key
        fileName = name
        fileData = data
        this.addHeader("uploaded_file", name)
    }
}
