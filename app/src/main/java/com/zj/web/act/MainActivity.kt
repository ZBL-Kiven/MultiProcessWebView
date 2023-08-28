package com.zj.web.act

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.zj.web.R
import com.zj.webkit.nimbus.client.ClientService

class MainActivity : AppCompatActivity() {

    private val token = this::class.java.name
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress = findViewById(R.id.progress)
        findViewById<View>(R.id.hello).setOnClickListener {
            progress.visibility = View.VISIBLE
            initWeb()
        }
    }

    private fun initWeb() {
        ClientService.startServer(this, "com.zj.web.act.CCWebActivity") {
            runOnUiThread { progress.visibility = if (it) View.VISIBLE else View.GONE }
        }
        ClientService.setLogIn(true) {
            Log.e("===== ", it)
        }
        ClientService.addCommendListener(token) { cmd: String?, _: Int, _: Int, _: String? ->
            when (cmd) {
                "example" -> {
                }
            }
            200
        }
    }
}