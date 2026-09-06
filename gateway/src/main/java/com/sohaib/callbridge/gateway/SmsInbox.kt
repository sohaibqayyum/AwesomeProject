package com.sohaib.callbridge.gateway

import android.content.Context

object SmsInbox {
    private const val PREFS="sms_private_queue"
    private const val KEY="items"

    @Synchronized fun push(context:Context, encrypted:String){
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val old=p.getString(KEY,"") ?: ""
        val updated=if(old.isBlank()) encrypted else old+"\n"+encrypted
        p.edit().putString(KEY,updated).apply()
    }

    @Synchronized fun pop(context:Context):String?{
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val raw=p.getString(KEY,"") ?: ""
        if(raw.isBlank()) return null
        val items=raw.split('\n').filter{it.isNotBlank()}
        if(items.isEmpty()) return null
        val first=items.first()
        p.edit().putString(KEY,items.drop(1).joinToString("\n")).apply()
        return first
    }
}
