package com.example.ferme_intelligente.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class Authentification extends AppCompatActivity {
    private LinearLayout loginLayout, registerLayout;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentification);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginLayout = findViewById(R.id.loginLayout);
        registerLayout = findViewById(R.id.registerLayout);

        EditText emailLogin = findViewById(R.id.emailLogin);
        EditText passwordLogin = findViewById(R.id.passwordLogin);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView switchToRegister = findViewById(R.id.switchToRegister);
        TextView switchToLogin = findViewById(R.id.switchToLogin);

        EditText nom = findViewById(R.id.nom);
        EditText prenom = findViewById(R.id.prenom);
        EditText contact = findViewById(R.id.contact);
        EditText localisation = findViewById(R.id.localisation);
        EditText emailRegister = findViewById(R.id.emailRegister);
        EditText passwordRegister = findViewById(R.id.passwordRegister);
        Button btnRegister = findViewById(R.id.btnRegister);

        // Changer de connexion à inscription
        switchToRegister.setOnClickListener(v -> {
            loginLayout.setVisibility(View.GONE);
            registerLayout.setVisibility(View.VISIBLE);
        });

        // Changer d'inscription à connexion
        switchToLogin.setOnClickListener(v -> {
            registerLayout.setVisibility(View.GONE);
            loginLayout.setVisibility(View.VISIBLE);
        });

        // Connexion
        btnLogin.setOnClickListener(v -> {
            String email = emailLogin.getText().toString().trim();
            String password = passwordLogin.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Veuillez entrer un email valide", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Veuillez entrer un mot de passe", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Toast.makeText(Authentification.this, "Bienvenue " + user.getEmail(), Toast.LENGTH_SHORT).show();

                        // Rediriger vers AccueilFermier après connexion
                        Intent intent = new Intent(Authentification.this, AccueilFermier.class);
                        startActivity(intent);
                        finish(); // Ferme l'activité actuelle
                    }
                } else {
                    Toast.makeText(Authentification.this, "Échec de connexion : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Inscription
        btnRegister.setOnClickListener(v -> {
            String email = emailRegister.getText().toString().trim();
            String password = passwordRegister.getText().toString().trim();
            String nomF = nom.getText().toString().trim();
            String prenomF = prenom.getText().toString().trim();
            String contactF = contact.getText().toString().trim();
            String localisationF = localisation.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Veuillez entrer un email valide", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password) || password.length() < 6) {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(nomF) || TextUtils.isEmpty(prenomF) || TextUtils.isEmpty(contactF) || TextUtils.isEmpty(localisationF)) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        String userId = user.getEmail();  // Utilisation de l'UID Firebase

                        HashMap<String, Object> fermier = new HashMap<>();

                        fermier.put("nom", nomF);
                        fermier.put("prenom", prenomF);
                        fermier.put("contact", contactF);
                        fermier.put("localisation", localisationF);
                        fermier.put("email", email);

                        db.collection("fermiers").document(userId).set(fermier)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(Authentification.this, "Inscription réussie !", Toast.LENGTH_SHORT).show();

                                    // Rediriger automatiquement vers AccueilFermier après inscription
                                    Intent intent = new Intent(Authentification.this, AccueilFermier.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(Authentification.this, "Erreur d'inscription : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Toast.makeText(Authentification.this, "Erreur d'inscription : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
