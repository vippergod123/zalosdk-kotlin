package com.zing.zalo.zalosdk.kotlin.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.analytics.helper.AppInfoHelper
import com.zing.zalo.zalosdk.kotlin.analytics.helper.DataHelper
import com.zing.zalo.zalosdk.kotlin.analytics.helper.DeviceHelper
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(RobolectricTestRunner::class)
class EventTrackerTest {
    private lateinit var context: Context

    @MockK
    private lateinit var httpClient: HttpClient
    @MockK
    private lateinit var request: HttpUrlEncodedRequest

    private lateinit var sut: EventTracker


    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        sut = EventTracker.getInstance()
        startModuleTest()

    }

    @Test
    fun `dispatch Event To Server`() {
        mockDataWithDeviceIdNotExpired()
        //#1. Setup mock
        val event = DataHelper.mockEvent()
        val okResult = "{\"error\":0,\"errorMsg\":\"Success\"}"

        sut.setListener(object : EventTrackerListener {
            override fun dispatchComplete() {
                super.dispatchComplete()
                Log.d("got here")
                assertThat(EventStorage.events).isEmpty()
            }
        })

        every { httpClient.send(any()).getJSON() } returns JSONObject(okResult)
        sut.httpClient = httpClient
        sut.request = request
        //#2. Execute

        sut.addEvent(event.action, event.params, event.timestamp)
        sut.dispatchEvent()


        //wait for complete thread's task
        shadowOf(sut.thread.looper).idle()
        //#3. verify
        verifyRequest(request, 1)
        verifyPreloadInfo()
    }

    @Test
    fun `dispatch Immediately Event To Server`() {
        mockDataWithDeviceIdNotExpired()
//        DeviceTracking.init(context, null)

        //#1. Setup mock
        val event = DataHelper.mockEvent()
        val okResult = "{\"error\":0,\"errorMsg\":\"Success\"}"

        sut.setListener(object : EventTrackerListener {
            override fun dispatchComplete() {
                super.dispatchComplete()
                Log.d("got here")
                assertThat(EventStorage.events).isEmpty()
            }
        })

        every { httpClient.send(any()).getJSON() } returns JSONObject(okResult)
        sut.httpClient = httpClient
        sut.request = request
        //#2. Execute
        sut.dispatchEventImmediate(event)
        //wait for complete thread's task
        shadowOf(sut.thread.looper).idle()

        //#3. verify
        verifyRequest(request, 1)
        verifyPreloadInfo()
    }

    @Test
    fun `save events when http fail`() {
        mockDataWithDeviceIdNotExpired()
//        DeviceTracking.init(context, null)
        //#1. Setup mock
        val event = DataHelper.mockEvent()
        val failResult = "{\"params\":{\"name\":\"Luke\",\"age\":\"0\"},\"action\":\"0\"}"


        sut.setListener(object : EventTrackerListener {
            override fun dispatchComplete() {
                super.dispatchComplete()
                Log.d("got here")
                assertThat(EventStorage.events).isNotEmpty()
            }
        })

        every { httpClient.send(any()).getJSON() } returns JSONObject(failResult)
        sut.httpClient = httpClient
        sut.request = request
        //#2. Execute
//        eventTracker.loadEvents()
        sut.addEvent(event.action, event.params, event.timestamp)
        sut.addEvent(event.action, event.params, event.timestamp)

        sut.dispatchEvent()


        //wait for complete thread's task
        shadowOf(sut.thread.looper).idle()
        //#3. verify
        verifyRequest(request, 1)
        verifyPreloadInfo()
    }

    val signal = CountDownLatch(1)

    @Test
    fun `test thread blocking`() {
        mockDataWithDeviceIdNotExpired()
//        DeviceTracking.init(context, null)
        //#1. Setup mock
        val event = DataHelper.mockEvent()
        val failResult = "{\"params\":{\"name\":\"Luke\",\"age\":\"0\"},\"action\":\"0\"}"

        sut.setListener(object : EventTrackerListener {
            override fun dispatchComplete() {
                super.dispatchComplete()
                Log.d("got here")
                assertThat(EventStorage.events).isNotEmpty()
                signal.await(5, TimeUnit.SECONDS)
            }
        })

        every { httpClient.send(any()).getJSON() } returns JSONObject(failResult)
        sut.httpClient = httpClient
        sut.request = request
        //#2. Execute
//        eventTracker.loadEvents()
        sut.addEvent(event.action, event.params, event.timestamp)
        sut.dispatchEvent()
        sut.addEvent(event.action, event.params, event.timestamp)


        Log.d("Test Thread Blocking")
        //wait for complete thread's task


        shadowOf(sut.thread.looper).idle()
        //#3. verify
        verifyRequestWithTimeOut(request, 1, 7)
        verifyPreloadInfo()
    }

    //#region private supportive method
    private fun startModuleTest() {
        sut.start(context)
        DeviceTracking.getInstance().start(context)
    }

    private fun mockDataWithDeviceIdExpired() {

        val expiredTime = System.currentTimeMillis() + 10000L
        mockData(expiredTime)
    }

    private fun mockDataWithDeviceIdNotExpired() {
        mockData(0L)
    }

    private fun mockData(deviceExpiredTime: Long) {
        mockkObject(DeviceInfo)
        mockkObject(DeviceTracking)
        mockkObject(Utils)
        mockkObject(AppInfo)

        val deviceIdSettingJSON =
            "{\"deviceId\":\"${DeviceHelper.deviceId}\",\"expiredTime\":\"${deviceExpiredTime}\"}"
//        every { Utils.readFromFile(context, EventStorage.EVENTS_FILE_NAME) } returns DataHelper.EVENT_STORED_IN_DEVICE
        every {
            Utils.readFromFile(
                context,
                DeviceTracking.DID_FILE_NAME
            )
        } returns deviceIdSettingJSON

        every { DeviceInfo.getAdvertiseID(context) } returns DeviceHelper.adsId
        every { AppInfo.getVersionName(context) } returns AppInfoHelper.versionName
        every { AppInfo.getAppName(context) } returns AppInfoHelper.appName
        every { AppInfo.getAppId(context) } returns AppInfoHelper.appId

        //returns data preloadInfo
        every { Utils.readFileData(File("/data/etc/appchannel/zalo_appchannel.in")) } returns "${DataHelper.preloadInfo}:${DataHelper.preloadInfo}"
//        every { DeviceTracking.getDeviceId() } returns DeviceHelper.deviceId
    }

    private fun verifyRequestWithTimeOut(
        request: HttpUrlEncodedRequest,
        times: Int,
        timeOut: Long
    ) {
        val delay = timeOut * 1000L
        verify(exactly = times, timeout = delay) { request.addParameter("pl", "android") }
        verify(exactly = times, timeout = delay) {
            request.addParameter(
                "appId",
                AppInfoHelper.appId
            )
        }
        verify(exactly = times, timeout = delay) { request.addParameter("oauthCode", any()) }
        verify(exactly = times, timeout = delay) { request.addParameter("zdId", any()) }
        verify(exactly = times, timeout = delay) { request.addParameter("data", any()) }
        verify(exactly = times, timeout = delay) { request.addParameter("apps", any()) }
        verify(exactly = times, timeout = delay) { request.addParameter("ts", any()) }
        verify(exactly = times, timeout = delay) { request.addParameter("sig", any()) }
        verify(exactly = times, timeout = delay) {
            request.addParameter(
                "an",
                AppInfoHelper.appName
            )
        }
        verify(exactly = times, timeout = delay) {
            request.addParameter(
                "av",
                AppInfoHelper.versionName
            )
        }
        verify(exactly = times, timeout = delay) { request.addParameter("gzip", any()) }
        verify(exactly = times, timeout = delay) { request.addParameter("et", any()) }
        verify(exactly = times, timeout = delay) { request.addParameter("socialAcc", any()) }
        verify(exactly = times, timeout = delay) {
            request.addParameter(
                "packageName",
                context.packageName
            )
        }


    }

    private fun verifyRequest(request: HttpUrlEncodedRequest, times: Int) {
        verifyRequestWithTimeOut(request, times, 0L)
    }

    private fun verifyPreloadInfo() {
        val times = 1
        verify(exactly = times) { AppInfo.getPreloadChannel(context) }
        verify(exactly = times) { DeviceInfo.getPreloadInfo(context) }
        val preloadInfo = DeviceInfo.getPreloadInfo(context)
        assertThat(preloadInfo.preload).isEqualTo(DataHelper.preloadInfo)
    }
    //#endregion
}