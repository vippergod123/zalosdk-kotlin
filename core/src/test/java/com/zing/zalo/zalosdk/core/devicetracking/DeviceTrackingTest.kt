package com.zing.zalo.zalosdk.core.devicetracking

import android.content.Context
import android.text.TextUtils
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.j2objc.annotations.Weak
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.devicetrackingsdk.DeviceTrackingAsyncTask
import com.zing.zalo.zalosdk.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.core.helper.Utils
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
class DeviceTrackingTest {
    private lateinit var context: Context

    @MockK
    private lateinit var getDeviceIdAsyncTask: DeviceTrackingAsyncTask.GetDeviceId
    @MockK
    private lateinit var getSdkIdAsyncTask: DeviceTrackingAsyncTask.GetSdkId

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

    }

    @Test
    fun `GetDeviceId GetSDKId CachedValid ReturnCached`() {
        mockkObject(DeviceTracking)
        mockkObject(DeviceTrackingAsyncTask)

        every { DeviceTracking.isDeviceIdExpired() } returns false
        every { DeviceTracking.getSDKId() } returns "ABC"

        DeviceTracking.getSdkIdAsyncTask = getSdkIdAsyncTask
        DeviceTracking.getDeviceIdAsyncTask = getDeviceIdAsyncTask
        DeviceTracking.init(context,null)

        //TODO:
        // - Cái này là assert trên mock object, ko có ý nghĩa, phải dùng object thật cần test
        assertThat(DeviceTracking.getDeviceId()).isNotNull()
        assertThat(DeviceTracking.getSDKId()).isNotNull()

        verify(exactly = 0) { getDeviceIdAsyncTask.execute()}
        verify(exactly = 0) { getSdkIdAsyncTask.execute()}
    }

    //TODO: cần test thêm các case:
    // - Chưa có cache: mock httpClient và trả về kết quả mong muống, sau đó assert DeviceTracking.getDeviceId()
    // trả về device id đã mock trong http response
    // - Device id đã expire: trả về cái cũ & đi request tiếp ở background
    // - Tách test của sdk ra riêng
}