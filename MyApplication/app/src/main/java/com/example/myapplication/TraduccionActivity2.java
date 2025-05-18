package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TraduccionActivity2 extends AppCompatActivity {

    private TextView translatedText;
    private Button btnCapturar, btnIniciarTraduccion, btnPararTraduccion, btnLeerTexto, btnGuardarIP, btnBorrarImagen, btnGuardarTraduccion;
    private Button btnIngles, btnFrances, btnAleman, btnEsp;
    private ImageView imagePreview;
    private EditText etCameraIP;
    private WebView webView;
    private Switch switchCamara;
    private TextToSpeech textToSpeech;

    private String textoTraducido = "";
    private String cameraIP = "";
    private String snapshotURL = "";
    private boolean ttsEnabled = false;
    private boolean usarCamaraCelular = false;

    @Nullable
    private String idiomaSeleccionado = null;

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Nullable
    private DatabaseReference databaseRef = null;

    @SuppressLint({"SetJavaScriptEnabled", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        ttsEnabled = sharedPreferences.getBoolean("tts_enabled", false);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traduccion);

        translatedText = findViewById(R.id.translatedText);
        btnCapturar = findViewById(R.id.btnCapturar);
        btnIniciarTraduccion = findViewById(R.id.btnIniciarTraduccion);
        btnPararTraduccion = findViewById(R.id.btnPararTraduccion);
        btnLeerTexto = findViewById(R.id.btnLeerTexto);
        btnGuardarIP = findViewById(R.id.btnGuardarIP);
        btnBorrarImagen = findViewById(R.id.btnBorrarImagen);
        btnGuardarTraduccion = findViewById(R.id.btnGuardarTraduccion);
        btnIngles = findViewById(R.id.btnIngles);
        btnFrances = findViewById(R.id.btnFrances);
        btnAleman = findViewById(R.id.btnAleman);
        btnEsp = findViewById(R.id.btnEsp);
        imagePreview = findViewById(R.id.imagePreview);
        etCameraIP = findViewById(R.id.etCameraIP);
        webView = findViewById(R.id.webView);
        switchCamara = findViewById(R.id.switchCamara);

        try {
            databaseRef = FirebaseDatabase.getInstance().getReference("traducciones");
        } catch (Exception e) {
            databaseRef = null;
            Toast.makeText(this, "No estás conectado a Firebase", Toast.LENGTH_LONG).show();
        }

        // Inicializa TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault());
            }
        });

        // Switch para seleccionar cámara celular o IP
        switchCamara.setOnCheckedChangeListener((buttonView, isChecked) -> {
            usarCamaraCelular = isChecked;
            if (usarCamaraCelular) {
                Toast.makeText(this, "Usando cámara del celular", Toast.LENGTH_SHORT).show();
                if (ttsEnabled)
                    textToSpeech.speak("Usando cámara del celular", TextToSpeech.QUEUE_FLUSH, null, null);
                webView.setVisibility(WebView.GONE);
                imagePreview.setVisibility(ImageView.VISIBLE);
            } else {
                Toast.makeText(this, "Usando cámara por IP", Toast.LENGTH_SHORT).show();
                if (ttsEnabled)
                    textToSpeech.speak("Usando cámara por IP", TextToSpeech.QUEUE_FLUSH, null, null);
                webView.setVisibility(WebView.VISIBLE);
                imagePreview.setVisibility(ImageView.GONE);
            }
        });

        btnIngles.setOnClickListener(v -> {
            idiomaSeleccionado = TranslateLanguage.ENGLISH;
            Toast.makeText(this, "Idioma seleccionado: Inglés", Toast.LENGTH_SHORT).show();
            if (ttsEnabled) {
                textToSpeech.speak("Idioma seleccionado: Inglés", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnFrances.setOnClickListener(v -> {
            idiomaSeleccionado = TranslateLanguage.FRENCH;
            Toast.makeText(this, "Idioma seleccionado: Francés", Toast.LENGTH_SHORT).show();
            if (ttsEnabled) {
                textToSpeech.speak("Idioma seleccionado: Francés", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnAleman.setOnClickListener(v -> {
            idiomaSeleccionado = TranslateLanguage.GERMAN;
            Toast.makeText(this, "Idioma seleccionado: Alemán", Toast.LENGTH_SHORT).show();
            if (ttsEnabled) {
                textToSpeech.speak("Idioma seleccionado: Alemán", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnEsp.setOnClickListener(v -> {
            idiomaSeleccionado = TranslateLanguage.SPANISH;
            Toast.makeText(this, "Idioma seleccionado: Español", Toast.LENGTH_SHORT).show();
            if (ttsEnabled) {
                textToSpeech.speak("Idioma seleccionado: Español", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnGuardarIP.setOnClickListener(v -> {
            if (ttsEnabled) {
                textToSpeech.speak("Intentando conectarse a la IP", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            guardarIPYConectar();
        });

        btnCapturar.setOnClickListener(v -> {
            if (ttsEnabled) {
                textToSpeech.speak("Capturar y traducir", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            if (usarCamaraCelular) {
                capturarDesdeCamaraCelular();
            } else {
                capturarDesdeCamara();
            }
        });

        btnIniciarTraduccion.setOnClickListener(v -> {
            if (ttsEnabled) {
                textToSpeech.speak("Iniciar Traducción", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            iniciarTraduccion();
        });

        btnPararTraduccion.setOnClickListener(v -> {
            if (ttsEnabled) {
                textToSpeech.speak("Parar Traducción", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            pararTraduccion();
        });

        btnLeerTexto.setOnClickListener(v -> {
            if (ttsEnabled) {
                textToSpeech.speak("Leer texto traducido", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            leerTextoEnVoz();
        });

        btnBorrarImagen.setOnClickListener(v -> {
            if (ttsEnabled) {
                textToSpeech.speak("Borrar Captura", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            borrarImagen();
        });

        btnGuardarTraduccion.setOnClickListener(v -> {
            if (textoTraducido.isEmpty() || translatedText.getText().toString().isEmpty()) {
                Toast.makeText(this, "No hay traducción para guardar.", Toast.LENGTH_SHORT).show();
                return;
            }
            guardarEnFirebase(textoTraducido, translatedText.getText().toString(), idiomaSeleccionado != null ? idiomaSeleccionado : TranslateLanguage.SPANISH);
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // Inicializar estado del switch (por defecto apagado)
        usarCamaraCelular = switchCamara.isChecked();
        if (usarCamaraCelular) {
            webView.setVisibility(WebView.GONE);
            imagePreview.setVisibility(ImageView.VISIBLE);
        } else {
            webView.setVisibility(WebView.VISIBLE);
            imagePreview.setVisibility(ImageView.GONE);
        }
    }

    private void guardarIPYConectar() {
        if (!hayConexionInternet()) {
            Toast.makeText(this, "Por favor, activa tu conexión a internet.", Toast.LENGTH_LONG).show();
            return;
        }

        cameraIP = etCameraIP.getText().toString().trim();
        if (!cameraIP.startsWith("http://") && !cameraIP.startsWith("https://")) {
            cameraIP = "http://" + cameraIP;
        }

        if (!cameraIP.isEmpty()) {
            snapshotURL = cameraIP + "/shot.jpg";
            webView.loadUrl(cameraIP);
            Toast.makeText(this, "IP guardada y video cargado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Por favor ingresa una IP válida.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hayConexionInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void capturarDesdeCamara() {
        if (cameraIP.isEmpty()) {
            Toast.makeText(this, "Primero guarda una IP válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullSnapshotUrl = cameraIP + "/shot.jpg?time=" + System.currentTimeMillis();
        imagePreview.setImageBitmap(null);

        Glide.with(this)
                .asBitmap()
                .load(fullSnapshotUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        imagePreview.setImageBitmap(bitmap);
                        procesarTextoDesdeBitmap(bitmap);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        imagePreview.setImageResource(R.drawable.gafas_err);
                        translatedText.setText("Error al capturar imagen.");
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        imagePreview.setImageBitmap(null);
                    }
                });
    }



    // Método para lanzar la cámara nativa y capturar imagen
    private void capturarDesdeCamaraCelular() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No se encontró aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    // Método que recibe el resultado de la cámara
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data"); // thumbnail pequeño
            if (imageBitmap != null) {
                imagePreview.setImageBitmap(imageBitmap); // mostrar en ImageView
                procesarTextoDesdeBitmap(imageBitmap);    // procesar texto con ML Kit
            } else {
                Toast.makeText(this, "Error al obtener imagen de cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void procesarTextoDesdeBitmap(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    textoTraducido = visionText.getText();
                    translatedText.setText(textoTraducido);
                    if (ttsEnabled && !textoTraducido.isEmpty()) {
                        textToSpeech.speak(textoTraducido, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    translatedText.setText("Error reconociendo texto: " + e.getMessage());
                });
    }

    private Translator obtenerTraductor(String idiomaDestino) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.SPANISH)
                .setTargetLanguage(idiomaDestino)
                .build();

        return Translation.getClient(options);
    }

    private void iniciarTraduccion() {
        if (textoTraducido.isEmpty()) {
            Toast.makeText(this, "No hay texto para traducir.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idiomaSeleccionado == null) {
            Toast.makeText(this, "Selecciona un idioma de destino.", Toast.LENGTH_SHORT).show();
            return;
        }

        Translator translator = obtenerTraductor(idiomaSeleccionado);
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> translator.translate(textoTraducido)
                        .addOnSuccessListener(translated -> {
                            translatedText.setText(translated);
                            if (ttsEnabled) {
                                textToSpeech.speak(translated, TextToSpeech.QUEUE_FLUSH, null, null);
                            }
                        })
                        .addOnFailureListener(e -> {
                            translatedText.setText("Error al traducir: " + e.getMessage());
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error descargando modelo de traducción: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void pararTraduccion() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        Toast.makeText(this, "Traducción detenida.", Toast.LENGTH_SHORT).show();
    }

    private void leerTextoEnVoz() {
        String texto = translatedText.getText().toString();
        if (texto.isEmpty()) {
            Toast.makeText(this, "No hay texto para leer.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (textToSpeech != null) {
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void borrarImagen() {
        imagePreview.setImageBitmap(null);
        translatedText.setText("");
        textoTraducido = "";
    }

    private void guardarEnFirebase(String textoOriginal, String textoTraducido, String idiomaDestino) {
        if (databaseRef == null) {
            Toast.makeText(this, "No conectado a Firebase", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = databaseRef.push().getKey();
        if (id == null) {
            Toast.makeText(this, "Error generando ID para Firebase", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear un objeto Map con los datos a guardar
        Map<String, Object> datosTraduccion = new HashMap<>();
        datosTraduccion.put("textoOriginal", textoOriginal);
        datosTraduccion.put("textoTraducido", textoTraducido);
        datosTraduccion.put("idiomaDestino", idiomaDestino);
        datosTraduccion.put("timestamp", System.currentTimeMillis()); // opcional para orden

        databaseRef.child(id).setValue(datosTraduccion)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Guardado en Firebase", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}