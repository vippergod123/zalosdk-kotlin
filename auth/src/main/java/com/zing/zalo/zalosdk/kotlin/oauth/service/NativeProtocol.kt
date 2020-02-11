package com.zing.zalo.zalosdk.kotlin.oauth.service

import android.content.Context
import android.content.Intent


internal object NativeProtocol {
    val CMD_GET_LOGIN_STATUS = 10001
    private val INTENT_ACTION_PLATFORM_SERVICE = "com.zing.zalo.action.PlatformService"
    private val INTENT_PACKAGE_PLATFORM_SERVICE = "com.zing.zalo"
    val KEY_REQUEST_APPLICATION_ID = "com.zing.zalo.platform.request.APPLICATION_ID"
    val KEY_RESULT_DATA = "com.zing.zalo.platform.result.DATA"
    val KEY_RESULT_ERROR_CODE = "com.zing.zalo.platform.result.ERROR_CODE"

    fun createPlatformServiceIntent(context: Context): Intent? {
        return validateServiceIntent(
            context,
            Intent(INTENT_ACTION_PLATFORM_SERVICE).setPackage(INTENT_PACKAGE_PLATFORM_SERVICE).addCategory(
                "android.intent.category.DEFAULT"
            )
        )
    }

    private fun validateServiceIntent(context: Context, intent: Intent?): Intent? {
        if (intent == null) {
            return null
        }
        val resolveInfo = context.packageManager.resolveService(intent, 0) ?: return null
        if (ZaloSignatureValidator.validateSignature(
                context,
                resolveInfo.serviceInfo.packageName
            )
        ) {
            return intent
        }
        return null
    }
}
