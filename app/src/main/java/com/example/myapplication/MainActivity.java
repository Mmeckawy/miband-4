package com.example.myapplication;

import android.Manifest;
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
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.bluetooth.DeviceConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationFinePermissionGranted = false;
    private boolean isLocationCoarsePermissionGranted = false;
    private boolean isBluetoothPermissionGranted = false;
    private boolean isBluetoothScanPermissionGranted = false;
    private boolean isBluetoothAdminPermissionGranted = false;
    private boolean isBluetoothConnectPermissionGranted = false;
    private boolean isBluetoothAdvertisePermissionGranted = false;
    public BluetoothManager bluetoothManager;
    public LocationManager locationManager;
    public static BluetoothAdapter bluetoothAdapter;

    BluetoothDevice mDevice;

    private BluetoothAdapter mBluetoothAdapter = null;
    static String TAG = "Ranu-BLE";


    DeviceConnector deviceConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Config.context = this;
        //requestPermissions();

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_COARSE_LOCATION) != null) {
                    isLocationCoarsePermissionGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                    isLocationFinePermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                }
//                if(result.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != null){
//                    isLocationBackgroundPermissionGranted = result.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
//                }
                if (result.get(Manifest.permission.BLUETOOTH) != null) {
                    isBluetoothPermissionGranted = result.get(Manifest.permission.BLUETOOTH);
                }
                if (result.get(Manifest.permission.BLUETOOTH_ADMIN) != null) {
                    isBluetoothAdminPermissionGranted = result.get(Manifest.permission.BLUETOOTH_ADMIN);
                }
                if (result.get(Manifest.permission.BLUETOOTH_CONNECT) != null) {
                    isBluetoothConnectPermissionGranted = result.get(Manifest.permission.BLUETOOTH_CONNECT);
                }
                if (result.get(Manifest.permission.BLUETOOTH_SCAN) != null) {
                    isBluetoothScanPermissionGranted = result.get(Manifest.permission.BLUETOOTH_SCAN);
                }
                if (result.get(Manifest.permission.BLUETOOTH_ADVERTISE) != null) {
                    isBluetoothAdvertisePermissionGranted = result.get(Manifest.permission.BLUETOOTH_ADVERTISE);
                }
            }
        });

        requestPermission();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(this, "Access background location not granted", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_CODE);
            }
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);

        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent enableGpsIntent = new Intent(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            enableGpsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableGpsIntent);
        }

        deviceConnector = new DeviceConnector(this);
        deviceConnector.discoverDevices();

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);

        //this.configBLE();
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
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
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
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
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
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
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
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
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
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            mBluetoothGatt.discoverServices();
                            mBluetoothGattSuccess = gatt;
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            //Device disconnected
                            Log.i(TAG, "DEVICE DISCONNECTED");
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
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

    private void requestPermission(){

        isLocationFinePermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        isLocationCoarsePermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        isBluetoothPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED;

        isBluetoothConnectPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED;

        isBluetoothAdminPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
        ) == PackageManager.PERMISSION_GRANTED;

        isBluetoothScanPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED;

        isBluetoothAdvertisePermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
        ) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequest = new ArrayList<String>();

        if(!isLocationCoarsePermissionGranted){
            permissionRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(!isLocationFinePermissionGranted){
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
//        if(!isLocationBackgroundPermissionGranted){
//            permissionRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
//        }
        if(!isBluetoothPermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH);
        }
        if(!isBluetoothAdminPermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if(!isBluetoothConnectPermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(!isBluetoothScanPermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if(!isBluetoothAdvertisePermissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }

        if(!permissionRequest.isEmpty()){
            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }


        // Check if BLE is supported on the device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // BLE is not supported
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            return;
        }
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
}