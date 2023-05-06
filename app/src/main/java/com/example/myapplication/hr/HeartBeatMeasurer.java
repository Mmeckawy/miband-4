package com.example.myapplication.hr;

import static com.example.myapplication.common.UUIDs.CHAR_HEART_RATE_CONTROL;
import static com.example.myapplication.common.UUIDs.CHAR_HEART_RATE_MEASURE;
import static com.example.myapplication.common.UUIDs.CHAR_SENSOR;
import static com.example.myapplication.common.UUIDs.SERVICE1;
import static com.example.myapplication.common.UUIDs.SERVICE_HEART_RATE;

import android.Manifest;
import android.annotation.SuppressLint;
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

/**
 *  Declares main set of methods which will be used by react UI during data fetching procedure
 * Last one includes only heart beat measurement.
 */
public class HeartBeatMeasurer {

    private String TAG = "Ranu BLE";

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

    /**
     * keeps current heart beat value taken from miband device
     */
    private String heartRateValue = "0";

    public HeartBeatMeasurer() {

    }

    @SuppressLint("MissingPermission")
    public void updateHrChars(BluetoothGatt gatt) {
        this.btGatt = gatt;
        service1 = btGatt.getService(UUID.fromString(SERVICE1));
        heartService = btGatt.getService(UUID.fromString(SERVICE_HEART_RATE));
        hrCtrlChar = heartService.getCharacteristic(UUID.fromString(CHAR_HEART_RATE_CONTROL));
        hrMeasureChar = heartService.getCharacteristic(UUID.fromString(CHAR_HEART_RATE_MEASURE));
        sensorChar = service1.getCharacteristic(UUID.fromString(CHAR_SENSOR));
        btGatt.setCharacteristicNotification(hrCtrlChar, true);
        btGatt.setCharacteristicNotification(hrMeasureChar, true);
        startHrCalculation();
    }


    public void handleHeartRateData(final BluetoothGattCharacteristic characteristic) {
        byte currentHrValue = characteristic.getValue()[1];
        heartRateValue = String.valueOf(currentHrValue);
        Log.d(TAG, "HeartRateValue " + heartRateValue);
    }


    //    @ReactMethod
    @SuppressLint("MissingPermission")
    private void startHrCalculation() {
        sensorChar.setValue(new byte[]{0x01, 0x03, 0x19});
        btGatt.writeCharacteristic(sensorChar);

        Log.i(TAG, "START HR CALCULATION: " + heartRateValue);

    }

    //    @ReactMethod
    @SuppressLint("MissingPermission")
    private void stopHrCalculation() {
        hrCtrlChar.setValue(new byte[]{0x15, 0x01, 0x00});
        Log.d("INFO", "hrCtrlChar: " + btGatt.writeCharacteristic(hrCtrlChar));
    }


    //    @ReactMethod
    @SuppressLint("MissingPermission")
    private void getHeartRate(String currentHeartBeat) {
        if (Integer.valueOf(heartRateValue).equals(Integer.valueOf(currentHeartBeat))) {
            hrCtrlChar.setValue(new byte[]{0x16});
            btGatt.writeCharacteristic(hrCtrlChar);
        }
        Log.i(TAG, "HR RATE: " + heartRateValue);
    }


    public void updateBluetoothConfig(BluetoothGatt bluetoothGatt){
        this.btGatt = bluetoothGatt;
    }


    public String getName() {
        return HeartBeatMeasurer.class.getSimpleName();
    }
}
