package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TraduccionAdapter adapter;
    private List<Traduccion> traducciones = new ArrayList<>();
    private Handler handler = new Handler();
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextToSpeech tts;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerViewTraducciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TraduccionAdapter();
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::cargarTraduccionesDesdeFirebase);

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        cargarTraduccionesDesdeFirebase();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void cargarTraduccionesDesdeFirebase() {
        swipeRefreshLayout.setRefreshing(true);

        FirebaseDatabase.getInstance().getReference("traducciones")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        traducciones.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Traduccion traduccion = data.getValue(Traduccion.class);
                            if (traduccion != null) {
                                traduccion.firebaseKey = data.getKey(); // Guardamos la key Firebase
                                traducciones.add(traduccion);
                            }
                        }
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        runOnUiThread(() -> {
                            Toast.makeText(HistorialActivity.this, "Error al cargar traducciones: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                });
    }

    private void mostrarDialogoNombreFavorito(Traduccion traduccion, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nombre para el favorito");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombreFavorito = input.getText().toString().trim();
            if (!nombreFavorito.isEmpty()) {
                traduccion.favorito = true;
                traduccion.nombreFavorito = nombreFavorito;
                guardarFavoritoEnFirebase(traduccion);
                Toast.makeText(HistorialActivity.this, "Añadido a favoritos: " + nombreFavorito, Toast.LENGTH_SHORT).show();
                adapter.notifyItemChanged(position);
                hablar("Favorito guardado: " + nombreFavorito);
            } else {
                Toast.makeText(HistorialActivity.this, "Nombre vacío, no se añadió favorito", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void guardarFavoritoEnFirebase(Traduccion traduccion) {
        if (traduccion.firebaseKey == null) return;

        // Actualizar traducción en "traducciones"
        FirebaseDatabase.getInstance().getReference("traducciones")
                .child(traduccion.firebaseKey)
                .setValue(traduccion)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (traduccion.favorito) {
                            // Guardar también en tabla "favoritos"
                            FirebaseDatabase.getInstance().getReference("favoritos")
                                    .child(traduccion.firebaseKey)
                                    .setValue(traduccion)
                                    .addOnCompleteListener(taskFav -> {
                                        if (!taskFav.isSuccessful()) {
                                            Toast.makeText(HistorialActivity.this, "Error guardando favorito en tabla favoritos", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // Si se quita favorito, eliminar de tabla "favoritos"
                            FirebaseDatabase.getInstance().getReference("favoritos")
                                    .child(traduccion.firebaseKey)
                                    .removeValue()
                                    .addOnCompleteListener(taskDel -> {
                                        if (!taskDel.isSuccessful()) {
                                            Toast.makeText(HistorialActivity.this, "Error eliminando favorito en tabla favoritos", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(HistorialActivity.this, "Error al guardar favorito en Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hablar(String texto) {
        SharedPreferences prefs = getSharedPreferences("preferencias", MODE_PRIVATE);
        boolean leerFavoritos = prefs.getBoolean("leerFavoritos", false);

        if (leerFavoritos && tts != null) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private class TraduccionAdapter extends RecyclerView.Adapter<TraduccionAdapter.TraduccionViewHolder> {

        @NonNull
        @Override
        public TraduccionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_traduccion, parent, false);
            return new TraduccionViewHolder(view);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull TraduccionViewHolder holder, int position) {
            Traduccion traduccion = traducciones.get(position);
            String textoMostrar = traduccion.textoTraducido != null ? traduccion.textoTraducido : traduccion.textoOriginal;
            holder.textView.setText(textoMostrar);

            if (traduccion.favorito) {
                holder.textView.setBackgroundColor(Color.YELLOW);
            } else {
                holder.textView.setBackgroundColor(Color.WHITE);
            }

            final Runnable longPressRunnable = () -> {
                if (traduccion.favorito) {
                    // Quitar favorito
                    traduccion.favorito = false;
                    traduccion.nombreFavorito = null;
                    guardarFavoritoEnFirebase(traduccion);
                    Toast.makeText(HistorialActivity.this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                } else {
                    runOnUiThread(() -> mostrarDialogoNombreFavorito(traduccion, position));
                }
            };

            holder.itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(longPressRunnable, 1000);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        handler.removeCallbacks(longPressRunnable);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPressRunnable);
                        break;
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return traducciones.size();
        }

        class TraduccionViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            TraduccionViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textViewTraduccion);
            }
        }
    }

    // Clase modelo para traducción
    public static class Traduccion {
        public String textoOriginal;
        public String textoTraducido;
        public String idiomaDestino;
        public boolean favorito = false;          // Campo para favorito
        public String nombreFavorito = null;      // Nombre del favorito
        public String firebaseKey = null;          // Para referencia Firebase

        public Traduccion() {
            // Constructor vacío para Firebase
        }

        public Traduccion(String textoOriginal, String textoTraducido, String idiomaDestino) {
            this.textoOriginal = textoOriginal;
            this.textoTraducido = textoTraducido;
            this.idiomaDestino = idiomaDestino;
        }
    }
}
