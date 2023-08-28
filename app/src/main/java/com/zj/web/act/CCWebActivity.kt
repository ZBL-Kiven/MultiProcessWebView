package com.zj.web.act

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zj.web.R
import com.zj.web.view.CusWebView
import com.zj.webkit.nimbus.web.WebViewService

open class CCWebActivity : AppCompatActivity() {

    private lateinit var webView: CusWebView
    private lateinit var adView: TextView

    private val cmdListener = { cmd: String?, _: Int, _: Int, _: String? ->
        when (cmd) {
            "adReady" -> {
                runOnUiThread {
                    adView.visibility = View.VISIBLE
                    adView.setOnClickListener {
                        WebViewService.postToClient("example")
                    }
                }
            }
        }
        200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cc_web_activity_content)
        WebViewService.registerCmdListener(this, cmdListener)
        webView = findViewById(R.id.cus_web_view)
        adView = findViewById(R.id.cc_web_show_ad)
        webView.loadUrl("http://www.ik123.com/")
        WebViewService.postToClient("init")
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroyWebView()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}