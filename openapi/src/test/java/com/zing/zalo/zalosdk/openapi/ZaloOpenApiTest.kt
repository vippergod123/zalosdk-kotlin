package com.zing.zalo.zalosdk.openapi

import android.content.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.content.pm.ApplicationInfoBuilder
import androidx.test.core.content.pm.PackageInfoBuilder
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.core.module.ModuleManager
import com.zing.zalo.zalosdk.oauth.helper.AuthStorage
import com.zing.zalo.zalosdk.openapi.helper.AppInfoHelper
import com.zing.zalo.zalosdk.openapi.helper.DataHelper
import com.zing.zalo.zalosdk.openapi.helper.DeviceHelper
import com.zing.zalo.zalosdk.openapi.model.FeedData
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager
import java.io.File
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class ZaloOpenApiTest {

    private lateinit var context: Context

    private lateinit var authStorage: AuthStorage

    private lateinit var packageMgr: ShadowPackageManager

    @MockK
    private lateinit var request: HttpUrlEncodedRequest
    @MockK
    private lateinit var httpClient: HttpClient

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        packageMgr = shadowOf(context.packageManager)
        mockData(System.currentTimeMillis() - 10000)
        ModuleManager.initializeApp(context)

    }

    @Test
    fun `Get Access Token`() {
        every { httpClient.send(request).getJSON() } returns JSONObject(DataHelper.accessTokenData)

        val sut = GetAccessTokenAsyncTask(WeakReference(context), object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {
                val accessToken = data?.getString("access_token")
                assertThat(accessToken).isEqualTo(DataHelper.accessToken)
            }

        })
        sut.request = request
        sut.httpClient = httpClient
        sut.execute()
        verifyRequest(request, 1)
    }

    @Test
    fun `get Profile with access token valid`() {

        val mock = spyk<ZaloOpenApi>(recordPrivateCalls = true)
        every { mock["isAccessTokenValid"]() } returns true
        every { mock getProperty "enableUnitTest" } returns true


        every { httpClient.send(any()).getJSON() } returns JSONObject(DataHelper.profile)
        val fields = arrayOf("id", "birthday", "gender", "picture", "name")

        val callback = object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {
                assertThat(data.toString()).isEqualTo(DataHelper.profile)
            }
        }

        val callApiAsyncTask = CallApiAsyncTask(callback)
        callApiAsyncTask.httpClient = httpClient
        mock.callApiAsyncTask = callApiAsyncTask
        mock.getProfile(fields, callback)

        verifyRequest(request, 0)
    }

    @Test
    fun `get Profile when access token invalid `() {
        val mock = spyk<ZaloOpenApi>(recordPrivateCalls = true)
        every { mock["isAccessTokenValid"]() } returns false
        every { mock getProperty "enableUnitTest" } returns true

        every {
            httpClient.send(any()).getJSON()
        } returns JSONObject(DataHelper.accessTokenData) andThen JSONObject(DataHelper.profile)
        val fields = arrayOf("id", "birthday", "gender", "picture", "name")

        val callback = object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {
                assertThat(data.toString()).isEqualTo(DataHelper.profile)
            }
        }

        val getAccessTokenAsyncTask = GetAccessTokenAsyncTask(WeakReference(context), null)
        getAccessTokenAsyncTask.request = request
        getAccessTokenAsyncTask.httpClient = httpClient
        val callApiAsyncTask = CallApiAsyncTask(callback)
        callApiAsyncTask.httpClient = httpClient
        mock.callApiAsyncTask = callApiAsyncTask
        mock.getAccessTokenAsyncTask = getAccessTokenAsyncTask
        mock.getProfile(fields, callback)

        verifyRequest(request, 1)
    }

    @Test
    fun `get Profile fail when auth code invalid `() {
        val mock = spyk<ZaloOpenApi>(recordPrivateCalls = true)
        every { mock["isAccessTokenValid"]() } returns false
        every { mock getProperty "enableUnitTest" } returns true
        every {
            httpClient.send(any()).getJSON()
        } returns JSONObject(DataHelper.accessTokenData) andThen JSONObject(DataHelper.profile)

        val fields = arrayOf("id", "birthday", "gender", "picture", "name")
        val callback = object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {
                val invalidAuthCodeResult = "{\"error\":-1019}"
                assertThat(data.toString()).isEqualTo(invalidAuthCodeResult)
            }
        }

        val getAccessTokenAsyncTask = GetAccessTokenAsyncTask(WeakReference(context), null)
        authStorage.setAuthCode("")
        getAccessTokenAsyncTask.request = request
        getAccessTokenAsyncTask.httpClient = httpClient
        val callApiAsyncTask = CallApiAsyncTask(callback)
        callApiAsyncTask.httpClient = httpClient
        mock.callApiAsyncTask = callApiAsyncTask
        mock.getAccessTokenAsyncTask = getAccessTokenAsyncTask
        mock.getProfile(fields, callback)

        verifyRequest(request, 0)
    }

    @Test
    fun `send Message via App`() {
        //1. mock
        mockZaloInstalled()
        val mock = spyk<ZaloOpenApi>(recordPrivateCalls = true)
        val broadcastReceiver = slot<BroadcastReceiver>()
        val receiverFilter = slot<IntentFilter>()
        val intent = slot<Intent>()
        val ctx = mockk<Context>(relaxUnitFun = true)

        every {
            ctx.registerReceiver(
                capture(broadcastReceiver),
                capture(receiverFilter)
            )
        } answers { nothing }

        every {
            ctx.startActivity(capture(intent))
        } answers { nothing }

        every { ctx.packageManager } returns context.packageManager

        //2. run
        mock.shareZalo(ctx, mockFeedData(), "message", null)

        //3.a verify
        assertThat(broadcastReceiver.isCaptured).isTrue()
        assertThat(intent.captured.getBooleanExtra("hidePostFeed", false)).isTrue()
        verifyIntent(receiverFilter.captured, intent.captured)
        //resume
    }

    @Test
    fun `share post via App`() {
        //1. mock
        mockZaloInstalled()
        val mock = spyk<ZaloOpenApi>(recordPrivateCalls = true)
        val broadcastReceiver = slot<BroadcastReceiver>()
        val receiverFilter = slot<IntentFilter>()
        val intent = slot<Intent>()

        var ctx = mockk<Context>(relaxUnitFun = true)

        every {
            ctx.registerReceiver(
                capture(broadcastReceiver),
                capture(receiverFilter)
            )
        } answers { nothing }

        every {
            ctx.startActivity(capture(intent))
        } answers { nothing }

        every { ctx.packageManager } returns context.packageManager

        //2. run
        mock.shareZalo(ctx, mockFeedData(), "feed", null)

        //3.a verify
        assertThat(broadcastReceiver.isCaptured).isTrue()
        assertThat(intent.captured.getBooleanExtra("postFeed", false)).isTrue()
        verifyIntent(receiverFilter.captured, intent.captured)
        //resume
    }


    //#region private supportive method
    private fun verifyIntent(receiverFilter: IntentFilter,intent: Intent) {
        assertThat(receiverFilter.hasAction("com.zing.zalo.shareFeedResultInfo")).isTrue()
        assertThat(intent.getBooleanExtra("autoBack2S", false)).isTrue()
        assertThat(intent.getBooleanExtra("backToSource", false)).isTrue()
        assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(intent.component).isEqualTo(
            ComponentName(
                Constant.ZALO_PACKAGE_NAME,
                "com.zing.zalo.ui.TempShareViaActivity"
            )
        )

        val feedData = mockFeedData()
        assertThat(intent.extras?.get(Intent.EXTRA_SUBJECT)).isEqualTo(feedData.msg)
        assertThat(intent.extras?.get(Intent.EXTRA_TEXT)).isEqualTo(feedData.link)
    }
    private fun verifyRequest(request: HttpUrlEncodedRequest, times: Int) {
        verify(exactly = times) { request.addQueryStringParameter("code", any()) }
        verify(exactly = times) {
            request.addQueryStringParameter(
                "pkg_name",
                AppInfoHelper.packageName
            )
        }
        verify(exactly = times) {
            request.addQueryStringParameter(
                "sign_key",
                AppInfoHelper.applicationHashKey
            )
        }
        verify(exactly = times) { request.addQueryStringParameter("app_id", AppInfoHelper.appId) }
        verify(exactly = times) { request.addQueryStringParameter("version", any()) }
        verify(exactly = times) { request.addQueryStringParameter("zdevice", any()) }
        verify(exactly = times) { request.addQueryStringParameter("ztracking", any()) }
    }


    private fun mockData(deviceExpiredTime: Long) {
        mockkObject(DeviceInfo)
        mockkObject(DeviceTracking)
        mockkObject(Utils)

        val deviceIdSettingJSON =
            "{\"deviceId\":\"${DeviceHelper.deviceId}\",\"expiredTime\":\"${deviceExpiredTime}\"}"
        every {
            Utils.readFromFile(
                context,
                DeviceTracking.DID_FILE_NAME
            )
        } returns deviceIdSettingJSON

        every { DeviceInfo.getAdvertiseID(context) } returns DeviceHelper.adsId

        //returns data preloadInfo
        every { Utils.readFileData(File("/data/etc/appchannel/zalo_appchannel.in")) } returns "${DataHelper.preloadInfo}:${DataHelper.preloadInfo}"

        authStorage = AuthStorage(context)
        authStorage.setAuthCode("auth_code_abc")
        AppInfoHelper.setup()
    }
    //#endregion

    private fun mockFeedData(): FeedData {
        val feed = FeedData()
        feed.msg = "Prefill message"
        feed.link = "https://news.zing.vn"
        feed.linkTitle = "Zing News"
        feed.linkSource = "https://news.zing.vn"
        feed.linkThumb =
            listOf("https://img.v3.news.zdn.vn/w660/Uploaded/xpcwvovb/2015_12_15/cua_kinh_2.jpg")
        return feed
    }

    private fun mockZaloInstalled() {
        mockkObject(Utils)
        val appInfo =
            ApplicationInfoBuilder.newBuilder().setName("Zalo")
                .setPackageName(Constant.ZALO_PACKAGE_NAME)
                .build()
        val packageInfo = PackageInfoBuilder.newBuilder().setApplicationInfo(appInfo)
            .setPackageName(Constant.ZALO_PACKAGE_NAME).build()
        packageMgr.installPackage(packageInfo)

        every { Utils.isZaloSupportCallBack(any()) } returns true
    }
}
