package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.bluetooth.DeviceConnector;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    BluetoothDevice mDevice;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 123;

    private BluetoothAdapter mBluetoothAdapter = null;
    static String TAG = "Ranu-BLE";


    DeviceConnector deviceConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Config.context = this;
        requestPermissions();

        deviceConnector = new DeviceConnector(this);
        deviceConnector.discoverDevices();

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);

        this.configBLE();
        Log.i(TAG, "start discovering BLE device");

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadCaseBondStatusReceiver, filter);

    }

    //==================================== bluetooth event ===============

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        // Assuming here we are reconnecting
                        deviceConnector.startBonding();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            checkBluetoothPermission();
                        }
                        Log.i(TAG, "Device name: " + deviceConnector.getDevice().getName());
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }

            }
        }
    };

    private final BroadcastReceiver mBroadCaseBondStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    checkBluetoothPermission();
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //means device paired
                    Log.i(TAG, "bonded");
                } else if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.i(TAG, "bonding");
                }
            }
        }
    };

    //=================================
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 15000;
    BluetoothGatt mBluetoothGatt;
    BluetoothGatt mBluetoothGattSuccess;

    private void configBLE() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        checkBluetoothPermission();
                    }
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            result.getDevice();
            Log.i(TAG, "Found BLE: " + result.getDevice().getAddress());

//            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            BluetoothDevice device = result.getDevice();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermission();
            }
            mBluetoothGatt = device.connectGatt(null, false, mGattCallback);
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    };


    //Connection callback
    BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.i(TAG, "DEVICE CONNECTED. DISCOVERING SERVICES...");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                checkBluetoothPermission();
                            }
                            mBluetoothGatt.discoverServices();
                            mBluetoothGattSuccess = gatt;
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            //Device disconnected
                            Log.i(TAG, "DEVICE DISCONNECTED");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                checkBluetoothPermission();
                            }
                            gatt.close();
                        }
                    } else {
                        Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...");
                        gatt.close();
                    }
                }

                // On discover services method
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //Services discovered successfully. Start parsing services and characteristics
                        Log.i(TAG, "SERVICES DISCOVERED. PARSING...");
                        displayGattServices(gatt.getServices());
                    } else {
                        //Failed to discover services
                        Log.i(TAG, "FAILED TO DISCOVER SERVICES");
                    }
                }

                //When reading a characteristic, here you receive the task result and the value
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //READ WAS SUCCESSFUL
                        Log.i(TAG, "ON CHARACTERISTIC READ SUCCESSFUL");
                        //Read characteristic value like:
                        //characteristic.getValue();
                        //Which it returns a byte array. Convert it to HEX. string.
                    } else {
                        Log.i(TAG, "ERROR READING CHARACTERISTIC");
                    }
                }

                //When writing, here you can check whether the task was completed successfully or not
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "ON CHARACTERISTIC WRITE SUCCESSFUL");
                    } else {
                        Log.i(TAG, "ERROR WRITING CHARACTERISTIC");
                    }
                }

                //In this method you can read the new values from a received notification
                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    Log.i(TAG, "NEW NOTIFICATION RECEIVED");
                    //New notification received. Check the characteristic it comes from and parse to string
                /*if(characteristic.getUuid().toString().contains("0000fff3")){
                    characteristic.getValue();
                }*/
                }

                //RSSI values from the connection with the remote device are received here
                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    Log.i(TAG, "NEW RSSI VALUE RECEIVED");
                    //Read remote RSSI like: mBluetoothGatt.readRemoteRssi();
                    //Here you get the gatt table where the rssi comes from, the rssi value and the
                    //status of the task.
                }
            };

    //Method which parses all services and characteristics from the GATT table.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        //Check if there is any gatt services. If not, return.
        if (gattServices == null) return;

        // Loop through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            Log.i(TAG, "SERVICE FOUND: "+gattService.getUuid().toString());
//            Log.i(TAG, "SERVICE FOUND: "+gattService.get());

            //Loop through available characteristics for each service
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                Log.i(TAG, "  CHAR. FOUND: "+gattCharacteristic.getUuid().toString());
//                String desc = gattCharacteristic.getDescriptor(gattCharacteristic.getUuid()).getCharacteristic().getStringValue(0);
                Log.i(TAG, "  CHAR. DESC: "+gattCharacteristic.getDescriptor(gattCharacteristic.getUuid()));

            }
        }

        //****************************************
        // CONNECTION PROCESS FINISHED!
        //****************************************
        Log.i(TAG, "*************************************");
        Log.i(TAG, "CONNECTION COMPLETED SUCCESFULLY");
        Log.i(TAG, "*************************************");

    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted

            } else {
                // Permission is not granted
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void checkBluetoothPermission() {
        ActivityCompat.requestPermissions((Activity) MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions((Activity) MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH);
        } else {
            // Permission already granted, do something
            // ...
        }
    }
}