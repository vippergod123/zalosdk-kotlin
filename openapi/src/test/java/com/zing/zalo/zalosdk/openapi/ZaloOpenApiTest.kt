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
import com.zing.zalo.zalosdk.core.http.BaseHttpRequest
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.core.http.IHttpRequest
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.openapi.helper.AppInfoHelper
import com.zing.zalo.zalosdk.openapi.helper.DataHelper
import com.zing.zalo.zalosdk.openapi.helper.DeviceHelper
import com.zing.zalo.zalosdk.openapi.model.FeedData
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager
import java.io.File
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ZaloOpenApiTest {

    private lateinit var context: Context

    private lateinit var openApiStorage: OpenApiStorage

    private lateinit var packageMgr: ShadowPackageManager

    @MockK
    private lateinit var httpClient: HttpClient
    @MockK
    private lateinit var accessTokenHttpClient: HttpClient

    private lateinit var mock: ZaloOpenApi

    @ExperimentalCoroutinesApi
    private val testScope = TestCoroutineScope()

    private var isBroadcastRegistered = false

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        packageMgr = shadowOf(context.packageManager)
        mockData(System.currentTimeMillis() - 10000)

        mock = ZaloOpenApi(
            context,
            DataHelper.authCode
        )
        mock.openApi = OpenApi(
            context,
            DataHelper.authCode,
            isBroadcastRegistered,
            httpClient,
            accessTokenHttpClient,
            testScope
        )

    }

    @Test
    fun `get Profile with access token invalid`() = runBlockingTest {
        //#1. Mock data
        val accessTokenSlot = slot<HttpUrlEncodedRequest>()
        val openApiSlot = slot<IHttpRequest>()
        every {
            accessTokenHttpClient.send(capture(accessTokenSlot)).getJSON()
        } returns JSONObject(DataHelper.accessTokenData)
        every {
            httpClient.send(capture(openApiSlot)).getJSON()
        } returns JSONObject(DataHelper.profile)
        val fields = arrayOf("id", "birthday", "gender", "picture", "name")

        //#2. Start
        mock.getProfile(fields, object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {
                //#3. Verify
                assertSlotRequest(accessTokenSlot, openApiSlot, fields)
                assertThat(data.toString()).isEqualTo(DataHelper.profile)
            }
        })
    }


    @Test
    fun `get Profile with access token valid`() = runBlockingTest {
        //#1. Mock data
        val accessTokenSlot = slot<HttpUrlEncodedRequest>()
        val openApiSlot = slot<IHttpRequest>()

        mock.openApi?.accessToken = DataHelper.accessToken
        mock.openApi?.accessTokenExpiredTime =
            System.currentTimeMillis() + System.currentTimeMillis() + Utils.convertTimeToMilliSeconds(
                1,
                TimeUnit.HOURS
            )
        every {
            accessTokenHttpClient.send(capture(accessTokenSlot)).getJSON()
        } returns JSONObject(DataHelper.accessTokenData)
        every {
            httpClient.send(capture(openApiSlot)).getJSON()
        } returns JSONObject(DataHelper.profile)
        val fields = arrayOf("id", "birthday", "gender", "picture", "name")

        //#2. Start
        mock.getProfile(fields, object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {
                //#3. Verify
                assertSlotRequest(accessTokenSlot, openApiSlot, fields)
                assertThat(data.toString()).isEqualTo(DataHelper.profile)
            }
        })
    }

    @Test
    fun `get Profile fail when auth code invalid `() = runBlockingTest {
        //#1. Mock data
        val accessTokenSlot = slot<HttpUrlEncodedRequest>()
        val openApiSlot = slot<IHttpRequest>()
        every {
            accessTokenHttpClient.send(capture(accessTokenSlot)).getJSON()
        } returns JSONObject(DataHelper.accessTokenData)
        every {
            httpClient.send(capture(openApiSlot)).getJSON()
        } returns JSONObject(DataHelper.profile)
        val fields = arrayOf("id", "birthday", "gender", "picture", "name")

        mock.openApi =
            OpenApi( //set authCode invalid
                context,
                "",
                isBroadcastRegistered,
                httpClient,
                accessTokenHttpClient,
                testScope
            )
        //#2. Start
        Log.d(Thread.currentThread().name)

        mock.getProfile(fields, object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {
                //#3. Verify
                assertSlotRequest(accessTokenSlot, openApiSlot, fields)
                val failResult = "{\"error\":-1019,\"message\":\"OAuth Code is invalid\"}"
                assertThat(data.toString()).isEqualTo(failResult)
            }
        })
    }

    @Test
    fun `send Message via App`() {
        //1. mock
        mockZaloInstalled()
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

        every { ctx.applicationContext } returns context
        every { ctx.packageManager } returns context.packageManager

        //2. run
        mock.openApi =
            OpenApi( //set authCode invalid
                ctx,
                DataHelper.authCode,
                isBroadcastRegistered,
                httpClient,
                accessTokenHttpClient,
                testScope
            )
        mock.shareMessage(mockFeedData(), null)

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
        isBroadcastRegistered = false
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

        every { ctx.applicationContext } returns context
        every { ctx.packageManager } returns context.packageManager

        //2. run
        mock.openApi =
            OpenApi( //set authCode invalid
                ctx,
                DataHelper.authCode,
                isBroadcastRegistered,
                httpClient,
                accessTokenHttpClient,
                testScope
            )
        mock.shareFeed(mockFeedData(), null)

        //3.a verify
        assertThat(broadcastReceiver.isCaptured).isTrue()
        assertThat(intent.captured.getBooleanExtra("postFeed", false)).isTrue()
        verifyIntent(receiverFilter.captured, intent.captured)
        //resume
    }

    //#region private supportive method
    private fun verifyIntent(receiverFilter: IntentFilter, intent: Intent) {
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

    private fun assertSlotRequest(
        accessTokenSlot: CapturingSlot<HttpUrlEncodedRequest>,
        openApiSlot: CapturingSlot<IHttpRequest>,
        fields: Array<String>
    ) {
        if (accessTokenSlot.isCaptured) {
            val accessTokenRequest = accessTokenSlot.captured as BaseHttpRequest
            assertThat(accessTokenRequest.mQueryParams["code"]).isEqualTo(DataHelper.authCode)
            assertThat(accessTokenRequest.mQueryParams["pkg_name"]).isEqualTo(AppInfoHelper.packageName)
            assertThat(accessTokenRequest.mQueryParams["sign_key"]).isEqualTo(AppInfoHelper.applicationHashKey)
            assertThat(accessTokenRequest.mQueryParams["app_id"]).isEqualTo(AppInfoHelper.appId)
            assertThat(accessTokenRequest.mQueryParams["version"]).isEqualTo(Constant.VERSION)
            assertThat(accessTokenRequest.path).isEqualTo(Constant.api.AUTH_MOBILE_ACCESS_TOKEN_PATH)
        }
        if (openApiSlot.isCaptured) {
            val openApiRequest = openApiSlot.captured as BaseHttpRequest
            assertThat(openApiRequest.mQueryParams["access_token"]).isEqualTo(DataHelper.accessToken)
            assertThat(openApiRequest.mQueryParams["fields"]).isEqualTo(buildFieldsParam(fields))
            assertThat(openApiRequest.path).isEqualTo(Constant.api.GRAPH_V2_ME_PATH)
        }
    }

    private fun verifyRequest(request: HttpUrlEncodedRequest, times: Int) {
        verify(exactly = times) { request.addQueryStringParameter("code", DataHelper.authCode) }
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

        openApiStorage = OpenApiStorage(context)
        openApiStorage.setAuthCode(DataHelper.authCode)
        AppInfoHelper.setup()
    }


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

    private fun buildFieldsParam(fields: Array<String>): String {
        if (fields.isNotEmpty()) {
            val param = StringBuffer()
            for (each in fields) {
                param.append(each).append(",")
            }
            return param.substring(0, param.length - 1)
        }
        return ""
    }
    //#endregion
}
