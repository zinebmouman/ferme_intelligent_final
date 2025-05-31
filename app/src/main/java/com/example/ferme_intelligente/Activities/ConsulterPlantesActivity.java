package com.example.ferme_intelligente.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ferme_intelligente.Adapter.PlanteAdapter;
import com.example.ferme_intelligente.Model.Plante;
import com.example.ferme_intelligente.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ConsulterPlantesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private PlanteAdapter planteAdapter;
    private List<Plante> planteList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FloatingActionButton fabAddPlant;

    // Pour gérer les listeners Firestore
    private ListenerRegistration plantesListener;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater le contenu dans le fragment_container de BaseActivity
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_consulter_plantes, container, true);

        // Set the selected item in bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_plantes);

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vérification de l'utilisateur connecté
        if (!isUserAuthenticated()) {
            return; // L'activité sera fermée si l'utilisateur n'est pas connecté
        }

        // Initialisation des vues
        initializeViews();

        // Chargement des plantes
        loadPlantes();
    }

    private boolean isUserAuthenticated() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Log.e("Auth", "Utilisateur non connecté");
            Toast.makeText(this, "Erreur : Vous devez être connecté pour consulter les plantes", Toast.LENGTH_LONG).show();

            // Rediriger vers la page d'authentification
            Intent intent = new Intent(this, Authentification.class);
            startActivity(intent);
            finish();
            return false;
        }

        currentUserEmail = user.getEmail();
        Log.d("Auth", "Utilisateur connecté: " + currentUserEmail);
        return true;
    }

    private void initializeViews() {
        // Initialisation du RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            planteList = new ArrayList<>();
            planteAdapter = new PlanteAdapter(this, planteList);
            recyclerView.setAdapter(planteAdapter);
        } else {
            Log.e("ViewError", "RecyclerView introuvable dans le layout");
            Toast.makeText(this, "Erreur : Interface non disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialisation du FloatingActionButton
        fabAddPlant = findViewById(R.id.fabAddPlant);
        if (fabAddPlant != null) {
            fabAddPlant.setOnClickListener(view -> {
                Intent intent = new Intent(ConsulterPlantesActivity.this, AjoutePlante.class);
                startActivity(intent);
            });
        } else {
            Log.w("ViewWarning", "FloatingActionButton non trouvé dans le layout");
        }
    }

    private void loadPlantes() {
        if (currentUserEmail == null) {
            Log.e("LoadPlantes", "Email utilisateur null");
            return;
        }

        Log.d("LoadPlantes", "Chargement des plantes pour: " + currentUserEmail);

        CollectionReference plantesRef = db.collection("fermiers")
                .document(currentUserEmail)
                .collection("plantes");

        // Utilisation d'un SnapshotListener pour des mises à jour en temps réel
        plantesListener = plantesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Erreur lors de la récupération des plantes", error);
                Toast.makeText(this, "Erreur lors du chargement des plantes: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                handleSuccessfulLoad(value);
            } else {
                Log.w("Firestore", "Snapshot vide reçu");
                handleEmptyResult();
            }
        });
    }

    private void handleSuccessfulLoad(com.google.firebase.firestore.QuerySnapshot value) {
        planteList.clear();
        int loadedCount = 0;

        for (QueryDocumentSnapshot document : value) {
            try {
                Plante plante = document.toObject(Plante.class);
                if (plante != null) {
                    planteList.add(plante);
                    loadedCount++;
                    Log.d("FirestoreData", "Plante chargée: " + plante.getNom());
                } else {
                    Log.w("FirestoreData", "Plante null pour le document: " + document.getId());
                }
            } catch (Exception e) {
                Log.e("FirestoreError", "Erreur parsing document " + document.getId(), e);
            }
        }

        if (planteAdapter != null) {
            planteAdapter.notifyDataSetChanged();
        }

        Log.d("LoadPlantes", "Nombre de plantes chargées: " + loadedCount);

        if (loadedCount == 0) {
            handleEmptyResult();
        } else {
            Toast.makeText(this, loadedCount + " plante(s) trouvée(s)", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEmptyResult() {
        if (planteList != null) {
            planteList.clear();
        }
        if (planteAdapter != null) {
            planteAdapter.notifyDataSetChanged();
        }
        Toast.makeText(this, "Aucune plante trouvée. Ajoutez votre première plante !", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les listeners pour éviter les fuites mémoire
        if (plantesListener != null) {
            plantesListener.remove();
            plantesListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vérifier à nouveau l'authentification au retour sur l'écran
        if (!isUserAuthenticated()) {
            return;
        }

        // Si les listeners ont été supprimés, les recréer
        if (plantesListener == null) {
            loadPlantes();
        }
    }

    // Méthode publique pour actualiser manuellement la liste
    public void refreshPlantes() {
        if (isUserAuthenticated()) {
            loadPlantes();
        }
    }

    @Override
    public void onBackPressed() {
        // Retourner à l'accueil
        super.onBackPressed();
        Intent intent = new Intent(this, AccueilFermier.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}