package com.zing.zalo.zalosdk.kotlin.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfoHelper
import com.zing.zalo.zalosdk.kotlin.core.helper.AppTrackerHelper
import com.zing.zalo.zalosdk.kotlin.core.helper.PrivateSharedPreferenceInterface
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.kotlin.core.http.HttpResponse
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager.Companion.KEY_EXPIRE_TIME
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager.Companion.KEY_SETTINGS_OUT_APP_LOGIN
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager.Companion.KEY_SETTINGS_WEB_VIEW
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager.Companion.KEY_WAKEUP_ENABLE
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager.Companion.KEY_WAKEUP_INTERVAL
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SettingsManagerTest {

    private lateinit var context: Context

    @MockK
    private lateinit var client: HttpClient
    @MockK
    private lateinit var response: HttpResponse
    @MockK
    private lateinit var storage: PrivateSharedPreferenceInterface
    @MockK
    private lateinit var deviceTracking: DeviceTracking
    private lateinit var sut: SettingsManager

    private var testScope = TestCoroutineScope()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
        sut = SettingsManager.getInstance()
        sut.deviceTracking = deviceTracking
        sut.httpClient = client
        sut.wakeUpStorage = storage

        every { deviceTracking.getDeviceId() } returns AppTrackerHelper.deviceId
        AppInfoHelper.setup()
    }

    @After
    fun teardown() {
        sut.stop()
    }

    @Test
    fun `cached settings`() {
        //1. mock
        every { storage.getLong(KEY_EXPIRE_TIME) } returns System.currentTimeMillis() + 1000
        every { storage.getBoolean(KEY_SETTINGS_OUT_APP_LOGIN) } returns true
        every { storage.getBoolean(KEY_SETTINGS_WEB_VIEW) } returns true
        every { storage.getBoolean(KEY_WAKEUP_ENABLE) } returns true
        every { storage.getLong(KEY_WAKEUP_INTERVAL) } returns 3600

        //2. run
        sut.start(context)

        //3. verify
        verify(exactly = 0) { client.send(any()) }
        assertThat(sut.isLoginViaBrowser()).isTrue()
        assertThat(sut.isUseWebViewLoginZalo()).isTrue()
        assertThat(sut.getWakeUpSetting()).isTrue()
        assertThat(sut.getWakeUpInterval()).isEqualTo(3600)
    }

    @Test
    fun `load settings success`() {
        //1. mock
        val data = """{
            "data":{
                "webview_login":1,
                "isOutAppLogin":true,
                "setting":{
                    "wakeup_interval_enable":true,
                    "wakeup_send_gid_to_other_app_enable":true,
                    "wakeup_interval":86400000,
                    "expiredTime":86400000
                }
            },
            "error":0,"errorMsg":"Success."
        }"""

        val request = slot<HttpGetRequest>()
        val expireTime = slot<Long>()
        val now = System.currentTimeMillis()
        every { response.getJSON() } returns JSONObject(data)
        every { client.send(capture(request)) } returns response
        every { storage.getLong(KEY_EXPIRE_TIME) } returns now - 1000
        every { storage.setLong(KEY_EXPIRE_TIME, capture(expireTime)) } just Runs

        //2. run
        sut.scope = testScope
        sut.start(context)

        //3. verify
        verify(exactly = 1) { client.send(any()) }
        verify(exactly = 1) { storage.setBoolean(KEY_SETTINGS_OUT_APP_LOGIN, true) }
        verify(exactly = 1) { storage.setBoolean(KEY_SETTINGS_WEB_VIEW, true) }
        verify(exactly = 1) { storage.setBoolean(KEY_WAKEUP_ENABLE, true) }
        verify(exactly = 1) { storage.setLong(KEY_WAKEUP_INTERVAL, 86400000L) }
        assertThat(expireTime.captured).isGreaterThan(now + 86400000 - 1000)

        assertThat(request.captured.getUrl("")).isEqualTo(
            "/sdk/mobile/setting?pl=android&appId=${AppInfoHelper.appId}&sdkv=${Constant.VERSION}" +
                    "&pkg=${context.packageName}&zdId=${AppTrackerHelper.deviceId}"
        )
    }
}