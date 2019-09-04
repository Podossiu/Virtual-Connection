package com.redcarrottt.sc.internal.bt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.redcarrottt.sc.internal.adapter.ClientSocket;
import com.redcarrottt.testapp.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

class RfcommClientSocket extends ClientSocket {
    private static String kTag = "RfcommClientSocket";

    @Override
    protected boolean openImpl() {
        // Cancel scanning BT devices since scanning makes socket open unstable
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        while(BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }

        // Initialize socket
        this.mSocket = null;
        Set<BluetoothDevice> pairedBtDevices = BluetoothAdapter.getDefaultAdapter()
                .getBondedDevices();
        for (BluetoothDevice btDevice : pairedBtDevices) {
            Logger.DEBUG(kTag, "Found Device: " + btDevice.getAddress() + " / " + btDevice
                    .getName());
            if (btDevice.getAddress().equals(this.mTargetMacAddr)) {
                try {
                    this.mSocket = btDevice.createInsecureRfcommSocketToServiceRecord(this.mServiceUuid);
                } catch (IOException e) {
                    Logger.ERR(kTag, "Socket initialization failed");
                    return false;
                }
            }
        }
        if (this.mSocket == null) {
            Logger.ERR(kTag, "Cannot create bluetooth socket");
            return false;
        }

        // Try to open socket
        final int kMaxTries = 100;
        for (int tries = 0; tries < kMaxTries; tries++) {
            try {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

                if(tries == 0) {
                    Logger.VERB(kTag, "Try RFCOMM socket open " + tries);
                } else {
                    Logger.WARN(kTag, "Try RFCOMM socket open " + tries);
                }
                this.mSocket.connect();
                this.mInputStream = new BufferedInputStream(this.mSocket.getInputStream());
                this.mOutputStream = new BufferedOutputStream(this.mSocket.getOutputStream());
                break;
            } catch (IOException e) {
                this.mInputStream = null;
                this.mOutputStream = null;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
            }
        }

        if (this.mSocket.isConnected()) {
            Logger.VERB(kTag, "Socket open success");
            return true;
        } else {
            Logger.ERR(kTag, "Socket open failed!");
            this.mSocket = null;
            return false;
        }
    }

    @Override
    protected boolean closeImpl() {
        if (this.mSocket != null && this.mSocket.isConnected()) {
            try {
                this.mInputStream.close();
                this.mOutputStream.close();
                this.mSocket.close();
                Logger.VERB(kTag, "Socket closed");
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected int sendImpl(byte[] dataBuffer, int dataLength) {
        if (this.mSocket == null || !this.mSocket.isConnected() || this.mOutputStream == null) {
            Logger.WARN(kTag, "Send - Socket closed! 1");
            return -1;
        }
        try {
            this.mOutputStream.write(dataBuffer, 0, dataLength);
            this.mOutputStream.flush();
            return dataLength;
        } catch (IOException e) {
            Logger.WARN(kTag, "Send - Socket closed! 2 / " + e.getMessage());
            return -2;
        }
    }

    @Override
    protected int receiveImpl(byte[] dataBuffer, int dataLength) {
        if (this.mSocket == null || !this.mSocket.isConnected() || this.mInputStream == null) {
            Logger.ERR(kTag, "Receive - Socket closed! 1");
            return -1;
        }
        try {
            int receivedBytes = 0;
            while (receivedBytes < dataLength) {
                int onceReceivedBytes = this.mInputStream.read(dataBuffer, receivedBytes,
                        dataLength - receivedBytes);
                if (onceReceivedBytes < 0) {
                    Logger.ERR(kTag, "Receive failed! 1");
                    return -2;
                }
                receivedBytes += onceReceivedBytes;
            }
            return dataLength;
        } catch (IOException e) {
            Logger.WARN(kTag, "Receive - Socket closed! 2 / " + e.getMessage());
            return -3;
        }
    }

    // Constructor
    RfcommClientSocket(String targetMacAddr, String serviceUuid, Activity ownerActivity) {
        this.mTargetMacAddr = targetMacAddr;
        this.mServiceUuid = UUID.fromString(serviceUuid);
        this.mOwnerActivity = ownerActivity;
    }

    private Activity mOwnerActivity;
    private BluetoothSocket mSocket;
    private BufferedInputStream mInputStream;
    private BufferedOutputStream mOutputStream;

    // Attributes
    private String mTargetMacAddr;
    private UUID mServiceUuid;
}
