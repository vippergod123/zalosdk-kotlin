package com.zing.zalo.zalosdk.core.http

enum class HttpMethod {
    GET,
    POST,
    POST_MULIIPART
}

interface IHttpRequest {
    fun setPath(path: String)
    fun addParameter(key: String, value: String)
    fun addQueryStringParameter(key: String, value: String)
    fun addHeader(key: String, value: String)
    fun setMethod(httpMethod: HttpMethod)
}

interface IMultipartHttpRequest : IHttpRequest {
    fun addFileParameter(key: String, fileName: String, data: ByteArray)
}

interface IHttpResponse {
    fun getStatusCode(): Int
    fun getText(): String?
}

interface IHttpClient {
    fun send(): IHttpResponse
}