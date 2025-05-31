package com.example.ferme_intelligente.Activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ferme_intelligente.R;
import com.google.firebase.firestore.*;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RecommendationActivity extends BaseActivity {

    private TextView textEtat, textRecommandation;
    private FirebaseFirestore db;
    private String planteIdFiltrée;
    private static final String TAG = "RecommendationActivity";

    private JSONObject recommandationsJson;  // Chargée UNE fois

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater le contenu dans le fragment_container de BaseActivity
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_recommendation, container, true);

        textEtat = findViewById(R.id.textEtat);
        textRecommandation = findViewById(R.id.textRecommandation);
        db = FirebaseFirestore.getInstance();

        planteIdFiltrée = getIntent().getStringExtra("planteId");
        if (planteIdFiltrée != null) {
            Log.d(TAG, "PlanteId filtrée reçue : " + planteIdFiltrée);
        }

        // Charger JSON UNE fois
        try {
            InputStream is = getAssets().open("recommendations.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            recommandationsJson = new JSONObject(json);
            Log.d(TAG, "JSON recommandations chargé avec succès");
        } catch (IOException | JSONException e) {
            recommandationsJson = null;
            Log.e(TAG, "Erreur chargement JSON recommandations", e);
            Toast.makeText(this, "Erreur chargement recommandations", Toast.LENGTH_LONG).show();
        }

        chargerRecommandations();
    }

    private void chargerRecommandations() {
        db.collection("fermiers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> fermiers = queryDocumentSnapshots.getDocuments();
                    if (fermiers.isEmpty()) {
                        textEtat.setText("Aucun fermier trouvé.");
                        Log.w(TAG, "Aucun fermier trouvé.");
                        return;
                    }

                    for (DocumentSnapshot fermierDoc : fermiers) {
                        String userEmail = fermierDoc.getId();
                        Log.d(TAG, "Fermier trouvé : " + userEmail);

                        db.collection("fermiers")
                                .document(userEmail)
                                .collection("plantes")
                                .get()
                                .addOnSuccessListener(plantesSnapshot -> {
                                    for (DocumentSnapshot planteDoc : plantesSnapshot.getDocuments()) {
                                        String planteId = planteDoc.getId();

                                        if (planteIdFiltrée != null && !planteIdFiltrée.equals(planteId)) {
                                            continue;
                                        }

                                        Log.d(TAG, "Plante trouvée : " + planteId);

                                        db.collection("fermiers")
                                                .document(userEmail)
                                                .collection("plantes")
                                                .document(planteId)
                                                .collection("plant_states")
                                                .orderBy("collection_time", Query.Direction.DESCENDING)
                                                .limit(1)
                                                .get()
                                                .addOnSuccessListener(stateSnapshots -> {
                                                    for (DocumentSnapshot stateDoc : stateSnapshots) {
                                                        String predictedState = stateDoc.getString("predicted_state");
                                                        Log.d(TAG, "Dernier état de la plante " + planteId + ": " + predictedState);

                                                        if (predictedState != null) {
                                                            textEtat.append("État détecté : " + predictedState + "\n\n");

                                                            showRecommendation(predictedState);
                                                        } else {
                                                            Log.w(TAG, "predicted_state est null pour la plante : " + planteId);
                                                        }
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Erreur récupération état plante : " + planteId, e);
                                                    Toast.makeText(this, "Erreur état plante : " + planteId, Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Erreur récupération plantes fermier : " + userEmail, e);
                                    Toast.makeText(this, "Erreur chargement plantes du fermier " + userEmail, Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement des fermiers.", e);
                    Toast.makeText(this, "Erreur chargement des fermiers.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showRecommendation(String label) {
        if (recommandationsJson == null) {
            textRecommandation.append("Erreur interne : recommandations non chargées.\n\n-----------------------\n\n");
            return;
        }

        try {
            if (recommandationsJson.has(label)) {
                JSONObject reco = recommandationsJson.getJSONObject(label);
                String status = reco.getString("status");
                String description = reco.getString("description");
                String recommendation = reco.getString("recommendation");

                String text = "État : " + status + "\n\nDescription : " + description + "\n\nRecommandation : " + recommendation + "\n\n-----------------------\n\n";
                textRecommandation.append(text);

                Log.d(TAG, "Recommandation affichée pour : " + label);
            } else {
                textRecommandation.append("Aucune recommandation trouvée pour : " + label + "\n\n-----------------------\n\n");
                Log.w(TAG, "Label non trouvé dans JSON : " + label);
            }
        } catch (JSONException e) {
            textRecommandation.append("Erreur lecture recommandation pour : " + label + "\n\n");
            Log.e(TAG, "Erreur JSON pour label : " + label, e);
        }
    }
}