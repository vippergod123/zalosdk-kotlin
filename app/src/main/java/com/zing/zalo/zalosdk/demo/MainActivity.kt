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
import com.zing.zalo.zalosdk.kotlin.core.apptracking.AppTracker
import com.zing.zalo.zalosdk.kotlin.core.apptracking.AppTrackerListener
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.oauth.*
import com.zing.zalo.zalosdk.kotlin.oauth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.kotlin.oauth.callback.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthStorage


class MainActivity : AppCompatActivity(), ValidateOAuthCodeCallback, GetZaloLoginStatus {
    private lateinit var loginWebButton: Button
    private lateinit var loginViaButton: Button
    private lateinit var loginMobileButton: Button
    private lateinit var registerButton: Button
    private lateinit var validateButton: Button
    private lateinit var checkAppLoginButton: Button
    private lateinit var appTrackingButton: Button
    private lateinit var eventTrackingButton: Button
    private lateinit var openApiButton: Button
    private lateinit var appIDTextView: TextView
    private lateinit var loginStatusTextView: TextView
    private lateinit var authCodeTextView: TextView
    private lateinit var userIDTextView: TextView

    private lateinit var mStorage: AuthStorage
    
    private lateinit var zaloSDK:ZaloSDK

    private val appTrackerListener: AppTrackerListener = object : AppTrackerListener {
        override fun onAppTrackerCompleted(
            didRun: Boolean,
            scanId: String,
            packageNames: List<String>,
            installedApps: List<String>
        ) {
            if (!didRun)
                Log.d("appTrackerListener", "Got into main activity")
            if (didRun && installedApps.isNotEmpty())
                Log.d("appTrackerListener", "Submit complete")
        }

    }

    private val authenticateListener = object : IAuthenticateCompleteListener {
        @SuppressLint("SetTextI18n")
        override fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>) {
            val displayName = data[Constant.user.DISPLAY_NAME]
            authCodeTextView.text = "Auth code: $code"
            userIDTextView.text = "User: $displayName \nUID: $uid"
        }

        override fun onAuthenticateError(errorCode: Int, message: String) {
            if (!TextUtils.isEmpty(message)) {
                showAlertDialog(message)
                authCodeTextView.text = null
                userIDTextView.text = null
            }
        }
    }

//    private val eventTrackerListener = object : EventTrackerListener {
//        override fun dispatchComplete() {
//            super.dispatchComplete()
//            Log.d("got Main Activity ")
//        }
//    }

    //#region override activity method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.setLogLevel()
        bindUI()
        configureLogic()
        configureUI()
        bindViewsListener()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        zaloSDK.onActivityResult(this, requestCode, resultCode, data)
    }
    //#endregion

    //#region override interface method
    @SuppressLint("SetTextI18n")
    override fun onValidateComplete(
        validated: Boolean,
        errorCode: Int,
        userId: Long,
        authCode: String?
    ) {
        showToast("validated: $validated - errorCode: $errorCode")

        authCodeTextView.text = authCode

        val userIDText: String? = userId.toString()
        userIDTextView.text = userIDText

    }

    @SuppressLint("SetTextI18n")
    override fun onGetZaloLoginStatusCompleted(status: Int) {
        runOnUiThread {
            when (status) {
                1 -> {
                    loginStatusTextView.text = "Zalo login: yes"
                    showToast("Zalo login: yes")
                }
                0 -> {
                    loginStatusTextView.text = "Zalo login: no"
                    showToast("Zalo login: no")
                }
                else -> {
                    loginStatusTextView.text = "Error: $status"
                    showToast("Error: $status")
                }
            }

        }
    }
    //#endregion

    private fun bindUI() {
        loginMobileButton = findViewById(R.id.login_mobile_button)
        loginViaButton = findViewById(R.id.login_via_button)
        loginWebButton = findViewById(R.id.login_web_button)
        registerButton = findViewById(R.id.register_button)
        validateButton = findViewById(R.id.validate_oauth_code_button)
        checkAppLoginButton = findViewById(R.id.check_app_login_button)
        openApiButton = findViewById(R.id.open_api_button)
        eventTrackingButton = findViewById(R.id.event_tracking_button)
        appTrackingButton = findViewById(R.id.app_tracking_button)
        eventTrackingButton = findViewById(R.id.event_tracking_button)


        appIDTextView = findViewById(R.id.app_id_text_view)
        userIDTextView = findViewById(R.id.user_id_text_view)
        authCodeTextView = findViewById(R.id.auth_code_text_view)
        loginStatusTextView = findViewById(R.id.login_status_text_view)

//        ZaloSDK.initialize(this)
    }

    @SuppressLint("SetTextI18n")
    private fun configureUI() {
        mStorage = AuthStorage(this)


        appIDTextView.text = "App ID: ${AppInfo.getAppId(this)}"
        authCodeTextView.text = "Auth code: ${zaloSDK.getOauthCode()}"
        userIDTextView.text = "User ID: ${mStorage.getZaloDisplayName()}"
    }

    private fun configureLogic() {
        zaloSDK = ZaloSDK(this)
    }
    private fun bindViewsListener() {
        loginMobileButton.setOnClickListener {
            zaloSDK.unAuthenticate()
            zaloSDK.authenticate(this, LoginVia.APP, authenticateListener)
        }

        loginWebButton.setOnClickListener {
            zaloSDK.unAuthenticate()
            zaloSDK.authenticate(this, LoginVia.WEB, authenticateListener)
        }
        loginViaButton.setOnClickListener {
            zaloSDK.unAuthenticate()
            zaloSDK.authenticate(this, LoginVia.APP_OR_WEB, authenticateListener)
        }

        registerButton.setOnClickListener {
            //			ZaloSDK.unAuthenticate()
            zaloSDK.registerZalo(this, authenticateListener)
        }

        validateButton.setOnClickListener {
            zaloSDK.isAuthenticate(this)
        }

        checkAppLoginButton.setOnClickListener {
            zaloSDK.getZaloLoginStatus(this)
        }

        appTrackingButton.setOnClickListener {
            val appTracker = AppTracker()
            appTracker.listener = appTrackerListener
        }

        eventTrackingButton.setOnClickListener {

//            val eventTracker = EventTracker.getInstance()
//
//            eventTracker.addEvent(mockEvent())
//            eventTracker.setListener(eventTrackerListener)
//            eventTracker.dispatchEventImmediate(mockEvent())
        }

        openApiButton.setOnClickListener {
            val intent = Intent(this, OpenApiActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showAlertDialog(message: String) {
        AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(message)
            .setPositiveButton(android.R.string.yes, null).show()
    }

//    private fun mockEvent(): Event {
//        val timeStamp = System.currentTimeMillis()
//        val action = "action-$timeStamp"
//        val params = mutableMapOf<String, String>()
//
//        params["name"] = "datahelper-$timeStamp"
//        params["age"] = timeStamp.toString()
//        return Event(action, params, timeStamp)
//    }

}
