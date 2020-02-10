package com.zing.zalo.zalosdk.kotlin.core.apptracking

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.SdkTracking
import com.zing.zalo.zalosdk.kotlin.core.helper.*
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpResponse
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppTrackerTest {


    @MockK
    private lateinit var httpClient: HttpClient
    @MockK
    private lateinit var response: HttpResponse
    @MockK
    private lateinit var appTrackerStorage: AppTrackerStorage
    @MockK
    private lateinit var storage: Storage
    @MockK
    private lateinit var sdkTracking: SdkTracking

    private lateinit var context: Context
    private lateinit var sut: AppTracker


    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        sut = AppTracker()
        sut.deviceId = AppTrackerHelper.deviceId
    }

    @Test
    fun testAppTracking() {
        mockkObject(Utils)
        mockDataSubmitInstalledApp()
        spyk(sut)

        val resultJson = JSONObject(DataHelper.PACKAGES_NAME)
        val authCode =
            "nRHRPtwUxNE8smukCyQjIBdU0rvbeza6wArCKcUZwaAxrJTBMv_KSudR0d9qaj8wzROn0Ypu6fvGihxBlcg"

        sut.httpClient = httpClient
        sut.appTrackerStorage = appTrackerStorage
        sut.storage = storage

        every { storage.getOAuthCode() } returns authCode
        every { appTrackerStorage.getInstallExpireTime() } returns 0L
        every { Utils.isPackageExisted(any(), any()) } returns true
        every { httpClient.send(any()) } returns response
        every { response.getJSON() } returns resultJson

        sut.start(context)

        val appTrackerListener = object : AppTrackerListener {
            override fun onAppTrackerCompleted(
                didRun: Boolean,
                scanId: String,
                packageNames: List<String>,
                installedApps: List<String>
            ) {
                Log.d(scanId)
                assertThat(installedApps).isEqualTo(DataHelper.INSTALLED_APP_LIST)
                assertThat(scanId).isEqualTo(AppInfoHelper.scanId)
                assertThat(packageNames).isEqualTo(getPackagesNameArrayFromJSON())

                verify(exactly = 1) { sut.needToScanInstalledApp() }
                verify(exactly = 1) { appTrackerStorage.setInstallExpireTime(any()) }
                verify(exactly = 1) { appTrackerStorage.getInstallExpireTime() }
            }
        }

        sut.listener = appTrackerListener
        TestUtils.waitTaskRunInBackgroundAndForeground()

        shadowOf(Looper.getMainLooper()).idle()

    }


    @Test
    fun `testAppTracking sometimes fail to get SdkID`() {
        mockkObject(Utils)
        mockDataSubmitInstalledApp()
        spyk(sut)

        val resultJson = JSONObject(DataHelper.PACKAGES_NAME)
        val authCode =
            "nRHRPtwUxNE8smukCyQjIBdU0rvbeza6wArCKcUZwaAxrJTBMv_KSudR0d9qaj8wzROn0Ypu6fvGihxBlcg"

        sut.httpClient = httpClient
        sut.appTrackerStorage = appTrackerStorage
        sut.storage = storage

        every { storage.getOAuthCode() } returns authCode
        every { appTrackerStorage.getInstallExpireTime() } returns 0L
        every { Utils.isPackageExisted(any(), any()) } returns true
        every { httpClient.send(any()) } returns response
        every { response.getJSON() } returns resultJson

        sut.start(context)
        sut.needToScanInstalledApp()
        sut.downloadPackages()
        sut.scanInstalledApps()
        sut.submitInstalledApps()

        TestUtils.waitTaskRunInBackgroundAndForeground()

//        verify(atLeast = 1) { sut.submitInstalledApps() }
    }


    //#region private supportive method
    private fun mockDataSubmitInstalledApp() {
        AppInfoHelper.setup()
        every { sdkTracking.getSDKId() } returns AppTrackerHelper.sdkId
        every { sdkTracking.getPrivateKey() } returns AppTrackerHelper.privateKey

        sut.scanId = AppInfoHelper.scanId
        sut.sdkTracking = sdkTracking
        AppTracker.installedPackagedNames = DataHelper.INSTALLED_APP_LIST
    }

    private fun getPackagesNameArrayFromJSON(): ArrayList<String> {
        val jsonObject = JSONObject(DataHelper.PACKAGES_NAME)
        val error = jsonObject.getInt("error")
        if (error != 0) throw Exception("Error when call api Download Packages")

        val data = jsonObject.getJSONObject("data")
        val apps = data.optJSONArray("apps")

        Log.d("downloadPackages", data.toString())
        return UtilsJSON.jsonArrayToArrayList(apps)
    }
    //#endregion
}