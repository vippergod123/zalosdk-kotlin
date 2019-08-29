package com.zing.zalo.zalosdk.demo

import android.content.Context
import android.content.Intent
import android.os.Build
import com.zing.zalo.zalosdk.auth.*
import com.zing.zalo.zalosdk.auth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.auth.validateauthcode.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.core.SettingsManager
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.log.Log
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], manifest = Config.NONE)
class IAuthenticateTest : IAuthenticateCompleteListener, ValidateOAuthCodeCallback, GetZaloLoginStatus {
    override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
        Log.d("Got here - onAuthenticateSuccess test")
    }

    override fun onAuthenticateError(errorCode: Int, message: String) {
        Log.d("Got here - onAuthenticateError test")
    }

     private lateinit var mAuthenticator: Authenticator
     private lateinit var activity: MainActivity
     private lateinit var context: Context
     
     
     private val oAuthCode = "emHsrXosE4ZzBYBW5AGRJjXY8fnS-WvCc6TjjqxSDsx9Ks_A7ifmO85jIRPybd1c-WfVjmg5B63X0dIqQuml6RyKDRPJYGfOboSGinIy9sJHFNoDFQ4cNxWsNDqO_XiphYW4xqJN93ZMKN6nHOjWKRziDP11jJz_odLmnm_-RXdm42ovR_OCRw9v9y4HfoqFuq82fp-W03x0837W0CuxHxfWAeT0p3f9ooDqhdIK4HlSyJg30qmhF8VL1fuHAJegijzrf48aCmhKmmpNPqbYQ-hH1jjP9XRyzMjzlHWt"
     
     @MockK
     private lateinit var storage: AuthStorage
     
     override fun onGetZaloLoginStatusCompleted(status: Int)
     {
          Log.d("Got to onGetZaloLoginStatusCompleted")
     }

    override fun onValidateComplete(validated: Boolean, errorCode: Int, userId: Long, authCode: String?)
     {
          Log.d("Got to onValidateComplete")
     }
     
     
     private fun returnExpectedValues()
     {
          every { storage.getOAuthCode() } returns oAuthCode
     }
     
     @Before
     fun setUp()
     {
          MockKAnnotations.init(this, relaxUnitFun = true)
          
          activity = Robolectric.buildActivity(MainActivity::class.java).create().resume().get()
          context = activity.applicationContext
          
          mockkObject(SettingsManager)
          mockkObject(AppInfo)
          
          ZaloSDK.initialize(context)
          ZaloSDK.setAuthStorage(storage, context)

         mAuthenticator = Authenticator(context, storage)
          
          returnExpectedValues()
     }
     
     
     @Test
     fun `login via web`()
     {
          ZaloSDK.authenticate(activity, LoginVia.WEB, this)
     }
     
     @Test
     fun `login via app`()
     {
//          ZaloSDK.authenticate(activity, LoginVia.APP, this)
          mAuthenticator.loginViaApp(activity)
     }
     
     @Test
     fun `login via app or web `()
     {
          //App not install
          ZaloSDK.authenticate(activity, LoginVia.APP_OR_WEB, this)
          
          //App  installed
          every { SettingsManager.isUseWebViewUnLoginZalo(context) } returns true
         every { AppInfo.isPackageExists(activity, Constant.core.ZALO_PACKAGE_NAME) } returns true
          ZaloSDK.authenticate(activity, LoginVia.APP_OR_WEB, this)
     }
     
     @Test
     fun register()
     {
          ZaloSDK.registerZalo(activity, this)
     }
     
     @Test
     fun unAuthenticate()
     {
          ZaloSDK.unAuthenticate()
          helper.waitTaskRunInBackgroundAndForeground()
     }
     
     @Test
     fun isAuthenticate()
     {
          ZaloSDK.isAuthenticate(this)
     }
     
     @Test
     fun `check app login`()
     {
          ZaloSDK.getZaloLoginStatus(this)
     }
     
     @Test
     fun `on Activity Result`()
     {
          val data = Intent()
          val requestCode = 64725
          val resultCode = -1
          val uid = 7793733042068913573
          val authCode = "MbyxZTW1D1W4TM-xq1CN7Y4vRyUOJG8SOJmWx8fhEpun1IQRtNbqEdyo3EQo97Kp25CQxiKmA38JMp7jfpWdGaX9U-EJE0K6QdfUvjGBA3mYUIxLzGat1aro38_GLWfgMsnKlOn_DM4Z5I7ZZIDn24eePSA97IucFp4WbSTMVq05L7phfruTDbSzTvFP3pnM5WPIzVS84MSrP66oo6Og7ayaUjM8P3wDFMSnORyyuWXFjeYRhUVOBnIIj8Q_Wiav8PZ7lEoFm1b7XBxFoUAHU7cMjCDP9YiGj62v2XHd"
          val jsonData = "{\"data\":{\"uid\":7793733042068913573,\"code\":\"MbyxZTW1D1W4TM-xq1CN7Y4vRyUOJG8SOJmWx8fhEpun1IQRtNbqEdyo3EQo97Kp25CQxiKmA38JMp7jfpWdGaX9U-EJE0K6QdfUvjGBA3mYUIxLzGat1aro38_GLWfgMsnKlOn_DM4Z5I7ZZIDn24eePSA97IucFp4WbSTMVq05L7phfruTDbSzTvFP3pnM5WPIzVS84MSrP66oo6Og7ayaUjM8P3wDFMSnORyyuWXFjeYRhUVOBnIIj8Q_Wiav8PZ7lEoFm1b7XBxFoUAHU7cMjCDP9YiGj62v2XHd\",\"gender\":\"male\",\"phone\":\"\",\"dob\":\"22\\/08\\/1989\",\"socialId\":\"\",\"display_name\":\"Duydbidjmdkdldidj́smdhshsnmmwhsnshsnsush\"},\"error\":0,\"errorMsg\":\"\"}"

         data.putExtra(Constant.user.UID, uid)
         data.putExtra(Constant.user.AUTH_CODE, authCode)
         data.putExtra(Constant.user.IS_REGISTER, false)
          data.putExtra("data", jsonData)

         data.putExtra("error", Constant.RESULT_CODE_SUCCESSFUL)
          ZaloSDK.onActivityResult(activity, requestCode, resultCode, data)

         data.putExtra("error", Constant.RESULT_CODE_ZALO_NOT_LOGIN)
          ZaloSDK.onActivityResult(activity, requestCode, resultCode, data)
          
          data.putExtra("error", -120321)
          ZaloSDK.onActivityResult(activity, requestCode, resultCode, data)
     }
     
}