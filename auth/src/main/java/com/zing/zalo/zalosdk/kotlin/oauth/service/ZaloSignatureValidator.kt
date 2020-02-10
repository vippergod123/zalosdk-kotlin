package com.zing.zalo.zalosdk.kotlin.oauth.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build


internal object ZaloSignatureValidator {
    private val ZALO_DEBUG_HASH = "701554e30e4dadc7e21e132746ee0c4922bad83f"
    private val ZALO_RELEASE_HASH = "9487ba76b32e9e36785fb4c3540021f85af8d7b7"

    private var validAppSignatureHashes: HashSet<String>

    init {
        val set = HashSet<String>()
        set.add(ZALO_DEBUG_HASH)
        set.add(ZALO_RELEASE_HASH)
        validAppSignatureHashes = set
    }

    @SuppressLint("WrongConstant")
    @Suppress("DEPRECATION")
    fun validateSignature(context: Context, packageName: String): Boolean {
        val brand = Build.BRAND
        val applicationFlags = context.applicationInfo.flags
        if (brand.startsWith("generic") && (applicationFlags and 2 != 0)) {
            return true
        }
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 64)
            if (packageInfo.signatures == null || packageInfo.signatures.isEmpty()) {
                return false
            }
            for (signature in packageInfo.signatures) {
                if (!validAppSignatureHashes.contains(Utility.sha1hash(signature.toByteArray()))) {
                    return false
                }
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }

    }
}
