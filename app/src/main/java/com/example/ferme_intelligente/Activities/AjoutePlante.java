package com.example.ferme_intelligente.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.ferme_intelligente.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AjoutePlante extends BaseActivity {

    private EditText etNom, etDescription, etPeriodePlantation;
    private Spinner spinnerType, spinnerImage;
    private ImageView imgPreview;
    private Button btnEnregistrer;

    private final String[] typesPlantes = {"Fruit", "Légume", "Fleur", "Arbre"};
    private final String[] imagesPlantes = {"plante1", "plante2", "plante3", "plante4", "plante6"};
    private final int[] imagesResIds = {R.mipmap.plante1, R.mipmap.plante2, R.mipmap.plante3, R.mipmap.plante4};

    private String selectedType = "Fruit";
    private String selectedImageName = "plante1";
    private int selectedImageResId = R.mipmap.farm;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String fermierEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater le contenu dans le fragment_container de BaseActivity
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_ajoute_plante, container, true);

        // Pas de sélection spécifique pour la navigation car cette activité
        // peut être accédée depuis plusieurs endroits

        // Initialisation Firestore et Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.e("Firestore", "Utilisateur non connecté");
            Toast.makeText(this, "Erreur : Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Authentification.class));
            finish();
            return;
        }

        fermierEmail = user.getEmail(); // Récupérer l'email comme identifiant

        // Liaison avec le layout
        etNom = findViewById(R.id.etNom);
        etDescription = findViewById(R.id.etDescription);
        etPeriodePlantation = findViewById(R.id.etPeriodePlantation);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerImage = findViewById(R.id.spinnerImage);
        imgPreview = findViewById(R.id.imgPreview);
        btnEnregistrer = findViewById(R.id.btnEnregistrer);

        // Configuration des spinners
        setupSpinners();

        // Bouton d'enregistrement
        btnEnregistrer.setOnClickListener(v -> ajouterPlante());
    }

    private void setupSpinners() {
        // Configuration du spinner Type
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typesPlantes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = typesPlantes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Configuration du spinner Image
        ArrayAdapter<String> imageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, imagesPlantes);
        imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImage.setAdapter(imageAdapter);
        spinnerImage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedImageName = imagesPlantes[position];
                if (position < imagesResIds.length) {
                    selectedImageResId = imagesResIds[position];
                    imgPreview.setImageResource(selectedImageResId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void ajouterPlante() {
        String nom = etNom.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String periodePlantation = etPeriodePlantation.getText().toString().trim();

        if (nom.isEmpty() || description.isEmpty() || periodePlantation.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Désactiver le bouton pendant l'enregistrement
        btnEnregistrer.setEnabled(false);
        btnEnregistrer.setText("Enregistrement...");

        // Référence à la collection "plantes" sous l'email du fermier
        db.collection("fermiers").document(fermierEmail)
                .collection("plantes")
                .add(new HashMap<>()) // Créer un document vide temporairement pour obtenir un ID
                .addOnSuccessListener(documentReference -> {
                    String idPlante = documentReference.getId(); // Récupérer l'ID généré

                    // Création de l'objet plante avec l'ID
                    Map<String, Object> plante = new HashMap<>();
                    plante.put("id", idPlante);
                    plante.put("nom", nom);
                    plante.put("description", description);
                    plante.put("periodePlantation", periodePlantation);
                    plante.put("type", selectedType);
                    plante.put("image", selectedImageName);

                    // Mise à jour du document avec l'ID
                    documentReference.set(plante)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Plante ajoutée avec succès", Toast.LENGTH_SHORT).show();

                                // Retourner à l'accueil ou à la page des plantes
                                Intent intent = new Intent(this, AccueilFermier.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Erreur lors de l'ajout : " + e.getMessage());
                                Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Erreur lors de la création du document : " + e.getMessage());
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        btnEnregistrer.setEnabled(true);
        btnEnregistrer.setText("Enregistrer");
    }

    @Override
    public void onBackPressed() {
        // Retourner à l'accueil au lieu de la page précédente
        super.onBackPressed();
        Intent intent = new Intent(this, AccueilFermier.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}