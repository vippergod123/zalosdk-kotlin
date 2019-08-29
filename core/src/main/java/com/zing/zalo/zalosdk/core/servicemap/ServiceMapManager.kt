package com.zing.zalo.zalosdk.core.servicemap

import android.content.Context
import android.os.AsyncTask
import android.text.TextUtils
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.http.HttpClientFactory
import com.zing.zalo.zalosdk.core.http.HttpMethod
import com.zing.zalo.zalosdk.core.log.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.*

object ServiceMapManager
{
     const val KEY_URL_OAUTH = "oauth_http_s"
     const val KEY_URL_GRAPH = "graph_http_s"
     const val KEY_URL_CENTRALIZED = "centralized_http_s"
     
     private const val URL_OAUTH = "https://oauth.zaloapp.com"
     private const val URL_GRAPH = "https://graph.zaloapp.com"
     private const val URL_CENTRALIZED = "https://centralized.zaloapp.com"
     
     private const val URL_DEV_OAUTH = "https://dev.oauth.zaloapp.com"
     private const val URL_DEV_GRAPH = URL_GRAPH
     private const val URL_DEV_CENTRALIZED = URL_CENTRALIZED
     
     private const val ONE_DATE_DURATION = (1000 * 60 * 60 * 24).toLong()
     private var expireTime: Long = -1
     
     private val SERVICE_MAP_URLS = arrayOf("https://mp3.zing.vn/zdl/service_map_all.bin", "https://zaloapp.com/zdl/service_map_all.bin", "https://news.zing.vn/zdl/service_map_all.bin", "https://n.zing.vn/zdl/service_map_all.bin", "https://srv.mp3.zing.vn/zdl/service_map_all.bin")
     
     private var urls: MutableMap<String, String> = HashMap()
     private var httpClientFactory = HttpClientFactory()
     
     private var storage: ServiceMapStorage? = null
     
     
     init
     {
          if (Constant.DEV_MODE)
          {
               urls[KEY_URL_OAUTH] = URL_DEV_OAUTH
               urls[KEY_URL_GRAPH] = URL_DEV_GRAPH
               urls[KEY_URL_CENTRALIZED] = URL_DEV_CENTRALIZED
          }
          else
          {
               urls[KEY_URL_OAUTH] = URL_OAUTH
               urls[KEY_URL_GRAPH] = URL_GRAPH
               urls[KEY_URL_CENTRALIZED] = URL_CENTRALIZED
          }
     }
     
     fun load(context: Context)
     {
          storage = getStorage(context)
     
          updateMapUrlsFromPreference()
          if (isExpiredTime() && !Constant.DEV_MODE)
          {
               val serviceTask = DownloadServiceMapFilesAsyncTask(httpClientFactory, object : ServiceMapListener
               {
                    override fun receiveJSONObject(dataObject: JSONObject?)
                    {
                         if (dataObject == null)
                         {
                              Log.v("Service map not found!")
                              return
                         }
                         else
                         {
                              try
                              {
                                   val urlOauth = dataObject.getJSONArray(KEY_URL_OAUTH).getString(0)
                                   val urlGraph = dataObject.getJSONArray(KEY_URL_GRAPH).getString(0)
                                   val urlCentralized = dataObject.getJSONArray(KEY_URL_CENTRALIZED).getString(0)
     
                                   updateMapUrls(urlOauth, urlGraph, urlCentralized)
                              }
                              catch (e: JSONException)
                              {
                                   Log.e(e.message.toString())
                              }
                         }
                    }
               })
               serviceTask.execute()
     
          }
     }
     
     fun urlFor(key: String, path: String): String
     {
          val url = urls[key]
          if (TextUtils.isEmpty(url))
          {
               return path
          }
          
          return if (!url!!.endsWith("/") && !path.startsWith("/"))
          {
               "$url/$path"
          }
          else url + path
     }
     
     
     private fun updateMapUrls(urlOauth: String, urlGraph: String, urlCentralized: String)
     {
          storage!!.setKeyUrlCentralized(urlCentralized)
          storage!!.setKeyUrlGraph(urlGraph)
          storage!!.setKeyUrlOauth(urlOauth)
          
          urls[KEY_URL_OAUTH] = urlOauth
          urls[KEY_URL_GRAPH] = urlGraph
          urls[KEY_URL_CENTRALIZED] = urlCentralized
          
          val currentTimeMillis = System.currentTimeMillis()
          expireTime = currentTimeMillis + ONE_DATE_DURATION
          storage!!.setExpireTime(expireTime)
     }
     
     private fun updateMapUrlsFromPreference()
     {
     
          val urlOauth = storage!!.getKeyUrlOauth()
          val urlGraph = storage!!.getKeyUrlGraph()
          val urlCentralized = storage!!.getKeyUrlCentralized()
          
          if (!TextUtils.isEmpty(urlOauth) && !TextUtils.isEmpty(urlGraph) && !TextUtils.isEmpty(urlCentralized))
          {
               urls[KEY_URL_OAUTH] = urlOauth!!
               urls[KEY_URL_GRAPH] = urlGraph!!
               urls[KEY_URL_CENTRALIZED] = urlCentralized!!
          }
     }
     
     fun isExpiredTime(): Boolean
     {
          val currentTimeMillis = System.currentTimeMillis()
          
          if (expireTime == -1L)
          {
               expireTime = if (storage!!.getExpireTime() != 0L) storage!!.getExpireTime()
               else currentTimeMillis
          }
          
          return currentTimeMillis >= expireTime
     }
     
     fun setHttpClientFactory(httpClientFactory: HttpClientFactory)
     {
          this.httpClientFactory = httpClientFactory
     }

     fun setServiceMapStorage(mStorage: ServiceMapStorage)
     {
          this.storage = mStorage
     }
     
     fun getStorage(context: Context): ServiceMapStorage
     {
          if (storage == null)
          {
               storage = ServiceMapStorage(context)
          }
          return storage!!
     }
     
     
     class DownloadServiceMapFilesAsyncTask(private val httpClientFactory: HttpClientFactory, private val listener: ServiceMapListener) :
          AsyncTask<String?, Void, JSONObject>()
     {


         override fun doInBackground(vararg p0: String?): JSONObject?
          {
               val input: InputStream? = null
               val connection: HttpURLConnection? = null
               
               for (serviceMapUrl in SERVICE_MAP_URLS)
               {
                    try
                    {
                         val request = httpClientFactory.newRequest(HttpMethod.GET, serviceMapUrl)
                        val str = request.send().getText() ?: return null
                        val decryptString = ServiceMapTools.decryptString(str)
                         return JSONObject(decryptString)
                    }
                    catch (e: Exception)
                    {
                         Log.e(e)
                    }
                    finally
                    {
                         try
                         {
                              input?.close()
                         }
                         catch (ignored: IOException)
                         {
                              Log.e(ignored)
                         }
                         
                         connection?.disconnect()
                    }
               }
               return null
          }
          
          override fun onPostExecute(result: JSONObject?)
          {
               super.onPostExecute(result)
               listener.receiveJSONObject(result)
          }
     }
}