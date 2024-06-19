package com.maxvision.plugin.udp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSenderService extends Service {
    private static final String TAG = "UDPSenderService";
    private static final int BROADCAST_PORT = 8888;
    private static final String BROADCAST_IP = "255.255.255.255";
    private static final int BROADCAST_INTERVAL = 1000; // Broadcast every second

    private Thread broadcastThread;
    private boolean running = true;

    @Override
    public void onCreate() {
        super.onCreate();
        startBroadcast();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBroadcast() {
        broadcastThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    while (running) {
                        String message = "DeviceInfo: [DeviceName, DeviceIP]";
                        byte[] buffer = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(BROADCAST_IP), BROADCAST_PORT);
                        socket.send(packet);
                        Thread.sleep(BROADCAST_INTERVAL);
                    }
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error in broadcasting", e);
                }
            }
        });
        broadcastThread.start();
    }
}
