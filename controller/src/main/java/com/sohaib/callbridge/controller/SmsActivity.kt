package com.sohaib.callbridge.controller

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import kotlin.concurrent.thread

class SmsActivity : Activity() {
    @Volatile private var running = false
    private lateinit var messagesBox: LinearLayout
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var scroll: ScrollView
    private lateinit var host: String
    private lateinit var number: String
    private lateinit var secret: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("controller", MODE_PRIVATE)
        host = prefs.getString("host", "").orEmpty()
        number = prefs.getString("number", "").orEmpty()
        secret = prefs.getString("secret", "").orEmpty()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 20) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val back = Button(this).apply { text = "‹"; textSize = 22f; setOnClickListener { finish() } }
        val person = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply { text = "Messages"; textSize = 22f; setTypeface(null, Typeface.BOLD) }
        val recipient = TextView(this).apply { text = number; textSize = 14f }
        person.addView(title); person.addView(recipient)
        top.addView(back, LinearLayout.LayoutParams(80, LinearLayout.LayoutParams.WRAP_CONTENT))
        top.addView(person, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        status = TextView(this).apply { text = "Secure gateway chat • auto-checking for replies"; textSize = 13f; gravity = Gravity.CENTER_HORIZONTAL }
        messagesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 20, 8, 20) }
        scroll = ScrollView(this).apply { addView(messagesBox) }

        val composer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        input = EditText(this).apply { hint = "Message"; minLines = 1; maxLines = 5 }
        val send = Button(this).apply { text = "SEND" }
        composer.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        composer.addView(send)

        root.addView(top)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(composer)
        setContentView(root)

        loadHistory()

        send.setOnClickListener {
            val body = input.text.toString().trim()
            if (body.isBlank()) return@setOnClickListener
            input.isEnabled = false; send.isEnabled = false; status.text = "Sending…"
            thread {
                val result = try { GatewayClient.sendSms(host, secret, body) } catch (t: Throwable) { "ERROR: ${t.javaClass.simpleName}: ${t.message}" }
                runOnUiThread {
                    input.isEnabled = true; send.isEnabled = true
                    if (result == "SMS_SENT") {
                        addBubble(true, body, save = true)
                        input.setText("")
                        status.text = "Sent via Pakistan gateway"
                    } else status.text = result
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
        thread(name = "sms-auto-poll") {
            while (running) {
                try {
                    val msg = GatewayClient.fetchSms(host, secret)
                    if (msg != null) runOnUiThread {
                        val body = msg.substringAfter('\n', msg)
                        addBubble(false, body, save = true)
                        status.text = "New reply received"
                    }
                } catch (_: Throwable) { }
                try { Thread.sleep(3000) } catch (_: InterruptedException) { }
            }
        }
    }

    override fun onPause() { running = false; super.onPause() }

    private fun addBubble(mine: Boolean, text: String, save: Boolean) {
        val row = LinearLayout(this).apply {
            gravity = if (mine) Gravity.END else Gravity.START
            setPadding(0, 6, 0, 6)
        }
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 17f
            setPadding(24, 16, 24, 16)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            maxWidth = (resources.displayMetrics.widthPixels * 0.78).toInt()
        }
        row.addView(bubble)
        messagesBox.addView(row)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        if (save) saveHistory(mine, text)
    }

    private fun saveHistory(mine: Boolean, text: String) {
        val prefs = getSharedPreferences("controller", MODE_PRIVATE)
        val oldEncrypted = prefs.getString("chat_history", "").orEmpty()
        val old = if (oldEncrypted.isBlank()) "" else try { Crypto.decrypt(secret, oldEncrypted) } catch (_: Throwable) { "" }
        val safe = text.replace("\u001e", " ")
        val line = (if (mine) "M|" else "R|") + safe
        prefs.edit().putString("chat_history", Crypto.encrypt(secret, if (old.isBlank()) line else old + "\u001e" + line)).apply()
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences("controller", MODE_PRIVATE)
        val encrypted = prefs.getString("chat_history", "").orEmpty()
        if (encrypted.isBlank()) return
        val plain = try { Crypto.decrypt(secret, encrypted) } catch (_: Throwable) { return }
        plain.split("\u001e").forEach { item ->
            when {
                item.startsWith("M|") -> addBubble(true, item.removePrefix("M|"), save = false)
                item.startsWith("R|") -> addBubble(false, item.removePrefix("R|"), save = false)
            }
        }
    }
}
