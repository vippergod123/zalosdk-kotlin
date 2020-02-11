package com.zing.zalo.zalosdk.kotlin.oauth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.content.pm.ApplicationInfoBuilder
import androidx.test.core.content.pm.PackageInfoBuilder
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.Constant.ZALO_PACKAGE_NAME
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager
import com.zing.zalo.zalosdk.kotlin.oauth.Constant.AUTHORIZATION_LOGIN_SUCCESSFUL_ACTION
import com.zing.zalo.zalosdk.kotlin.oauth.Constant.EXTRA_AUTHORIZATION_LOGIN_SUCCESSFUL
import com.zing.zalo.zalosdk.kotlin.oauth.Constant.RESULT_CODE_SUCCESSFUL
import com.zing.zalo.zalosdk.kotlin.oauth.Constant.RESULT_CODE_ZALO_NOT_LOGIN
import com.zing.zalo.zalosdk.kotlin.oauth.Constant.ZALO_AUTHENTICATE_REQUEST_CODE
import com.zing.zalo.zalosdk.kotlin.oauth.ZaloOAuthResultCode.RESULTCODE_USER_BACK
import com.zing.zalo.zalosdk.kotlin.oauth.ZaloOAuthResultCode.RESULTCODE_ZALO_APPLICATION_NOT_INSTALLED
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AppInfoHelper
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthStorage
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager
import kotlin.reflect.jvm.jvmName

@RunWith(RobolectricTestRunner::class)
class AuthenticatorTest {
    private lateinit var context: Context

    @MockK
    private lateinit var activity: Activity
    @MockK
    private lateinit var storage: AuthStorage
    @MockK
    private lateinit var settingMgr: SettingsManager
    @MockK
    private lateinit var resources: Resources


    private lateinit var sut: IAuthenticator
    private lateinit var packageMgr: ShadowPackageManager
    private lateinit var successIntent: Intent

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
        packageMgr = shadowOf(context.packageManager)
        sut = Authenticator(context, storage)
        AppInfoHelper.setup()
        AuthUtils.settingsManager = settingMgr

        val configuration = Configuration()
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        every { activity.packageName } returns context.packageName
        every { activity.resources } returns resources
        every { resources.configuration } returns configuration


