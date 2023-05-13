package com.example.myapplication.info;

import static com.example.myapplication.common.UUIDs.CHAR_STEPS;
import static com.example.myapplication.common.UUIDs.SERVICE1;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.Config;

import java.util.UUID;

public class InfoReceiver {

    private String TAG = "Ranu BLE";
    private BluetoothGattCharacteristic stepsChar;

    private BluetoothGatt btGatt;
    private Context context;
    private String steps = "0";
    private String battery = "0";

    private static final int PERMISSION_REQUEST_BLUETOOTH = 123;

    public InfoReceiver() {
        this.context = Config.context;

    }

    public void updateInfoChars(BluetoothGatt gatt) {
        this.btGatt = gatt;
        BluetoothGattService service1 = btGatt.getService(UUID.fromString(SERVICE1));
        stepsChar = service1.getCharacteristic(UUID.fromString(CHAR_STEPS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermission();
        }
        btGatt.readCharacteristic(stepsChar);
    }


    //    @ReactMethod
    private void getInfo() {
        if (btGatt != null && stepsChar != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            btGatt.readCharacteristic(stepsChar);
        }
//        successCallback.invoke(null, steps, battery);
        Log.i(TAG, "BATTERY INFO: "+ battery);
    }

    /**
     * Updates steps variable with current step value on device side
     * @param value - an array with step value
     *
     */
    public void handleInfoData(final byte[] value) {
        if(value != null){
            if(value[1] < 0) {
                int receivedSteps = ((value[2] + 1) * 256) + value[1];
                steps = String.valueOf(receivedSteps);
                Log.i(TAG, "Recieved Steps: " + receivedSteps);
            }
            else{
                int receivedSteps = (value[2] * 256) + value[1];
                steps = String.valueOf(receivedSteps);
                Log.i(TAG, "Recieved Steps: " + receivedSteps);
            }
        }
    }

    /**
     * Updates steps variable with current battery value on device side
     * @param value - an array with battery value
     *
     */
    public void handleBatteryData(final byte[] value) {
        if(value != null){
            byte currentSteps = value[1];
            battery = String.valueOf(currentSteps);
            Log.i(TAG, "Battery Value: "+ currentSteps);
        }
    }

//    @Nonnull
//    @Override
//    public String getName() {
//        return InfoReceiver.class.getSimpleName();
//    }

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
