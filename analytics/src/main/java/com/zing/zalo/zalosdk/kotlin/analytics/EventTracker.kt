package com.zing.zalo.zalosdk.kotlin.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTrackingListener
import com.zing.zalo.zalosdk.kotlin.analytics.model.Event
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.module.BaseModule
import com.zing.zalo.zalosdk.kotlin.core.module.ModuleManager
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

@SuppressLint("StaticFieldLeak")
class EventTracker : BaseModule(), IEventTracker {
    companion object {
        const val ACT_DISPATCH_EVENTS = 0x5000
        const val ACT_DISPATCH_EVENT_IMMEDIATE = 0x5001
        const val ACT_PUSH_EVENTS = 0x5002
        const val DELAY_SECOND = 120

        private val instance = EventTracker()

        fun getInstance(): EventTracker {
            return instance
        }

        init {
            ModuleManager.addModule(instance)
        }
    }

    private lateinit var dispatchHandler: Handler
    private lateinit var eventStorage: EventStorage
    private lateinit var handler: Handler
    private var listener: EventTrackerListener? = null
    private var isDispatchHandlerRunning = false


    private var dispatchRunnable = object : Runnable {
        override fun run() {
            dispatchEvent()
            dispatchHandler.postDelayed(this, DELAY_SECOND * 1000L)
        }
    }

