package com.zing.zalo.zalosdk.core.helper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import java.util.*

object Utils
{
	private var language: String? = null

	@SuppressLint("WrongConstant")
	fun isPermissionGranted(context: Context, permission: String): Boolean
	{
		val permissionCheck = if (android.os.Build.VERSION.SDK_INT >= 23) {
			context.checkSelfPermission(permission)
		} else {
			context.packageManager.checkPermission(permission, context.packageName)
		}
		return permissionCheck == PackageManager.PERMISSION_GRANTED
	}

	fun isOnline(ctx: Context): Boolean
	{
		if (!isPermissionGranted(ctx, Manifest.permission.ACCESS_NETWORK_STATE)) return false

		val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
	}

	fun getLanguage(): String
	{
		return if (language != null)
		{
			if (!Locale.getDefault().language.equals("vi", ignoreCase = true))
			{
				"my"
			}
			else
			{
				"vi"
			}
		}
		else Locale.getDefault().language
	}

	fun setLanguage(lang: String)
	{
		language = lang
	}

}