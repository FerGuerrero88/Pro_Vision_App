package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView logoImage;
    private TextView welcomeText;
    private Button btnWelcome;

    private TextToSpeech tts;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean leerAutomaticamente = prefs.getBoolean("ttsEnabled", false);
        if (leerAutomaticamente) {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.getDefault());

                    // Si está activado, decir saludo
                    if (sharedPreferences.getBoolean("tts_enabled", false)) {
                        speak("Entrar a la aplicacion");
                    }
                }
            });
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoImage = findViewById(R.id.logoApp);
        welcomeText = findViewById(R.id.welcomeText);
        btnWelcome = findViewById(R.id.btnWelcome);

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logoImage.startAnimation(fadeIn);
        welcomeText.startAnimation(fadeIn);
        btnWelcome.startAnimation(fadeIn);

        // Solicitar permisos al iniciar
        solicitarPermisos();

        btnWelcome.setOnClickListener(v -> {
            speak("Abriendo Aplicacion");
            String savedUsername = sharedPreferences.getString("username", null);
            Intent intent = new Intent(MainActivity.this, UserActivity.class);
            if (savedUsername != null) {
                intent.putExtra("username", savedUsername);
            }
            startActivity(intent);
            finish();
        });
    }

    private void speak(String texto) {
        if (tts != null && !tts.isSpeaking()) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
        }
    }
    private void solicitarPermisos() {
        String[] permisos = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisos = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        // Verificar si falta algún permiso
        boolean permisosFaltantes = false;
        for (String permiso : permisos) {
            if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                permisosFaltantes = true;
                break;
            }
        }

        if (permisosFaltantes) {
            ActivityCompat.requestPermissions(this, permisos, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
}
