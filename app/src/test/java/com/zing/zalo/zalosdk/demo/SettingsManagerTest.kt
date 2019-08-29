package com.zing.zalo.zalosdk.demo

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.core.GetSDKSettingAsyncTask
import com.zing.zalo.zalosdk.core.SettingsManager
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsManagerTest {
    private lateinit var context: Context
    private lateinit var activity: MainActivity

    private var currentTime = System.currentTimeMillis()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        activity = Robolectric.buildActivity(MainActivity::class.java).create().resume().get()
        context = activity.applicationContext
    }

    @Test
    fun `Storage Setting Manager Test`() {

        //1. Set Value
        SettingsManager.setting(context).setExpiredSetting(currentTime)
        SettingsManager.setting(context).setLastTimeWakeUp(123L)
        SettingsManager.setting(context).setWakeUpInterval(123L)
        SettingsManager.setting(context).setWakeUpSetting(true)


        //2. Assert value
        assertThat(currentTime).isEqualTo(SettingsManager.setting(context).getExpiredSetting())
        assertThat(123L).isEqualTo(SettingsManager.setting(context).getLastTimeWakeup())
        assertThat(123L).isEqualTo(SettingsManager.setting(context).getWakeUpInterval())
        assertThat(true).isEqualTo(SettingsManager.setting(context).getWakeupSetting())

        // check is not Expired Setting
        SettingsManager.setting(context).setExpiredSetting(currentTime + 100000000000)
        assertThat(false).isEqualTo(SettingsManager.setting(context).isExpiredSetting())

        // check ís Expired Setting
        SettingsManager.setting(context).setExpiredSetting(currentTime - 100000000000)
        assertThat(true).isEqualTo(SettingsManager.setting(context).isExpiredSetting())
    }

    @Test
    fun `Get SDK Setting Test`() {
        GetSDKSettingAsyncTask(context, "3000.4258336839922934833").execute()
        helper.waitTaskRunInBackgroundAndForeground()
//        assertThat(currentTime).isEqualTo(SettingsManager.setting(context).getExpiredSetting())
        assertThat(true).isEqualTo(SettingsManager.setting(context).getWakeupSetting())

    }

}