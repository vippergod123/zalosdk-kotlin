package com.zing.zalo.zalosdk.kotlin.core.helper

import com.zing.zalo.zalosdk.kotlin.core.log.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


object UtilsJSON {
    fun <T> listToJSONArray(array: ArrayList<T>): JSONArray {
        val jsonArray = JSONArray()

        for (str in array) {
            jsonArray.put(str)
        }

        return jsonArray
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> jsonArrayToArrayList(jsonArray: JSONArray): ArrayList<T> {
        val listData = arrayListOf<T>()
        for (i in 0 until jsonArray.length()) {
            val element = jsonArray.get(i) as T
            listData.add(element)
        }
        return listData
    }


    fun mapToJSONObject(map: Map<String, String>?): JSONObject {
        val jsObj = JSONObject()

        if (map == null) {
            return jsObj
        }

        for (key in map.keys) {
            try {
                jsObj.put(key, map[key])
            } catch (e: JSONException) {
                Log.w("mapToJSONObject", e)
            }
        }
        return jsObj
    }

    fun jsonObjectToMap(jsObj: JSONObject?): Map<String, String> {
        if (jsObj == null)
            return emptyMap()
        val keys = jsObj.keys()
        val map = mutableMapOf<String, String>()

        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jsObj.optString(key)
        }
        return map
    }
}