package com.zing.zalo.zalosdk.kotlin.core.http

import org.json.JSONObject
import java.io.OutputStream

enum class HttpMethod {
    GET,
    POST
}

interface IHttpRequest {
    fun addQueryStringParameter(key: String, value: String)
    fun addQueryStringParameters(params: Map<String, String>)
    fun addHeader(key: String, value: String)
    fun getHeaders(): Map<String, String>
    fun getUrl(baseUrl: String): String
    fun getMethod(): HttpMethod
}

interface IPostHttpRequest : IHttpRequest{
    fun addParameter(key: String, value: String)
    fun addParameters(params: Map<String, String>)
    fun encodeBody(stream: OutputStream)
}

interface IMultipartHttpRequest : IPostHttpRequest {
    fun setFileParameter(key: String, name: String, data: ByteArray)
}

interface IHttpResponse {
    fun getStatusCode(): Int
    fun getText(): String?
    fun getJSON() : JSONObject?
}

interface IHttpClient {
    fun send(request: IHttpRequest): IHttpResponse
}