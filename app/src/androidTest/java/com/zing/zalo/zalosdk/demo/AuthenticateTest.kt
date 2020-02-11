package com.zing.zalo.zalosdk.demo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthUtils
import com.zing.zalo.zalosdk.kotlin.oauth.WebLoginActivity
import com.zing.zalo.zalosdk.kotlin.core.SharedPreferenceConstant
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager
import io.mockk.MockKAnnotations
import org.hamcrest.core.IsNull.notNullValue
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AuthenticateTest : AppBase() {

    private lateinit var contextApp: Context

    @Before
    @Throws(IOException::class, UiObjectNotFoundException::class)
    override fun startApp() {
        super.startApp()

        MockKAnnotations.init(this, relaxUnitFun = true)
        contextApp = context.applicationContext
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun checkAppLoginSuccess() {
        clickButtonWithText("Check App Login")
        Runtime.getRuntime().exec(arrayOf("am", "force-stop", APP_PACKAGE_NAME))
        startApp()
        verifyCheckIsAppLogin()
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun loginZaloViaAppSuccessAndPersistentOauthCode() {
        clickButtonWithText("Login Mobile")
        clickButtonWithResId("com.zing.zalo:id/authorization_app_action_accept_btn")

        Runtime.getRuntime().exec(arrayOf("am", "force-stop", APP_PACKAGE_NAME))
        startApp()
        verifyLogin()
    }

    @Test
    @Throws(UiObjectNotFoundException::class)
    fun loginZaloViaAppCancelBackToApp() {
        clickButtonWithText("Login Mobile")
        clickButtonWithResId("com.zing.zalo:id/authorization_app_action_cancel_btn")
        assertEquals(device.currentPackageName, APP_PACKAGE_NAME)
        clickButtonWithText("VALIDATE OAUTH CODE")
        verifyLoginFailed()
    }

    @Test
    @Throws(UiObjectNotFoundException::class, InterruptedException::class)
    fun loginZaloViaWebSuccess() {
        //#1 Setup mock


        val wakeUp =
            Storage(context).privateSharedPreferences(SharedPreferenceConstant.PREFS_NAME_WAKEUP)
        wakeUp.setBoolean(SettingsManager.KEY_SETTINGS_OUT_APP_LOGIN, false)
        val setting = SettingsManager.getInstance()
        setting.wakeUpStorage = wakeUp
        AuthUtils.settingsManager = setting
        //#2

        val inst = InstrumentationRegistry.getInstrumentation()
        val monitor = inst.addMonitor("com.zing.zalo.zalosdk.kotlin.oauth.WebLoginActivity", null, false)

        clickButtonWithText("Login Web")

        val uid: Long = 1234
        val oauthCode = "abcd"

        val activity = monitor.waitForActivityWithTimeout(10000) as WebLoginActivity
        val webView = activity.findViewById<WebView>(R.id.zalosdk_login_webview)
        val signal = CountDownLatch(1)

        val input = device.findObject(
            UiSelector()
                .instance(0)
                .className(EditText::class.java)
        )
        input.waitForExists(10000)

        activity.runOnUiThread {
            val url = webView.originalUrl
            val uri = Uri.parse(url)

            assertEquals(uri.scheme, "https")
//            assertEquals(uri.host, "id.zaloapp.com")
//            assertEquals(uri.path, "/v3/auth")
//            assertEquals(uri.getQueryParameter("app_id"), AppInfo.getAppId(context))
//            assertEquals(uri.getQueryParameter("pkg_name"), AppInfo.getPackageName(context))

            val callbackUrl =
                "http://" + context.packageName + "/?uid=" + uid + "&code=" + oauthCode + "&display_name=abc"
            webView.evaluateJavascript("window.location = '$callbackUrl';", null)
            signal.countDown()
        }

        assertThat(webView, notNullValue())
        signal.await()

    }

    @Test
    @Throws(UiObjectNotFoundException::class, InterruptedException::class)
    fun loginZaloViaBrowserSuccess() {

        //#1 setup
        val wakeUp =
            Storage(context).privateSharedPreferences(SharedPreferenceConstant.PREFS_NAME_WAKEUP)
        wakeUp.setBoolean(SettingsManager.KEY_SETTINGS_OUT_APP_LOGIN, true)
        val setting = SettingsManager.getInstance()
        setting.wakeUpStorage = wakeUp
        AuthUtils.settingsManager = setting

        //#2 start intent
        val inst = InstrumentationRegistry.getInstrumentation()
        clickButtonWithText("Login Web")

        //#3 Assert Activity & result
        val appUID = AppInfo.getAppId(context)
        val resultUri =
            "zalo-$appUID://oauthcode?uid=5981149385211560544&code=vFeAvARfeb_Dodhjfxxh0hdBKRgIYVqe-SuUaO6AbXhmh2oHfeRm8hlCQxoUXzOHfF1xWyU8iW6_z3gCojUA1vJpKxJuZUiKxl06ZV-rh06SzdIsqOldEwhdJvVRkCXlxvCyeTRRdnphXqhNruVRTOEB2Agpykbs_TOln9kEboVAtbAJuPhY4VAVU-JZXQCzwlfVgh7RW0RVgZY1Wf-jMDAd6DhikveckB52_f78usQyidcBZ93E4xhHLR2KdVmhwDacWBpPz5cNend1gj2N8eN-hx2JWW90ZyQVmjdpVbkbdiJZikCZ1lptouHY4-V8T6vgHKXBix0vUtvxQ6ATcGmXFsXAVFwwHdinDXKYxfvS22uQlDyNB9Iyjry&gender=male&phone=&dob=22%2F08%2F1989&socialId=&display_name=Duydbidjmdkdldidj%CC%81smdhshsnmmwhsnshsnsush&error=0&errorMsg=&scope=access_profile,access_friends_list"
        val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resultUri))
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        inst.startActivitySync(myIntent)

        Runtime.getRuntime().exec(arrayOf("am", "force-stop", APP_PACKAGE_NAME))
        startApp()
        verifyLogin()
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class, InterruptedException::class)
    fun registerZaloViaWebSuccess() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val monitor = inst.addMonitor("com.zing.zalo.zalosdk.kotlin.oauth.WebLoginActivity", null, false)

        clickButtonWithText("REGISTER")

        val uid: Long = 1234
        val oauthCode = "abcd"

        val activity = monitor.waitForActivityWithTimeout(10000) as WebLoginActivity
        val webView = activity.findViewById<WebView>(R.id.zalosdk_login_webview)
        val signal = CountDownLatch(1)

        val input = device.findObject(
            UiSelector()
                .instance(0)
                .className(EditText::class.java)
        )
        input.waitForExists(5000)

        activity.runOnUiThread {
            val url = webView.url
            val uri = Uri.parse(url)

            assertEquals(uri.scheme, "https")
//            assertEquals(uri.host, "id.zalo.me")
//            assertEquals(uri.path, "/account/login")

//            val authUri = Uri.parse(uri.getQueryParameter("continue"))
//            assertEquals(authUri.getQueryParameter("app_id"), AppInfo.getAppId(context))
//            assertEquals(authUri.getQueryParameter("pkg_name"), AppInfo.getPackageName(context))
//            assertEquals(authUri.getQueryParameter("zregister"), "true")

            val callbackUrl =
                "http://" + context.packageName + "/?uid=" + uid + "&code=" + oauthCode + "&display_name=abc"
            webView.evaluateJavascript("window.location = '$callbackUrl';", null)
            signal.countDown()
        }

        assertThat(webView, notNullValue())
        signal.await()

    }


    private fun verifyLogin() {
        val userIdTextView = TestHelper.getUiObject("user_id_text_view")
        val authCodeTextView = TestHelper.getUiObject("auth_code_text_view")

        assertNotNull(userIdTextView?.text)
        assertNotNull(authCodeTextView?.text)
    }

    private fun verifyLoginFailed() {
        val userIdTextView = TestHelper.getUiObject("user_id_text_view")
        val authCodeTextView = TestHelper.getUiObject("auth_code_text_view")

        assertEquals(authCodeTextView?.text?.length, 0)
        assertEquals(userIdTextView?.text, (-1).toString())
    }

    private fun verifyCheckIsAppLogin() {
        val checkAppLoginButton = TestHelper.getUiObject("login_status_text_view")
        val text = checkAppLoginButton?.text
        val isAppLogin = text?.contains("yes") ?: false

        assertNotEquals(isAppLogin, true)
    }


}