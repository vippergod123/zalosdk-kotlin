package com.zing.zalo.zalosdk.core.devicetracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.devicetrackingsdk.DeviceTrackingAsyncTask
import com.zing.zalo.devicetrackingsdk.DeviceTrackingListener
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class DeviceTrackingAsyncTaskTest {
    private lateinit var context: Context

    @MockK
    private lateinit var httpClient: HttpClient

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        DeviceTracking.init(context,null)
    }

    @Test
    fun `test GetSdkIdAsyncTask`() {
        mockkObject(DeviceTracking)
        //#1 fail Request
        val failRequest = mockk<HttpUrlEncodedRequest>(relaxUnitFun = true)
        every { httpClient.send(failRequest).getJSON() } returns null

        var getSdkIdAsyncTask = DeviceTrackingAsyncTask.GetSdkId(
            WeakReference(context),
            object : DeviceTrackingListener {
                override fun onComplete(result: String?) {
                    Assert.assertNull(result)
                }
            })

        getSdkIdAsyncTask.httpClient = httpClient
        getSdkIdAsyncTask.request = failRequest
        getSdkIdAsyncTask.execute()

        verifyRequestOnceForSdkId(failRequest)

        //#2 ok Request
        val okRequest = mockk<HttpUrlEncodedRequest>(relaxUnitFun = true)
        val okResponse =
            "{\"data\":{\"privateKey\":\"Y7MFG7TnNMM2u7kL\",\"sdkId\":\"Qzry0sFBtLGciYbCBv6RRHMxB3Xw-k4ZHCXZD7ZwqcSNi1WjHCRiLYM-BHHho-eVMTCUP3EWyrKVc4qNGShL17haP2W1dvWY79G1JJRuyd0FrKe-UQAr24kSRMj9bkPt8EekBcN4sar1n3Hd0TAILqc94micmjms6wi70XMtXdmnRb1N37pMObKW01LVI9uRDWH1AMioYLLhLnKl5XAmVZfS57vUMIQQW2fiKiNw5m\"},\"error\":0,\"errorMsg\":\"\"}"

        val jsonObject = JSONObject(okResponse)
        every { httpClient.send(okRequest).getJSON() } returns jsonObject

        getSdkIdAsyncTask = DeviceTrackingAsyncTask.GetSdkId(
            WeakReference(context),
            object : DeviceTrackingListener {
                override fun onComplete(result: String?) {
                    assertThat(result).isNotNull()
                }

            })

        getSdkIdAsyncTask.httpClient = httpClient
        getSdkIdAsyncTask.request = okRequest
        getSdkIdAsyncTask.execute()

        val data = jsonObject.getJSONObject("data")
        val sdkId = data.optString("sdkId")
        val privateKey = data.optString("privateKey")
        verify(exactly = 1) { DeviceTracking.setSDKId(sdkId) }
        verify(exactly = 1) { DeviceTracking.setPrivateKey(privateKey) }
        verifyRequestOnceForSdkId(okRequest)
    }

    private fun verifyRequestOnceForSdkId(request: HttpUrlEncodedRequest) {
        verify(exactly = 1) { request.addParameter("appId", any()) }
        verify(exactly = 1) { request.addParameter("sdkv", any()) }
        verify(exactly = 1) { request.addParameter("pl", any()) }
        verify(exactly = 1) { request.addParameter("osv", any()) }
        verify(exactly = 1) { request.addParameter("model", any()) }
        verify(exactly = 1) { request.addParameter("screenSize", any()) }
        verify(exactly = 1) { request.addParameter("device", any()) }
        verify(exactly = 1) { request.addParameter("ref", any()) }
    }

    @Test
    fun `test GetDeviceIdAsyncTask`() {
        //#1 fail Request

        val failRequest = mockk<HttpUrlEncodedRequest>(relaxUnitFun = true)

        every { httpClient.send(failRequest).getJSON() } returns null

        val currentMillis = System.currentTimeMillis()
        var getDeviceIdAsyncTask = DeviceTrackingAsyncTask.GetDeviceId(
            WeakReference(context),
            "abc",
            currentMillis,
            object : DeviceTrackingListener {
                override fun onComplete(result: String?) {
                    Assert.assertNull(result)
                }
            })
        getDeviceIdAsyncTask.httpClient = httpClient
        getDeviceIdAsyncTask.request = failRequest
        getDeviceIdAsyncTask.execute()

        verifyRequestOnceForDeviceId(failRequest)

//        #2 ok Request
        val okRequest = mockk<HttpUrlEncodedRequest>(relaxUnitFun = true)
        val okResponse = "{\"data\":{\"deviceId\":\"2002.25b0de5d67408e1ed751.1568371271685.7d8a5fa3\",\"expiredTime\":43200000},\"error\":0,\"errorMsg\":\"\"}"

        val jsonObject = JSONObject(okResponse)
        val data = jsonObject.getJSONObject("data")
        val okDeviceId = data.optString("deviceId")

        every { httpClient.send(okRequest).getJSON() } returns JSONObject(okResponse)

        getDeviceIdAsyncTask = DeviceTrackingAsyncTask.GetDeviceId(WeakReference(context),"abc", currentMillis, object : DeviceTrackingListener {
            override fun onDeviceIdSuccess(deviceId: String?) {
                super.onDeviceIdSuccess(deviceId)
                assertThat(deviceId).isEqualTo(okDeviceId)
            }
        })
        getDeviceIdAsyncTask.httpClient = httpClient
        getDeviceIdAsyncTask.request = okRequest
        getDeviceIdAsyncTask.execute()

        verifyRequestOnceForDeviceId(okRequest)
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


}