package com.example.ferme_intelligente.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ferme_intelligente.R;

public class AccueilFermier extends AppCompatActivity {

    private Button btnConsulterPlantes, btnAjouterPlante, btnConsulterAlertes, btnModifierProfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accueil_fermier);

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
