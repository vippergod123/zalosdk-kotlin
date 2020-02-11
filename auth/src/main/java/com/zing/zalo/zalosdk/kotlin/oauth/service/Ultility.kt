package com.zing.zalo.zalosdk.kotlin.oauth.service

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


internal object Utility {
    private const val HASH_ALGORITHM_SHA1 = "SHA-1"

    fun sha1hash(bytes: ByteArray): String? {
        return hashWithAlgorithm(HASH_ALGORITHM_SHA1, bytes)
    }

    private fun hashWithAlgorithm(algorithm: String, bytes: ByteArray): String? {
        return try {
            hashBytes(MessageDigest.getInstance(algorithm), bytes)
        } catch (e: NoSuchAlgorithmException) {
            return null
        }

    }

    private fun hashBytes(hash: MessageDigest, bytes: ByteArray): String {
        hash.update(bytes)
        val digest = hash.digest()
        val builder = StringBuilder()
        for (b in digest) {
            val bInt = b.toInt()
            builder.append(Integer.toHexString(bInt shr 4 and 15))
            builder.append(Integer.toHexString(bInt shr 0 and 15))
        }
        return builder.toString()
    }
}
