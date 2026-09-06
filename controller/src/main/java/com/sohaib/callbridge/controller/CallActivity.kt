package com.sohaib.callbridge.controller

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import kotlin.concurrent.thread

class CallActivity : Activity() {
    @Volatile private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("controller", MODE_PRIVATE)
        val host = prefs.getString("host", "").orEmpty()
        val number = prefs.getString("number", "").orEmpty()
        val secret = prefs.getString("secret", "").orEmpty()

        val title = TextView(this).apply { text = "Calls"; textSize = 26f; gravity = Gravity.CENTER_HORIZONTAL }
        val note = TextView(this).apply {
            text = "Remote dial control only for now. Voice bridging will be added later."
            textSize = 15f; gravity = Gravity.CENTER_HORIZONTAL
        }
        val recipient = TextView(this).apply { text = number; textSize = 20f; gravity = Gravity.CENTER_HORIZONTAL }
        val call = Button(this).apply { text = "CALL" }
        val refresh = Button(this).apply { text = "REFRESH STATUS" }
        val end = Button(this).apply { text = "END CALL" }
        val status = TextView(this).apply { text = "Ready"; textSize = 17f; gravity = Gravity.CENTER_HORIZONTAL }
        val back = Button(this).apply { text = "BACK"; setOnClickListener { finish() } }

        call.setOnClickListener {
            call.isEnabled = false; status.text = "Connecting…"
            thread {
                val r = try { GatewayClient.requestCall(host, number, secret) } catch (t: Throwable) { "ERROR: ${t.javaClass.simpleName}: ${t.message}" }
                runOnUiThread { status.text = r; call.isEnabled = true }
                if (r == "DIALING") {
                    polling = true
                    repeat(120) {
                        if (!polling) return@repeat
                        Thread.sleep(1000)
                        val s = try { GatewayClient.requestStatus(host, secret) } catch (_: Throwable) { "" }
                        if (s.isNotBlank()) runOnUiThread { status.text = s.removePrefix("STATUS:") }
                        if (s == "STATUS:IDLE" && it > 2) polling = false
                    }
                }
            }
        }

        refresh.setOnClickListener {
            thread {
                val r = try { GatewayClient.requestStatus(host, secret) } catch (t: Throwable) { "ERROR: ${t.message}" }
                runOnUiThread { status.text = r.removePrefix("STATUS:") }
            }
        }

        end.setOnClickListener {
            thread {
                val r = try { GatewayClient.requestEnd(host, secret) } catch (t: Throwable) { "ERROR: ${t.message}" }
                polling = false
                runOnUiThread { status.text = r }
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(40, 56, 40, 40)
            addView(title); addView(note); addView(recipient)
            addView(call); addView(refresh); addView(end); addView(status); addView(back)
        }
        setContentView(content)
    }

    override fun onDestroy() { polling = false; super.onDestroy() }
}
