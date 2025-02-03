package com.ramware.espracer;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class ControlActivity extends AppCompatActivity {
    private static final UUID ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket;
    private OutputStream outputStream;

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AtomicReference<SharedPreferences> prefs = new AtomicReference<>(getSharedPreferences("app_prefs", MODE_PRIVATE));
        boolean isLandscape = prefs.get().getBoolean("isLandscape", false);

        if(isLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_control);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.button_exit), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.i("ORIENTATION: ", "onCreate: ControlActivity");

        String deviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");
        BluetoothManager bluetoothmanager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothDevice device = bluetoothmanager.getAdapter().getRemoteDevice(deviceAddress);
        Log.i("INFO", "Got DEVICE at control activity: " + device.getName() + " " + device.getAddress());
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                return;
            }
            socket = device.createRfcommSocketToServiceRecord(ESP32_UUID);
            socket.connect();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            Toast.makeText(this, "Error connecting to device", Toast.LENGTH_SHORT).show();
            finish();
        }
        Log.i("INFO", "connected to device: " + device.getName() + " " + device.getAddress());

        DigitalJoystick joystick = findViewById(R.id.joystick);

        joystick.setOnJoystickMoveListener((x, y) -> {
            Toast.makeText(this, "Joystick moved: X=" + x + " Y=" + y, Toast.LENGTH_SHORT).show();
            if(x == 0 && y == 1) {
                // up
                sendCommand('0');
            } else
            if(x == 1 && y == 0) {
                // right
                sendCommand('2');
            } else
            if(x == 0 && y == -1) {
                // down
                sendCommand('1');
            } else
            if(x == -1 && y == 0) {
                // left
                sendCommand('3');
            }
        });

        Button buttonX = findViewById(R.id.button_F);
        Button buttonY = findViewById(R.id.button_B);
        Button buttonA = findViewById(R.id.button_R);
        Button buttonB = findViewById(R.id.button_L);
        Button stopButton = findViewById(R.id.button_stop);
        Button exitActivity = findViewById(R.id.button_exit_to_scan);
        Button switchMode  = findViewById(R.id.switch_orientation_mode);
        switchMode.setOnClickListener(v -> {
            prefs.set(getSharedPreferences("app_prefs", MODE_PRIVATE));
            SharedPreferences.Editor editor = prefs.get().edit();
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                switchMode.setText("Portriat");
                editor.putBoolean("isLandscape", true);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                switchMode.setText("Landscape");
                editor.putBoolean("isLandscape", false);
            }
            editor.apply();
        });

        buttonX.setOnTouchListener((view, event) -> handleButtonTouch(event, '4'));
        buttonY.setOnTouchListener((view, event) -> handleButtonTouch(event, '5'));
        buttonB.setOnTouchListener((view, event) -> handleButtonTouch(event, '6'));
        buttonA.setOnTouchListener((view, event) -> handleButtonTouch(event, '7'));
        stopButton.setOnTouchListener((view, event) -> handleButtonTouch(event, '9'));
        exitActivity.setOnClickListener(view -> finish());
    }

    private boolean handleButtonTouch(MotionEvent event, char command) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                sendCommand(command);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                sendCommand('9');
                return true;
        }
        return false;
    }

    private void sendCommand(char command) {
        try {
            if (outputStream != null) {
                outputStream.write(command);
                Log.d("SEND", String.valueOf(command));
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error sending command", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error closing socket", Toast.LENGTH_SHORT).show();
        }
    }
}