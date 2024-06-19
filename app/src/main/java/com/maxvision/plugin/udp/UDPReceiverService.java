package com.maxvision.plugin.udp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiverService extends Service {
    private static final String TAG = "UDPReceiverService";
    private static final int BROADCAST_PORT = 8888;

    private Thread receiveThread;
    private boolean running = true;

    @Override
    public void onCreate() {
        super.onCreate();
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock lock = wifi.createMulticastLock("mylock");
        lock.setReferenceCounted(true);
        lock.acquire();
        startReceiving();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startReceiving() {
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket(BROADCAST_PORT);
                    byte[] buffer = new byte[1024];
                    while (running) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        Log.d(TAG, "Received message: " + message + " from " + packet.getAddress());
                        // Parse the message to extract device information
                    }
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error in receiving", e);
                }
            }
        });
        receiveThread.start();
    }
}
