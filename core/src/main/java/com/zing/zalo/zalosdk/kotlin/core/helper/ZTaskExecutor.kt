package com.zing.zalo.zalosdk.kotlin.core.helper

import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object ZTaskExecutor
{
    interface TaskQueue {
        fun queueRunnable(runnable: Runnable)
        fun queueDelayedRunnable(runnable: Runnable, milliseconds: Long)
    }

    private var mExecutor: ThreadPoolExecutor? = null
    private var mTaskQueue: TaskQueue = DefaultTaskQueue()

    init {
        mExecutor = ThreadPoolExecutor(1, 3, 30L, TimeUnit.SECONDS, ArrayBlockingQueue(100))
        mExecutor!!.threadFactory = MyThreadFactory("ZDKTaskExecutor")
        if (Build.VERSION.SDK_INT >= 14) {
            mExecutor!!.allowCoreThreadTimeOut(true)
        }
    }

    fun queueRunnable(runnable: Runnable) {
        mTaskQueue.queueRunnable(runnable)
    }
//
//    fun queueDelayedRunnable(runnable: Runnable, milliseconds: Long) {
//        mTaskQueue.queueDelayedRunnable(runnable, milliseconds)
//    }
//
//    fun setTaskQueue(taskQueue: TaskQueue) {
//        mTaskQueue = taskQueue
//    }


    private class DefaultTaskQueue : TaskQueue {
        internal var handler: Handler = Handler(Looper.getMainLooper())

        override fun queueRunnable(runnable: Runnable) {
            mExecutor!!.execute(runnable)
        }

        override fun queueDelayedRunnable(runnable: Runnable, milliseconds: Long) {
            handler.postDelayed({ queueRunnable(runnable) }, milliseconds)
        }
    }

}

internal class MyThreadFactory(poolName: String) : ThreadFactory
{
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String = "pool-$poolName-thread-"

    override fun newThread(r: Runnable): Thread {
        val t = Thread(r, namePrefix + threadNumber.getAndIncrement())
        t.priority = Thread.MAX_PRIORITY
        return t
    }
}

