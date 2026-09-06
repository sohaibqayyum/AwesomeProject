package com.sohaib.callbridge.gateway

import java.net.NetworkInterface
import java.util.Collections

object NetInfo {
    fun localIpv4():List<String> { val result=mutableListOf<String>(); val interfaces=Collections.list(NetworkInterface.getNetworkInterfaces()); for(ni in interfaces){ for(addr in Collections.list(ni.inetAddresses)){ val host=addr.hostAddress?:continue; if(!addr.isLoopbackAddress && host.indexOf(':')<0) result+=host } }; return result }
}
