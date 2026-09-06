package com.sohaib.callbridge.gateway

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager

object CallControl {
    fun state(context: Context): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        return try {
            when (tm.callState) {
                TelephonyManager.CALL_STATE_RINGING -> "RINGING"
                TelephonyManager.CALL_STATE_OFFHOOK -> "ACTIVE"
                TelephonyManager.CALL_STATE_IDLE -> "IDLE"
                else -> "UNKNOWN"
            }
        } catch (_: SecurityException) { "NO_PHONE_STATE_PERMISSION" }
    }

    @Suppress("DEPRECATION")
    fun end(context: Context): Boolean {
        if (context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) return false
        val telecom = context.getSystemService(TelecomManager::class.java)
        return try { telecom.endCall() } catch (_: Throwable) { false }
    }
}
