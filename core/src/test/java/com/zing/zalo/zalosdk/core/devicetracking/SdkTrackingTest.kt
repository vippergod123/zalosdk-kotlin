package com.zing.zalo.zalosdk.core.devicetracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.devicetrackingsdk.SdkTracking
import com.zing.zalo.devicetrackingsdk.SdkTrackingListener
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.AppTrackerHelper
import com.zing.zalo.zalosdk.core.helper.DataHelper
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class SdkTrackingTest {
    private lateinit var context: Context


    @MockK
    private lateinit var sdkStorage: Storage

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `GetSdkId CachedValid ReturnCached`() {
        //#1. setup mock
        val sdkTracking = spyk(SdkTracking(context))
        val returnResult = "ABCSAD"
        every { sdkStorage.getString(Constant.sharedPreference.PREF_SDK_ID) } returns returnResult
        sdkTracking.sdkStorage = sdkStorage

        //#2. Run
        sdkTracking.getSDKId(object : SdkTrackingListener {
            override fun onComplete(result: String?) {
                assertThat(result).isEqualTo(returnResult)
            }
        })
        //#3. Verify

        verify(exactly = 0) { sdkTracking.runGetSdkIDAsyncTask(any()) }

    }

    @Test
    fun `GetSdkId Not Cached`() {
        //#1. setup mock
        val sdkTracking = spyk(SdkTracking(context))
        val request = mockk<HttpUrlEncodedRequest>(relaxUnitFun = true)
        val httpClient = mockk<HttpClient>(relaxUnitFun = true)

        every { sdkStorage.getString(Constant.sharedPreference.PREF_SDK_ID) } returns null
        sdkTracking.sdkStorage = sdkStorage
        val getSdkIdAsyncTask = SdkTracking.GetSdkIdAsyncTask(WeakReference(context), null)

        sdkTracking.getSdkIdAsyncTask = getSdkIdAsyncTask
        getSdkIdAsyncTask.request = request
        getSdkIdAsyncTask.httpClient = httpClient

        every { httpClient.send(request).getJSON() } returns JSONObject(DataHelper.responseGetSdkIdAsyncTask)
        //#2. Run
        sdkTracking.getSDKId(object : SdkTrackingListener {
            override fun onComplete(result: String?) {
                assertThat(result).isEqualTo(AppTrackerHelper.sdkId)
            }
        })
        //#3. Verify

        verify(exactly = 1) { sdkTracking.runGetSdkIDAsyncTask(any()) }
        verify (exactly = 1){ getSdkIdAsyncTask.request.addParameter("pl", "android") }
        verifyRequestOnceForSdkId(request)
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

}