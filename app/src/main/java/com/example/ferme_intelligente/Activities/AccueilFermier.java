package com.example.ferme_intelligente.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;

import com.example.ferme_intelligente.R;

public class AccueilFermier extends BaseActivity {

    private Button btnConsulterPlantes, btnAjouterPlante, btnConsulterAlertes, btnModifierProfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater votre contenu dans le fragment_container
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_accueil_fermier, container, true);

        // Set the selected item in bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_accueil);

        // Liaison avec les boutons du layout XML
        btnConsulterPlantes = findViewById(R.id.btnConsulterPlantes);
        btnAjouterPlante = findViewById(R.id.btnAjouterPlante);
        btnConsulterAlertes = findViewById(R.id.btnConsulterAlertes);
        btnModifierProfil = findViewById(R.id.btnModifierProfil);

        // Action pour consulter les plantes
        btnConsulterPlantes.setOnClickListener(v -> {
            Intent intent = new Intent(AccueilFermier.this, ConsulterPlantesActivity.class);
            startActivity(intent);
        });

        // Action pour ajouter une plante
        btnAjouterPlante.setOnClickListener(v -> {
            Intent intent = new Intent(AccueilFermier.this, AjoutePlante.class);
            startActivity(intent);
        });

        // Action pour consulter les alertes
        btnConsulterAlertes.setOnClickListener(v -> {
            Intent intent = new Intent(AccueilFermier.this, ConsulterAlertesActivity.class);
            startActivity(intent);
        });

        // Action pour modifier le profil
        btnModifierProfil.setOnClickListener(v -> {
            Intent intent = new Intent(AccueilFermier.this, ProfilActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnViewData).setOnClickListener(v -> {
            startActivity(new Intent(this, Mydata.class));
        });
        findViewById(R.id.btnViewDashbord).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
        });
    }
}