package com.sohaib.callbridge.controller

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.*
import kotlin.concurrent.thread

class MainActivity : Activity() {
    @Volatile private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply { text = "Italy Controller – Stage 4 SMS"; textSize = 22f }
        val host = EditText(this).apply { hint = "Gateway IP, e.g. 192.168.1.55" }
        val number = EditText(this).apply { hint = "Allowed destination number"; inputType = InputType.TYPE_CLASS_PHONE }
        val secret = EditText(this).apply { hint = "Shared secret"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val call = Button(this).apply { text = "CALL" }
        val refresh = Button(this).apply { text = "Refresh Call Status" }
        val end = Button(this).apply { text = "END CALL" }
        val status = TextView(this).apply { text = "Ready"; textSize = 17f }

        val smsTitle = TextView(this).apply { text = "Private SMS Bridge"; textSize = 20f }
        val smsText = EditText(this).apply { hint = "Type SMS message"; minLines = 3; gravity = android.view.Gravity.TOP }
        val sendSms = Button(this).apply { text = "SEND SMS" }
        val fetchSms = Button(this).apply { text = "FETCH NEW REPLY" }
        val chat = TextView(this).apply { text = "Messages appear here only on this controller."; textSize = 16f; setTextIsSelectable(true) }

        val prefs = getSharedPreferences("controller", MODE_PRIVATE)
        host.setText(prefs.getString("host", ""))
        number.setText(prefs.getString("number", ""))
        secret.setText(prefs.getString("secret", "change-this-secret"))

        fun values(): Triple<String,String,String> = Triple(host.text.toString().trim(), number.text.toString().trim(), secret.text.toString())
        fun save(h:String,n:String,s:String){ prefs.edit().putString("host",h).putString("number",n).putString("secret",s).apply() }
        fun appendChat(line:String){ chat.text = if(chat.text.toString().startsWith("Messages appear")) line else chat.text.toString()+"\n\n"+line }
        fun pollOnce(){
            val (h,_,s)=values(); if(h.isBlank()||s.isBlank()) return
            thread {
                val r=try{GatewayClient.requestStatus(h,s)}catch(t:Throwable){"ERROR: ${t.javaClass.simpleName}: ${t.message}"}
                runOnUiThread{status.text=r.removePrefix("STATUS:")}
            }
        }

        call.setOnClickListener {
            val (h,n,s)=values()
            if (h.isBlank() || n.isBlank() || s.isBlank()) { status.text="Enter gateway IP, number and secret."; return@setOnClickListener }
            save(h,n,s); call.isEnabled=false; status.text="Connecting…"
            thread {
                val result=try { GatewayClient.requestCall(h,n,s) } catch(t:Throwable) { "ERROR: ${t.javaClass.simpleName}: ${t.message}" }
                runOnUiThread { status.text=result; call.isEnabled=true }
                if(result=="DIALING"){
                    polling=true
                    repeat(60){
                        if(!polling)return@repeat
                        Thread.sleep(1000)
                        val r=try{GatewayClient.requestStatus(h,s)}catch(_:Throwable){""}
                        if(r.isNotBlank()) runOnUiThread{status.text=r.removePrefix("STATUS:")}
                        if(r=="STATUS:IDLE" && it>2){ polling=false }
                    }
                }
            }
        }

        refresh.setOnClickListener { pollOnce() }
        end.setOnClickListener {
            val (h,_,s)=values(); if(h.isBlank()||s.isBlank()){status.text="Enter gateway IP and secret.";return@setOnClickListener}
            thread{
                val r=try{GatewayClient.requestEnd(h,s)}catch(t:Throwable){"ERROR: ${t.message}"}
                polling=false; runOnUiThread{status.text=r}
            }
        }

        sendSms.setOnClickListener {
            val (h,n,s)=values(); val body=smsText.text.toString()
            if(h.isBlank()||n.isBlank()||s.length<8||body.isBlank()){status.text="Enter gateway, number, secret (8+ chars), and message.";return@setOnClickListener}
            save(h,n,s); sendSms.isEnabled=false; status.text="Sending encrypted SMS command…"
            thread {
                val r=try{GatewayClient.sendSms(h,s,body)}catch(t:Throwable){"ERROR: ${t.javaClass.simpleName}: ${t.message}"}
                runOnUiThread{
                    sendSms.isEnabled=true; status.text=r
                    if(r=="SMS_SENT"){ appendChat("You: $body"); smsText.setText("") }
                }
            }
        }

        fetchSms.setOnClickListener {
            val (h,_,s)=values(); if(h.isBlank()||s.length<8){status.text="Enter gateway and secret.";return@setOnClickListener}
            fetchSms.isEnabled=false
            thread {
                try {
                    val msg=GatewayClient.fetchSms(h,s)
                    runOnUiThread{ if(msg==null) status.text="No new SMS" else { appendChat("Reply: $msg"); status.text="New SMS received" } }
                } catch(t:Throwable){ runOnUiThread{status.text="ERROR: ${t.message}"} }
                runOnUiThread{fetchSms.isEnabled=true}
            }
        }

        val content=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(32,48,32,32)
            addView(title); addView(host); addView(number); addView(secret)
            addView(call); addView(refresh); addView(end); addView(status)
            addView(smsTitle); addView(smsText); addView(sendSms); addView(fetchSms); addView(chat)
        }
        setContentView(ScrollView(this).apply{addView(content)})
    }
}
