package com.zing.zalo.zalosdk.kotlin.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.oauth.web.ZaloWebLoginBaseFragment
import com.zing.zalo.zalosdk.kotlin.oauth.web.ZaloWebLoginFragment
import com.zing.zalo.zalosdk.kotlin.oauth.web.ZaloWebRegisterFragment
import org.json.JSONException
import org.json.JSONObject

class WebLoginActivity : FragmentActivity(),
    ZaloWebLoginBaseFragment.ZaloWebLoginBaseFragmentListener,
    View.OnClickListener {
    private lateinit var loginFragment: ZaloWebLoginFragment
    private lateinit var frameLayout: FrameLayout
    private lateinit var titleView: TextView
    private lateinit var backButton: ImageView

    private lateinit var zaloSDK: ZaloSDK

    private var registerOnly: Boolean = false
    private var frameLayoutId: Int = 0


    companion object {
        fun newIntent(context: Context, registerOnly: Boolean): Intent {
            val intent = Intent(context, WebLoginActivity::class.java)
            intent.putExtra("registerOnly", registerOnly)

            return intent
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        zaloSDK.onActivityResult(this, requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeStatusBarColor()
        registerOnly = intent.getBooleanExtra("registerOnly", false)
        //show web login view
        initLateVar()
        configureUI()
        bindViewListener()


        if (savedInstanceState == null) {
            if (registerOnly) {
                val registerFragment = ZaloWebRegisterFragment
                val fragmentManager = supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(frameLayoutId, registerFragment, "register-fragment")
                transaction.commit()
            } else {
                showWebLoginFragment()
            }
        }
    }


    override fun setTitle(title: String) {
        titleView.text = title
    }

    override fun onLoginCompleted(
        error: Int,
        uid: Long,
        oauth: String,
        zProtect: Int,
        name: String,
        isRegister: Boolean
    ) {
        val data = Intent()
        data.putExtra("error", error)
        data.putExtra(Constant.user.UID, uid)
        data.putExtra(Constant.user.AUTH_CODE, oauth)
        data.putExtra(Constant.user.IS_REGISTER, isRegister)

        val extra = JSONObject()
        val extraData = JSONObject()
        try {
            extraData.put(Constant.user.DISPLAY_NAME, name)
            extraData.put("zprotect", zProtect)
            extra.put("data", extraData)
        } catch (ex: JSONException) {
            Log.w("onLoginCompleted", ex)
        }



        data.putExtra("data", extra.toString())
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onClick(p0: View?) {
        if (p0 === backButton) {
            hideSoftKeyboard()
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (backButton.visibility == View.VISIBLE) {
            super.onBackPressed()
        }
    }

    private fun configureUI() {
        setContentView(R.layout.zalosdk_activity_zalo_web_login)
        frameLayoutId = R.id.zalosdk_weblogin_container
        frameLayout = findViewById(frameLayoutId)

        titleView = findViewById(R.id.zalosdk_txt_title)
        backButton = findViewById(R.id.zalosdk_back_control)
    }

    private fun initLateVar() {
        zaloSDK = ZaloSDK(this)
    }

    private fun bindViewListener() {
        backButton.setOnClickListener(this)
    }

    private fun changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= 21) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.zing_pressed)
        }
    }

    private fun showWebLoginFragment() {
        loginFragment = ZaloWebLoginFragment.newInstance()
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(frameLayoutId, loginFragment, "login-fragment")
        transaction.commitAllowingStateLoss()
    }

    private fun hideSoftKeyboard() {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val focus = currentFocus
        if (focus != null) {
            inputMethodManager.hideSoftInputFromWindow(
                focus.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
    }

}