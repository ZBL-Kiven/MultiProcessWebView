package com.zj.webkit.nimbus.client

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.zj.webkit.*
import com.zj.webkit.aidl.WebViewAidlIn
import com.zj.webkit.exception.TargetNotFoundException
import com.zj.webkit.nimbus.web.ServerBridge

@Suppress("unused")
class ClientService : Service() {

    companion object {
        const val ACTION_NAME = "com.zj.web.client"
        private var webServiceCommendListeners: MutableMap<String, (cmd: String?, level: Int, callId: Int, content: String?) -> Int> = mutableMapOf()
        private var onServiceBind: ((onBind: Boolean) -> Unit)? = null
        private val handler = Handler(Looper.getMainLooper()) {
            return@Handler if (it.what == SERVICE_HEARTBEATS_CALL_ID) {
                if (ServerBridge.isServerInit()) {
                    ping()
                } else {
                    nextPing()
                }
                true
            } else false
        }

        fun setLogIn(logAble: Boolean, logIn: (s: String) -> Unit) {
            CCWebLogUtils.setLogIn(logAble, logIn)
        }

        fun startServer(c: Context, target: String, onServiceBind: (onBind: Boolean) -> Unit) {
            if (target.isEmpty()) throw TargetNotFoundException("Empty")
            ServerBridge.bindWebViewService(c.applicationContext, target, onServiceBind)
        }

        fun startServer(c: Context, target: Class<*>, onServiceBind: (onBind: Boolean) -> Unit) {
            startServer(c, target.canonicalName ?: throw TargetNotFoundException(target.name), onServiceBind)
        }

        fun stopServer() {
            ServerBridge.destroy(false)
        }

        private fun postToWebService(cmd: String, level: Int = DEFAULT_I, callId: Int = DEFAULT_I, content: String? = "") {
            if (ServerBridge.isServerInit()) {
                var callStr = ""
                if (cmd != SERVICE_PING) {
                    callStr = "cmd=$cmd&level=$level&callId=$callId"
                    CCWebLogUtils.log("post to remote ---> $callStr  \ncontent = $content")
                }
                val result = ServerBridge.postToService(cmd, level, callId, content)
                if (cmd != SERVICE_PING) CCWebLogUtils.log("result form remote <--- $result  $callStr")
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
            postToWebService(SERVICE_PING)
        }
    }

    private val appAidlIn = object : WebViewAidlIn.Stub() {
        override fun dispatchCommend(cmd: String?, level: Int, callId: Int, content: String?): Int {
            var result = HANDLE_OK
            when (cmd) {
                SERVICE_INIT -> nextPing()
                SERVICE_DESTROY -> ServerBridge.destroy()
                SERVICE_LOG -> CCWebLogUtils.log(content ?: "")
                SERVICE_PONG -> {
                    CCWebLogUtils.log("form remote: pong received  at ${System.currentTimeMillis()}")
                    nextPing()
                }
                else -> {
                    webServiceCommendListeners.forEach { (t, u) ->
                        result = u.invoke(cmd, level, callId, content)
                        if (result != HANDLE_OK) {
                            CCWebLogUtils.log("form client : the listener [$t] execute is not success")
                        }
                    }
                }
            }
            return result
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return appAidlIn
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webServiceCommendListeners.clear()
        ServerBridge.onServiceDestroyed()
        super.onDestroy()
    }
}