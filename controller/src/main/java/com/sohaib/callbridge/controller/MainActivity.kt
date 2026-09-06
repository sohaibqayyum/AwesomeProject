package com.sohaib.callbridge.controller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import kotlin.concurrent.thread

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("controller", MODE_PRIVATE)

        val title = TextView(this).apply { text = "CallBridge"; textSize = 30f; gravity = Gravity.CENTER_HORIZONTAL }
        val subtitle = TextView(this).apply {
            text = "Connect securely to your Pakistan gateway"
            textSize = 16f
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val mode = TextView(this).apply {
            text = "Global mode: use the Gateway phone's Tailscale 100.x.x.x address. Same-Wi-Fi 192.168.x.x also works for local testing."
            textSize = 14f
        }
        val host = EditText(this).apply {
            hint = "Gateway address (100.x.x.x for global)"
            setText(prefs.getString("host", ""))
        }
        val number = EditText(this).apply {
            hint = "Recipient number in Pakistan"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(prefs.getString("number", ""))
        }
        val secret = EditText(this).apply {
            hint = "Shared secret (8+ characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("secret", "change-this-secret"))
        }
        val test = Button(this).apply { text = "TEST CONNECTION" }
        val sms = Button(this).apply { text = "MESSAGES" }
        val calls = Button(this).apply { text = "CALLS" }
        val status = TextView(this).apply { text = "Not connected"; textSize = 16f; gravity = Gravity.CENTER_HORIZONTAL }

        fun save(): Boolean {
            val h = host.text.toString().trim()
            val n = number.text.toString().trim()
            val s = secret.text.toString()
            if (h.isBlank() || n.isBlank() || s.length < 8) {
                status.text = "Enter gateway address, recipient number and a secret of at least 8 characters."
                return false
            }
            prefs.edit().putString("host", h).putString("number", n).putString("secret", s).apply()
            return true
        }

        test.setOnClickListener {
            if (!save()) return@setOnClickListener
            val h = host.text.toString().trim(); val s = secret.text.toString()
            test.isEnabled = false; status.text = "Connecting…"
            thread {
                val r = try { GatewayClient.requestStatus(h, s) } catch (t: Throwable) { "ERROR: ${t.javaClass.simpleName}: ${t.message}" }
                runOnUiThread {
                    test.isEnabled = true
                    status.text = if (r.startsWith("STATUS:")) "Connected ✓  Gateway call state: ${r.removePrefix("STATUS:")}" else r
                }
            }
        }

        sms.setOnClickListener {
            if (save()) startActivity(Intent(this, SmsActivity::class.java))
        }
        calls.setOnClickListener {
            if (save()) startActivity(Intent(this, CallActivity::class.java))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 56, 40, 40)
            addView(title)
            addView(subtitle)
            addView(Space(context).apply { minimumHeight = 32 })
            addView(mode)
            addView(Space(context).apply { minimumHeight = 20 })
            addView(host); addView(number); addView(secret)
            addView(test); addView(status)
            addView(Space(context).apply { minimumHeight = 32 })
            addView(sms); addView(calls)
        }
        setContentView(ScrollView(this).apply { addView(content) })
    }
}
