package com.zing.zalo.zalosdk.kotlin.oauth.callback

interface ValidateOAuthCodeCallback {
    /**
     * This method is called after complete oath code verification on server side
     * @param validated
     * @param errorCode
     * @param userId
     * @param authCode
     */
    fun onValidateComplete(validated: Boolean, errorCode: Int, userId: Long, authCode: String?)
}
