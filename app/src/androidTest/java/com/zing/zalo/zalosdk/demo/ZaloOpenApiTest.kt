package com.zing.zalo.zalosdk.demo

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiObjectNotFoundException
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ZaloOpenApiTest: AppBase() {

    private lateinit var contextApp: Context

    @Before
    @Throws(IOException::class, UiObjectNotFoundException::class)
    override fun startApp() {
        super.startApp()

        MockKAnnotations.init(this, relaxUnitFun = true)
        contextApp = context.applicationContext
        clickButtonWithText("Open Api")
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun getProfile() {
        clickButtonWithText("Get Profile")
        verifyResultCallBack()
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun getFriendListUsedApp() {
        clickButtonWithText("Get Friend List Used App")
        verifyResultCallBack()
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun getFriendListInvitable() {
        clickButtonWithText("Get Friend List Invitable")
        verifyResultCallBack()
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun inviteFriendUseApp() {
        clickButtonWithText("Invite Friend Use App")
        verifyResultCallBack()
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun postToWall() {
        clickButtonWithText("Post To Wall")
        verifyResultCallBack()
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun sendMsgToFriend() {
        clickButtonWithText("Send Message To Friend")
        verifyResultCallBack()
    }

    //#region private supportive method
    private fun verifyResultCallBack() {
        val callbackTextView = TestHelper.getUiObject("callback_text_view")
        val result = callbackTextView?.text

        assertThat(isJSON(result)).isEqualTo(true)
    }
    private fun isJSON(str: String?): Boolean {
        if (str == null ) return false
        try {
            JSONObject(str)
        } catch (ex: JSONException) {
            try {
                JSONArray(str)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

    //#endregion
}