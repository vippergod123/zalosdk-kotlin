package com.zing.zalo.zalosdk.demo

import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.json.JSONObject

object TestHelper {

    @Throws(UiObjectNotFoundException::class)
    fun getUiObject(viewID: String): UiObject? {
        val containerLinearLayout = UiScrollable(
            UiSelector()
                .className("android.widget.LinearLayout")
        )

        return containerLinearLayout.getChild(
            UiSelector()
                .resourceId("com.zing.zalo.zalosdk.demo:id/$viewID")
        )
    }



}