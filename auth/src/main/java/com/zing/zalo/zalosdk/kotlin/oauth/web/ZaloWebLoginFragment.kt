package com.zing.zalo.zalosdk.kotlin.oauth.web

import com.zing.zalo.zalosdk.kotlin.oauth.R

class ZaloWebLoginFragment : ZaloWebLoginBaseFragment() {

    override fun onResume() {
        setTitle(getString(R.string.txt_title_login_zalo))
        super.onResume()
    }

    companion object {
        fun newInstance(): ZaloWebLoginFragment {
            return ZaloWebLoginFragment()
        }
    }
}
