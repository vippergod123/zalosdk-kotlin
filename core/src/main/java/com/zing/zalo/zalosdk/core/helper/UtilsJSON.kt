package com.zing.zalo.zalosdk.core.helper

import org.json.JSONArray

object UtilsJSON {
    fun <T> jsonArrayToArrayList(jsonArray: JSONArray): ArrayList<T> {

        val packageNames = arrayListOf<T>()

        for (i in 0 until jsonArray.length()) {
            @Suppress("UNCHECKED_CAST")
            val data = jsonArray.get(i) as T
            packageNames.add(data)
        }
        return packageNames
    }
}