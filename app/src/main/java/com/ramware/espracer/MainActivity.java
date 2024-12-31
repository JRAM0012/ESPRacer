package com.ramware.espracer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button scanOrExitButton = findViewById(R.id.scan_button);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // bluetooth disabled
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
            scanOrExitButton.setText("Exit");
            scanOrExitButton.setOnClickListener(v -> {
                finish();
                System.exit(0);
            });
        } else {
            // bluetooth enabled
            Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
            scanOrExitButton.setText("Scan");
            scanOrExitButton.setOnClickListener(v -> {
                if (checkPermissions()) {
                    BluetoothScanner scanner = new BluetoothScanner(this, new OnScanResultListener() {
                        @Override
                        public void onScanStarted() {
                            Toast.makeText(MainActivity.this, "Scan started", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDeviceFound(BluetoothDevice device) {
                            Toast.makeText(MainActivity.this, "Device found: " + device.getName(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onScanFinished(List<BluetoothDevice> devices) {
                            Toast.makeText(MainActivity.this, "Scan finished. Found " + devices.size() + " devices.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onScanFailed(String message) {
                            Toast.makeText(MainActivity.this, "Scan failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                    scanner.startScan();
                }
            });
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            }, BluetoothScanner.REQUEST_LOCATION_PERMISSION);
            return false;
        }
        return true;
    }

    public class BluetoothScanner {
        private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
        private final OnScanResultListener scanResultListener;
        private final Activity activity;
        private final BluetoothAdapter bluetoothAdapter;
        private static final int REQUEST_ENABLE_BT = 1001;
        private static final int REQUEST_LOCATION_PERMISSION = 1002;

        private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && !discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        scanResultListener.onDeviceFound(device);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    scanResultListener.onScanFinished(discoveredDevices);
                    activity.unregisterReceiver(this);
                }
            }
        };

        public BluetoothScanner(Activity activity, OnScanResultListener scanResultListener) {
            this.activity = activity;
            this.scanResultListener = scanResultListener;
            BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        }

        public void startScan() {
            if (bluetoothAdapter == null) {
                scanResultListener.onScanFailed("Bluetooth adapter is not available");
                return;
            }
            if (!bluetoothAdapter.isEnabled()) {
                scanResultListener.onScanFailed("Bluetooth is not enabled");
                return;
            }

            if (!checkPermissions()) {
                scanResultListener.onScanFailed("Permissions are not granted");
                return;
            }

            startDiscovery();
        }

        private void startDiscovery() {
            discoveredDevices.clear();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            activity.registerReceiver(discoveryReceiver, filter);

            if (!bluetoothAdapter.startDiscovery()) {
                scanResultListener.onScanFailed("Failed to start discovery");
            } else {
                scanResultListener.onScanStarted();
            }
        }

        public void stopScan() {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            try {
                activity.unregisterReceiver(discoveryReceiver);
            } catch (IllegalArgumentException ignored) {
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        private boolean checkPermissions() {
            return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public interface OnScanResultListener {
        void onScanStarted();

        void onDeviceFound(BluetoothDevice device);

        void onScanFinished(List<BluetoothDevice> devices);

        void onScanFailed(String message);
    }
}