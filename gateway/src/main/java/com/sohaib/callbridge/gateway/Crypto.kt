package com.sohaib.callbridge.gateway

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Crypto {
    fun hmacSha256Hex(secret:String,payload:String):String { val mac=Mac.getInstance("HmacSHA256"); mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8),"HmacSHA256")); return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)} }
    fun secureEquals(a:String,b:String):Boolean=MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8),b.toByteArray(Charsets.UTF_8))
}