        successIntent = Intent()
        successIntent.putExtra("error", RESULT_CODE_SUCCESSFUL)
        successIntent.putExtra(Constant.user.UID, 12345L)
        successIntent.putExtra(Constant.user.AUTH_CODE, "abcdef")
        successIntent.putExtra(
            "data",
            "{ \"data\": { \"${Constant.user.DISPLAY_NAME}\": \"abc\"  } }"
        )
    }

    @Test
    fun `test login via app success`() {
        //1. mock
        mockZaloInstalled()

        val broadcastReceiver = slot<BroadcastReceiver>()
        val receiverFilter = slot<IntentFilter>()
        val intent = slot<Intent>()
        val reqCode = slot<Int>()
        val oauthCode = slot<String>()
        val cuid = slot<Long>()
        val displayName = slot<String>()

        every {
            activity.registerReceiver(
                capture(broadcastReceiver),
                capture(receiverFilter)
            )
        } answers { nothing }
        every {
            activity.startActivityForResult(
                capture(intent),
                capture(reqCode)
            )
        } answers { nothing }
        every { storage.setAuthCode(capture(oauthCode)) } answers { nothing }
        every { storage.setZaloId(capture(cuid)) } answers { nothing }
        every { storage.setZaloDisplayName(capture(displayName)) } answers { nothing }

        //2. run
        sut.authenticate(activity, LoginVia.APP, object : IAuthenticateCompleteListener {
            override fun onAuthenticateError(errorCode: Int, message: String) {
                assertThat(true).isFalse() //must not go in here
            }

            override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
                //3.b verify )
                assertThat(uid).isEqualTo(12345L)
                assertThat(code).isEqualTo("abcdef")
                assertThat(data[Constant.user.DISPLAY_NAME]).isEqualTo("abc")

                assertThat(cuid.captured).isEqualTo(12345L)
                assertThat(oauthCode.captured).isEqualTo("abcdef")
                assertThat(displayName.captured).isEqualTo("abc")

                verify(atLeast = 1) { activity.unregisterReceiver(broadcastReceiver.captured) }
            }
        })

        //3.a verify
        assertThat(broadcastReceiver.isCaptured).isTrue()
        assertThat(receiverFilter.captured.hasAction(AUTHORIZATION_LOGIN_SUCCESSFUL_ACTION)).isTrue()
        assertThat(intent.captured.action).isEqualTo("com.zing.zalo.intent.action.THIRD_PARTY_APP_AUTHORIZATION")
        assertThat(
            intent.captured.getLongExtra(
                Intent.EXTRA_UID,
                0
            )
        ).isEqualTo(AppInfo.getAppIdLong(context))

        sut.onActivityResult(activity, reqCode.captured, Activity.RESULT_OK, successIntent)
    }

    @Test
    fun `test login via app not installed`() {
        //2. run
        sut.authenticate(activity, LoginVia.APP, object : IAuthenticateCompleteListener {
            override fun onAuthenticateError(errorCode: Int, message: String) {
                assertThat(errorCode).isEqualTo(RESULTCODE_ZALO_APPLICATION_NOT_INSTALLED)
            }

            override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
                //3.b verify
                assertThat(true).isFalse() //must not go in here
            }
        })
    }

    @Test
    fun `test login via app cancel`() {
        mockZaloInstalled()

        val reqCode = slot<Int>()
        val cancelIntent = Intent()
        cancelIntent.putExtra("error", RESULT_CODE_ZALO_NOT_LOGIN)

        every { activity.registerReceiver(any(), any()) } answers { nothing }
        every { activity.startActivityForResult(any(), capture(reqCode)) } answers { nothing }

        //2. run
        sut.authenticate(activity, LoginVia.APP, object : IAuthenticateCompleteListener {
            override fun onAuthenticateError(errorCode: Int, message: String) {
                assertThat(errorCode).isEqualTo(RESULTCODE_USER_BACK)
            }

            override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
                assertThat(true).isFalse() //must not go in here
            }
        })

        sut.onActivityResult(activity, reqCode.captured, Activity.RESULT_OK, cancelIntent)
    }

    @Test
    fun `test login via app re login`() {
        //1. mock
        mockZaloInstalled()

        val broadcastReceiver = slot<BroadcastReceiver>()
        val reqCode = slot<Int>()
        val cancelIntent = Intent()
        cancelIntent.putExtra("error", RESULT_CODE_ZALO_NOT_LOGIN)

        every { activity.registerReceiver(capture(broadcastReceiver), any()) } answers { nothing }
        every { activity.startActivityForResult(any(), capture(reqCode)) } answers { nothing }

        //2. run
        sut.authenticate(activity, LoginVia.APP, object : IAuthenticateCompleteListener {
            override fun onAuthenticateError(errorCode: Int, message: String) {
                assertThat(true).isFalse() //must not go in here
            }

            override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
                assertThat(code).isEqualTo("abcdef")
            }
        })

        val intent = Intent(AUTHORIZATION_LOGIN_SUCCESSFUL_ACTION)
        intent.putExtra(EXTRA_AUTHORIZATION_LOGIN_SUCCESSFUL, true)
        broadcastReceiver.captured.onReceive(context, intent)
        sut.onActivityResult(activity, reqCode.captured, Activity.RESULT_OK, cancelIntent)
        sut.onActivityResult(activity, reqCode.captured, Activity.RESULT_OK, successIntent)
    }

    @Test
    fun `test login via web view success`() {
        //1. mock
        val intent = slot<Intent>()
        val reqCode = slot<Int>()

        every { settingMgr.isLoginViaBrowser() } returns false
        every {
            activity.startActivityForResult(
                capture(intent),
                capture(reqCode)
            )
        } answers { nothing }

        //2. run
        sut.authenticate(activity, LoginVia.WEB, object : IAuthenticateCompleteListener {
            override fun onAuthenticateError(errorCode: Int, message: String) {
                assertThat(true).isFalse() //must not go in here
            }

            override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
                //3. verify
                assertThat(uid).isEqualTo(12345L)
                assertThat(code).isEqualTo("abcdef")
                assertThat(data[Constant.user.DISPLAY_NAME]).isEqualTo("abc")
            }
        })

        assertThat(intent.captured.component?.className).isEqualTo(WebLoginActivity::class.jvmName)
        sut.onActivityResult(activity, reqCode.captured, Activity.RESULT_OK, successIntent)
    }


    @Test
    fun `test login via browser success`() {
        //1. mock
        val intent = slot<Intent>()

        every { settingMgr.isLoginViaBrowser() } returns true
        every { activity.startActivity(capture(intent)) } answers { nothing }

        mockBrowserActivity()

        //2. run
        sut.authenticate(activity, LoginVia.WEB, object : IAuthenticateCompleteListener {
            override fun onAuthenticateError(errorCode: Int, message: String) {
                assertThat(true).isFalse() //must not go in here
            }

            override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
                //3. verify
                assertThat(uid).isEqualTo(12345L)
                assertThat(code).isEqualTo("abcdef")
                assertThat(data[Constant.user.DISPLAY_NAME]).isEqualTo("abc")
            }
        })

        assertThat(intent.captured.data!!.toString()).startsWith(
            "https://oauth.zaloapp.com/v3/auth?" +
                    "app_id=${AppInfoHelper.appId}&sign_key=${AppInfoHelper.applicationHashKey}&" +
                    "pkg_name=${context.packageName}&orientation=${activity.resources.configuration.orientation}"
        )
        sut.onActivityResult(
            activity,
            ZALO_AUTHENTICATE_REQUEST_CODE,
            Activity.RESULT_OK,
            successIntent
        )
    }

    private fun mockBrowserActivity() {
        val applicationInfo =
            ApplicationInfoBuilder.newBuilder().setPackageName(context.packageName).build()
        val activityInfo = ActivityInfo()
        activityInfo.name = BrowserLoginActivity::class.jvmName
        activityInfo.packageName = context.packageName
        activityInfo.applicationInfo = applicationInfo
        val resolveInfo = ResolveInfo()
        resolveInfo.activityInfo = activityInfo
        val browserIntent = Intent()
        browserIntent.setPackage(context.packageName)
        browserIntent.data = Uri.parse("zalo-${AppInfo.getAppId(context)}://")
        packageMgr.addResolveInfoForIntent(browserIntent, resolveInfo)
    }

    private fun mockZaloInstalled() {
        val appInfo =
            ApplicationInfoBuilder.newBuilder().setName("Zalo").setPackageName(ZALO_PACKAGE_NAME)
                .build()
        val packageInfo = PackageInfoBuilder.newBuilder().setApplicationInfo(appInfo)
            .setPackageName(ZALO_PACKAGE_NAME).build()
        packageMgr.installPackage(packageInfo)
    }
}