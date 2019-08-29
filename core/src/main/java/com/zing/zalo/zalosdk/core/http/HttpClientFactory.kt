package com.zing.zalo.zalosdk.core.http

class HttpClientFactory {
    fun newRequest(type: HttpMethod, url: String): HttpClientRequest
     {
          return HttpClientRequest(type, url)
     }
}