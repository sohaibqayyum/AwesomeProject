package com.sohaib.callbridge.gateway

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.*

class MainActivity:Activity(){
 private var server:GatewayServer?=null
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("gateway",MODE_PRIVATE)
  val title=TextView(this).apply{text="Gateway – Stage 2";textSize=22f}
  val ips=TextView(this).apply{text="Device IP(s): ${NetInfo.localIpv4().joinToString()}\nPort: ${GatewayServer.PORT}"}
  val number=EditText(this).apply{hint="Only number this phone may dial";inputType=InputType.TYPE_CLASS_PHONE;setText(prefs.getString("number",""))}
  val secret=EditText(this).apply{hint="Shared secret";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;setText(prefs.getString("secret","change-this-secret"))}
  val start=Button(this).apply{text="Start Gateway"}
  val stop=Button(this).apply{text="Stop Gateway";isEnabled=false}
  val status=TextView(this).apply{text="Stopped"}
  val rootInfo=TextView(this).apply{text="Audio bridge: not configured. Pixel 6 Pro root/system access is required for cellular audio capture/injection."}

  fun currentSettings()=GatewayServer.Settings(number.text.toString().trim(),secret.text.toString())

  start.setOnClickListener{
   val wanted=arrayOf(Manifest.permission.CALL_PHONE,Manifest.permission.READ_PHONE_STATE,Manifest.permission.ANSWER_PHONE_CALLS,Manifest.permission.RECORD_AUDIO)
   val missing=wanted.filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED}
   if(missing.isNotEmpty()){
    requestPermissions(missing.toTypedArray(),7)
    status.text="Grant requested permissions, then press Start again."
    return@setOnClickListener
   }
   val cfg=currentSettings()
   if(cfg.allowedNumber.isBlank()||cfg.secret.length<8){status.text="Enter a destination and a secret of at least 8 characters.";return@setOnClickListener}
   prefs.edit().putString("number",cfg.allowedNumber).putString("secret",cfg.secret).apply()
   server=GatewayServer(this,::currentSettings){status.text=it}.also{it.start()}
   start.isEnabled=false;stop.isEnabled=true
  }
  stop.setOnClickListener{server?.stop();server=null;start.isEnabled=true;stop.isEnabled=false}
  val layout=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;setPadding(32,48,32,32)
   addView(title)
   addView(TextView(context).apply{text="Keep this app open during Stage 2 testing."})
   addView(ips);addView(number);addView(secret);addView(start);addView(stop);addView(status);addView(rootInfo)
  }
  setContentView(layout)
 }
 override fun onDestroy(){server?.stop();super.onDestroy()}
}
