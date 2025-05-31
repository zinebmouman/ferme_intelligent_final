package com.example.ferme_intelligente.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ferme_intelligente.Adapter.AlertesAdapter;
import com.example.ferme_intelligente.Model.Alerte;
import com.example.ferme_intelligente.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ConsulterAlertesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private AlertesAdapter adapter;
    private final List<Alerte> alerteList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater le contenu dans le fragment_container de BaseActivity
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_consulter_alertes, container, true);

        // Cette activité peut être accédée depuis l'accueil ou depuis une navigation
        // On peut définir une sélection par défaut si souhaité
        // bottomNavigationView.setSelectedItemId(R.id.navigation_accueil);

        // Initialisation Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Vérification de l'utilisateur connecté
        if (!isUserAuthenticated()) {
            return; // L'activité sera fermée si l'utilisateur n'est pas connecté
        }

        // Initialisation des vues
        initializeViews();

        // Chargement des alertes
        loadAlertes();
    }

    private boolean isUserAuthenticated() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Log.e("Auth", "Utilisateur non connecté");
            Toast.makeText(this, "Erreur : Vous devez être connecté pour consulter les alertes", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.alertesRecyclerView);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new AlertesAdapter(alerteList);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e("ViewError", "RecyclerView introuvable dans le layout");
            Toast.makeText(this, "Erreur : Interface non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAlertes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Log.e("Auth", "Utilisateur non connecté lors du chargement");
            return;
        }

        String farmerEmail = user.getEmail();
        Log.d("LoadAlertes", "Chargement des alertes pour: " + farmerEmail);

        // Afficher un indicateur de chargement si nécessaire
        // showLoadingIndicator(true);

        db.collection("fermiers")
                .document(farmerEmail)
                .collection("alertes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    // showLoadingIndicator(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        handleSuccessfulLoad(task.getResult().getDocuments());
                    } else {
                        handleLoadError(task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    // showLoadingIndicator(false);
                    handleLoadError(e);
                });
    }

    private void handleSuccessfulLoad(List<DocumentSnapshot> documents) {
        alerteList.clear();
        int loadedCount = 0;

        for (DocumentSnapshot doc : documents) {
            try {
                Alerte alerte = Alerte.fromFirestore(doc);
                alerteList.add(alerte);
                loadedCount++;
                Log.d("FirestoreData", "Alerte chargée: " + alerte.getType());
            } catch (Exception e) {
                Log.e("FirestoreError", "Erreur parsing doc " + doc.getId(), e);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Log.d("LoadAlertes", "Nombre d'alertes chargées: " + loadedCount);

        if (loadedCount == 0) {
            Toast.makeText(this, "Aucune alerte trouvée", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLoadError(Exception exception) {
        Log.e("Firestore", "Erreur de chargement des alertes", exception);
        Toast.makeText(this, "Erreur lors du chargement des alertes", Toast.LENGTH_SHORT).show();

        // En cas d'erreur, on peut garder la liste vide ou afficher un message
        if (adapter != null) {
            alerteList.clear();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les alertes quand l'activité revient au premier plan
        if (isUserAuthenticated()) {
            loadAlertes();
        }
    }

    // Méthode pour actualiser les alertes (peut être appelée depuis l'interface)
    public void refreshAlertes() {
        if (isUserAuthenticated()) {
            loadAlertes();
        }
    }

    @Override
    public void onBackPressed() {
        // Optionnel : retourner à l'accueil au lieu de la page précédente
        super.onBackPressed();
    }
}