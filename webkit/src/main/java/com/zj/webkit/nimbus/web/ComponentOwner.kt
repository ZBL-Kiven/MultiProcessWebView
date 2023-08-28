package com.zj.webkit.nimbus.web

import androidx.lifecycle.*

@Suppress("unused")
class ComponentOwner : LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            WebViewService.destroyService()
        }
    }
}