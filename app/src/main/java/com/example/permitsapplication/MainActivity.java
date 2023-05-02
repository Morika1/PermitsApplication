package com.example.permitsapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private TextInputEditText main_EDT_password;
    private MaterialButton main_BTN_login;

    private SensorManager sensorManager;
    private Sensor magneticSensor;
    private Sensor accelerometerSensor;
    private float[] magneticValues = new float[3];
    private float[] accelerometerValues = new float[3];
    private int batteryLevel;
    private float northOrientation;
    AudioManager audioManager;
    private int currentAudioMode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        audioManager= (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        currentAudioMode = audioManager.getRingerMode();

        IntentFilter filter=new IntentFilter(
                AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(receiver,filter);



        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        main_BTN_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginButtonClick(v);
            }
        });

    }

    BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            currentAudioMode = audioManager.getRingerMode();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister BroadcastReceiver to avoid memory leaks
        unregisterReceiver(receiver);
    }

    private void findViews() {
        main_EDT_password = findViewById(R.id.main_EDT_password);
        main_BTN_login = findViewById(R.id.main_BTN_login);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener((SensorEventListener) this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener((SensorEventListener) this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        batteryLevel = getBatteryLevel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener((SensorEventListener) this, magneticSensor);
        sensorManager.unregisterListener((SensorEventListener) this, accelerometerSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values.clone();
        }

        float[] rotationMatrix = new float[9];
        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticValues);
        if (success) {
            float[] orientationValues = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            float azimuth = (float) Math.toDegrees(orientationValues[0]);
            if (azimuth < 0) {
                azimuth += 360;
            }
            northOrientation = Math.round(azimuth);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private int getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return Math.round((float) level / (float) scale * 100.0f);
    }

    private void onLoginButtonClick(View view) {
        String password = main_EDT_password.getText().toString();
        int roundedNorthOrientation = Math.round(northOrientation);
        if (password.equals(String.valueOf(batteryLevel)) &&
                (roundedNorthOrientation >= 0 && roundedNorthOrientation <= 45 ||
                        roundedNorthOrientation >= 315 && roundedNorthOrientation <= 360) &&
                                currentAudioMode == AudioManager.RINGER_MODE_SILENT) {

            Intent intent = new Intent(MainActivity.this, PermittedActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

        }
        else
            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();

    }

}