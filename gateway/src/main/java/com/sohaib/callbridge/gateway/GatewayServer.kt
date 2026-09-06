package com.sohaib.callbridge.gateway

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class GatewayServer(private val context:Context,private val settings:()->Settings,private val onStatus:(String)->Unit){
 data class Settings(val allowedNumber:String,val secret:String)
 @Volatile private var running=false
 private var server:ServerSocket?=null
 private val seenNonces=ConcurrentHashMap<String,Long>()
 private val mainHandler=Handler(Looper.getMainLooper())

 fun start(){
  if(running)return
  running=true
  thread(name="gateway-server"){
   try{
    server=ServerSocket(PORT)
    status("Listening on port $PORT")
    while(running){
     val socket=server?.accept()?:break
     thread(name="gateway-client"){
      socket.use{s->
       val input=s.getInputStream().bufferedReader()
       val output=s.getOutputStream().bufferedWriter()
       val response=handle(input.readLine())
       output.write(response+"\n")
       output.flush()
      }
     }
    }
   }catch(t:Throwable){ if(running)status("Server error: ${t.message}") }
  }
 }

 fun stop(){ running=false; server?.close(); server=null; status("Stopped") }

 private fun sendSms(number:String, body:String){
  val manager=SmsManager.getDefault()
  val parts=manager.divideMessage(body)
  if(parts.size<=1) manager.sendTextMessage(number,null,body,null,null)
  else manager.sendMultipartTextMessage(number,null,parts,null,null)
 }

 private fun handle(line:String?):String{
  if(line.isNullOrBlank())return "DENIED:EMPTY"
  val p=line.split('|')
  if(p.size!=5)return "DENIED:FORMAT"
  val action=p[0]
  val value=p[1]
  val timestamp=p[2].toLongOrNull()?:return "DENIED:TIME"
  val nonce=p[3]
  val signature=p[4]
  val now=System.currentTimeMillis()/1000L
  if(kotlin.math.abs(now-timestamp)>60L)return "DENIED:STALE"
  if(seenNonces.putIfAbsent(nonce,now)!=null)return "DENIED:REPLAY"
  seenNonces.entries.removeIf{now-it.value>300L}
  val cfg=settings()
  val payload="$action|$value|$timestamp|$nonce"
  val expected=Crypto.hmacSha256Hex(cfg.secret,payload)
  if(!Crypto.secureEquals(signature,expected))return "DENIED:AUTH"

  return when(action){
   "CALL" -> {
    if(value!=cfg.allowedNumber||value.isBlank())return "DENIED:NUMBER"
    mainHandler.post{
     try{ CallPlacer.placeCall(context,value); status("Dial requested") }
     catch(t:Throwable){ status("Dial failed: ${t.javaClass.simpleName}") }
    }
    "DIALING"
   }
   "STATUS" -> "STATUS:${CallControl.state(context)}"
   "END" -> {
    val ok=CallControl.end(context)
    status(if(ok) "Hangup requested" else "Hangup failed or permission missing")
    if(ok) "ENDED" else "END_FAILED"
   }
   "SMS_SEND" -> {
    if(cfg.allowedNumber.isBlank()) return "DENIED:NUMBER"
    try{
     val plaintext=Crypto.decrypt(cfg.secret,value)
     if(plaintext.isBlank()) return "DENIED:EMPTY_SMS"
     sendSms(cfg.allowedNumber,plaintext)
     status("Encrypted SMS command sent to SIM")
     "SMS_SENT"
    }catch(t:SecurityException){"SMS_PERMISSION_REQUIRED"}
     catch(t:Throwable){"SMS_FAILED:${t.javaClass.simpleName}"}
   }
   "SMS_FETCH" -> {
    val encrypted=SmsInbox.pop(context)
    if(encrypted==null) "SMS:NONE" else "SMS:$encrypted"
   }
   else -> "DENIED:ACTION"
  }
 }

 private fun status(text:String)=mainHandler.post{onStatus(text)}
 companion object{const val PORT=45678}
}
