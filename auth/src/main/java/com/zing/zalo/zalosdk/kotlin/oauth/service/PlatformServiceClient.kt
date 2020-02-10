package com.zing.zalo.zalosdk.kotlin.oauth.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.*
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.log.Log


class PlatformServiceClient(var context: Context, private val requestMessage: Int) :
    ServiceConnection {
    private val handler: Handler
    private var listener: CompletedListener? = null
    private var running: Boolean = false
    private var sender: Messenger? = null

    interface CompletedListener {
        fun completed(bundle: Bundle?)
    }

    init {
        var applicationContext: Context? = context.applicationContext
        if (applicationContext == null) {
            applicationContext = context
        }
        context = applicationContext
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                this@PlatformServiceClient.handleMessage(message)
            }
        }
    }

    internal fun setCompletedListener(listener: CompletedListener) {
        this.listener = listener
    }

    @SuppressLint("WrongConstant")
    fun start(): Boolean {
        if (this.running) {
            return false
        }
        val intent = NativeProtocol.createPlatformServiceIntent(context) ?: return false
        running = true
        context.bindService(intent, this, 1)
        return true
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        sender = Messenger(service)
        sendMessage()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        sender = null
        try {
            this.context.unbindService(this)
        } catch (e: IllegalArgumentException) {
        }

        callback(null)
    }

    private fun sendMessage() {
        val data = Bundle()
        data.putString(NativeProtocol.KEY_REQUEST_APPLICATION_ID, AppInfo.getAppId(context))
//		populateRequestBundle(data)
        val request = Message.obtain(null, this.requestMessage)
        request.arg1 = 1
        request.arg2 = 1
        request.data = data
        request.replyTo = Messenger(handler)
        try {
            sender!!.send(request)
        } catch (e: RemoteException) {
            callback(null)
        }

    }

//	private fun populateRequestBundle(data: Bundle)
//	{
//	}

    private fun handleMessage(message: Message) {
        if (message.what == requestMessage && message.arg2 == 2) {
            val extras = message.data
            if (extras.getInt(NativeProtocol.KEY_RESULT_ERROR_CODE) == 0) {
                callback(extras)
            } else {
                callback(null)
            }
            try {
                context.unbindService(this)
            } catch (e: IllegalArgumentException) {
                Log.e("PlatformServiceClient: handleMessage() ", e)
            }

        }
    }

    private fun callback(result: Bundle?) {
        if (running) {
            running = false
            val callback = listener
            callback!!.completed(result)
        }
    }
}
