package com.example.ferme_intelligente.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ferme_intelligente.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfilActivity extends AppCompatActivity {

    private TextView tvFullName ,tvNom, tvPrenom, tvContact, tvLocalisation, tvEmail;
    private EditText editNom, editPrenom, editContact, editLocalisation;
    private Button btnEditProfile, btnSaveChanges, btnCancelEdit, btnChangePassword, btnLogout;
    private LinearLayout viewProfileLayout, editProfileLayout;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        // Initialisation des vues
        tvNom = findViewById(R.id.tvNom);
        tvPrenom = findViewById(R.id.tvPrenom);
        tvContact = findViewById(R.id.tvContact);
        tvLocalisation = findViewById(R.id.tvLocalisation);
        tvEmail = findViewById(R.id.tvEmail);
        tvFullName = findViewById(R.id.tvFullName);

        editNom = findViewById(R.id.editNom);
        editPrenom = findViewById(R.id.editPrenom);
        editContact = findViewById(R.id.editContact);
        editLocalisation = findViewById(R.id.editLocalisation);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        viewProfileLayout = findViewById(R.id.viewProfileLayout);
        editProfileLayout = findViewById(R.id.editProfileLayout);

        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            loadUserProfile();
        }

        btnEditProfile.setOnClickListener(v -> switchToEditMode());
        btnSaveChanges.setOnClickListener(v -> updateProfile());
        btnCancelEdit.setOnClickListener(v -> switchToViewMode());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void loadUserProfile() {
        if (currentUser != null) {
            String userId = currentUser.getEmail();
            db.collection("fermiers").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nom = documentSnapshot.getString("nom");
                            String prenom = documentSnapshot.getString("prenom");

                            tvNom.setText( documentSnapshot.getString("nom"));
                            tvPrenom.setText( documentSnapshot.getString("prenom"));
                            tvContact.setText( documentSnapshot.getString("contact"));
                            tvLocalisation.setText( documentSnapshot.getString("localisation"));
                            tvFullName.setText(prenom + " " + nom);

                            // Pré-remplir les champs d'édition
                            editNom.setText(documentSnapshot.getString("nom"));
                            editPrenom.setText(documentSnapshot.getString("prenom"));
                            editContact.setText(documentSnapshot.getString("contact"));
                            editLocalisation.setText(documentSnapshot.getString("localisation"));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfilActivity.this, "Erreur de chargement du profil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void switchToEditMode() {
        viewProfileLayout.setVisibility(View.GONE);
        editProfileLayout.setVisibility(View.VISIBLE);
    }

    private void switchToViewMode() {
        editProfileLayout.setVisibility(View.GONE);
        viewProfileLayout.setVisibility(View.VISIBLE);
    }


    private void updateProfile() {
        String nom = editNom.getText().toString().trim();
        String prenom = editPrenom.getText().toString().trim();
        String contact = editContact.getText().toString().trim();
        String localisation = editLocalisation.getText().toString().trim();

        if (TextUtils.isEmpty(nom) || TextUtils.isEmpty(prenom) || TextUtils.isEmpty(contact) || TextUtils.isEmpty(localisation)) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser != null) {
            String userId = currentUser.getEmail();
            Map<String, Object> updates = new HashMap<>();
            updates.put("nom", nom);
            updates.put("prenom", prenom);
            updates.put("contact", contact);
            updates.put("localisation", localisation);

            db.collection("fermiers").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfilActivity.this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show();
                        // Mettre à jour l'affichage
                        tvNom.setText("Nom: " + nom);
                        tvPrenom.setText("Prénom: " + prenom);
                        tvContact.setText("Contact: " + contact);
                        tvLocalisation.setText("Localisation: " + localisation);


                        switchToViewMode();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfilActivity.this, "Erreur de mise à jour: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showChangePasswordDialog() {
        // Implémentez cette méthode pour changer le mot de passe
        Toast.makeText(this, "Fonctionnalité de changement de mot de passe", Toast.LENGTH_SHORT).show();
    }

    private void logoutUser() {
        auth.signOut();
        Intent intent = new Intent(ProfilActivity.this, Authentification.class);
        startActivity(intent);
        finish();
    }
}