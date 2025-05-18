package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritosActivity extends AppCompatActivity {

    private TextView textViewTextoTraducido, textViewFavoritoSeleccionado, textViewIdiomaActual;
    private RecyclerView recyclerView;
    private Button btnToggleLista;
    private View cardListaFavoritos;
    private String textoSeleccionado = null;

    private final Map<String, String> idiomaMap = new HashMap<String, String>() {{
        put("es", "Español");
        put("en", "Inglés");
        put("fr", "Francés");
        put("de", "Alemán");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritos); // Asegúrate que coincida con tu XML

        textViewTextoTraducido = findViewById(R.id.textViewTextoTraducido);
        textViewFavoritoSeleccionado = findViewById(R.id.textViewFavoritoSeleccionado);
        textViewIdiomaActual = findViewById(R.id.textViewIdiomaActual);
        btnToggleLista = findViewById(R.id.btnToggleLista);
        recyclerView = findViewById(R.id.recyclerViewFavoritos);
        cardListaFavoritos = findViewById(R.id.cardListaFavoritos);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnToggleLista.setOnClickListener(v -> {
            if (cardListaFavoritos.getVisibility() == View.GONE) {
                cardListaFavoritos.setVisibility(View.VISIBLE);
                btnToggleLista.setText("Ocultar lista");
            } else {
                cardListaFavoritos.setVisibility(View.GONE);
                btnToggleLista.setText("Mostrar lista");
            }
        });

        cargarTraducciones();

        findViewById(R.id.btnEspañol).setOnClickListener(v -> traducir("es"));
        findViewById(R.id.btnIngles).setOnClickListener(v -> traducir("en"));
        findViewById(R.id.btnFrances).setOnClickListener(v -> traducir("fr"));
        findViewById(R.id.btnAleman).setOnClickListener(v -> traducir("de"));
    }

    private void cargarTraducciones() {
        FirebaseDatabase.getInstance().getReference("traducciones")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> lista = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String texto = snap.child("textoTraducido").getValue(String.class);
                            if (texto != null) {
                                lista.add(texto);
                            }
                        }

                        if (lista.isEmpty()) {
                            Toast.makeText(FavoritosActivity.this, "No hay traducciones registradas", Toast.LENGTH_SHORT).show();
                        }

                        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                            @NonNull
                            @Override
                            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View view = LayoutInflater.from(parent.getContext())
                                        .inflate(android.R.layout.simple_list_item_1, parent, false);
                                return new RecyclerView.ViewHolder(view) {};
                            }

                            @Override
                            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                                TextView textView = holder.itemView.findViewById(android.R.id.text1);
                                String texto = lista.get(position);
                                textView.setText(texto);
                                holder.itemView.setOnClickListener(v -> {
                                    textoSeleccionado = texto;
                                    textViewFavoritoSeleccionado.setText("Texto seleccionado: " + texto);
                                });
                            }

                            @Override
                            public int getItemCount() {
                                return lista.size();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FavoritosActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void traducir(String idiomaDestino) {
        if (textoSeleccionado == null) {
            Toast.makeText(this, "Primero selecciona un texto", Toast.LENGTH_SHORT).show();
            return;
        }

        textViewIdiomaActual.setText("Idioma actual: " + idiomaMap.get(idiomaDestino));

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.SPANISH)
                .setTargetLanguage(idiomaDestino)
                .build();

        Translator translator = Translation.getClient(options);

        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> translator.translate(textoSeleccionado)
                        .addOnSuccessListener(traducido -> textViewTextoTraducido.setText(traducido))
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al traducir", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(this, "Error al descargar modelo", Toast.LENGTH_SHORT).show());
    }
}
