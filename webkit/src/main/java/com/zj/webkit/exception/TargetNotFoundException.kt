package com.zj.webkit.exception

class TargetNotFoundException(private val clsName: String) : Throwable() {

    override val message: String; get() = "couldn't find the server plat target \'$clsName\' \n check your target package name is wright or the intent-filters action and category !"
}