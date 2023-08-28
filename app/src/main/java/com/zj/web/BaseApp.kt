package com.zj.web

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.util.Log
import com.zj.webkit.CCWebView

class BaseApp : Application() {

    override fun onCreate() {
        Thread.sleep(1000)
        super.onCreate()
        Log.e("=====", "app onCreate   ${getProcessName(this)}")
        CCWebView.onAppAttached(this, ":web.test")
    }

    companion object {
        fun getProcessName(context: Context): String? {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.runningAppProcesses.forEach {
                if (it.pid == android.os.Process.myPid()) {
                    return it.processName
                }
            }
            return ""
        }
    }
}