package com.zj.web

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object Constance {

    private const val GAME_JSON_FILENAME = "game_inject.js"
    /**
     * 获取本地js注入脚本
     */
    fun getJsReadStr(context: Context): String {
        return loadFormFile(context, GAME_JSON_FILENAME)
    }

    @Suppress("SameParameterValue")
    private fun loadFormFile(context: Context, fileName: String): String {
        var jsStr = ""
        var injs: InputStream? = null
        var fromFile: ByteArrayOutputStream? = null
        try {
            injs = context.assets.open(fileName)
            val buff = ByteArray(1024)
            fromFile = ByteArrayOutputStream()
            do {
                val numRead = injs.read(buff)
                if (numRead <= 0) {
                    break
                }
                fromFile.write(buff, 0, numRead)
            } while (true)
            jsStr = fromFile.toString()
            return jsStr
        } catch (e: IOException) {
            e.printStackTrace()
            return jsStr
        } finally {
            injs?.close()
            fromFile?.close()
        }
    }
}