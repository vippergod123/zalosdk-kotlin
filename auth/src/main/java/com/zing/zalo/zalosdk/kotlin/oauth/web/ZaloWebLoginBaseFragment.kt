package com.zing.zalo.zalosdk.kotlin.oauth.web

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import com.zing.zalo.zalosdk.kotlin.oauth.R
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.regex.Pattern

abstract class ZaloWebLoginBaseFragment : Fragment() {
    private var listener: ZaloWebLoginBaseFragmentListener? = null
    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar

    companion object {
        //		private var WEB_LOGIN_URL: String = if (Constant.IS_DEV == true) "http://dev-oauth.zaloapp.com/v3/auth?app_id="
//		else "https://oauth.zaloapp.com/v3/auth?app_id="
        private var WEB_LOGIN_URL: String =
            ServiceMapManager.getInstance()
                .urlFor(ServiceMapManager.KEY_URL_OAUTH, "/v3/auth?app_id=")
        private val WZUIN = Pattern.compile("(wzuin)([^;][\\D\\w])*")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as ZaloWebLoginBaseFragmentListener
        } catch (ex: ClassCastException) {
            Log.e(context.javaClass.simpleName + " must implement " + ZaloWebLoginBaseFragmentListener::class.java.simpleName)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.zalosdk_fragment_zalo_web_login, container, false)
        setupWebView(view)

        progressBar = view.findViewById(R.id.zalosdk_progress)
        progressBar.visibility = View.VISIBLE//SOFT_INPUT_ADJUST_PAN

        val activity = activity
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        return view

    }

    fun setTitle(title: String) {
        if (listener != null) listener!!.setTitle(title)
    }

    open fun onLoginCompleted(
        error: Int,
        uid: Long,
        oauth: String,
        zProtect: Int,
        name: String,
        isRegister: Boolean
    ) {
        if (listener != null) listener!!.onLoginCompleted(
            error,
            uid,
            oauth,
            zProtect,
            name,
            isRegister
        )
    }

    @Suppress("DEPRECATION")
    private fun setupWebView(parentView: View) {
        webView = parentView.findViewById(R.id.zalosdk_login_webview)

        val callbackUrl = "http://${context?.packageName}"

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.settings.allowContentAccess = true

        val webClient = LoginEventDispatcher(WeakReference(this), callbackUrl)
        webView.webViewClient = webClient

        try {
            var currentUserAgent: String? = webView.settings.userAgentString
            if (currentUserAgent != null && !TextUtils.isEmpty(currentUserAgent)) {
                currentUserAgent += "ZaloSDK"
            } else {
                currentUserAgent = "ZaloSDK"
            }
            webView.settings.userAgentString = currentUserAgent
        } catch (ex: Exception) {
            Log.w("setupWebView", ex)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // chromium, enable hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            // older android version, disable hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        val sUrl = generateLoginUrl(context!!)
        webView.loadUrl(sUrl)
    }

    internal open fun generateLoginUrl(ctx: Context): String {
        val url = StringBuilder()
        url.append(WEB_LOGIN_URL)
        try {
            url.append(AppInfo.getAppIdLong(ctx))
            url.append("&sign_key=")
            url.append(URLEncoder.encode(AppInfo.getApplicationHashKey(ctx), "UTF-8"))
            url.append("&pkg_name=")
            url.append(URLEncoder.encode(ctx.packageName, "UTF-8"))
            url.append("&orientation=")
            url.append(ctx.resources.configuration.orientation)
            url.append("&zregister=true")
            url.append("&ts=")
            url.append(System.currentTimeMillis())
            url.append("&lang=")
            url.append(Utils.getLanguage())
        } catch (e: UnsupportedEncodingException) {
            Log.w("generateLoginUrl", e)
        }

        return url.toString()
    }

    interface ZaloWebLoginBaseFragmentListener {
        fun setTitle(title: String)
        fun onLoginCompleted(
            error: Int,
            uid: Long,
            oauth: String,
            zprotect: Int,
            name: String,
            isRegister: Boolean
        )
    }
}