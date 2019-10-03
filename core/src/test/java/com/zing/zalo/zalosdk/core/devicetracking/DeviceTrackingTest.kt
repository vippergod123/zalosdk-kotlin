package com.zing.zalo.zalosdk.core.devicetracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.devicetrackingsdk.DeviceTrackingListener
import com.zing.zalo.zalosdk.core.helper.AppTrackerHelper
import com.zing.zalo.zalosdk.core.helper.DataHelper
import com.zing.zalo.zalosdk.core.helper.TestUtils
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import io.mockk.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceTrackingTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `GetDeviceId CachedValid ReturnCached`() {
        //#1. Setup mock data
        val getDeviceIdAsyncTask = mockk<DeviceTracking.GetDeviceIdAsyncTask>(relaxUnitFun = true)
        DeviceTracking.deviceIdExpiredTime = System.currentTimeMillis() + 10000L // make not expired
        DeviceTracking.getDeviceIdAsyncTask = getDeviceIdAsyncTask

        //#2. Run
        DeviceTracking.init(context, null)

        //#3. Verify & assert
        assertThat(DeviceTracking.getDeviceId()).isNotNull()
        verify(exactly = 0) { getDeviceIdAsyncTask.execute() }
    }

    @Test
    fun `GetDeviceId Not Cached`() {
        //#1. Setup mock data
        val request = mockk<HttpUrlEncodedRequest>(relaxUnitFun = true)
        val httpClient = mockk<HttpClient>(relaxUnitFun = true)

        every {
            httpClient.send(request).getJSON()
        } returns JSONObject(DataHelper.responseGetDeviceIdAsyncTask)

        DeviceTracking.deviceIdExpiredTime = System.currentTimeMillis() - 10000L  // make expired
        DeviceTracking.request = request
        DeviceTracking.httpClient = httpClient

        //#2. Run
        DeviceTracking.init(context, null)

        //#3. Verify & assert
        assertThat(DeviceTracking.getDeviceId()).isEqualTo(AppTrackerHelper.deviceId)
        verifyRequestOnceForDeviceId(request)
    }

    private fun verifyRequestOnceForDeviceId(request: HttpUrlEncodedRequest) {
        verify(exactly = 1) { request.addQueryStringParameter("pl", any()) }
        verify(exactly = 1) { request.addQueryStringParameter("appId", any()) }
        verify(exactly = 1) { request.addQueryStringParameter("oauthCode", any()) }
        verify(exactly = 1) { request.addQueryStringParameter("device", any()) }
        verify(exactly = 1) { request.addQueryStringParameter("data", any()) }
        verify(exactly = 1) { request.addQueryStringParameter("ts", any()) }
        verify(exactly = 1) { request.addQueryStringParameter("sig", any()) }
        verify(exactly = 1) { request.addQueryStringParameter("sdkId", any()) }
    }


    @Test
    fun `GetDeviceId When Expired`() {

        var times = 1
        val request = mockk<HttpUrlEncodedRequest>(relaxUnitFun = true)
        val httpClient = mockk<HttpClient>(relaxUnitFun = true)

        val deviceIdAfter = "deviceId_second"
        val responseAfter = "{\"data\":{\"deviceId\":\"$deviceIdAfter\",\"expiredTime\":43200000},\"error\":0,\"errorMsg\":\"\"}"
        DeviceTracking.deviceIdExpiredTime = System.currentTimeMillis() - 10000L  // make expired
        DeviceTracking.request = request
        DeviceTracking.httpClient = httpClient

        every {
            httpClient.send(request).getJSON()
        } returns JSONObject(DataHelper.responseGetDeviceIdAsyncTask)

        DeviceTracking.init(context, null)
        DeviceTracking.deviceIdExpiredTime = 0L

        every {
            httpClient.send(request).getJSON()
        } returns JSONObject(responseAfter)

        DeviceTracking.getDeviceId(object: DeviceTrackingListener{
            override fun onComplete(result: String?) {
                when (times) {
                    1 -> assertThat(result).isEqualTo(AppTrackerHelper.deviceId)
                    2 -> assertThat(result).isEqualTo(deviceIdAfter)
                }
                times++
            }
        })
    }
}