package com.zing.zalo.zalosdk.kotlin.core.servicemap

import android.content.Context
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage

open class ServiceMapStorage(context: Context) : Storage(context)
{

    private val prefKeyUrlAuth = Constant.sharedPreference.PREF_KEY_URL_AUTH
    private val prefKeyUrlCentralized = Constant.sharedPreference.PREF_KEY_URL_CENTRALIZED
    private val prefKeyUrlGraph = Constant.sharedPreference.PREF_KEY_URL_GRAPH
    private val prefKeyUrlExpireTime = Constant.sharedPreference.PREF_EXPIRE_TIME

     
     fun getKeyUrlOauth(): String?
     {
         return getString(prefKeyUrlAuth)
     }
     
     fun getKeyUrlCentralized(): String?
     {
         return getString(prefKeyUrlCentralized)
     }
     
     fun getKeyUrlGraph(): String?
     {
         return getString(prefKeyUrlGraph)
     }
     
     fun getExpireTime(): Long
     {
         return getLong(prefKeyUrlExpireTime)
     }
     
     fun setKeyUrlOauth(urlOauth: String)
     {
         setString(prefKeyUrlAuth, urlOauth)
     }
     
     fun setKeyUrlCentralized(urlGraph: String)
     {
         setString(prefKeyUrlCentralized, urlGraph)
     }
     
     fun setKeyUrlGraph(urlCentralized: String)
     {
         setString(prefKeyUrlGraph, urlCentralized)
     }
     
     fun setExpireTime(expireTime: Long)
     {
         setLong(prefKeyUrlExpireTime, expireTime)
     }
}