package com.sohaib.callbridge.gateway

import android.content.Context
import android.content.Intent
import android.net.Uri

object CallPlacer {
    fun placeCall(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }
}
