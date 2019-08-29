package com.zing.zalo.zalosdk.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.zing.zalo.zalosdk.auth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.auth.validateauthcode.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.log.Log

@SuppressLint("StaticFieldLeak")
object ZaloSDK
{
	private var mAuthenticator: Authenticator? = null
	private var mStorage: AuthStorage? = null
	
	private var isInitialized = false
	
	
	/**
	 * Initialize the SDK
	 * @param app Application
	 */
	fun initialize(context: Context)
	{
		if (isInitialized)
			return
		
		isInitialized = true
		mStorage = AuthStorage(context)
		mAuthenticator = Authenticator(context, mStorage!!)
	}
	
	/**
	 * Authenticate by using Zalo account
	 * @param activity Activity the login activity
	 * @param loginVia not support, SDK will login with Zalo app only.
	 * @param listener AuthCompleteListenerI listener to receive authenticate event
	 */
	fun authenticate(activity: Activity, loginVia: LoginVia, listener: IAuthenticateCompleteListener)
	{
		if (checkInitialize())
			mAuthenticator?.authenticate(activity, loginVia, listener)
	}
	

	fun unAuthenticate()
	{
		if (checkInitialize())
			mAuthenticator?.unAuthenticate()
	}
	
	fun registerZalo(activity: Activity, listener: IAuthenticateCompleteListener)
	{
		if (checkInitialize())
			mAuthenticator?.registerZalo(activity, listener)
	}
	
	fun getZaloLoginStatus(callback: GetZaloLoginStatus)
	{
		if (checkInitialize())
			mAuthenticator?.getZaloLoginStatus(callback)
	}
	
	/**
	 * Check if users have already authenticated.
	 * @param callback Callback will be called after verify with server. If passed null, no server verification will be made.
	 * @return True if oauth code cached, otherwise false
	 */
	fun isAuthenticate(callback: ValidateOAuthCodeCallback): Boolean
	{
		if (checkInitialize()) {
			return mAuthenticator?.isAuthenticate(mStorage?.getOAuthCode().toString(), callback)!!
		}
		return false
	}
	
	/**
	 * Get the current app id
	 * @return App id
	 */
	fun getAppID(context: Context): Long
	{
		return AppInfo.getAppIdLong(context)
	}

	
	fun getVersion(): String
	{
		return Constant.core.VERSION
	}
	
	/**
	 * Set language for ZaloSDK
	 * language: vi, my
	 */
     private fun setLanguageSDK(language: String)
	{
		Utils.setLanguage(language)
	}
	
	fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?): Boolean
	{
		if (checkInitialize())
			return mAuthenticator?.onActivityResult(activity, requestCode, resultCode, data)!!
		return false
	}
	
	
	//================================================================================
	// Utility methods //TODO: nen remove
	//================================================================================
	fun setAuthStorage(storage: AuthStorage, context: Context)
     {
          this.mStorage = storage
		 mAuthenticator = Authenticator(context, mStorage!!)
     }

	private fun checkInitialize(): Boolean {
		if (isInitialized && mAuthenticator != null)
			return true

		Log.d("Missing call declare com.zing.zalo.zalosdk.oauth.ZaloSDKApplication in Application or call wrap init")
		return false
	}
	
	
}