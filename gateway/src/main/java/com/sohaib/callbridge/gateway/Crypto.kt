package com.sohaib.callbridge.gateway

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    fun hmacSha256Hex(secret:String,payload:String):String {
        val mac=Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8),"HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    }

    fun secureEquals(a:String,b:String):Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8),b.toByteArray(Charsets.UTF_8))

    fun encrypt(secret:String, plaintext:String):String {
        val key=MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
        val iv=ByteArray(12).also{SecureRandom().nextBytes(it)}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
        val encrypted=cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv+encrypted,Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun decrypt(secret:String, encoded:String):String {
        val raw=Base64.decode(encoded,Base64.NO_WRAP or Base64.URL_SAFE)
        require(raw.size>28){"Invalid encrypted message"}
        val iv=raw.copyOfRange(0,12)
        val encrypted=raw.copyOfRange(12,raw.size)
        val key=MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
        return String(cipher.doFinal(encrypted),Charsets.UTF_8)
    }
}
