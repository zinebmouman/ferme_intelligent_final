package com.example.ferme_intelligente.Activities;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditPlante extends BaseActivity {

    private EditText etNom, etDescription, etPeriodePlantation;
    private Spinner spinnerType, spinnerImage;
    private ImageView imgPreview;
    private Button btnSauvegarder;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String fermierId;
    private String selectedImage; // Pour stocker l'image sélectionnée

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater le contenu dans le fragment_container de BaseActivity
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_edit_plante, container, true);

        // Initialisation des vues
        etNom = findViewById(R.id.etNom);
        etDescription = findViewById(R.id.etDescription);
        etPeriodePlantation = findViewById(R.id.etPeriodePlantation);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerImage = findViewById(R.id.spinnerImage);
        imgPreview = findViewById(R.id.imgPreview);
        btnSauvegarder = findViewById(R.id.btnSauvegarder);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Récupération des données passées depuis l'adapter
        fermierId = getIntent().getStringExtra("fermierId");
        String planteId = getIntent().getStringExtra("planteId");
        Log.d("EditPlante", "Plante ID reçu : " + planteId);

        if (planteId == null || planteId.isEmpty()) {
            Toast.makeText(this, "Erreur : Information manquante", Toast.LENGTH_SHORT).show();
        }

        if (fermierId == null || planteId == null) {
            Toast.makeText(this, "Erreur : informations manquantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etNom.setText(getIntent().getStringExtra("nomPlante"));
        etDescription.setText(getIntent().getStringExtra("descriptionPlante"));
        etPeriodePlantation.setText(getIntent().getStringExtra("periodePlantationPlante"));
        selectedImage = getIntent().getStringExtra("imagePlante");

        // Configuration du spinner des types
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.types_plantes, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
        spinnerType.setSelection(typeAdapter.getPosition(getIntent().getStringExtra("typePlante")));

        // Configuration du spinner des images
        ArrayAdapter<CharSequence> imageAdapter = ArrayAdapter.createFromResource(
                this, R.array.images_plantes, android.R.layout.simple_spinner_item);
        imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImage.setAdapter(imageAdapter);
        spinnerImage.setSelection(imageAdapter.getPosition(selectedImage));

        spinnerImage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedImage = parent.getItemAtPosition(position).toString();
                int imageResource = getResources().getIdentifier(selectedImage, "mipmap", getPackageName());
                imgPreview.setImageResource(imageResource);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSauvegarder.setOnClickListener(v -> updatePlante());
    }

    private void updatePlante() {
        String nom = etNom.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String periodePlantation = etPeriodePlantation.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();

        if (nom.isEmpty() || description.isEmpty() || periodePlantation.isEmpty() || type.isEmpty() || selectedImage == null) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        String planteId = getIntent().getStringExtra("planteId");
        Log.d("EditPlante", "Plante ID reçu : " + planteId);

        if (planteId == null || planteId.isEmpty()) {
            Toast.makeText(this, "Erreur : Information manquante", Toast.LENGTH_SHORT).show();
        }
        Map<String, Object> planteData = new HashMap<>();
        planteData.put("nom", nom);
        planteData.put("description", description);
        planteData.put("periodePlantation", periodePlantation);
        planteData.put("type", type);
        planteData.put("image", selectedImage);

        db.collection("fermiers")
                .document(fermierId)
                .collection("plantes")
                .document(planteId)
                .set(planteData, SetOptions.merge()) // Met à jour sans écraser d'autres champs
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditPlante.this, "Plante mise à jour avec succès", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(EditPlante.this, "Erreur de mise à jour", Toast.LENGTH_SHORT).show());
    }
}