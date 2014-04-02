/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
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

package com.motelabs.chromemote.bridge.java.anymote.client;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.motelabs.chromemote.bridge.java.anymote.connection.ConnectingTask;
import com.motelabs.chromemote.bridge.java.anymote.connection.KeyStoreManager;
import com.motelabs.chromemote.bridge.java.anymote.connection.TvDevice;
import com.motelabs.chromemote.bridge.java.anymote.connection.TvDiscoveryService;
import com.motelabs.chromemote.bridge.java.anymote.util.Log;
import com.motelabs.chromemote.bridge.java.anymote.util.Platform;

/**
 * The central point to connect to Anymote serivce running on a Google TV device
 * and send commands. The clients of this library should bind to this service
 * and implement the ClientListener interface provided in this service.
 */
public class AnymoteClientService implements ConnectingTask.ConnectionListener, DeviceSelectListener {
    private static final String LOG_TAG = "AnymoteConnectionService";
    private static AnymoteClientService instance;
    private ArrayList<ClientListener> clientListeners;
    private ConnectingTask connectingTask;
    private Platform platform;
    private TvDiscoveryService tvDiscovery;
    private TvDevice target;
    private KeyStoreManager keyStoreManager;
    private AnymoteSender anymoteSender;
    private List<TvDevice> trackedDevices = new ArrayList<TvDevice>();
    private InputListener inputListener;

    private AnymoteClientService(Platform platform) {
    	this.platform = platform;
    	initialize();
    }
    
    public static synchronized AnymoteClientService getInstance(Platform platform) {
    	if (instance == null) {
    		instance = new AnymoteClientService(platform);
    	}
    	return instance;
    }
    
    public Platform getPlatform() {
    	return platform;
    }

    private void initialize() {
        clientListeners = new ArrayList<ClientListener>();

        try {
            keyStoreManager = new KeyStoreManager();
            keyStoreManager.initialize(platform);
        } catch (GeneralSecurityException e) {
            Log.e(LOG_TAG, "Security exception during initialization! Aborting", e);
            System.exit(1);
            return;
        }
    }

    /**
     * Service lost existing connection.
     */
    @Override
    public void onConnectionDisconnected() {
        this.anymoteSender = null;
        if (target != null) {
            for (ClientListener listener : (ArrayList<ClientListener>)clientListeners.clone()) {
                listener.onDisconnected();
            }
            target = null;
        }
    }

    /**
     * Initiate new connection to specified TV device.
     * 
     * @param device the device to connect to.
     * @param activity which uses the connection.
     * @return {@code true} if already connected to the specified device.
     */
    public boolean connectDevice(TvDevice device) {
        if (target != null && target.equals(device)) {
            return true;
        }

        if (connectingTask != null) {
            connectingTask.cancel();
            connectingTask = null;
        }

        target = null;
        connectingTask = new ConnectingTask(device, keyStoreManager, platform);
        connectingTask.setConnectionListener(this);
        connectingTask.start();
        return false;
    }

    /**
     * Re-establish connection to current target.
     */
    public void reconnect() {
        TvDevice device = target;

        if (device != null) {
        	target = null;
            connectDevice(device);
        }
    }

    /**
     * The TV device that is connected.
     * 
     * @return connected TV device.
     */
    public TvDevice getCurrentDevice() {
        return target;
    }

    /**
     * Adds client listeners.
     * 
     * @param listener client listener.
     */
    public void attachClientListener(ClientListener listener) {
        clientListeners.add(listener);
    }

    /**
     * Removes client listener.
     * 
     * @param listener client listener.
     */
    public void detachClientListener(ClientListener listener) {
        clientListeners.remove(listener);
    }

    /**
     * Called by anybody who wants to cancel pending connection.
     */
    public void cancelConnection() {
        if (connectingTask != null) {
            connectingTask.cancel();
            connectingTask = null;
        }
    }
    
    /**
     * Called when attempting to connect to a device.
     */
    public void attemptToConnect(TvDevice device) {
    	for (ClientListener listener : (ArrayList<ClientListener>)clientListeners.clone()) {
            listener.attemptToConnect(device);
        }
    }

    /**
     * Called when connecting task successfully established connection.
     */
    public void onConnected(TvDevice device, AnymoteSender anymoteSender) {
        target = device;
        this.anymoteSender = anymoteSender;
        // Broadcast new connection.
        for (ClientListener listener : (ArrayList<ClientListener>)clientListeners.clone()) {
            listener.onConnected(anymoteSender);
        }
    }

    public AnymoteSender getAnymoteSender() {
        return anymoteSender;
    }

    /**
     * Returns instance of TV discovery service, creates new instance if one
     * does not already exist.
     * 
     * @return instance of TV discovery service.
     */
    public synchronized TvDiscoveryService getTvDiscovery() {
        if (tvDiscovery == null) {
            tvDiscovery = TvDiscoveryService.getInstance(platform); 
        }
        return tvDiscovery;
    }

    @Override
    public void onSecretRequired(final PinListener pinListener) {
    	Thread thread = new Thread(new Runnable() { // important to be a thread

			@Override
			public void run() {
				if (inputListener!=null) {
					inputListener.onPinRequired(pinListener);
				} else {
					pinListener.onCancel();
				}
			}
    		
    	});
    	thread.start();
    }

    @Override
    public void onConnectionFailed() {
        anymoteSender = null;
        for (ClientListener listener : (ArrayList<ClientListener>)clientListeners.clone()) {
            listener.onConnectionFailed();
        }
    }

    @Override
    public void onConnectionPairing() {
    }
    
    /**
     * Shows the device selection dialog.
     */
    public void selectDevice() {
    	Thread thread = new Thread(new Runnable() {
			public void run() {
				if (inputListener!=null) {
			    	inputListener.onDiscoveringDevices();
			
					try {
						trackedDevices = getTvDiscovery().discoverTvs();
					} catch (Exception e) {
						Log.e(LOG_TAG, "selectDevice failed", e);
					}
					inputListener.onSelectDevice(trackedDevices, AnymoteClientService.this);
		    	} 
			}
		});
    	thread.start();
    }

    /**
     * DeviceSelectListener callback
     * @see DeviceSelectListener#onDeviceSelected(com.motelabs.chromemote.bridge.java.anymote.connection.TvDevice)
     */
    public void onDeviceSelected(TvDevice device) {
        connectDevice(device);
    }

    /**
     * DeviceSelectListener callback
     * @see DeviceSelectListener#onDeviceSelectCancelled()
     */
    public void onDeviceSelectCancelled() {
    }
    
    /**
     * Adds input listeners.
     * 
     * @param listener input listener.
     */
    public void attachInputListener(InputListener listener) {
        inputListener = listener;
    }

    /**
     * Removes input listener.
     * 
     * @param listener input listener.
     */
    public void detachInputListener(InputListener listener) {
        inputListener = null;
    }
    
}
