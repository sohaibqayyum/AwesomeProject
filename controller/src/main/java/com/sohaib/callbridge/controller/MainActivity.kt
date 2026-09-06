package com.sohaib.callbridge.controller

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.*
import kotlin.concurrent.thread

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply { text = "Italy Controller – Stage 1"; textSize = 22f }
        val host = EditText(this).apply { hint = "Gateway IP, e.g. 192.168.1.55" }
        val number = EditText(this).apply { hint = "Allowed destination number"; inputType = InputType.TYPE_CLASS_PHONE }
        val secret = EditText(this).apply { hint = "Shared secret"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val call = Button(this).apply { text = "Send CALL command" }
        val status = TextView(this).apply { text = "Ready" }
        val prefs = getSharedPreferences("controller", MODE_PRIVATE)
        host.setText(prefs.getString("host", "")); number.setText(prefs.getString("number", "")); secret.setText(prefs.getString("secret", "change-this-secret"))
        call.setOnClickListener {
            val h=host.text.toString().trim(); val n=number.text.toString().trim(); val s=secret.text.toString()
            if (h.isBlank() || n.isBlank() || s.isBlank()) { status.text="Enter gateway IP, number and secret."; return@setOnClickListener }
            prefs.edit().putString("host",h).putString("number",n).putString("secret",s).apply()
            call.isEnabled=false; status.text="Connecting…"
            thread {
                val result=try { GatewayClient.requestCall(h,n,s) } catch(t:Throwable) { "ERROR: ${t.javaClass.simpleName}: ${t.message}" }
                runOnUiThread { status.text=result; call.isEnabled=true }
            }
        }
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,32); addView(title); addView(TextView(context).apply{text="Device A sends an authenticated request to Device B."}); addView(host); addView(number); addView(secret); addView(call); addView(status) }
        setContentView(layout)
    }
}
