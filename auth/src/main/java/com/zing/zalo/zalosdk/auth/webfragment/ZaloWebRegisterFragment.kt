package com.zing.zalo.zalosdk.auth.webfragment

import android.annotation.SuppressLint
import android.content.Context
import com.zing.zalo.zalosdk.auth.R

@SuppressLint("StaticFieldLeak")
object ZaloWebRegisterFragment : ZaloWebLoginBaseFragment()
{
	override fun onResume()
	{
		super.onResume()
		setTitle(getString(R.string.txt_regis_acc))
	}
	
	override fun generateLoginUrl(ctx: Context): String
	{
		return super.generateLoginUrl(ctx) + "#register"
	}
     
     override fun onLoginCompleted(error: Int, uid: Long, oauth: String, zProtect: Int, name: String, isRegister: Boolean)
     {
          super.onLoginCompleted(error, uid, oauth, zProtect, name, true)
	}
}