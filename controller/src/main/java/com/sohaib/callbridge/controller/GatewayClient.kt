package com.sohaib.callbridge.controller

import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

object GatewayClient {
    private const val PORT = 45678

    fun requestCall(host: String, number: String, secret: String): String = request(host,"CALL",number,secret)
    fun requestStatus(host: String, secret: String): String = request(host,"STATUS","-",secret)
    fun requestEnd(host: String, secret: String): String = request(host,"END","-",secret)

    fun sendSms(host:String, secret:String, plaintext:String):String {
        val encrypted=Crypto.encrypt(secret,plaintext)
        return request(host,"SMS_SEND",encrypted,secret)
    }

    fun fetchSms(host:String, secret:String):String? {
        val response=request(host,"SMS_FETCH","-",secret)
        if(response=="SMS:NONE") return null
        if(!response.startsWith("SMS:")) throw IllegalStateException(response)
        return Crypto.decrypt(secret,response.removePrefix("SMS:"))
    }

    private fun request(host:String, action:String, value:String, secret:String):String {
        val timestamp = System.currentTimeMillis() / 1000L
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val payload = "$action|$value|$timestamp|$nonce"
        val signature = Crypto.hmacSha256Hex(secret, payload)
        val line = "$payload|$signature\n"
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, PORT), 5000)
            socket.soTimeout = 5000
            val out = socket.getOutputStream().bufferedWriter()
            out.write(line)
            out.flush()
            return socket.getInputStream().bufferedReader().readLine() ?: "NO_RESPONSE"
        }
    }
}
