package com.zing.zalo.zalosdk.kotlin.oauth

enum class LoginVia {
    /**
     * Login via web
     */
    APP,
    /**
     * Login via web view
     */
    WEB,
    /**
     * Login via app if its installed. If not, use web view.
     */
    APP_OR_WEB

}