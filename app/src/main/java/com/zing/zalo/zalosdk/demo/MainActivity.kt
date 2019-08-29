package com.zing.zalo.zalosdk.demo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zing.zalo.zalosdk.auth.*
import com.zing.zalo.zalosdk.auth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.auth.validateauthcode.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager


class MainActivity : AppCompatActivity(), ValidateOAuthCodeCallback, GetZaloLoginStatus
{
     
     
     private lateinit var loginWebButton: Button
     private lateinit var loginViaButton: Button
     private lateinit var loginMobileButton: Button
     private lateinit var registerButton: Button
     private lateinit var validateButton: Button
     private lateinit var checkAppLoginButton: Button
     
     private lateinit var appIDTextView: TextView
     private lateinit var loginStatusTextview: TextView
     private lateinit var authCodeTextView: TextView
     private lateinit var userIDTextview: TextView
     
     private lateinit var mStorage: AuthStorage


     private val listener = object : IAuthenticateCompleteListener
     {
          @SuppressLint("SetTextI18n")
          override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
               val displayName = data[Constant.user.DISPLAY_NAME]
               authCodeTextView.text = "Auth code: $code"
               userIDTextview.text = "User: $displayName \nUID: $uid"
          }

          override fun onAuthenticateError(errorCode: Int, message: String)
          {
               if (!TextUtils.isEmpty(message))
               {
                    showAlertDialog(message)
                    authCodeTextView.text = null
                    userIDTextview.text = null
               }
          }
     }
     
     //#region override activity method
     override fun onCreate(savedInstanceState: Bundle?)
     {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.activity_main)
          ServiceMapManager.load(this)
          bindUI()
          configureUI()
          bindViewsListener()
     }
     
     
     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
     {
          super.onActivityResult(requestCode, resultCode, data)
          ZaloSDK.onActivityResult(this, requestCode, resultCode, data)
     }
     //#endregion
     
     //#region override interface method
     @SuppressLint("SetTextI18n")
     override fun onValidateComplete(validated: Boolean, errorCode: Int, userId: Long, authCode: String?)
     {
          showToast("validated: $validated - errorCode: $errorCode")

          authCodeTextView.text = authCode

          val userIDText: String? = userId.toString()
          userIDTextview.text = userIDText

     }
     
     @SuppressLint("SetTextI18n")
     override fun onGetZaloLoginStatusCompleted(status: Int)
     {
          runOnUiThread {
               when (status)
               {
                    1    ->
                    {
                         loginStatusTextview.text = "Zalo login: yes"
                         showToast("Zalo login: yes")
                    }
                    0    ->
                    {
                         loginStatusTextview.text = "Zalo login: no"
                         showToast("Zalo login: no")
                    }
                    else ->
                    {
                         loginStatusTextview.text = "Error: $status"
                         showToast("Error: $status")
                    }
               }
               
          }
     }
     //#endregion
     
     private fun bindUI()
     {
          loginMobileButton = findViewById(R.id.login_mobile_button)
          loginViaButton = findViewById(R.id.login_via_button)
          loginWebButton = findViewById(R.id.login_web_button)
          registerButton = findViewById(R.id.register_button)
          validateButton = findViewById(R.id.validate_oauth_code_button)
          checkAppLoginButton = findViewById(R.id.check_app_login_button)
          
          appIDTextView = findViewById(R.id.app_id_text_view)
          userIDTextview = findViewById(R.id.user_id_text_view)
          authCodeTextView = findViewById(R.id.auth_code_text_view)
          loginStatusTextview = findViewById(R.id.login_status_text_view)
          
          ZaloSDK.initialize(this)
     }
     
     @SuppressLint("SetTextI18n")
     private fun configureUI()
     {
          mStorage = AuthStorage(this)
          
          
          appIDTextView.text = "App ID: ${AppInfo.getAppId(this)}"
          authCodeTextView.text = "Auth code: ${mStorage.getOAuthCode()}"
          userIDTextview.text = "User ID: ${mStorage.getZaloDisplayName()}"
     }
     
     private fun bindViewsListener()
     {
          loginMobileButton.setOnClickListener {
               ZaloSDK.unAuthenticate()
               ZaloSDK.authenticate(this, LoginVia.APP, listener)
          }
          
          loginWebButton.setOnClickListener {
               ZaloSDK.unAuthenticate()
               ZaloSDK.authenticate(this, LoginVia.WEB, listener)
          }
          loginViaButton.setOnClickListener {
               ZaloSDK.unAuthenticate()
               ZaloSDK.authenticate(this, LoginVia.APP_OR_WEB, listener)
          }
          
          registerButton.setOnClickListener {
               //			ZaloSDK.unAuthenticate()
               ZaloSDK.registerZalo(this, listener)
          }
          
          validateButton.setOnClickListener {
               ZaloSDK.isAuthenticate(this)
          }
          
          checkAppLoginButton.setOnClickListener {
               ZaloSDK.getZaloLoginStatus(this)
          }
     }
     
     private fun showToast(msg: String)
     {
          Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
     }
     
     private fun showAlertDialog(message: String)
     {
          AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(message)
               .setPositiveButton(android.R.string.yes, null).show()
     }
     
}
