package com.sohaib.callbridge.gateway

import android.content.Context

object EncryptedSmsStore {
    private const val PREFS="encrypted_sms_queue"
    private const val KEY="queue"
    private const val SEP="\u001e"

    @Synchronized
    fun enqueue(context:Context, encrypted:String){
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val current=prefs.getString(KEY,"").orEmpty()
        val next=if(current.isBlank()) encrypted else current+SEP+encrypted
        prefs.edit().putString(KEY,next).apply()
    }

    @Synchronized
    fun poll(context:Context):String?{
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val current=prefs.getString(KEY,"").orEmpty()
        if(current.isBlank()) return null
        val parts=current.split(SEP,limit=2)
        val first=parts[0]
        val rest=if(parts.size>1) parts[1] else ""
        prefs.edit().putString(KEY,rest).apply()
        return first
    }
}
