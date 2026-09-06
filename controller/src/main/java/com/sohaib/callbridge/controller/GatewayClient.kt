package com.sohaib.callbridge.controller

import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

object GatewayClient {
    private const val PORT = 45678
    fun requestCall(host: String, number: String, secret: String): String {
        val timestamp = System.currentTimeMillis() / 1000L
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val payload = "CALL|$number|$timestamp|$nonce"
        val signature = Crypto.hmacSha256Hex(secret, payload)
        val line = "$payload|$signature\n"
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, PORT), 5000)
            socket.soTimeout = 5000
            val out = socket.getOutputStream().bufferedWriter()
            out.write(line); out.flush()
            return socket.getInputStream().bufferedReader().readLine() ?: "NO_RESPONSE"
        }
    }
}
