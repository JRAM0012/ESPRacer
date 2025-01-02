// original
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
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ListView deviceListView;
    private TextView noDevicesTextView;
    private BluetoothDevice selectedDevice;
    private ArrayAdapter<String> deviceListAdapter;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private BluetoothScanner bluetoothScanner;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button actionButton = findViewById(R.id.scan_button);
        deviceListView = findViewById(R.id.device_list);
        noDevicesTextView = findViewById(R.id.no_devices_text);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setEnabled(false);
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // bluetooth disabled
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
            noDevicesTextView.setText("Cannot connect to ESPRacer with bluetooth disabled");
            noDevicesTextView.setVisibility(TextView.VISIBLE);
            actionButton.setText("Exit");
            actionButton.setOnClickListener(v -> {
                finish();
                System.exit(0);
            });
        }

        // bluetooth enabled
        swipeRefreshLayout.setEnabled(true);
        Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
        actionButton.setText("Scan");
        if (checkPermissions()) {
            deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
            deviceListView.setAdapter(deviceListAdapter);

            bluetoothScanner = new BluetoothScanner(this, new OnScanResultListener() {
                @Override
                public void onScanStarted() {
                    Toast.makeText(MainActivity.this, "Scan started", Toast.LENGTH_SHORT).show();
                    actionButton.setVisibility(Button.GONE);
                    discoveredDevices.clear();
                    deviceListAdapter.clear();
                    noDevicesTextView.setVisibility(TextView.GONE);
                    deviceListView.setVisibility(ListView.VISIBLE);
                    Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
                    if (pairedDevice != null && !pairedDevice.isEmpty()) {
                        for (BluetoothDevice device : pairedDevice) {
                            discoveredDevices.add(device);
                            deviceListAdapter.add(device.getName() + " (" + device.getAddress() + ")");
                        }
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onDeviceFound(BluetoothDevice device) {
//                        Toast.makeText(MainActivity.this, "Device found: " + device.getName(), Toast.LENGTH_SHORT).show();
                    discoveredDevices.add(device);
                    deviceListAdapter.add(device.getName() + " (" + device.getAddress() + ")");
                }

                @Override
                public void onScanFinished(List<BluetoothDevice> devices) {
//                        Toast.makeText(MainActivity.this, "Scan finished. Found " + devices.size() + " devices.", Toast.LENGTH_SHORT).show();
                    if (devices.isEmpty()) {
                        deviceListView.setVisibility(ListView.GONE);
                        noDevicesTextView.setVisibility(TextView.VISIBLE);
                    }
                    actionButton.setVisibility(Button.VISIBLE);
                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onScanFailed(String message) {
                    Toast.makeText(MainActivity.this, "Scan failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });

            deviceListView.setOnItemClickListener((parent, view, position, id) -> {
                selectedDevice = discoveredDevices.get(position);
                Toast.makeText(this, "Selected: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                actionButton.setText("Connect");
            });

            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (checkPermissions()) {
                    bluetoothScanner.startScan();
                }
            });

            actionButton.setOnClickListener(var -> {
                if ("Scan".contentEquals(actionButton.getText())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (checkPermissions()) {
                            bluetoothScanner.startScan();
                        }
                    } else {
                        Log.d("Error", "onCreate: if scan equals sdk version error");
                    }
                } else if ("Connect".contentEquals(actionButton.getText())) {
//                            connectToDevice();
                    Toast.makeText(this, "connectToDevice", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
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

    public interface OnScanResultListener {
        void onScanStarted();

        void onDeviceFound(BluetoothDevice device);

        void onScanFinished(List<BluetoothDevice> devices);

        void onScanFailed(String message);
    }

    public class BluetoothScanner {
        private static final int REQUEST_ENABLE_BT = 1001;
        private static final int REQUEST_LOCATION_PERMISSION = 1002;
        private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
        private final OnScanResultListener scanResultListener;
        private final Activity activity;
        private final BluetoothAdapter bluetoothAdapter;
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

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permissions are not granted at StartDiscovery", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!bluetoothAdapter.startDiscovery()) {
                scanResultListener.onScanFailed("Failed to start discovery");
            } else {
                scanResultListener.onScanStarted();
            }
        }

        public void stopScan() {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permissions are not granted at stopScan", Toast.LENGTH_SHORT).show();
                return;
            }
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
}