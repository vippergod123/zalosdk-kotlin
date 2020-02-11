package com.zing.zalo.zalosdk.kotlin.core.devicetracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.SdkTracking
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.SharedPreferenceConstant.PREF_PRIVATE_KEY
import com.zing.zalo.zalosdk.kotlin.core.SharedPreferenceConstant.PREF_SDK_ID
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfoHelper
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpResponse
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.net.URLEncoder

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SdkTrackingTest {
    private lateinit var context: Context

    @MockK
    private lateinit var client: HttpClient
    @MockK
    private lateinit var resp: HttpResponse
    @MockK
    private lateinit var sdkStorage: Storage

    private val testScope = TestCoroutineScope()
    lateinit var sut: SdkTracking
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
        sut = SdkTracking.getInstance()
        sut.storage = sdkStorage
        sut.httpClient = client
        sut.scope = testScope



        AppInfoHelper.setup()
    }

    @After
    fun teardown() {
        sut.stop()
    }

    @Test
    fun `GetSdkId CachedValid ReturnCached`() {
        //#1. setup mock
        val sdkId = "ABCSAD"
        val privateKey = "123456"
        every { sdkStorage.getString(PREF_SDK_ID) } returns sdkId
        every { sdkStorage.getString(PREF_PRIVATE_KEY) } returns privateKey
        every { client.send(any()) } returns resp

        //#2. Run
        sut.start(context)

        //#3. Verify
        assertThat(sut.getSDKId()).isEqualTo(sdkId)
        assertThat(sut.getPrivateKey()).isEqualTo(privateKey)
        verify(exactly = 0) { client.send(any()) }

    }

    @Test
    fun `GetSdkId Not Cached`() {
        //#1. setup mock
        val json = JSONObject("{ error: 0, data: { sdkId:'1234', privateKey:'abcdef' } }")
        val slot = slot<HttpUrlEncodedRequest>()
        every { client.send(capture(slot)) } returns resp
        every { resp.getJSON() } returns json
        every { sdkStorage.getString(PREF_SDK_ID) } returns null
        every { sdkStorage.getString(PREF_PRIVATE_KEY) } returns "abc"

        //#2. Run
        sut.start(context)

        //#3. Verify
        verify { sdkStorage.setString(PREF_SDK_ID, "1234") }
        verify { sdkStorage.setString(PREF_PRIVATE_KEY, "abcdef") }
        assertThat(sut.getSDKId()).isEqualTo("1234")
        assertThat(sut.getPrivateKey()).isEqualTo("abcdef")

        assertThat(slot.isCaptured).isTrue()
        val req = slot.captured
        val url = req.getUrl("")
        assertThat(url).isEqualTo(Constant.api.API_SDK_ID)

        val outputStream = ByteArrayOutputStream()
        req.encodeBody(outputStream)
        val byteArray = outputStream.toByteArray()
        val body = String(byteArray)

        assertThat(body.contains("pl=android")).isTrue()
        assertThat(body.contains("appId=${AppInfoHelper.appId}")).isTrue()

        val dIdResult =
            "{\"dId\":\"${AppInfoHelper.advertiserId}\",\"aId\":\"unknown\",\"mod\":\"robolectric\",\"ser\":\"unknown\"}"
        assertThat(body.contains(URLEncoder.encode(dIdResult, "UTF-8"))).isTrue()
    }

    @Test
    fun `GetSdkId call multiple times`() {
        //#1. setup mock
        val json = JSONObject("{ error: 0, data: { sdkId:'1234', privateKey:'abcdef' } }")
        every { client.send(any()) } returns resp
        every { resp.getJSON() } returns json
        every { sdkStorage.getString(PREF_SDK_ID) } returns "bbb"
        every { sdkStorage.getString(PREF_PRIVATE_KEY) } returns null

        //#2. Run
        sut.getSDKId()
        sut.start(context)
        sut.getSDKId()
        sut.getPrivateKey()

        //#3. Verify
        verify(exactly = 1) { client.send(any()) }
    }
}