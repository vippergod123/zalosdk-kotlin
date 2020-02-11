package com.zing.zalo.zalosdk.demo

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNull.notNullValue
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
abstract class AppBase {
    lateinit var device: UiDevice
    lateinit var context: Context

    @Throws(IOException::class, UiObjectNotFoundException::class)
    open fun startApp() {

        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Start from the home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage = device.launcherPackageName
        assertThat<String>(launcherPackage, notNullValue())
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            LAUNCH_TIMEOUT.toLong()
        )

        // Launch the app
        context = ApplicationProvider.getApplicationContext()
        val intent = context.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE_NAME)
        // Clear out any previous instances
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        // Wait for the app to appear
        device.wait(
            Until.hasObject(By.pkg(APP_PACKAGE_NAME).depth(0)),
            LAUNCH_TIMEOUT.toLong()
        )
    }

    @Throws(UiObjectNotFoundException::class)
    internal fun clickButtonWithResId(resId: String) {
        device.findObject(
            UiSelector()
                .resourceId(resId)
        )
            .clickAndWaitForNewWindow()
    }

    @Throws(UiObjectNotFoundException::class)
    internal fun clickButtonWithText(text: String) {
        val uiSelector = UiSelector()
            .text(text)
            .className("android.widget.Button")
        device.findObject(uiSelector)
            .clickAndWaitForNewWindow()
    }

    companion object {
        private const val LAUNCH_TIMEOUT = 5000
        internal const val APP_PACKAGE_NAME = "com.zing.zalo.zalosdk.demo"
    }
}
