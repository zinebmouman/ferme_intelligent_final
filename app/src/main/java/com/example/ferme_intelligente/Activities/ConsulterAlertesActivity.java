package com.example.ferme_intelligente.Activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ferme_intelligente.Model.Alerte;
import com.example.ferme_intelligente.Adapter.AlertesAdapter;
import com.example.ferme_intelligente.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ConsulterAlertesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AlertesAdapter adapter;
    private List<Alerte> alerteList;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consulter_alertes);

        recyclerView = findViewById(R.id.alertesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        alerteList = new ArrayList<>();
        adapter = new AlertesAdapter(alerteList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String farmerEmail = user.getEmail();
            if (farmerEmail != null && !farmerEmail.isEmpty()) {
                Log.d("Firestore", "Chargement des alertes pour : " + farmerEmail);

                db.collection("fermiers")
                        .document(farmerEmail)
                        .collection("alertes")
                        .orderBy("collection_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            alerteList.clear();
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Alerte alerte = doc.toObject(Alerte.class);
                                alerteList.add(alerte);
                                Log.d("AlerteData", "Alerte récupérée : " + alerte.toString());
                            }
                            Log.d("AlerteData", "Total alertes récupérées : " + alerteList.size());
                            adapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> Log.e("Firestore", "Erreur récupération alertes", e));
            } else {
                Log.e("Firestore", "Email utilisateur vide ou nul");
            }
        } else {
            Log.e("Auth", "Aucun utilisateur connecté !");
        }
    }
}
