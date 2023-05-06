package com.example.myapplication.hr;

import static com.example.myapplication.common.UUIDs.CHAR_HEART_RATE_CONTROL;
import static com.example.myapplication.common.UUIDs.CHAR_HEART_RATE_MEASURE;
import static com.example.myapplication.common.UUIDs.CHAR_SENSOR;
import static com.example.myapplication.common.UUIDs.SERVICE1;
import static com.example.myapplication.common.UUIDs.SERVICE_HEART_RATE;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

//import com.facebook.react.bridge.Callback;
//import com.facebook.react.bridge.ReactApplicationContext;
//import com.facebook.react.bridge.ReactContextBaseJavaModule;
//import com.facebook.react.bridge.ReactMethod;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.Config;

import java.util.UUID;

//import javax.annotation.Nonnull;
//
//import static com.sbp.common.UUIDs.CHAR_HEART_RATE_CONTROL;
//import static com.sbp.common.UUIDs.CHAR_HEART_RATE_MEASURE;
//import static com.sbp.common.UUIDs.CHAR_SENSOR;
//import static com.sbp.common.UUIDs.SERVICE_HEART_RATE;
//import static com.sbp.common.UUIDs.SERVICE1;

/**
 *  Declares main set of methods which will be used by react UI during data fetching procedure
 * Last one includes only heart beat measurement.
 * @author Spayker
 * @version 1.0
 * @since 06/01/2019
 */
public class HeartBeatMeasurer {

    private String TAG = "Ranu BLE";
    private static final int PERMISSION_REQUEST_BLUETOOTH = 123;

    private BluetoothGattService service1;
    private BluetoothGattService heartService;
    private BluetoothGattCharacteristic hrCtrlChar;
    private BluetoothGattCharacteristic hrMeasureChar;
    private BluetoothGattCharacteristic sensorChar;

    /**
     * Public API for the Bluetooth GATT Profile.
     * This class provides Bluetooth GATT functionality to enable communication with Bluetooth
     * Smart or Smart Ready devices.
     * To connect to a remote peripheral device, create a BluetoothGattCallback and call
     * BluetoothDevice#connectGatt to get a instance of this class. GATT capable devices can be
     * discovered using the Bluetooth device discovery or BLE scan process.
     */
    private BluetoothGatt btGatt;
    private Context context;

    /**
     * keeps current heart beat value taken from miband device
     */
    private String heartRateValue = "0";

    public HeartBeatMeasurer(Context context) {
        this.context = Config.context;

    }

    public void updateHrChars(BluetoothGatt gatt) {
        this.btGatt = gatt;
        service1 = btGatt.getService(UUID.fromString(SERVICE1));
        heartService = btGatt.getService(UUID.fromString(SERVICE_HEART_RATE));

        hrCtrlChar = heartService.getCharacteristic(UUID.fromString(CHAR_HEART_RATE_CONTROL));
        hrMeasureChar = heartService.getCharacteristic(UUID.fromString(CHAR_HEART_RATE_MEASURE));
        sensorChar = service1.getCharacteristic(UUID.fromString(CHAR_SENSOR));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        btGatt.setCharacteristicNotification(hrCtrlChar, true);
        btGatt.setCharacteristicNotification(hrMeasureChar, true);
        startHrCalculation();
    }


    public void handleHeartRateData(final BluetoothGattCharacteristic characteristic) {
        byte currentHrValue = characteristic.getValue()[1];
        heartRateValue = String.valueOf(currentHrValue);
        Log.d(TAG, "HeartRateValue" + heartRateValue);
    }


    //    @ReactMethod
    private void startHrCalculation() {
        sensorChar.setValue(new byte[]{0x01, 0x03, 0x19});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        btGatt.writeCharacteristic(sensorChar);

        Log.i(TAG, "START HR CALCULATION: " + heartRateValue);
//        successCallback.invoke(null, heartRateValue);

    }

    //    @ReactMethod
    private void stopHrCalculation() {
        hrCtrlChar.setValue(new byte[]{0x15, 0x01, 0x00});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        Log.d("INFO", "hrCtrlChar: " + btGatt.writeCharacteristic(hrCtrlChar));
    }


    //    @ReactMethod
    private void getHeartRate(String currentHeartBeat) {
        if (Integer.valueOf(heartRateValue).equals(Integer.valueOf(currentHeartBeat))) {
            hrCtrlChar.setValue(new byte[]{0x16});
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            btGatt.writeCharacteristic(hrCtrlChar);
        }
        Log.i(TAG, "HR RATE: " + heartRateValue);
//        successCallback.invoke(null, heartRateValue);
    }


    public void updateBluetoothConfig(BluetoothGatt bluetoothGatt){
        this.btGatt = bluetoothGatt;
    }


    public String getName() {
        return HeartBeatMeasurer.class.getSimpleName();
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
