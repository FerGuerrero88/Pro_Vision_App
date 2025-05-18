package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class UserActivity extends AppCompatActivity {

    private TextView textUsername;
    private ImageView imageProfile;
    private Button btnSelectAct;
    private Button btinHistorialActivity, btnTraduccionActivity, btnDeviceActivity, btnNewDeviceActivity, btnDeviceManagementActivity;
    private Animation fadeIn;
    private Switch switchTTS;

    private SharedPreferences sharedPreferences;
    private TextToSpeech tts;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        textUsername = findViewById(R.id.textUsername);
        imageProfile = findViewById(R.id.imageProfile);
        btnTraduccionActivity = findViewById(R.id.btnTraduccionActivity);
        btnSelectAct = findViewById(R.id.btnSelectAct);
        btnDeviceActivity = findViewById(R.id.btnDeviceActivity);
        btnNewDeviceActivity = findViewById(R.id.btnNewDeviceActivity);
        btinHistorialActivity = findViewById(R.id.btnHistorial);
        btnDeviceManagementActivity = findViewById(R.id.btnDeviceManagementActivity);
        switchTTS = findViewById(R.id.switchTTS);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String savedUsername = "Bienvenido de nuevo";
        textUsername.setText("Hola, " + savedUsername);
        setProfileImage("perfil_default");

        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());

                if (sharedPreferences.getBoolean("tts_enabled", false)) {
                    speak("Hola, bienvenido de nuevo");
                }
            }
        });

        switchTTS.setChecked(sharedPreferences.getBoolean("tts_enabled", false));
        switchTTS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("tts_enabled", isChecked);
            editor.apply();

            if (isChecked) {
                speak("Lectura en voz alta activada");
            } else {
                tts.stop();
            }
        });

        btnSelectAct.setOnClickListener(v -> toggleActivityOptions());
        speak("Selecciona una Actividad");
        btnDeviceActivity.setOnClickListener(v -> {
            speak("Abriendo control de dispositivos");
            startActivity(new Intent(this, DevicesActivity2.class));
        });

        btnTraduccionActivity.setOnClickListener(v -> {
            speak("Abriendo traducción de textos");
            startActivity(new Intent(this, TraduccionActivity2.class));
        });

        btnNewDeviceActivity.setOnClickListener(v -> {
            speak("Abriendo nuevo dispositivo");
            startActivity(new Intent(this, NewDeviceActivity.class));
        });

        btinHistorialActivity.setOnClickListener(v -> {
            speak("Abriendo historial de traducciones");
            startActivity(new Intent(this, HistorialActivity.class));
        });

        btnDeviceManagementActivity.setOnClickListener(v -> {
            speak("Abriendo Traduccion desde Historial");
            startActivity(new Intent(this, FavoritosActivity.class));
        });
    }

    private void speak(String texto) {
        boolean habilitado = sharedPreferences.getBoolean("tts_enabled", false);
        if (habilitado && tts != null && !tts.isSpeaking()) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
        }
    }

    private void setProfileImage(String imageName) {
        int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        imageProfile.setImageResource(resId != 0 ? resId : R.drawable.gafas_perfil);
    }

    private void toggleActivityOptions() {
        int visibility = btnDeviceActivity.getVisibility() == View.GONE ? View.VISIBLE : View.GONE;

        btnDeviceActivity.setVisibility(visibility);
        btnNewDeviceActivity.setVisibility(visibility);
        btnDeviceManagementActivity.setVisibility(visibility);
        btnTraduccionActivity.setVisibility(visibility);
        btinHistorialActivity.setVisibility(visibility);

        if (visibility == View.VISIBLE) {
            btnDeviceActivity.startAnimation(fadeIn);
            btnNewDeviceActivity.startAnimation(fadeIn);
            btnDeviceManagementActivity.startAnimation(fadeIn);
            btnTraduccionActivity.startAnimation(fadeIn);
            btinHistorialActivity.startAnimation(fadeIn);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
