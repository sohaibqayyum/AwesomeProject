package com.sohaib.callbridge.gateway

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.*
import kotlin.concurrent.thread

class MainActivity:Activity(){
 private var server:GatewayServer?=null
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("gateway",MODE_PRIVATE)
  val title=TextView(this).apply{text="Gateway – Stage 4 Private SMS";textSize=22f}
  val ips=TextView(this).apply{text="Device IP(s): ${NetInfo.localIpv4().joinToString()}\nPort: ${GatewayServer.PORT}"}
  val number=EditText(this).apply{hint="Only allowed destination number";inputType=InputType.TYPE_CLASS_PHONE;setText(prefs.getString("number",""))}
  val secret=EditText(this).apply{hint="Shared secret (8+ chars)";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;setText(prefs.getString("secret","change-this-secret"))}
  val privateSms=Button(this).apply{text="ENABLE PRIVATE SMS MODE"}
  val start=Button(this).apply{text="START GATEWAY"}
  val stop=Button(this).apply{text="STOP GATEWAY";isEnabled=false}
  val diagnostic=Button(this).apply{text="RUN STAGE 3 AUDIO TEST"}
  val status=TextView(this).apply{text="Stopped";textSize=16f}
  val privacyInfo=TextView(this).apply{text="Private SMS mode makes CallBridge the default SMS app so incoming messages are encrypted immediately and are not shown in the normal Messages app. The gateway UI never displays message text."}
  val diagResult=TextView(this).apply{text="Audio diagnostic is optional.";setTextIsSelectable(true)}

  fun currentSettings()=GatewayServer.Settings(number.text.toString().trim(),secret.text.toString())
  fun saveSettings(){val cfg=currentSettings();prefs.edit().putString("number",cfg.allowedNumber).putString("secret",cfg.secret).apply()}

  privateSms.setOnClickListener{
   saveSettings()
   val smsPerms=arrayOf(Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.READ_SMS)
   val missing=smsPerms.filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED}
   if(missing.isNotEmpty()) requestPermissions(missing.toTypedArray(),20)
   if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
    val rm=getSystemService(RoleManager::class.java)
    if(rm.isRoleAvailable(RoleManager.ROLE_SMS)){
     if(rm.isRoleHeld(RoleManager.ROLE_SMS)) status.text="Private SMS mode already enabled."
     else startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_SMS),21)
    } else status.text="SMS role is not available on this device."
   } else status.text="Grant SMS permissions. On this Android version choose CallBridge as the default SMS app in Settings."
  }

  start.setOnClickListener{
   val wanted=arrayOf(Manifest.permission.CALL_PHONE,Manifest.permission.READ_PHONE_STATE,Manifest.permission.ANSWER_PHONE_CALLS,Manifest.permission.RECORD_AUDIO,Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS)
   val missing=wanted.filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED}
   if(missing.isNotEmpty()){
    requestPermissions(missing.toTypedArray(),7)
    status.text="Grant requested permissions, then press Start Gateway again."
    return@setOnClickListener
   }
   val cfg=currentSettings()
   if(cfg.allowedNumber.isBlank()||cfg.secret.length<8){status.text="Enter destination number and a secret of at least 8 characters.";return@setOnClickListener}
   saveSettings()
   server=GatewayServer(this,::currentSettings){status.text=it}.also{it.start()}
   start.isEnabled=false;stop.isEnabled=true
  }

  stop.setOnClickListener{server?.stop();server=null;start.isEnabled=true;stop.isEnabled=false}

  diagnostic.setOnClickListener{
   if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),8)
    diagResult.text="Grant Microphone permission, then run the audio test again."
    return@setOnClickListener
   }
   diagnostic.isEnabled=false;diagResult.text="Running audio probes…"
   thread(name="audio-diagnostic"){
    val report=try{AudioDiagnostics.run(this)}catch(t:Throwable){"Diagnostic failed: ${t.javaClass.simpleName}: ${t.message}"}
    runOnUiThread{diagResult.text=report;diagnostic.isEnabled=true}
   }
  }

  val content=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;setPadding(32,48,32,32)
   addView(title);addView(privacyInfo);addView(ips);addView(number);addView(secret)
   addView(privateSms);addView(start);addView(stop);addView(status);addView(diagnostic);addView(diagResult)
  }
  setContentView(ScrollView(this).apply{addView(content)})
 }

 override fun onActivityResult(requestCode:Int,resultCode:Int,data:android.content.Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==21){
   val rm=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)getSystemService(RoleManager::class.java) else null
   val held=Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q && rm?.isRoleHeld(RoleManager.ROLE_SMS)==true
   findViewById<android.R.id.content>(android.R.id.content)
   Toast.makeText(this,if(held)"Private SMS mode enabled" else "Private SMS mode was not enabled",Toast.LENGTH_LONG).show()
  }
 }

 override fun onDestroy(){server?.stop();super.onDestroy()}
}
