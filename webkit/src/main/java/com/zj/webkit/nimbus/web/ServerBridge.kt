package com.zj.webkit.nimbus.web

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.zj.webkit.CCWebLogUtils
import com.zj.webkit.DEFAULT_I
import com.zj.webkit.HANDLE_ABANDON
import com.zj.webkit.HANDLE_ERROR_SERVICE_DESTROYED
import com.zj.webkit.aidl.WebViewAidlIn
import com.zj.webkit.nimbus.client.ClientBridge
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.lang.ref.SoftReference

internal object ServerBridge {

    private var context: SoftReference<Context>? = null
    private var serverIn: WebViewAidlIn? = null
    private var onServiceBind: ((Boolean) -> Unit)? = null
    private var isDestroyed: Boolean = true
    private var nextBind: SoftReference<BindIn>? = null
    private var isServerRunning = false
    private val serviceConn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            onServiceDestroyed()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            CCWebLogUtils.log("On server service connected")
            serverIn = WebViewAidlIn.Stub.asInterface(service)
            onServiceBind?.invoke(true)
        }
    }

    /**
     *Send a command to the remote service
     * */
    fun postToService(cmd: String, level: Int = DEFAULT_I, callId: Int = DEFAULT_I, content: String? = ""): Int {
        return try {
            val rf = serverIn?.asBinder() ?: throw NullPointerException("get service error , the service IBinder is a null object !")
            if (rf.isBinderAlive && rf.pingBinder()) {
                serverIn?.dispatchCommend(cmd, level, callId, content) ?: if (!isDestroyed) throw IllegalArgumentException("the server in may not initialized ,has you call bindWebViewService before?") else HANDLE_ABANDON
            } else HANDLE_ABANDON
        } catch (e: Exception) {
            e.printStackTrace()
            HANDLE_ERROR_SERVICE_DESTROYED
        }
    }

    /**
     * Return whether the remote service is connected at current
     * */
    fun isServerInit(): Boolean {
        return serverIn != null
    }

    /**
     * Connect and start the remote service, Bridge only supports binding a single service
     * */
    fun bindWebViewService(context: Context, target: String, onServiceBind: (Boolean) -> Unit) {
        if (!isDestroyed || isServerRunning) {
            val bi = BindIn(context, target, onServiceBind);destroy(true)
            this.nextBind = SoftReference(bi);return
        }
        isServerRunning = true
        isDestroyed = false
        this.context = SoftReference(context)
        this.onServiceBind = onServiceBind
        val intent = Intent(WebViewService.ACTION_NAME)
        intent.`package` = context.packageName
        intent.putExtra("target", target)
        context.bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)
    }

    /**
     * Called when the service is determined to be terminated or lost
     * If accompanied by a start command, you need to try to connect to the new service after destroying the previous service
     * */
    fun onServiceDestroyed() {
        isServerRunning = false
        ClientBridge.destroy()
        serverIn = null
        context = null
        nextBind?.get()?.let { bi ->
            onServiceBind = null
            bindWebViewService(bi.context, bi.target, bi.onServiceBind)
        } ?: run {
            CCWebLogUtils.log("On server service disconnected")
            onServiceBind?.invoke(false)
            onServiceBind = null
        }
        nextBind = null
    }

    /**
     * Destroy current running remote service
     * */
    internal fun destroy(isStart: Boolean = false) {
        isDestroyed = true
        try {
            CCWebLogUtils.log("unbind server service and disconnecting")
            context?.get()?.unbindService(serviceConn)
        } catch (e: Exception) {
            if (!isStart) Log.e("=====", "destroy: unbind server service error case : ${e.message}")
            serviceConn.onServiceDisconnected(null)
        }
    }

    private data class BindIn(val context: Context, val target: String, val onServiceBind: (Boolean) -> Unit)
}