package com.motelabs.chromemote.bridge;

import java.io.*;
import java.util.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;
import android.util.Log;

import com.motelabs.chromemote.bridge.java.anymote.client.DeviceSelectListener;
import com.motelabs.chromemote.bridge.java.anymote.client.PinListener;
import com.motelabs.chromemote.bridge.java.anymote.connection.TvDevice;
import com.google.anymote.Key;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MoteServer extends NanoHTTPD
{
    static final String TAG="MOTEONLY";
    BackgroundService mActivity = null;

    public MoteServer(int port, Context ctx, BackgroundService act) throws IOException {
        super(port, ctx.getAssets());
        this.mActivity = act;
    }
    
    public MoteServer(int port, String wwwroot) throws IOException {
        super(port, new File(wwwroot).getAbsoluteFile() );
    }
    
    @Override
    public Response serve( String uri, String method, Properties header, Properties parms, Properties files ) {
        Log.d(TAG, "httpd request >>" + method + " '" + uri + "' " + "   " + parms);
        Response res = null;
        String responseString = "";

        if ( uri.startsWith("/mote") ) {

            String keyCodeString = "KEYCODE_";
            if(parms.containsKey("keycode")) {
                try {
                    keyCodeString = parms.getProperty("keycode").toUpperCase();
                    mActivity.sendKeyPress(Key.Code.valueOf(keyCodeString));

                    responseString = "{\"keyCodeReceived\":\""+ keyCodeString +"\"}";
                } catch(Exception e) { System.out.println("Invalid KEYCODE command received: "+ keyCodeString); }
            }

            if(parms.containsKey("getDeviceList") && deviceList != null) {
                //if(parms.getProperty("getDeviceList").toLowerCase().compareTo("true") == 0)
                    responseString = deviceList.toJSONString();
            }

            if(parms.containsKey("discoverDevices")) {
                //if(parms.getProperty("discoverDevices").toLowerCase().compareTo("true") == 0)
                    if (!mActivity.platform.isWifiAvailable()) {
//                       AlertDialog alertDialog = mActivity.buildNoWifiDialog();
//                       alertDialog.show();
                         responseString = "{\"isDiscovering\":false}";
                    } else {


                         mActivity.anymoteClientService.selectDevice();
                         responseString = "{\"isDiscovering\":true}";
                    }


            }

            if(parms.containsKey("connectDevice")) {
                if(parms.getProperty("connectDevice") != null){
                    if( isPreviouslyFoundDevice(parms.getProperty("connectDevice")) ) {

                        TvDevice TvDevice = getPreviouslyFoundDevice(parms.getProperty("connectDevice"));
                        deviceSelectListener.onDeviceSelected(TvDevice);

                        responseString = "{\"existing\":true, \"name\":\"" + TvDevice.getName() + "\", \"ip\":\"" + TvDevice.getAddress() + "\"}";


                    } else { //Manually pair to device not previously discovered.
                        try {
                            Inet4Address address = (Inet4Address) InetAddress.getByName(parms.getProperty("connectDevice"));
                            mActivity.anymoteClientService.connectDevice(new TvDevice("Google TV Device", address));

                            responseString = "{\"existing\":false, \"name\":\"" + "Google TV Device" + "\", \"ip\":\"" + address + "\"}";


                        } catch (UnknownHostException e) {}
                    }
                }
            }

            if(parms.containsKey("sendPairCode")) {
                if(parms.getProperty("sendPairCode") != null){
                    responseString = "{";
                    if(awaitingPin) {
                        String pinCode = parms.getProperty("sendPairCode");
                        sendPairCode(pinCode);

                        responseString = responseString + "\"wasAwaitingPin\":true,";
                    } else {
                        responseString = responseString + "\"wasAwaitingPin\":false,";
                    }

                    if(connectionSuccess)
                        responseString = responseString + "\"connectionSuccess\":true, ";
                    else
                        responseString = responseString + "\"connectionSuccess\":false, ";

                    if(connectionFailed)
                        responseString = responseString + "\"connectionFailed\":true";
                    else
                        responseString = responseString + "\"connectionFailed\":false";

                    responseString = responseString + "}";
                }
            }

            if(parms.containsKey("cancelPairCode")) {
                if(parms.getProperty("cancelPairCode") != null){
                    if(parms.getProperty("cancelPairCode").toLowerCase().compareTo("true") == 0) {
                        responseString = cancelPairCode();
                    }

                }
            }

            if(parms.containsKey("isDiscovering")) {
                if(parms.getProperty("isDiscovering") != null){
                    //if(parms.getProperty("isDiscovering").toLowerCase().compareTo("true") == 0)
                        if(discoveryInProgress)
                            responseString = "{\"isDiscovering\":true}";
                        else
                            responseString = "{\"isDiscovering\":false}";
                }
            }

            if(parms.containsKey("isAwaitingPin")) {
                if(parms.getProperty("isAwaitingPin") != null){
                    if(parms.getProperty("isAwaitingPin").toLowerCase().compareTo("true") == 0)
                        if(awaitingPin)
                            responseString = "{\"isAwaitingPin\":true}";
                        else
                            responseString = "{\"isAwaitingPin\":false}";
                }
            }

            if(parms.containsKey("isConnectingNow")) {
                if(parms.getProperty("isConnectingNow") != null){
                    if(parms.getProperty("isConnectingNow").toLowerCase().compareTo("true") == 0)
                        if(connectingNow)
                            responseString = "{\"isConnectingNow\":true}";
                        else
                            responseString = "{\"isConnectingNow\":false}";
                }
            }

            if(parms.containsKey("connectSuccessOrFail")) {
                if(parms.getProperty("connectSuccessOrFail") != null){
                    if(parms.getProperty("connectSuccessOrFail").toLowerCase().compareTo("true") == 0)
                        if(connectionSuccess)
                            responseString = "{\"connectionSuccess\":true, ";
                        else
                            responseString = "{\"connectionSuccess\":false, ";

                        if(connectionFailed)
                            responseString = responseString + "\"connectionFailed\":true}";
                        else
                            responseString = responseString + "\"connectionFailed\":false}";
                }
            }

            if(parms.containsKey("fling")) {
                if(parms.getProperty("fling") != null){
                    mActivity.sendUrl( parms.getProperty("fling") );
                }
                responseString = "{\"fling\":true, \"url\":\"" + parms.getProperty("fling") + "\"}";
            }


            if(parms.containsKey("sendMovement")) {
                if(parms.getProperty("sendMovement") != null){

                    int deltaX = 0;
                    int deltaY = 0;

                    deltaX = Integer.parseInt(parms.getProperty("deltaX"));
                    deltaY = Integer.parseInt(parms.getProperty("deltaY"));

                    mActivity.sendMoveRelative(deltaX, deltaY);

                    responseString = "{\"sendMovement\":true, \"deltaX\":\"" + deltaX + "\", \"deltaY\":\"" + deltaY + "\"}";
                }

            }

            if(parms.containsKey("sendScroll")) {
                if(parms.getProperty("sendScroll") != null){

                    int scrollX = 0;
                    int scrollY = 0;

                    scrollX = Integer.parseInt(parms.getProperty("scrollX"));
                    scrollY = Integer.parseInt(parms.getProperty("scrollY"));

                    mActivity.sendScroll(scrollX, scrollY);

                    responseString = "{\"sendScroll\":true, \"scrollX\":\"" + scrollX + "\", \"scrollY\":\"" + scrollY + "\"}";
                }

            }

            if(parms.containsKey("getInstalledApps")) {
                if(parms.getProperty("getInstalledApps") != null){


                    responseString = getInstalledAppsJsonString();
                }

            }

            if(parms.containsKey("getChannelListing")) {
                if(parms.getProperty("getChannelListing") != null){


                    responseString = getChannelListingJsonString();
                }

            }


        	res = new Response(HTTP_OK , MIME_HTML, responseString);
    		System.out.println("Params Received" + parms);

        } else {
            return super.serve(uri, method, header, parms, files); 
        }

        //} else if ( uri.startsWith("/cgi/") ) {
        //    return serveCGI(uri, method, header, parms, files);
        //} else if ( uri.startsWith("/stream/") ) {
        //    return serveStream(uri, method, header, parms, files);

        return res;
	}

    public Response serveStream( String uri, String method, Properties header, Properties parms, Properties files ) {
        CommonGatewayInterface cgi = cgiEntries.get(uri);
        if ( cgi == null)
            return null;

        InputStream ins;
        ins = cgi.streaming(parms);
        if ( ins == null)
            return null;

        Random rnd = new Random();
        String etag = Integer.toHexString( rnd.nextInt() );
        String mime = parms.getProperty("mime");
        if ( mime == null)
            mime = "application/octet-stream";
        Response res = new Response( HTTP_OK, mime, ins);
        res.addHeader( "ETag", etag);
        res.isStreaming = true; 
        
        return res;
    }

    public Response serveCGI( String uri, String method, Properties header, Properties parms, Properties files ) {
        CommonGatewayInterface cgi = cgiEntries.get(uri);
        if ( cgi == null)
            return null;
        
        String msg = cgi.run(parms);
        if ( msg == null)
            return null;

        Response res = new Response( HTTP_OK, MIME_PLAINTEXT, msg);
        return res;
    }
    
    @Override
    public void serveDone(Response r) {
       try{
            if ( r.isStreaming ) { 
                r.data.close();
            }
       } catch(IOException ex) {
       }
    } 

    public static interface CommonGatewayInterface {
        public String run(Properties parms); 
        public InputStream streaming(Properties parms);
    }
    private HashMap<String, CommonGatewayInterface> cgiEntries = new HashMap<String, CommonGatewayInterface>();
    public void registerCGI(String uri, CommonGatewayInterface cgi) {
        if ( cgi != null)
			cgiEntries.put(uri, cgi);
    }



    //Anymote to HTTP server bridge stuff.
    private int       deviceCount = 0;
    private JSONArray deviceList  = null;
    private List<TvDevice> tvDevices = null;
    private TvDevice currentDevice = null;
    private DeviceSelectListener deviceSelectListener = null;

    public void onSelectDevice(List<TvDevice> tvDeviceList, TvDevice currentTvDevice, DeviceSelectListener listener){
        tvDevices = tvDeviceList;
        currentDevice = currentTvDevice;
        deviceSelectListener = listener;
        deviceCount = tvDevices.size();
        deviceList = new JSONArray();
        for(int i = 0; i < deviceCount; i++){
            JSONObject device = new JSONObject();
            device.put("current", isCurrentDevice(tvDevices.get(i), currentDevice) );
            device.put("port", tvDevices.get(i).getPort() );
            device.put("address", tvDevices.get(i).getAddress().getHostAddress() );
            device.put("name", tvDevices.get(i).getName() );
            deviceList.add(device);
        }
        discoveryInProgress = false;
    }

    public boolean isCurrentDevice(TvDevice tvDevice, TvDevice currentDevice){
        if(currentDevice != null) if(tvDevice.getAddress().getHostAddress().compareTo(currentDevice.getAddress().getHostAddress()) == 0) return true;
        else return false;
        else return false;
    }

    public boolean isPreviouslyFoundDevice(String ipString){
        for(int i = 0; i < deviceCount; i++){
            if(tvDevices.get(i).getAddress().getHostAddress().compareTo(ipString) == 0)
                return true;
        }
        return false;
    }

    public TvDevice getPreviouslyFoundDevice(String ipString){
        for(int i = 0; i < deviceCount; i++){
            if(tvDevices.get(i).getAddress().getHostAddress().compareTo(ipString) == 0)
                return tvDevices.get(i);
        }
        return null;
    }


    private PinListener pinListener;
    private boolean awaitingPin = false;

    public void onPinRequired(PinListener listener){
        pinListener = listener;
        awaitingPin = true;
    }
    public void sendPairCode(String pinCodeString){
        pinListener.onSecretEntered(pinCodeString);
        awaitingPin = false;
    }
    public String cancelPairCode(){
        if(awaitingPin){
            pinListener.onCancel();
            mActivity.anymoteClientService.cancelConnection();

            connectionFailed = false;
            return "{\"cancelPairCode\":true}";
        } else {
            return "{\"cancelPairCode\":false}";
        }


    }


    private boolean discoveryInProgress = false;
    private boolean connectionSuccess = false;
    private boolean connectionFailed = false;
    private boolean connectingNow = false;

    public void onDiscoveringDevices(){
        discoveryInProgress = true;
    }
    public void attemptToConnect(){
        connectionFailed  = false;
        connectionSuccess = false;
        connectingNow = true;
    }
    public void onConnected(){
        connectionFailed  = false;
        connectionSuccess = true;
        connectingNow = false;
        awaitingPin = false;
    }
    public void onConnectionFailed(){
        connectionFailed  = true;
        connectionSuccess = false;
        connectingNow = false;
        awaitingPin = false;
    }
    public void onDisconnected(){
        connectionFailed  = false;
        connectionSuccess = false;
        connectingNow = false;
        awaitingPin = false;
    }



    private String getInstalledAppsJsonString(){
        mActivity.getInstalledApps();
        mActivity.getChannelListing();
        List<AppInfo> apps = mActivity.apps;

        JSONArray installedAppsList = new JSONArray();
        for(int i = 0; i < apps.size(); i++){
            JSONObject installedApp = new JSONObject();
            installedApp.put("name",         apps.get(i).name         );
            installedApp.put("packageName",  apps.get(i).packageName  );
            installedApp.put("activityName", apps.get(i).activityName );
            installedAppsList.add(installedApp);
        }

        return installedAppsList.toJSONString();
    }


    private String getChannelListingJsonString(){
        return mActivity.getChannelListing();
    }


}
