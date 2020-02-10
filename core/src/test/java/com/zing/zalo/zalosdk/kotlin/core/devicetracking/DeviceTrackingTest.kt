package com.zing.zalo.zalosdk.kotlin.core.devicetracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.SdkTracking
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfoHelper
import com.zing.zalo.zalosdk.kotlin.core.helper.DataHelper
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpResponse
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineScope
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder

@RunWith(RobolectricTestRunner::class)
class DeviceTrackingTest {
    private lateinit var context: Context
    @MockK
    private lateinit var client: HttpClient
    @MockK
    private lateinit var resp: HttpResponse
    @MockK
    private lateinit var sdkTracking: SdkTracking
    lateinit var sut: DeviceTracking
    private val testScope = TestCoroutineScope()
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
        sut = DeviceTracking.getInstance()
        sut.httpClient = client
        sut.sdkTracking = sdkTracking
        sut.scope = testScope

        every { sdkTracking.getSDKId() } returns "sdk-id"
        AppInfoHelper.setup()

        Storage(context).setAuthCode(DataHelper.authCode)
    }

    @After
    fun teardown() {
        val f = File(DeviceTracking.DID_FILE_NAME)
        f.delete()
        sut.stop()
    }

    @Test
    fun `GetDeviceId CachedValid ReturnCached`() {
        //#1. Setup mock data
        val data = JSONObject()
        data.put(DeviceTracking.KEY_DEVICE_ID, "1234")
        data.put(DeviceTracking.KEY_DEVICE_ID_EXPIRED_TIME, System.currentTimeMillis() + 5000)
        Utils.writeToFile(context, data.toString(), DeviceTracking.DID_FILE_NAME)

        //#2. Run
        sut.start(context)
        Robolectric.flushBackgroundThreadScheduler()

        //#3. Verify & assert
        assertThat(sut.getDeviceId()).isEqualTo("1234")
        verify(exactly = 0) { client.send(any()) }
    }

    @Test
    fun `GetDeviceId Not Cached`() {
        //#1. Setup mock data
        val json =
            JSONObject("{ error: 0, data: { deviceId:'1234', expiredTime:${System.currentTimeMillis() + 1000} } }")
        val slot = slot<HttpUrlEncodedRequest>()
        every { client.send(capture(slot)) } returns resp
        every { resp.getJSON() } returns json

        //#2. Run
        sut.start(context)
        Robolectric.flushBackgroundThreadScheduler()

        //#3. Verify
        assertThat(slot.isCaptured).isTrue()
        val req = slot.captured
        assertThat(sut.getDeviceId()).isEqualTo("1234")

        val url = req.getUrl("")
        assertThat(url).isEqualTo(Constant.api.API_HARDWARE_ID_URL)

        val outputStream = ByteArrayOutputStream()
        req.encodeBody(outputStream)
        val byteArray = outputStream.toByteArray()
        val body = String(byteArray)

        assertThat(body.contains("pl=android")).isTrue()
        assertThat(body.contains("appId=${AppInfoHelper.appId}")).isTrue()
        assertThat(body.contains("oauthCode=${DataHelper.authCode}")).isTrue()
        val dIdResult =
            "{\"dId\":\"${AppInfoHelper.advertiserId}\",\"aId\":\"unknown\",\"mod\":\"robolectric\",\"ser\":\"unknown\"}"
        assertThat(body.contains(URLEncoder.encode(dIdResult, "UTF-8"))).isTrue()
        val appNameResult = "\"an\":\"${AppInfoHelper.appName}\""
        assertThat(body.contains(URLEncoder.encode(appNameResult, "UTF-8"))).isTrue()

    }

    @Test
    fun `GetDeviceId When Expired`() {
        //#1. Setup mock data
        val data = JSONObject()
        data.put(DeviceTracking.KEY_DEVICE_ID, "1234")
        data.put(DeviceTracking.KEY_DEVICE_ID_EXPIRED_TIME, System.currentTimeMillis() - 5000)
        Utils.writeToFile(context, data.toString(), DeviceTracking.DID_FILE_NAME)

        val json =
            JSONObject("{ error: 0, data: { deviceId:'45678', expiredTime:${System.currentTimeMillis() + 1000} } }")
        val slot = slot<HttpUrlEncodedRequest>()
        every { client.send(capture(slot)) } returns resp
        every { resp.getJSON() } returns json

        //#2. Run
        sut.start(context)
        Robolectric.flushBackgroundThreadScheduler()

        //#3. Verify
        assertThat(slot.isCaptured).isTrue()
        assertThat(sut.getDeviceId()).isEqualTo("45678")
    }

    @Test
    fun `GetDeviceId Multiple times`() {
        val json =
            JSONObject("{ error: 0, data: { deviceId:'1234', expiredTime:${System.currentTimeMillis() + 1000} } }")
        every { client.send(any()) } returns resp
        every { resp.getJSON() } returns json

        //#2. Run
        sut.getDeviceId()
        sut.start(context)
        sut.getDeviceId()
        sut.getDeviceId()
        sut.getDeviceId()
        Robolectric.flushBackgroundThreadScheduler()

        //#3. Verify
        verify(exactly = 1) { client.send(any()) }
    }
}