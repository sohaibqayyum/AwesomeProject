package com.sohaib.callbridge.gateway

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SmsRoleService:Service(){
    override fun onBind(intent:Intent?):IBinder?=null
}
