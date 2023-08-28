// WebViewAidlIn.aidl
package com.zj.webkit.aidl;

// Declare any non-default types here with import statements

interface WebViewAidlIn {

    /**
     * the parceable user agent string
     */
    int dispatchCommend(String cmd,int level,int callId,String content);
}