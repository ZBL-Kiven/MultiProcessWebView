package com.zj.webkit.nimbus.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.zj.webkit.DEFAULT_I
import com.zj.webkit.HANDLE_ABANDON
import com.zj.webkit.HANDLE_ERROR_SERVICE_DESTROYED
import com.zj.webkit.aidl.WebViewAidlIn
import com.zj.webkit.nimbus.web.WebViewService.Companion.logToClient
import java.lang.NullPointerException
import java.lang.ref.SoftReference
import kotlin.system.exitProcess

internal object ClientBridge {

    private var clientIn: WebViewAidlIn? = null
    private var onClientBind: ((String) -> Unit)? = null
    private var isClientRunning = false
    private var nextBind: SoftReference<BindIn>? = null
    private var target: String = ""
    private var context: SoftReference<Context>? = null
    private val serviceConn = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            onServiceDestroyed()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            logToClient("On client service connected")
            clientIn = WebViewAidlIn.Stub.asInterface(service)
            onClientBind?.invoke(target)
        }
    }

    fun postToClient(cmd: String, level: Int = DEFAULT_I, callId: Int = DEFAULT_I, content: String? = ""): Int {
        return try {
            val rf = clientIn?.asBinder() ?: throw NullPointerException("get client error , the client IBinder is a null object !")
            if (rf.isBinderAlive && rf.pingBinder()) {
                clientIn?.dispatchCommend(cmd, level, callId, content) ?: HANDLE_ABANDON
            } else HANDLE_ABANDON
        } catch (e: Exception) {
            e.printStackTrace()
            HANDLE_ERROR_SERVICE_DESTROYED
        }
    }

    fun isClientInit(): Boolean {
        return clientIn != null
    }

    /**
     * Called when the service is determined to be terminated or lost
     * If accompanied by a start command, you need to try to connect to the new service after destroying the previous service
     * */
    fun onServiceDestroyed() {
        isClientRunning = false
        clientIn = null
        onClientBind = null
        nextBind?.get()?.let { bi ->
            bindClientService(bi.context, bi.target, bi.onClientBind)
        } ?: logToClient("On client service disconnected")
        nextBind = null
        exitProcess(0)
    }

    /**
     * It will always rebuild a service even if the Client is running
     * */
    fun bindClientService(context: Context, target: String, onClientBind: (String) -> Unit) {
        if (isClientRunning) {
            val bi = BindIn(context, target, onClientBind);destroy()
            this.nextBind = SoftReference(bi);return
        }
        isClientRunning = true
        this.context = SoftReference(context)
        this.target = target
        this.onClientBind = onClientBind
        val intent = Intent(ClientService.ACTION_NAME)
        intent.`package` = context.packageName
        context.bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)
    }

    fun destroy() {
        try {
            logToClient("unbind client service and disconnecting")
            this.context?.get()?.unbindService(serviceConn)
            context = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private data class BindIn(val context: Context, val target: String, val onClientBind: (String) -> Unit)
}