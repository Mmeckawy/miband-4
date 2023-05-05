package com.example.myapplication.bluetooth;

import android.Manifest;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.myapplication.Config;

import java.util.HashMap;
import java.util.Map;

public class DeviceScanCallback extends ScanCallback {
    private Context context;

    private Map<String, String> discoveredDevices;

    private static final String UNKNOWN_DEVICE = "Unknown Device";

    DeviceScanCallback(Context context) {
        discoveredDevices = new HashMap<>();
        this.context = Config.context;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
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

}