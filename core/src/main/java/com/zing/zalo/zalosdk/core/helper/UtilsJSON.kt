package com.zing.zalo.zalosdk.core.helper

import org.json.JSONArray

object UtilsJSON {
    fun<T> listToJSONArray(array:ArrayList<T>):JSONArray {
        val jsonArray = JSONArray()

        for (str in array) {
            jsonArray.put(str)
        }

        return jsonArray
    }

    @Suppress("UNCHECKED_CAST")
    fun<T> jsonArrayToArrayList(jsonArray: JSONArray):ArrayList<T> {
        val listData = arrayListOf<T>()
        for (i in 0 until jsonArray.length()) {
            val element = jsonArray.get(i) as T
            listData.add(element)
        }
        return listData
    }
}