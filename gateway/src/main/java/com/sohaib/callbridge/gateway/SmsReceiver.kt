package com.sohaib.callbridge.gateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.PhoneNumberUtils

class SmsReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action!=Telephony.Sms.Intents.SMS_DELIVER_ACTION)return
        val prefs=context.getSharedPreferences("gateway",Context.MODE_PRIVATE)
        val allowed=prefs.getString("number","").orEmpty()
        val secret=prefs.getString("secret","").orEmpty()
        if(allowed.isBlank()||secret.length<8)return
        val msgs=Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if(msgs.isEmpty())return
        val sender=msgs.first().originatingAddress.orEmpty()
        if(!PhoneNumberUtils.compare(sender,allowed))return
        val body=msgs.joinToString(""){it.messageBody.orEmpty()}
        if(body.isBlank())return
        try{EncryptedSmsStore.enqueue(context,Crypto.encrypt(secret,body))}catch(_:Throwable){}
    }
}
