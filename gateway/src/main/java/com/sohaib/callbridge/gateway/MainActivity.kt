package com.sohaib.callbridge.gateway

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.*
import kotlin.concurrent.thread

class MainActivity:Activity(){
 private var server:GatewayServer?=null
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("gateway",MODE_PRIVATE)
  val title=TextView(this).apply{text="Gateway – Stage 3 Diagnostic";textSize=22f}
  val ips=TextView(this).apply{text="Device IP(s): ${NetInfo.localIpv4().joinToString()}\nPort: ${GatewayServer.PORT}"}
  val number=EditText(this).apply{hint="Only number this phone may dial";inputType=InputType.TYPE_CLASS_PHONE;setText(prefs.getString("number",""))}
  val secret=EditText(this).apply{hint="Shared secret";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;setText(prefs.getString("secret","change-this-secret"))}
  val start=Button(this).apply{text="Start Gateway"}
  val stop=Button(this).apply{text="Stop Gateway";isEnabled=false}
  val diagnostic=Button(this).apply{text="Run Stage 3 Audio Test"}
  val status=TextView(this).apply{text="Stopped"}
  val diagResult=TextView(this).apply{text="Audio diagnostic not run yet. For the best test, start a SIM call and wait until it is ACTIVE, then press the audio-test button.";setTextIsSelectable(true)}

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

  diagnostic.setOnClickListener{
   if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),8)
    diagResult.text="Grant Microphone permission, keep/establish an ACTIVE SIM call, then press Run Stage 3 Audio Test again."
    return@setOnClickListener
   }
   diagnostic.isEnabled=false
   diagResult.text="Running audio probes… keep the SIM call ACTIVE and have the other person speak during the test."
   thread(name="audio-diagnostic"){
    val report=try{AudioDiagnostics.run(this)}catch(t:Throwable){"Diagnostic failed: ${t.javaClass.simpleName}: ${t.message}"}
    runOnUiThread{diagResult.text=report;diagnostic.isEnabled=true}
   }
  }

  val content=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;setPadding(32,48,32,32)
   addView(title)
   addView(TextView(context).apply{text="Stage 2 call control remains enabled. Stage 3 tests what audio access this Vivo actually exposes; it does not root or modify the phone."})
   addView(ips);addView(number);addView(secret);addView(start);addView(stop);addView(status)
   addView(diagnostic)
   addView(diagResult)
  }
  val scroll=ScrollView(this).apply{addView(content)}
  setContentView(scroll)
 }
 override fun onDestroy(){server?.stop();super.onDestroy()}
}
