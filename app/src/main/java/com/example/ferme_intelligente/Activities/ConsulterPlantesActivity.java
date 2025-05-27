package com.example.ferme_intelligente.Activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ferme_intelligente.Adapter.PlanteAdapter;
import com.example.ferme_intelligente.Model.Plante;
import com.example.ferme_intelligente.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class ConsulterPlantesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PlanteAdapter planteAdapter;
    private List<Plante> planteList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FloatingActionButton fabAddPlant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consulter_plantes);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        planteList = new ArrayList<>();
        planteAdapter = new PlanteAdapter(this, planteList);
        recyclerView.setAdapter(planteAdapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fabAddPlant = findViewById(R.id.fabAddPlant);
        fabAddPlant.setOnClickListener(view -> {
            Intent intent = new Intent(ConsulterPlantesActivity.this, AjoutePlante.class);
            startActivity(intent);
        });

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String emailFermier = user.getEmail(); // L'email est l'ID du fermier
            if (emailFermier != null) {
                loadPlantes(emailFermier);
            } else {
                Toast.makeText(this, "Erreur : email utilisateur non trouvé", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Erreur : utilisateur non connecté", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPlantes(String emailFermier) {
        CollectionReference plantesRef = db.collection("fermiers").document(emailFermier).collection("plantes");

        plantesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Erreur lors de la récupération des plantes", error);
                return;
            }

            if (value != null) {
                planteList.clear();
                for (QueryDocumentSnapshot document : value) {
                    Plante plante = document.toObject(Plante.class);
                    planteList.add(plante);
                }
                planteAdapter.notifyDataSetChanged();
            }
        });
    }
}
