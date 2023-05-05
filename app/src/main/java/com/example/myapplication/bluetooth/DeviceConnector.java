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

//import com.example.attempt1.ui.login.Config;
//import com.example.attempt1.ui.login.common.GattCallback;
//import com.example.attempt1.ui.login.hr.HeartBeatMeasurer;

//import com.facebook.react.bridge.Callback;
//import com.facebook.react.bridge.ReactApplicationContext;
//import com.facebook.react.bridge.ReactContextBaseJavaModule;
//import com.facebook.react.bridge.ReactMethod;
//import com.sbp.common.GattCallback;
//import com.sbp.metric.hr.HeartBeatMeasurer;
//import com.sbp.metric.hr.HeartBeatMeasurerPackage;

import java.util.Objects;

//import javax.annotation.Nonnull;

import static android.content.Context.BLUETOOTH_SERVICE;

import com.example.myapplication.Config;
import com.example.myapplication.common.GattCallback;
import com.example.myapplication.hr.HeartBeatMeasurer;
//import static com.sbp.common.ModuleStorage.getModuleStorage;

/**
 *  Declares main set of methods which will be used by react UI during data fetching procedure.
 *  Last one includes only device connection. Make sure your miband device has
 *  "Allow 3-rd party connect" option ON
 * @author Spayker
 * @version 1.0
 * @since 06/01/2019
 */
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

    public DeviceConnector(Context context) {
        heartBeatMeasurer = new HeartBeatMeasurer(context);
        gattCallback = new GattCallback(heartBeatMeasurer, context);
        this.context = Config.context;
    }

    public void startBonding() {
        try {
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

        final DeviceScanCallback deviceScanCallback = new DeviceScanCallback(this.context);
        BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothScanner != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            bluetoothScanner.startScan(deviceScanCallback);
        }

        final int DISCOVERY_TIME_DELAY_IN_MS = 15000;
        new Handler().postDelayed(() -> {
            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
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
        Log.i(TAG, "LINKED DEVICE: " + bluetoothGatt.getDevice().getBondState());

    }

    //    @ReactMethod
    void disconnectDevice() {
        if (bluetoothGatt != null) {
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

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(currentDeviceMacAddress);
        setBluetoothDevice(device);
//        HeartBeatMeasurerPackage hBMeasurerPackage = getModuleStorage().getHeartBeatMeasurerPackage();
//        HeartBeatMeasurer heartBeatMeasurer = hBMeasurerPackage.getHeartBeatMeasurer();
        gattCallback = new GattCallback(heartBeatMeasurer, context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        bluetoothGatt = bluetoothDevice.connectGatt(mainContext, isAuto, gattCallback);
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