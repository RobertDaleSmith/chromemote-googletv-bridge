/*
 * Copyright (C) 2012 ENTERTAILION, LLC. All rights reserved.
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
package com.motelabs.chromemote.bridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import org.apache.http.conn.util.InetAddressUtils;

public class MainActivity extends Activity{

    private static final String LOG_TAG = "MainActivity";
    private Intent backgroundServiceIntent = null;
    public boolean serviceEnabled = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = this.getSharedPreferences("com.motelabs.chromemote.bridge", Context.MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("com.motelabs.chromemote.bridge.active", true);


        TextView ipAddressTextView = (TextView)findViewById(R.id.ipAddressTextView);
        ipAddressTextView.setText(getLocalIpAddress());

        backgroundServiceIntent = new Intent(this, BackgroundService.class);

        if(isActive)
            initBackgroundService();
        else {
            serviceEnabled = false;
            //unloadBackgroundService();

            TextView serviceStatusTextView = (TextView)findViewById(R.id.serviceStatusTextView);
            serviceStatusTextView.setText("INACTIVE");

            ImageButton toggleServiceButton = (ImageButton)findViewById(R.id.toggleServiceButton);
            toggleServiceButton.setBackgroundResource(R.drawable.toggled_off_btn);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
//            case R.id.menu_switch:
//                return true;
		    default:
			    return super.onOptionsItemSelected(item);
		}
	}

    @Override
	protected void onResume() {
    	Log.d(LOG_TAG, "onResume");
		super.onResume();
    }

    public void initBackgroundService() {
        SharedPreferences prefs = this.getSharedPreferences("com.motelabs.chromemote.bridge", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("com.motelabs.chromemote.bridge.active", true).commit();
        startService(backgroundServiceIntent);


    }

    public void unloadBackgroundService() {
        SharedPreferences prefs = this.getSharedPreferences("com.motelabs.chromemote.bridge", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("com.motelabs.chromemote.bridge.active", false).commit();
        stopService(backgroundServiceIntent);
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress()) ) {
                        String ipAddr = inetAddress.getHostAddress();
                        return ipAddr;
                    }
                }
            }
        } catch (SocketException ex) {
            //Log.d(TAG, ex.toString());
        }
        return null;
    }


    public void toggleServiceEnabled( View view ) {
        if(!serviceEnabled){
            serviceEnabled = true;
            initBackgroundService();

            TextView serviceStatusTextView = (TextView)findViewById(R.id.serviceStatusTextView);
            serviceStatusTextView.setText("ACTIVE");

            ImageButton toggleServiceButton = (ImageButton)findViewById(R.id.toggleServiceButton);
            toggleServiceButton.setBackgroundResource(R.drawable.toggled_on_btn);

        } else {
            serviceEnabled = false;
            unloadBackgroundService();

            TextView serviceStatusTextView = (TextView)findViewById(R.id.serviceStatusTextView);
            serviceStatusTextView.setText("INACTIVE");

            ImageButton toggleServiceButton = (ImageButton)findViewById(R.id.toggleServiceButton);
            toggleServiceButton.setBackgroundResource(R.drawable.toggled_off_btn);
        }
    }

    public void closeEvent( View view ) {
        finish();
    }

}
