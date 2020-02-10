package com.zing.zalo.zalosdk.kotlin.core.module

import android.content.Context
import com.zing.zalo.zalosdk.kotlin.core.log.Log

interface IModule {
    fun start(context: Context)
    fun stop()
}

abstract class BaseModule: IModule {
    protected var context: Context? = null
        private set

    protected val hasContext: Boolean
        get() = context != null


    override fun start(context: Context) {
        Log.d("Start module ${javaClass.simpleName}")
        this.context = context.applicationContext
        onStart(context)
    }

    override fun stop() {
        Log.d("Stop module ${javaClass.simpleName}")
        onStop()
    }

    protected open fun onStart(context: Context) {}
    protected open fun onStop() {}
}
