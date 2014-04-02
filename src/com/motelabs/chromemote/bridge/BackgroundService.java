package com.motelabs.chromemote.bridge;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motelabs.chromemote.bridge.android.anymote.AndroidPlatform;
import com.motelabs.chromemote.bridge.android.anymote.DeviceSelectDialog;
import com.motelabs.chromemote.bridge.java.anymote.client.AnymoteClientService;
import com.motelabs.chromemote.bridge.java.anymote.client.AnymoteSender;
import com.motelabs.chromemote.bridge.java.anymote.client.ClientListener;
import com.motelabs.chromemote.bridge.java.anymote.client.DeviceSelectListener;
import com.motelabs.chromemote.bridge.java.anymote.client.InputListener;
import com.motelabs.chromemote.bridge.java.anymote.client.PinListener;
import com.motelabs.chromemote.bridge.java.anymote.connection.TvDevice;
import com.google.anymote.Key;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class BackgroundService extends IntentService implements ClientListener, InputListener {

    private static final String LOG_TAG = "MainActivity";
    public AnymoteClientService anymoteClientService;
    public AnymoteSender anymoteSender;
    public AndroidPlatform platform;

    private MoteServer moteServer = null;
    public final static List<AppInfo> apps = new ArrayList<AppInfo>();

    public BackgroundService() {
        super("BackgroundService");
    }


    public int onStartCommand (Intent intent, int flags, int startId) {

        if (anymoteClientService == null) {
            platform = new AndroidPlatform(this);
            anymoteClientService = AnymoteClientService.getInstance(platform);
            anymoteClientService.attachClientListener(this); // client service callback
            anymoteClientService.attachInputListener(this);  // user interaction callback

            initMoteServer();
            getInstalledApps();
        }
        return 1;

    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("BACKGROUND", "BackgroundService Started");



    }

    /**
     * ClientListener callback when attempting a connecion to a Google TV device
     * @see com.motelabs.chromemote.bridge.java.anymote.client.ClientListener#attemptToConnect(com.motelabs.chromemote.bridge.java.anymote.connection.TvDevice)
     */
    public void attemptToConnect(TvDevice device) {
        moteServer.attemptToConnect();
    }

    /**
     * ClientListener callback when Anymote is conneced to a Google TV device
     * @see com.motelabs.chromemote.bridge.java.anymote.client.ClientListener#onConnected(com.motelabs.chromemote.bridge.java.anymote.client.AnymoteSender)
     */
    public void onConnected(final AnymoteSender anymoteSender) {
        Log.d(LOG_TAG, "onConnected");
        if (anymoteSender != null) {
            Log.d(LOG_TAG, anymoteClientService.getCurrentDevice().toString());
            // Send events to Google TV using anymoteSender.
            // save handle to the anymoteSender instance.
            this.anymoteSender = anymoteSender;

            moteServer.onConnected();

        } else {
            Log.d(LOG_TAG, "Connection failed");
        }
    }

    /**
     * ClientListener callback when the Anymote service is disconnected from the Google TV device
     * @see com.motelabs.chromemote.bridge.java.anymote.client.ClientListener#onDisconnected()
     */
    public void onDisconnected() {
        Log.d(LOG_TAG, "onDisconnected");
        anymoteSender = null;

//        if (!platform.isWifiAvailable()) {
//            return;
//        }
        moteServer.onDisconnected();
        // Find Google TV devices to connect to
        anymoteClientService.selectDevice();
    }

    /**
     * ClientListener callback when the attempted connection to the Google TV device failed
     * @see com.motelabs.chromemote.bridge.java.anymote.client.ClientListener#onConnectionFailed()
     */
    public void onConnectionFailed() {
        Log.d(LOG_TAG, "onConnectionFailed");
        anymoteSender = null;

//        if (!platform.isWifiAvailable()) {
//            return;
//        }
        moteServer.onConnectionFailed();
        // Find Google TV devices to connect to
        anymoteClientService.selectDevice();
    }

    /**
     * Cleanup
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        if (anymoteClientService != null) {
            anymoteClientService.detachClientListener(this);
            anymoteClientService.detachInputListener(this);
            anymoteSender = null;
        }

        moteServer.stop();
    }

    /**
     * InputListener callback for feedback on starting the device discovery process
     * @see com.motelabs.chromemote.bridge.java.anymote.client.InputListener#onDiscoveringDevices()
     */
    public void onDiscoveringDevices() {
        Log.d(LOG_TAG, "onDiscoveringDevices");
        moteServer.onDiscoveringDevices();
    }

    /**
     * InputListener callback when a Google TV device needs to be selected
     * @see com.motelabs.chromemote.bridge.java.anymote.client.InputListener#onSelectDevice(java.util.List, com.motelabs.chromemote.bridge.java.anymote.client.DeviceSelectListener)
     */
    public DeviceSelectDialog deviceSelectDialog = null;
    public void onSelectDevice(final List<TvDevice> trackedDevices, final DeviceSelectListener listener) {
        Log.d(LOG_TAG, "onSelectDevice");
        moteServer.onSelectDevice(trackedDevices, anymoteClientService.getCurrentDevice(), listener);
    }



    /**
     * InputListener callback when PIN required to pair with Google TV device
     * @see com.motelabs.chromemote.bridge.java.anymote.client.InputListener#onPinRequired(com.motelabs.chromemote.bridge.java.anymote.client.PinListener)
     */
    public void onPinRequired(final PinListener listener) {
        Log.d(LOG_TAG, "onPinRequired");
        moteServer.onPinRequired(listener);
    }

    public void sendKeyPress(Key.Code keyCode) {
        if (anymoteSender!=null)
            anymoteSender.sendKeyPress(keyCode);
    }

    public void sendUrl(String url) {
        if (anymoteSender!=null)
            anymoteSender.sendUrl(url);
    }


    public void sendMoveRelative(int deltaX, int deltaY) {
        if (anymoteSender!=null)
            anymoteSender.sendMoveRelative(deltaX, deltaY);
    }

    public void sendScroll(int scrollX, int scrollY) {
        if (anymoteSender!=null)
            anymoteSender.sendScroll(scrollX, scrollY);
    }

    public void getInstalledApps() {
        apps.clear();
        try {
            Log.i(LOG_TAG, "getInstalledApps");
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            PackageManager pm = getPackageManager();
            List<ResolveInfo> resolveinfo_list = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo info : resolveinfo_list) {
                AppInfo appInfo = new AppInfo();
                String symbolicName = info.activityInfo.packageName;
                CharSequence appName = pm.getApplicationLabel(pm.getApplicationInfo(symbolicName, 0));
                appInfo.name = appName.toString();
                appInfo.packageName = info.activityInfo.packageName;
                appInfo.activityName = info.activityInfo.name;
                apps.add(appInfo);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "getInstalledApps", e);
        }
    }

    //Channel changing / get channel list stuff.
    private static final String LOG_TAG2 = "ChannelChangingActivity";
    // Constants for accessing the channel listing content provider.
    private static final String AUTHORITY = "com.google.android.tv.provider";
    private static final String CHANNEL_LISTING_PATH = "channel_listing";
    private static final Uri CHANNEL_LISTING_URI = Uri.parse("content://" + AUTHORITY).buildUpon().appendPath( CHANNEL_LISTING_PATH).build();
    // All available channel listing content provider columns.
    private static final String ID = "_id";
    private static final String CHANNEL_URI = "channel_uri";
    // This column stores the abbreviated channel name.
    private static final String CHANNEL_NAME = "channel_name";
    // This column stores the abbreviated channel number.
    private static final String CHANNEL_NUMBER = "channel_number";
    // This column stores the abbreviated channel name.
    private static final String CHANNEL_CALLSIGN = "callsign";

    // Retrieve specified columns (faster) or all columns (PROJECTION = null)
    // Column order specified here so that we can use the SimpleCursorAdapter
    // column-to-view binding.
    private static final String[] PROJECTION = new String[] {
            CHANNEL_CALLSIGN, CHANNEL_NUMBER, CHANNEL_NAME, CHANNEL_URI, ID };
    // Retrieve all channels
    private static final String SELECTION = null;
    private static final String[] ARGS = null;

    public String getChannelListing() {
        // Retrieve the channels from the channel listing content provider.
        // In production code, the query should be executed in an AsyncTask.
        ContentResolver mResolver = getContentResolver();
        Cursor mCursor = mResolver.query(CHANNEL_LISTING_URI, PROJECTION, SELECTION, ARGS, null);

        if (mCursor.isAfterLast()) {
            Log.w(LOG_TAG2, "No channels found.");
        }

        mCursor.moveToFirst();
        JSONArray channelsList = new JSONArray();
        for(int i = 0; i < mCursor.getCount(); i++){
            mCursor.moveToPosition(i);
            JSONObject channel = new JSONObject();
            channel.put("callsign",       mCursor.getString(mCursor.getColumnIndex("callsign"))       );
            channel.put("channel_number", mCursor.getString(mCursor.getColumnIndex("channel_number")) );
            channel.put("channel_name",   mCursor.getString(mCursor.getColumnIndex("channel_name"))   );
            channel.put("channel_uri",    mCursor.getString(mCursor.getColumnIndex("channel_uri"))    );
            channelsList.add(channel);
        }
        return channelsList.toJSONString();
    }

    //Chromemote Server Stuff
    private boolean initMoteServer() {
        String ipAddr = getLocalIpAddress();
        if ( ipAddr != null ) {
            try {
                moteServer = new MoteServer(8085, this, BackgroundService.this);
                if (platform.isWifiAvailable()) anymoteClientService.selectDevice();

            }catch (IOException e){
                moteServer = null;
            }
        }
        if ( moteServer != null) {
            //success
            return true;
        } else {
            //error
            return false;
        }
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
}