package com.zing.zalo.zalosdk.kotlin.oauth.service

import android.content.Context
import android.os.Bundle
import android.os.Looper
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


open class ZaloService {
    companion object {
        private val lock = ReentrantLock()
        private val condition = lock.newCondition()
    }


    @Throws(InterruptedException::class)
    fun getUserLoggedStatus(context: Context): Int {
        if (Looper.getMainLooper().thread === Thread.currentThread()) {
            throw IllegalThreadStateException("'getUserLoggedStatus' can't run in UI-thread")
        }
        val client = PlatformServiceClient(context, NativeProtocol.CMD_GET_LOGIN_STATUS)
        val waitingResult = AtomicInteger(-1)
        client.setCompletedListener(object : PlatformServiceClient.CompletedListener {
            override fun completed(bundle: Bundle?) {
                if (bundle != null) {
                    try {
                        if (bundle.getInt(NativeProtocol.KEY_RESULT_ERROR_CODE) == 0) {
                            val data =
                                JSONObject(bundle.getString(NativeProtocol.KEY_RESULT_DATA)!!)
                            if (data.has("isUserLogged")) {
                                waitingResult.set(if (data.getBoolean("isUserLogged")) 1 else 0)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("getUserLoggedStatus", e)
                    }

                }

                lock.withLock {
                    condition.signalAll()
                }
            }
        })
        if (!client.start()) {
            return -1
        }

        lock.withLock {
            condition.await()
        }
        return waitingResult.get()
    }
}

