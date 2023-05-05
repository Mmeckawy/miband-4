package com.example.myapplication.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.Config;

import java.util.HashMap;
import java.util.Map;

public class DeviceScanCallback extends ScanCallback {
    private Context context;

    private Map<String, String> discoveredDevices;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 123;
    private static final String UNKNOWN_DEVICE = "Unknown Device";

    DeviceScanCallback(Context context) {
        discoveredDevices = new HashMap<>();
        this.context = Config.context;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        String deviceName = result.getDevice().getName();
        String deviceMacAddress = result.getDevice().getAddress();

        if(deviceName == null){
            deviceName = UNKNOWN_DEVICE;
        }

        if(discoveredDevices.containsValue(deviceMacAddress)){
            Log.d("TAG", "Device with mac: " + deviceMacAddress + " already discovered");
        } else {
            discoveredDevices.put(deviceName, deviceMacAddress);
        }
    }

    public Map<String, String> getDiscoveredDevices() {
        Map<String, String> mappedDevices = new HashMap<>();
        for (String deviceName : discoveredDevices.keySet()) {
            mappedDevices.put("deviceName", deviceName);
            mappedDevices.put("deviceMac", discoveredDevices.get(deviceName));
        }
        return mappedDevices;
    }
    @RequiresApi(api = Build.VERSION_CODES.S)
    public void checkBluetoothPermission() {
        ActivityCompat.requestPermissions((Activity) this.context,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions((Activity) this.context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH);
        } else {
            // Permission already granted, do something
            // ...
        }
    }

}