package com.zj.webkit.nimbus.client

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.zj.webkit.*
import com.zj.webkit.aidl.WebViewAidlIn
import com.zj.webkit.getProcessName
import com.zj.webkit.nimbus.web.ServerBridge

class ClientService : Service() {

    companion object {
        const val ACTION_NAME = "com.zj.web.client"
        const val SERVICE_HEARTBEATS_CALL_ID = 0xFAC00A9
        private var webServiceCommendListeners: MutableMap<String, (cmd: String?, level: Int, callId: Int, content: String?) -> Int> = mutableMapOf()
        private var context: Context? = null
        private val handler = Handler {
            return@Handler if (it.what == SERVICE_HEARTBEATS_CALL_ID) {
                if (ServerBridge.isServerInit()) {
                    ping()
                } else {
                    nextPing()
                }
                true
            } else false
        }

        fun startWebAct(c: Context, targetIntent: String) {
            val ctx = c.applicationContext
            context = ctx
            LogUtils.e("client running in  ${getProcessName(ctx)}")
            ServerBridge.destroy(ctx, true)
            ServerBridge.bindWebViewService(ctx, targetIntent) {
                LogUtils.e("on Service bind")
            }
        }

        fun postToWebService(cmd: String, level: Int, callId: Int, content: String?) {
            if (ServerBridge.isServerInit()) {
                LogUtils.e("post to Service ---> \ncmd = $cmd \nlevel = $level \ncallId = $callId \ncontent = $content")
                LogUtils.e("result form Service ---> ${ServerBridge.postToService(cmd, level, callId, content)}")
            }
        }

        fun addCommendListener(name: String, l: (cmd: String?, level: Int, callId: Int, content: String?) -> Int) {
            webServiceCommendListeners[name] = l
        }

        private fun nextPing() {
            handler.removeMessages(SERVICE_HEARTBEATS_CALL_ID)
            handler.sendEmptyMessageDelayed(SERVICE_HEARTBEATS_CALL_ID, 10000)
        }

        private fun ping() {
            postToWebService(SERVICE_PING, 1, SERVICE_HEARTBEATS_CALL_ID, "")
        }

        private val appAidlIn = object : WebViewAidlIn.Stub() {
            override fun dispatchCommend(cmd: String?, level: Int, callId: Int, content: String?): Int {
                var result = HANDLE_OK
                if (cmd == SERVICE_INIT) {
                    nextPing()
                    return result
                }
                if (cmd == SERVICE_PONG && callId == SERVICE_HEARTBEATS_CALL_ID) {
                    LogUtils.e("form client: pong received callId = $callId")
                    nextPing()
                    return result
                }
                if (cmd == SERVICE_DESTROY) {
                    LogUtils.e("form client: stop service")
                    context?.let { ServerBridge.destroy(it) }
                    return result
                }
                webServiceCommendListeners.forEach { (t, u) ->
                    result = u.invoke(cmd, level, callId, content)
                    if (result != HANDLE_OK) {
                        LogUtils.e("form client : the listener [$t] execute is not success")
                    }
                }
                LogUtils.e("form client : $cmd     $level    $callId    $content")
                return result
            }
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        return appAidlIn
    }

    override fun onDestroy() {
        LogUtils.e("client service destroyed")
        handler.removeCallbacksAndMessages(null)
        webServiceCommendListeners.clear()
        super.onDestroy()
    }
}