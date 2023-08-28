package com.zj.webkit.nimbus.web

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.zj.webkit.*
import com.zj.webkit.aidl.WebViewAidlIn
import com.zj.webkit.exception.TargetNotFoundException
import com.zj.webkit.nimbus.client.ClientBridge

class WebViewService : Service() {

    companion object {
        const val ACTION_NAME = "com.zj.web.service"
        private var cmdListener: ((cmd: String?, level: Int, callId: Int, content: String?) -> Int)? = null

        fun isClientBind(): Boolean {
            return ClientBridge.isClientInit()
        }

        fun postToClient(cmd: String, level: Int = DEFAULT_I, callId: Int = DEFAULT_I, content: String? = "") {
            if (ClientBridge.isClientInit()) {
                var callStr = ""
                if (cmd != SERVICE_PONG && cmd != SERVICE_LOG) {
                    callStr = "cmd=$cmd&level=$level&callId=$callId"
                    logToClient("post to client ---> $callStr  \ncontent = $content")
                }
                val result = ClientBridge.postToClient(cmd, level, callId, content)
                if (cmd != SERVICE_PONG && cmd != SERVICE_LOG) logToClient("result form client <--- $result $callStr ")
            }
        }

        fun destroyService() {
            ClientBridge.postToClient(SERVICE_DESTROY)
        }

        fun registerCmdListener(token: LifecycleOwner, l: (cmd: String?, level: Int, callId: Int, content: String?) -> Int) {
            token.lifecycle.addObserver(ComponentOwner())
            this.cmdListener = l
        }

        internal fun logToClient(s: String) {
            Log.e("WebViewService:", s)
        }
    }

    private val appAidlIn = object : WebViewAidlIn.Stub() {
        override fun dispatchCommend(cmd: String?, level: Int, callId: Int, content: String?): Int {
            var result = HANDLE_OK
            if (cmd == SERVICE_PING) {
                logToClient("form client: ping received at ${System.currentTimeMillis()}")
                postToClient(SERVICE_PONG)
                return result
            }
            result = cmdListener?.invoke(cmd, level, callId, content) ?: HANDLE_ABANDON
            return result
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        val target = intent?.getStringExtra("target") ?: throw TargetNotFoundException("form intent")
        ClientBridge.bindClientService(this, target) {
            try {
                logToClient("web running in  ${getProgressName(this)} , start target : $it")
                val i = Intent(it)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                if (isClientBind()) postToClient(SERVICE_INIT)
            } catch (e: Exception) {
                logToClient("unable to starting the target intent \"$it\"")
            }
        }
        return appAidlIn
    }

    override fun onDestroy() {
        cmdListener = null
        ClientBridge.onServiceDestroyed()
        super.onDestroy()
    }
}