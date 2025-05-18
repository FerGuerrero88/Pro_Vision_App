package com.example.myapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewDeviceActivity extends AppCompatActivity {

    List<String> foundDevicesList;
    ArrayAdapter<String> foundDevicesAdapter;
    ListView listViewFoundDevices;
    EditText editTextDeviceName, editTextDeviceLocation;
    Button btnSearchDevice, btnAccept, btnBack;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothAdapter.LeScanCallback leScanCallback;

    // Firebase
    DatabaseReference databaseReference;

    // Text-to-Speech
    TextToSpeech textToSpeech;
    boolean ttsEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newdevice);

        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("Dispositivos");

        // Preferencias
        SharedPreferences preferences = getSharedPreferences("mis_preferencias", MODE_PRIVATE);
        ttsEnabled = preferences.getBoolean("tts_enabled", false);

        // Inicializar TTS
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        // UI
        foundDevicesList = new ArrayList<>();
        foundDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foundDevicesList);
        listViewFoundDevices = findViewById(R.id.listViewFoundDevices);
        listViewFoundDevices.setAdapter(foundDevicesAdapter);

        editTextDeviceName = findViewById(R.id.editTextDeviceName);
        editTextDeviceLocation = findViewById(R.id.editTextDeviceLocation);

        btnSearchDevice = findViewById(R.id.btnSearchDevice);
        btnAccept = findViewById(R.id.btnAccept);
        btnBack = findViewById(R.id.btnBack);

        // Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            speakAndToast("El dispositivo no soporta Bluetooth");
            finish();
        }

        // Scan callback
        leScanCallback = (device, rssi, scanRecord) -> {
            @SuppressLint("MissingPermission") String deviceName = device.getName() != null ? device.getName() : "Dispositivo desconocido";
            foundDevicesList.add(deviceName);
            runOnUiThread(() -> foundDevicesAdapter.notifyDataSetChanged());
        };

        btnSearchDevice.setOnClickListener(v -> {
            if (mBluetoothAdapter.isEnabled()) {
                foundDevicesList.clear();
                if (checkBluetoothPermission()) {
                    mBluetoothAdapter.startLeScan(leScanCallback);
                    speakAndToast("Buscando dispositivos...");
                }
            } else {
                speakAndToast("Bluetooth no está habilitado");
            }
        });

        btnAccept.setOnClickListener(v -> {
            String deviceName = editTextDeviceName.getText().toString().trim();
            String deviceLocation = editTextDeviceLocation.getText().toString().trim();

            if (deviceName.isEmpty() || deviceLocation.isEmpty()) {
                speakAndToast("Por favor, complete todos los campos");
            } else {
                String id = databaseReference.push().getKey();
                Dispositivo nuevoDispositivo = new Dispositivo(deviceName, deviceLocation);
                databaseReference.child(id).setValue(nuevoDispositivo)
                        .addOnSuccessListener(aVoid -> {
                            speakAndToast("Dispositivo guardado correctamente");
                            finish();
                        })
                        .addOnFailureListener(e -> speakAndToast("Error al guardar en Firebase"));
            }
        });

        btnBack.setOnClickListener(v -> {
            speakAndToast("Regresando");
            finish();
        });

        // Leer botones si TTS está activado
        if (ttsEnabled) {
            speakText("Nueva actividad para agregar dispositivo. Botones: Buscar dispositivo, Aceptar, Regresar.");
        }
    }

    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, 1);
                return false;
            }
        }
        return true;
    }

    private void speakAndToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (ttsEnabled) {
            speakText(message);
        }
    }

    private void speakText(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && checkBluetoothPermission()) {
            mBluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && checkBluetoothPermission()) {
            mBluetoothAdapter.startLeScan(leScanCallback);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    // Clase para guardar en Firebase
    public static class Dispositivo {
        public String nombre;
        public String ubicacion;

        public Dispositivo() {
        }

        public Dispositivo(String nombre, String ubicacion) {
            this.nombre = nombre;
            this.ubicacion = ubicacion;
        }
    }
}