    internal lateinit var thread: HandlerThread
    internal var httpClient = HttpClient(
        ServiceMapManager.getInstance().urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        )
    )
    internal var request = HttpUrlEncodedRequest(Constant.core.api.API_TRACKING_URL)


    override fun onStart(context: Context) {
        super.onStart(context)

        Log.d("EventTracker", "start thread zdt-event-tracker")
        thread = HandlerThread("zdt-event-tracker", HandlerThread.MIN_PRIORITY)
        thread.start()

        dispatchHandler = Handler(thread.looper)

        handler = Handler(thread.looper, Handler.Callback {
            this.handleMessage(it)
        })

        eventStorage = EventStorage(context)

        runDispatchEventLoop()
    }

    //#region handle send message for method
    override fun addEvent(action: String, params: Map<String, String>, timestamp: Long) {
        /** @see handleMessage */
        Log.d("handleMessage", "ACT_PUSH_EVENTS_FUNCTION")
        val event = Event(action, params, timestamp)
        val msg = Message()
        msg.what = ACT_PUSH_EVENTS
        msg.obj = event
        handler.sendMessage(msg)
    }

    override fun addEvent(event: Event) {
        /** @see handleMessage */
        Log.d("handleMessage", "ACT_PUSH_EVENTS_FUNCTION")
        val msg = Message()
        msg.what = ACT_PUSH_EVENTS
        msg.obj = event
        handler.sendMessage(msg)
    }

    override fun dispatchEvent() {
        /** @see handleMessage */
        Log.d("handleMessage", "ACT_DISPATCH_EVENTS_FUNCTION")
        val msg = Message()
        msg.what = ACT_DISPATCH_EVENTS
        handler.sendMessage(msg)
    }

    override fun dispatchEventImmediate(event: Event?) {
        /** @see handleMessage */
        if (event == null) return

        val msg = Message()
        msg.what = ACT_DISPATCH_EVENT_IMMEDIATE
        msg.obj = event
        handler.sendMessage(msg)
    }

    //#endregion

    fun setListener(listener: EventTrackerListener) {
        this.listener = listener
    }

    fun runDispatchEventLoop() {
        if (!isDispatchHandlerRunning) {
            isDispatchHandlerRunning = true
            dispatchHandler.post(dispatchRunnable)
        }
    }

    //#region private supportive method
    private fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            ACT_DISPATCH_EVENTS -> {
                Log.d("handleMessage", "ACT_DISPATCH_EVENTS")
                DeviceTracking.getInstance().getDeviceId(object : DeviceTrackingListener {
                    override fun onComplete(result: String) {
                        val events = eventStorage.loadEventsFromDevice()
                        doDispatchEvent(events)
                    }
                })
            }
            ACT_DISPATCH_EVENT_IMMEDIATE -> {
                Log.d("handleMessage", "ACT_DISPATCH_EVENT_IMMEDIATE")
                DeviceTracking.getInstance().getDeviceId(object : DeviceTrackingListener {
                    override fun onComplete(result: String) {
                        val e = mutableListOf<Event>()
                        e.add(msg.obj as Event)
                        eventStorage.addEvent(msg.obj as Event)
                        doDispatchEvent(e)
                    }
                })
            }
            ACT_PUSH_EVENTS -> {
                Log.d("handleMessage", "ACT_PUSH_EVENTS")
                eventStorage.addEvent(msg.obj as Event)
            }
            else -> return false
        }
        return true
    }

    private fun doDispatchEvent(events: List<Event>) {
        try {
            val ctx = context ?: throw Exception("Context init failed")
            val storage = Storage(ctx)
            if (events.isEmpty())
                return

            val appData = JSONArray()
            val eventData = prepareEventData(events)
            val zdId = DeviceTracking.getInstance().getDeviceId() ?: ""

            val an = AppInfo.getAppName(ctx)
            val av = AppInfo.getVersionName(ctx)
            val appId = AppInfo.getAppId(ctx)
            val oauthCode = storage.getOAuthCode() ?: ""
            val ts = Date().time.toString()
            val strEventData = eventData.toString()
            val strAppData = appData.toString()
            val strSocialAcc = "[]"
            val packageName = ctx.packageName
            val params = arrayOf(
                "pl",
                "appId",
                "oauthCode",
                "data",
                "apps",
                "ts",
                "zdId",
                "an",
                "av",
                "et",
                "gzip",
                "socialAcc",
                "packageName"
            )
            val values = arrayOf(
                "android",
                appId,
                oauthCode,
                strEventData,
                strAppData,
                ts,
                zdId,
                an,
                av,
                "0",
                "0",
                strSocialAcc,
                packageName
            )

            val sig = Utils.getSignature(
                params,
                values,
                Constant.core.key.TRK_SECRET_KEY
            )
            request.addParameter("pl", "android")
            request.addParameter("appId", appId)
            request.addParameter("oauthCode", oauthCode)
            request.addParameter("zdId", zdId)
            request.addParameter("data", strEventData)
            request.addParameter("apps", strAppData)
            request.addParameter("ts", ts)
            request.addParameter("sig", sig)
            request.addParameter("an", an)
            request.addParameter("av", av)
            request.addParameter("gzip", "0")
            request.addParameter("et", "0")
            request.addParameter("socialAcc", strSocialAcc)
            request.addParameter("packageName", ctx.packageName)

            val resp = httpClient.send(request)
            val jsonObject = resp.getJSON() ?: return

            val errorCode = jsonObject.getInt("error")
            if (errorCode != 0 && resp.responseCode < 400) return

            Log.d("doDispatchEvent", "success dispatch to server ")
            eventStorage.clearEventStorage()
            listener?.dispatchComplete()
        } catch (e: Exception) {
            Log.e("doDispatchEvent", e)
            eventStorage.storeEventsToDevice()
            listener?.dispatchComplete()
        }
    }

    @Throws(Exception::class)
    private fun prepareEventData(events: List<Event>): JSONObject {
        val data = JSONObject()
        val deviceId = DeviceTracking.getInstance().getDeviceId() ?: ""
        val ts = System.currentTimeMillis()
        val deviceInfoData = DeviceInfo.prepareTrackingData(context as Context, deviceId, ts)

        val jsonEvents = JSONArray()
        var jsonEvent: JSONObject

        for (e in events) {
            jsonEvent = JSONObject()
            val extras = e.params
            if (extras.containsKey("name")) {
                jsonEvent.put("name", extras["name"])
            }
            jsonEvent.put("extras", extras)
            jsonEvent.put("act", e.action)
            jsonEvent.put("ts", e.timestamp)

            jsonEvents.put(jsonEvent)
        }
        data.put("evt", jsonEvents)
        data.put("dat", deviceInfoData)


        return data
    }

    //#endregion

}