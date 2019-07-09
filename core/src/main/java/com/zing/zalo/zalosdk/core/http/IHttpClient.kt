package com.zing.zalo.zalosdk.core.http

import org.json.JSONObject

enum class HttpMethod {
    GET, POST
}

interface IHttpRequest {
    fun setPath(path: String)
    fun addParameter(key: String, value: String)
    fun addQueryStringParameter(key: String, value: String)
    fun addHeader(key: String, value: String)
    fun setMethod(method: HttpMethod)
}

interface IMultipartHttpRequest : IHttpRequest {
    fun addFileParameter(key: String, fileName: String, data: ByteArray)
}

interface IHttpResponse {
    fun getStatusCode(): Int
    fun getJSONData(): JSONObject
}

interface IHttpClient {
    fun send(request: IHttpRequest) : IHttpResponse
}