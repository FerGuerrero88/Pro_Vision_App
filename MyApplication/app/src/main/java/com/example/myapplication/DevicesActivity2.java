package com.example.myapplication;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class DevicesActivity2 extends AppCompatActivity {

    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mBtSocket;
    private BluetoothDevice dispositivoSelec;
    private UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Button btnBuscar, btnConectar, btnDesconectar;
    private TextView selectedDeviceText, selectedDeviceGafas;
    private LinearLayout deviceLi;

    private TextView bluetoothTextView;
    private Button btnHablarTexto;

    private TextToSpeech textToSpeech;
    private String textoRecibido = "";

    private volatile boolean isConnected = false;
    private Thread escuchaThread;

    private boolean ttsEnabled = false;

    private final ActivityResultLauncher<Intent> someActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            showToast("Bluetooth activado");
                        } else {
                            showToast("Se requiere Bluetooth para continuar");
                            finish();
                        }
                    });

    LinearLayout cardFuente;

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices2);

        btnBuscar = findViewById(R.id.btnBuscar);
        btnConectar = findViewById(R.id.btnConectar);
        btnDesconectar = findViewById(R.id.btnDesconectar);
        selectedDeviceText = findViewById(R.id.selectedDeviceText);
        selectedDeviceGafas = findViewById(R.id.selectedDeviceGafas);
        deviceLi = findViewById(R.id.deviceLi);
        cardFuente = findViewById(R.id.cardDispositivoGafas);

        bluetoothTextView = findViewById(R.id.bluetoothTextView);
        btnHablarTexto = findViewById(R.id.btnHablarTexto);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        ttsEnabled = sharedPreferences.getBoolean("tts_enabled", false);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        btnHablarTexto.setOnClickListener(v -> {
            if (!textoRecibido.isEmpty()) {
                textToSpeech.speak(textoRecibido, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                showToast("No hay texto para leer");
            }
        });

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            showToast("Este dispositivo no tiene Bluetooth");
            finish();
            return;
        }

        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            someActivityResultLauncher.launch(enableBtIntent);
        }

        // Botón Buscar con voz
        btnBuscar.setOnClickListener(v -> {
            listarDispositivosVinculados();
            if (ttsEnabled) {
                textToSpeech.speak("Buscando dispositivos Bluetooth", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        // Botón Conectar con voz
        btnConectar.setOnClickListener(v -> {
            conectarDispositivo();
            if (ttsEnabled) {
                textToSpeech.speak("Intentando conectar al dispositivo", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        // Botón Desconectar con voz
        btnDesconectar.setOnClickListener(v -> {
            desconectarDispositivo();
            if (ttsEnabled) {
                textToSpeech.speak("Desconectando el dispositivo", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void listarDispositivosVinculados() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        deviceLi.removeAllViews();

        if (pairedDevices.isEmpty()) {
            showToast("No hay dispositivos emparejados");
            return;
        }

        deviceLi.setVisibility(View.VISIBLE);

        for (BluetoothDevice device : pairedDevices) {
            Button btnDevice = new Button(this);
            btnDevice.setText(device.getName());
            btnDevice.setTextColor(getResources().getColor(android.R.color.white));
            btnDevice.setAllCaps(false);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(50);
            shape.setColor(getResources().getColor(R.color.teal_700)); // Puedes cambiar color
            btnDevice.setBackground(shape);

            btnDevice.setOnClickListener(v -> {
                dispositivoSelec = device;
                selectedDeviceText.setText("Dispositivo: " + device.getName());
                selectedDeviceGafas.setText("Conectado a: " + device.getName());
                showToast("Seleccionado: " + device.getName());
                deviceLi.setVisibility(View.GONE);

                if (ttsEnabled) {
                    textToSpeech.speak("Dispositivo " + device.getName() + " seleccionado", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            });

            deviceLi.addView(btnDevice);
        }
    }

    @SuppressLint("MissingPermission")
    private void conectarDispositivo() {
        if (dispositivoSelec == null) {
            showToast("Selecciona un dispositivo primero");
            return;
        }

        if (isConnected) {
            showToast("Ya estás conectado a un dispositivo");
            return;
        }

        new Thread(() -> {
            try {
                mBtSocket = dispositivoSelec.createRfcommSocketToServiceRecord(BTMODULEUUID);
                mBtAdapter.cancelDiscovery();
                mBtSocket.connect();
                isConnected = true;

                runOnUiThread(() -> {
                    showToast("Conectado a " + dispositivoSelec.getName());
                    leerDesdeFirebase();
                });

                escucharComandosBluetooth();

            } catch (IOException e) {
                runOnUiThread(() -> showToast("Error al conectar: " + e.getMessage()));
                Log.e(TAG, "Error al conectar Bluetooth", e);
                try {
                    if (mBtSocket != null) mBtSocket.close();
                } catch (IOException closeEx) {
                    Log.e(TAG, "Error al cerrar socket", closeEx);
                }
            }
        }).start();
    }

    private void leerDesdeFirebase() {
        DatabaseReference dbRef = FirebaseDatabase
                .getInstance("https://sistemas-programables-37c92-default-rtdb.firebaseio.com/")
                .getReference("sensores");

        dbRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String datosFirebase = task.getResult().getValue().toString();
                textoRecibido = datosFirebase;
                bluetoothTextView.setText(datosFirebase);
                showToast("Datos recibidos desde Firebase");

                if (ttsEnabled) {
                    textToSpeech.speak(datosFirebase, TextToSpeech.QUEUE_FLUSH, null, null);
                }

                Log.i(TAG, "Datos Firebase: " + datosFirebase);
            } else {
                showToast("Error al leer Firebase");
                Log.e(TAG, "Error Firebase", task.getException());
            }
        });
    }

    private void escucharComandosBluetooth() {
        escuchaThread = new Thread(() -> {
            try {
                InputStream inputStream = mBtSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;

                while (isConnected && !Thread.currentThread().isInterrupted()) {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String incomingMessage = new String(buffer, 0, bytes);
                        String comando = incomingMessage.trim();
                        textoRecibido = comando;
                        runOnUiThread(() -> {
                            bluetoothTextView.setText(comando);
                            showToast("Comando recibido");
                            if (ttsEnabled) {
                                textToSpeech.speak(comando, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        });
                        Log.i(TAG, "Comando recibido: " + comando);
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "Error al leer datos Bluetooth: " + e.getMessage());
                runOnUiThread(() -> showToast("Se perdió la conexión"));
                isConnected = false;
            }
        });

        escuchaThread.start();
    }

    private void desconectarDispositivo() {
        if (mBtSocket != null && isConnected) {
            try {
                isConnected = false;

                if (escuchaThread != null && escuchaThread.isAlive()) {
                    escuchaThread.interrupt();
                }

                mBtSocket.close();
                mBtSocket = null;

                showToast("Dispositivo desconectado");
                Log.i(TAG, "Dispositivo desconectado exitosamente");

            } catch (IOException e) {
                Log.e(TAG, "Error al cerrar el socket", e);
                showToast("Error al desconectar");
            }
        } else {
            showToast("No hay conexión activa");
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
