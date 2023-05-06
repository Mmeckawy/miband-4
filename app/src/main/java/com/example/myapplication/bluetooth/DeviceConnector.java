package com.example.myapplication.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Objects;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.content.Context.BLUETOOTH_SERVICE;

import com.example.myapplication.Config;
import com.example.myapplication.common.GattCallback;
import com.example.myapplication.hr.HeartBeatMeasurer;

public class DeviceConnector {

    private String TAG = "Ranu BLE";
    private Context context;
    // Bluetooth variable section
    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private GattCallback gattCallback;
    private ProgressDialog searchProgressDialog;
    private String currentDeviceMacAddress;
    private HeartBeatMeasurer heartBeatMeasurer;

    private static final int PERMISSION_REQUEST_BLUETOOTH = 123;

    public DeviceConnector() {
        heartBeatMeasurer = new HeartBeatMeasurer();
        gattCallback = new GattCallback(heartBeatMeasurer);
        this.context = Config.context;
    }

    public void startBonding() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            Log.i(TAG, "Start Pairing... with: " + bluetoothDevice.getName());
            bluetoothDevice.createBond();
        } catch (Exception e) {
            Log.i(TAG, "Error..." + e.getMessage());
        }

    }

    public BluetoothDevice getDevice() {
        return bluetoothDevice;
    }


    //    @ReactMethod // Callback successCallback
    public void discoverDevices() {
        Context mainContext = Config.context;


        bluetoothAdapter = ((BluetoothManager) Objects.requireNonNull(mainContext)
                .getSystemService(BLUETOOTH_SERVICE))
                .getAdapter();

//        bluetoothAdapter = ((BluetoothManager) Config.context.getSystemService(BLUETOOTH_SERVICE)).getAdapter();

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            bluetoothScanner.startScan(deviceScanCallback);
        }

        final int DISCOVERY_TIME_DELAY_IN_MS = 15000;
        new Handler().postDelayed(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            bluetoothAdapter.getBluetoothLeScanner().stopScan(deviceScanCallback);
            searchProgressDialog.dismiss();
//          successCallback.invoke(null, deviceScanCallback.getDiscoveredDevices());
            Log.i(TAG, "DISCOVERD DEVICES: " + deviceScanCallback.getDiscoveredDevices());
            linkWithDevice(deviceScanCallback.getDiscoveredDevices().get("deviceMac"), false);
        }, DISCOVERY_TIME_DELAY_IN_MS);


    }


    //    @ReactMethod
    public void linkWithDevice(String macAddress, boolean isAuto) {
        currentDeviceMacAddress = macAddress;
        updateBluetoothGatt(isAuto); // first time
//        getModuleStorage().getHeartBeatMeasurerPackage()
//                .getHeartBeatMeasurer()

        heartBeatMeasurer.updateBluetoothConfig(bluetoothGatt);
//        successCallback.invoke(null, bluetoothGatt.getDevice().getBondState());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        Log.i(TAG, "LINKED DEVICE: " + bluetoothGatt.getDevice().getBondState());

    }

    //    @ReactMethod
    void disconnectDevice() {
        if (bluetoothGatt != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            bluetoothGatt.disconnect();
            bluetoothGatt = null;
        }
        bluetoothDevice = null;
        bluetoothAdapter = null;
        Config.bluetoothDevice = bluetoothDevice;

//        successCallback.invoke(null, 0);
    }

    //    @ReactMethod
    private void getDeviceBondLevel() {
        if (bluetoothGatt == null) {
            Log.i(TAG, "getDeviceBondLevel: 00 NO IDEA");
        } else {
//            successCallback.invoke(null, bluetoothGatt.getDevice().getBondState());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            Log.i(TAG, "getDeviceBondLevel: " + bluetoothGatt.getDevice().getBondState());
        }
    }

//    @Nonnull
//    @Override
//    public String getName() {
//        return DeviceConnector.class.getSimpleName();
//    }

    private void updateBluetoothGatt(boolean isAuto) {
        Context mainContext = Config.context; // getReactApplicationContext().getCurrentActivity();
        bluetoothAdapter = ((BluetoothManager) Objects.requireNonNull(mainContext)
                .getSystemService(BLUETOOTH_SERVICE))
                .getAdapter();

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("E3:E6:98:B0:74:67"); //E3:E6:98:B0:74:67 //D0:F3:3D:D8:34:6A
        setBluetoothDevice(device);
//        HeartBeatMeasurerPackage hBMeasurerPackage = getModuleStorage().getHeartBeatMeasurerPackage();
//        HeartBeatMeasurer heartBeatMeasurer = hBMeasurerPackage.getHeartBeatMeasurer();
        gattCallback = new GattCallback(heartBeatMeasurer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        Log.d("Ranu","Before autoconnect");
        bluetoothGatt = device.connectGatt(mainContext, isAuto, gattCallback, TRANSPORT_LE);
    }

    void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        Config.bluetoothDevice = bluetoothDevice;
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
