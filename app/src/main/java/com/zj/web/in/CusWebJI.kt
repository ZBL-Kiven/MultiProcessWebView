package com.zj.web.`in`

import android.util.Log
import android.webkit.JavascriptInterface
import com.zj.webkit.proctol.WebJavaScriptIn

class CusWebJI(override val name: String = "webkit") : WebJavaScriptIn {

    @JavascriptInterface
    fun callcmd(json: String) {
        Log.e("=====    callCmd: ", "---->  $json")
    }
}