package com.zing.zalo.zalosdk.demo

import android.net.Uri
import android.webkit.WebView
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.zing.zalo.zalosdk.auth.WebLoginActivity
import com.zing.zalo.zalosdk.core.helper.AppInfo
import org.hamcrest.core.IsNull.notNullValue
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AuthenticateTest : AppBase() {
    @Before
    @Throws(IOException::class, UiObjectNotFoundException::class)
    override fun startApp() {
        super.startApp()

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
        clickButtonWithText("Validate oauth code")
        verifyLoginFailed()
    }

    @Test
    @Throws(UiObjectNotFoundException::class, InterruptedException::class)
    fun loginZaloViaWebSuccess() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val monitor = inst.addMonitor("com.zing.zalo.zalosdk.auth.WebLoginActivity", null, false)

        clickButtonWithText("Login Web")

        val uid: Long = 1234
        val oauthCode = "abcd"

        val activity = monitor.waitForActivityWithTimeout(4000) as WebLoginActivity
        val webView = activity.findViewById<WebView>(R.id.zalosdk_login_webview)
        val signal = CountDownLatch(1)

        val input = device.findObject(
            UiSelector()
                .instance(0)
                .className(EditText::class.java)
        )
        input.waitForExists(5000)

        activity.runOnUiThread {
            val url = webView.originalUrl
            val uri = Uri.parse(url)
            val authUri = Uri.parse(uri.getQueryParameter("continue"))
            println(uri)
            assertEquals(authUri.scheme, "https")
            assertEquals(authUri.host, "oauth.zaloapp.com")
            assertEquals(authUri.path, "/v3/auth")
            assertEquals(authUri.getQueryParameter("app_id"), AppInfo.getAppId(context))
            assertEquals(authUri.getQueryParameter("pkg_name"), AppInfo.getPackageName(context))

            val callbackUrl =
                "http://" + context.packageName + "/?uid=" + uid + "&code=" + oauthCode + "&display_name=abc"
            webView.evaluateJavascript("window.location = '$callbackUrl';", null)
            signal.countDown()
        }

        assertThat(webView, notNullValue())
        signal.await()

    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class, InterruptedException::class)
    fun registerZaloViaWebSuccess() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val monitor = inst.addMonitor("com.zing.zalo.zalosdk.auth.WebLoginActivity", null, false)

        clickButtonWithText("Register")

        val uid: Long = 1234
        val oauthCode = "abcd"

        val activity = monitor.waitForActivityWithTimeout(4000) as WebLoginActivity
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
            assertEquals(uri.host, "id.zalo.me")
            assertEquals(uri.path, "/account/login")

            val authUri = Uri.parse(uri.getQueryParameter("continue"))
            assertEquals(authUri.getQueryParameter("app_id"), AppInfo.getAppId(context))
            assertEquals(authUri.getQueryParameter("pkg_name"), AppInfo.getPackageName(context))
            assertEquals(authUri.getQueryParameter("zregister"), "true")

            val callbackUrl =
                "http://" + context.packageName + "/?uid=" + uid + "&code=" + oauthCode + "&display_name=abc"
            webView.evaluateJavascript("window.location = '$callbackUrl';", null)
            signal.countDown()
        }

        assertThat(webView, notNullValue())
        signal.await()

    }


    @Throws(UiObjectNotFoundException::class)
    private fun verifyLogin() {

        val userIdTextView = TestHelper.getUiObject("user_id_text_view")
        val authCodeTextView = TestHelper.getUiObject("auth_code_text_view")

        assertNotNull(userIdTextView?.text)
        assertNotNull(authCodeTextView?.text)
    }

    @Throws(UiObjectNotFoundException::class)
    private fun verifyLoginFailed() {

        val userIdTextView = TestHelper.getUiObject("user_id_text_view")
        val authCodeTextView = TestHelper.getUiObject("auth_code_text_view")

        assertEquals(authCodeTextView?.text?.length, 0)
        assertEquals(userIdTextView?.text, (-1).toString())
    }

    @Throws(UiObjectNotFoundException::class)
    private fun verifyCheckIsAppLogin() {

        val checkAppLoginButton = TestHelper.getUiObject("login_status_text_view")

        val text = checkAppLoginButton?.text

        val isAppLogin = text?.contains("yes") ?: false
        assertNotEquals(isAppLogin, true)
    }


}