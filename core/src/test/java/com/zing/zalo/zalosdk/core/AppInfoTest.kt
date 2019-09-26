package com.zing.zalo.zalosdk.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.zing.zalo.zalosdk.core.helper.AppInfo
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.collections.HashMap


@RunWith(RobolectricTestRunner::class)
class AppInfoTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test AppInfo`(){

    }
}
