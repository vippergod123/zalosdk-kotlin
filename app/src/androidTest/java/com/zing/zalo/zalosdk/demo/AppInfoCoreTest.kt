package com.zing.zalo.zalosdk.demo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiObjectNotFoundException
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppInfoCoreTest : AppBase() {
    @Before
    @Throws(IOException::class, UiObjectNotFoundException::class)
    override fun startApp() {
        super.startApp()

    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun testGetApplicationHashKey() {
        val hashKey = AppInfo.getApplicationHashKey(context)
        assertEquals("RtcyQvjS3YtXLs/yEPwB8LN3Hr0=", hashKey)
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun testAppInfoPackage() {
        //Todo: switch
        val isExist = AppInfo.isPackageExists(context, "com.zing.zalo")
//        val isExist = AppInfo.isPackageExists(context, Constant.core.ZALO_PACKAGE_NAME)
        
        assertEquals(isExist, true)

        val packageName = AppInfo.getPackageName(context)
        assertEquals(packageName, "com.zing.zalo.zalosdk.demo")
    }

    @Test
    @Throws(UiObjectNotFoundException::class, IOException::class)
    fun testGetAppID() {
        val appIdString = AppInfo.getAppId(context)
        assertEquals("1829577289837795818", appIdString)

        val appIdLong = AppInfo.getAppIdLong(context)
        assertEquals(1829577289837795818L, appIdLong)
    }

}