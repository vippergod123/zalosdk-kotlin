package com.zing.zalo.zalosdk.core.apptracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.core.TestData
import com.zing.zalo.zalosdk.core.TestUtils
import com.zing.zalo.zalosdk.core.helper.AppInfoHelper
import com.zing.zalo.zalosdk.core.helper.AppTrackerHelper
import com.zing.zalo.zalosdk.core.helper.AppTrackerHelper.prepareDataForSubmitInstalledApp
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpResponse
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppTrackerTest {
    private lateinit var context: Context

    @MockK
    private lateinit var httpClient: HttpClient

    @MockK
    private lateinit var response: HttpResponse

    @MockK
    private lateinit var appTrackerStorage: AppTrackerStorage

    @MockK
    private lateinit var storage: Storage

    private lateinit var appTracker: AppTracker

    private var privateKey: String = AppTrackerHelper.privateKey


    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        DeviceTracking.init(context,null)

        appTracker = AppTracker(context)
    }

    @Test
    fun testAppTracking() {
        mockkObject(Utils)
        mockDataSubmitInstalledApp()
        spyk(appTracker)

        val resultJson = JSONObject(TestData.INSTALL_APP)
        val authCode = "nRHRPtwUxNE8smukCyQjIBdU0rvbeza6wArCKcUZwaAxrJTBMv_KSudR0d9qaj8wzROn0Ypu6fvGihxBlcg"

        appTracker.httpClient = httpClient
        appTracker.appTrackerStorage = appTrackerStorage
        appTracker.storage = storage

        every { storage.getOAuthCode() } returns authCode
        every { appTrackerStorage.getInstallExpireTime() } returns 0L
        every { Utils.isPackageExisted(any(), any()) } returns true
        every { httpClient.send(any()) } returns response
        every { response.getJSON() } returns resultJson

        appTracker.needToScanInstalledApp()
        appTracker.downloadPackages()
        appTracker.scanInstalledApps()
        appTracker.submitInstalledApps()

        val appTrackerListener = object:AppTrackerListener {
            override fun onAppTrackerCompleted(
                didRun: Boolean,
                scanId: String,
                packageNames: List<String>,
                installedApps: List<String>
            ) {
                assertThat(installedApps).isNotEmpty()
                assertThat(scanId).isEqualTo(AppInfoHelper.scanId)
            }

        }

        appTracker.setListener(appTrackerListener)
        TestUtils.waitTaskRunInBackgroundAndForeground()

        val jsonData = prepareDataForSubmitInstalledApp(appTracker, authCode)
        verify(exactly = 1) { appTracker.needToScanInstalledApp()}
        verify(exactly = 1) { Utils.encrypt(privateKey, jsonData.toString()) }
        verify(exactly = 1) { appTrackerStorage.setInstallExpireTime(any()) }
        verify(exactly = 1) { appTrackerStorage.getInstallExpireTime() }
    }


    @Test
    fun `testAppTracking sometimes fail to get SdkID`() {
        mockkObject(Utils)
        mockDataSubmitInstalledApp()
        spyk(appTracker)

        val resultJson = JSONObject(TestData.INSTALL_APP)
        val authCode = "nRHRPtwUxNE8smukCyQjIBdU0rvbeza6wArCKcUZwaAxrJTBMv_KSudR0d9qaj8wzROn0Ypu6fvGihxBlcg"

        appTracker.httpClient = httpClient
        appTracker.appTrackerStorage = appTrackerStorage
        appTracker.storage = storage

        every { storage.getOAuthCode() } returns authCode
        every { appTrackerStorage.getInstallExpireTime() } returns 0L
        every { Utils.isPackageExisted(any(), any()) } returns true
        every { httpClient.send(any()) } returns response
        every { response.getJSON() } returns resultJson



        appTracker.needToScanInstalledApp()
        appTracker.downloadPackages()
        appTracker.scanInstalledApps()

        every { DeviceTracking.getDeviceId() } returns  "" andThen  AppTrackerHelper.deviceId
        appTracker.submitInstalledApps()



        TestUtils.waitTaskRunInBackgroundAndForeground()

//        verify(atLeast = 1) { appTracker.submitInstalledApps() }
    }

    private fun mockDataSubmitInstalledApp() {
        AppInfoHelper.setup()
        mockkObject(DeviceTracking)
        every { DeviceTracking.getDeviceId() } returns AppTrackerHelper.deviceId
        every { DeviceTracking.getSDKId() } returns AppTrackerHelper.sdkId
        every { DeviceTracking.getPrivateKey() } returns AppTrackerHelper.privateKey

        appTracker.scanId = AppInfoHelper.scanId
        AppTracker.installedPackagedNames = TestData.INSTALLED_APP_LIST
    }
}