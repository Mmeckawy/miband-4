package com.example.myapplication.bluetooth;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;


import java.util.Objects;


import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.content.Context.BLUETOOTH_SERVICE;

import com.example.myapplication.Config;
import com.example.myapplication.common.GattCallback;
import com.example.myapplication.hr.HeartBeatMeasurer;

/**
 *  Declares main set of methods which will be used by react UI during data fetching procedure.
 *  Last one includes only device connection. Make sure your miband device has
 *  "Allow 3-rd party connect" option ON
 */
public class DeviceConnector {

    private String TAG = "Ranu BLE";
    // Bluetooth variable section
    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private GattCallback gattCallback;
    private ProgressDialog searchProgressDialog;
    private HeartBeatMeasurer heartBeatMeasurer;


    public DeviceConnector() {
        heartBeatMeasurer = new HeartBeatMeasurer();
        gattCallback = new GattCallback(heartBeatMeasurer);
    }


    // Callback successCallback
    @SuppressLint("MissingPermission")
    public void discoverDevices() {
        Context mainContext = Config.context;


        bluetoothAdapter = ((BluetoothManager) Objects.requireNonNull(mainContext)
                .getSystemService(BLUETOOTH_SERVICE))
                .getAdapter();

        searchProgressDialog = new ProgressDialog(mainContext);
        searchProgressDialog.setIndeterminate(true);
        searchProgressDialog.setTitle("MiBand Bluetooth Scanner");
        searchProgressDialog.setMessage("Searching...");
        searchProgressDialog.setCancelable(false);
        searchProgressDialog.show();

        if (!bluetoothAdapter.isEnabled()) {
            ((AppCompatActivity)mainContext).startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                            1);
        }

        final DeviceScanCallback deviceScanCallback = new DeviceScanCallback();
        BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothScanner != null) {
            bluetoothScanner.startScan(deviceScanCallback);
        }

        final int DISCOVERY_TIME_DELAY_IN_MS = 15000;
        new Handler().postDelayed(() -> {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(deviceScanCallback);
            searchProgressDialog.dismiss();
            Log.i(TAG, "DISCOVERD DEVICES: " + deviceScanCallback.getDiscoveredDevices());
            linkWithDevice(deviceScanCallback.getDiscoveredDevices().get("deviceMac"), false);
        }, DISCOVERY_TIME_DELAY_IN_MS);


    }


    //    @ReactMethod
    @SuppressLint("MissingPermission")
    public void linkWithDevice(String macAddress, boolean isAuto) {
        updateBluetoothGatt(isAuto); // first time
        heartBeatMeasurer.updateBluetoothConfig(bluetoothGatt);
        Log.i(TAG, "LINKED DEVICE: " + bluetoothGatt.getDevice().getBondState());

    }

    @SuppressLint("MissingPermission")
    private void updateBluetoothGatt(boolean isAuto) {
        Context mainContext = Config.context;
        bluetoothAdapter = ((BluetoothManager) Objects.requireNonNull(mainContext)
                .getSystemService(BLUETOOTH_SERVICE))
                .getAdapter();

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("D0:F3:3D:D8:34:6A"); //E3:E6:98:B0:74:67 //D0:F3:3D:D8:34:6A
        setBluetoothDevice(device);
        gattCallback = new GattCallback(heartBeatMeasurer);
        bluetoothGatt = device.connectGatt(mainContext, isAuto, gattCallback, TRANSPORT_LE);
    }

    void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        Config.bluetoothDevice = bluetoothDevice;
    }

}