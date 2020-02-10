package com.zing.zalo.zalosdk.kotlin.core.servicemap

import android.content.Context
import android.text.TextUtils
import androidx.annotation.Keep
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.module.BaseModule
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject

class ServiceMapManager private constructor() : BaseModule() {
    @Keep
    companion object {
        private val instance = ServiceMapManager()
        fun getInstance(): ServiceMapManager {
            return instance
        }

        const val KEY_URL_OAUTH = "oauth_http_s"
        const val KEY_URL_OPENAPI = "openapi_http_s"
        const val KEY_URL_GRAPH = "graph_http_s"
        const val KEY_URL_CENTRALIZED = "centralized_http_s"

        private const val URL_OAUTH = "https://oauth.zaloapp.com"
        private const val URL_OPENAPI = "https://openapi.zalo.me"
        private const val URL_GRAPH = "https://graph.zaloapp.com"
        private const val URL_CENTRALIZED = "https://centralized.zaloapp.com"

        private const val URL_DEV_OAUTH = "https://dev.oauth.zaloapp.com"
        private const val URL_DEV_OPENAPI = URL_OPENAPI
        private const val URL_DEV_GRAPH = URL_GRAPH
        private const val URL_DEV_CENTRALIZED = URL_CENTRALIZED

        private const val ONE_DATE_DURATION = (1000 * 60 * 60 * 24).toLong()
        private val SERVICE_MAP_URLS = arrayOf(
            "https://mp3.zing.vn/zdl/service_map_all.bin",
            "https://zaloapp.com/zdl/service_map_all.bin",
            "https://news.zing.vn/zdl/service_map_all.bin",
            "https://n.zing.vn/zdl/service_map_all.bin",
            "https://srv.mp3.zing.vn/zdl/service_map_all.bin"
        )

    }

    private var expireTime: Long = -1L
    private var urls: MutableMap<String, String> = HashMap()

    var httpClient = HttpClient("")
    lateinit var storage: ServiceMapStorage

    private val job: Job = Job()
    var scope = CoroutineScope(Dispatchers.IO + job)

    override fun onStart(context: Context) {
        storage = getStorage(context)
        setUpDefaultUrl()

        updateMapUrlsFromPreference()
        if (isExpiredTime() && !Constant.DEV_MODE) {
            scope.launch {
                val dataObject = getServiceMapFiles()
                if (dataObject == null) {
                    Log.v("Service map not found!")
                } else {
                    try {
                        val urlOauth = dataObject.getJSONArray(KEY_URL_OAUTH).getString(0)
                        val urlGraph = dataObject.getJSONArray(KEY_URL_GRAPH).getString(0)
                        val urlCentralized =
                            dataObject.getJSONArray(KEY_URL_CENTRALIZED).getString(0)

                        updateMapUrls(urlOauth, urlGraph, urlCentralized)
                    } catch (e: JSONException) {
                        Log.e("ServiceMapManager: load()", e)
                    }
                }
            }
        }
    }

    fun urlFor(key: String): String {
        if (urls.isEmpty())
            setUpDefaultUrl()
        return urls[key] ?: ""
    }

    fun urlFor(key: String, path: String): String {
        val url = urls[key]
        if (TextUtils.isEmpty(url)) {
            return path
        }

        return if ((url?.endsWith("/")) == false && !path.startsWith("/")) {
            "$url/$path"
        } else url + path
    }


    private fun updateMapUrls(urlOauth: String, urlGraph: String, urlCentralized: String) {
        storage.setKeyUrlCentralized(urlCentralized)
        storage.setKeyUrlGraph(urlGraph)
        storage.setKeyUrlOauth(urlOauth)

        urls[KEY_URL_OAUTH] = urlOauth
        urls[KEY_URL_GRAPH] = urlGraph
        urls[KEY_URL_CENTRALIZED] = urlCentralized

        val currentTimeMillis = System.currentTimeMillis()
        expireTime = currentTimeMillis + ONE_DATE_DURATION
        storage.setExpireTime(expireTime)
    }

    private fun setUpDefaultUrl() {
        if (Constant.DEV_MODE) {
            urls[KEY_URL_OAUTH] = URL_DEV_OAUTH
            urls[KEY_URL_OPENAPI] = URL_DEV_OPENAPI
            urls[KEY_URL_GRAPH] = URL_DEV_GRAPH
            urls[KEY_URL_CENTRALIZED] = URL_DEV_CENTRALIZED
        } else {
            urls[KEY_URL_OAUTH] = URL_OAUTH
            urls[KEY_URL_GRAPH] = URL_GRAPH
            urls[KEY_URL_CENTRALIZED] = URL_CENTRALIZED
            urls[KEY_URL_OPENAPI] = URL_OPENAPI
        }
    }

    private fun updateMapUrlsFromPreference() {
        val urlOauth = storage.getKeyUrlOauth() ?: ""
        val urlGraph = storage.getKeyUrlGraph() ?: ""
        val urlCentralized = storage.getKeyUrlCentralized() ?: ""

        if (!TextUtils.isEmpty(urlOauth) && !TextUtils.isEmpty(urlGraph) && !TextUtils.isEmpty(
                urlCentralized
            )
        ) {
            urls[KEY_URL_OAUTH] = urlOauth
            urls[KEY_URL_GRAPH] = urlGraph
            urls[KEY_URL_CENTRALIZED] = urlCentralized
        }
    }

    private fun isExpiredTime(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()

        if (expireTime == -1L) {
            expireTime = if (storage.getExpireTime() != 0L)
                storage.getExpireTime()
            else currentTimeMillis
        }

        return currentTimeMillis >= expireTime
    }

    private fun getStorage(context: Context): ServiceMapStorage {
        if (!::storage.isInitialized) {
            storage = ServiceMapStorage(context)
        }
        return storage
    }

    private suspend fun getServiceMapFiles(): JSONObject? =
        withContext(scope.coroutineContext) {
            for (serviceMapUrl in SERVICE_MAP_URLS) {
                try {
                    val request = HttpGetRequest(serviceMapUrl)
                    val response = httpClient.send(request)
                    val str = response.getText() ?: ""
                    val decryptString = ServiceMapTools.decryptString(str)
                    return@withContext JSONObject(decryptString)
                } catch (e: Exception) {
                    Log.w("getServiceMapFiles", e)
                }
            }
            return@withContext null
        }

}