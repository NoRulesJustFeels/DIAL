/*
 * Copyright (C) 2013 ENTERTAILION, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.entertailion.android.dial;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;


public class Utils {

    private static final String LOG_TAG = "Utils";
    
    public static InetAddress getLocalInetAddress() {
    	InetAddress selectedInetAddress = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (intf.isUp()) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            if (inetAddress instanceof Inet4Address) { // only want ipv4 address
                            	if (inetAddress.getHostAddress().toString().charAt(0)!='0') {
                            		if (selectedInetAddress==null) {
                            			selectedInetAddress = inetAddress;
                            		} else if (intf.getName().startsWith("wlan")) {  // prefer wlan interface
                            			selectedInetAddress = inetAddress;
                            		}
                            	}
                            }
                        }
                    }
                }
            }
        	return selectedInetAddress;
        } catch (Throwable e) {
            Log.e(LOG_TAG, "Failed to get the IP address", e);
        }
        return null;
    }

    public static InetAddress getBroadcastAddress(Context context) throws IOException {
	    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcp = wifi.getDhcpInfo();
	    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	    byte[] quads = new byte[4];
	    for (int k = 0; k < 4; k++) {
	      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	    }
	    return InetAddress.getByAddress(quads);
	  }
}
