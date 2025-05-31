package com.example.ferme_intelligente.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.ferme_intelligente.MQTTManager;
import com.example.ferme_intelligente.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Mydata extends BaseActivity {
    private TextView textViewData;
    private MQTTManager mqttManager;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String farmerEmail;
    private SimpleDateFormat timeFormat;
    private ListenerRegistration dataListener;

    // Executor pour le traitement en arrière-plan
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater le contenu dans le fragment_container de BaseActivity
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_mydata, container, true);

        textViewData = findViewById(R.id.textViewData);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Log.e("Auth", "Utilisateur non authentifié");
            finish();
            return;
        }

        farmerEmail = user.getEmail();
        setupFirestoreListener();

        mqttManager = new MQTTManager(this::processIncomingMessage);
        mqttManager.connect();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        calculatePumpOperatingTime();

    }

    private void setupFirestoreListener() {
        dataListener = db.collection("fermiers")
                .document(farmerEmail)
                .collection("sensorData")
                .orderBy("collection_time", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("Firestore", "Erreur d'écoute", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        StringBuilder allData = new StringBuilder();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String rawData = doc.getString("raw_data");
                            Long timeStamp = doc.getLong("collection_time");
                            if (rawData != null && timeStamp != null) {
                                String formattedTime = timeFormat.format(new Date(timeStamp));
                                allData.append(String.format(Locale.getDefault(),
                                        "[%s] %s\n", formattedTime, rawData));
                            }
                        }
                        // Mise à jour UI dans thread UI
                        runOnUiThread(() -> textViewData.setText(allData.toString()));
                    } else {
                        runOnUiThread(() -> textViewData.setText("Aucune donnée disponible"));
                    }
                });
    }

    private void processIncomingMessage(String message) {
        // Déléguer le traitement au thread background
        executor.execute(() -> {
            long collectionTime = System.currentTimeMillis();

            try {
                JSONObject json = new JSONObject(message);
                if (json.has("R") && json.has("G") && json.has("B")) {
                    processPlantData(message, collectionTime);
                    return;
                }
            } catch (JSONException ignored) {
                // Message n'est pas un JSON
            }

            if (message.startsWith("Temp:")) {
                processSensorData(message, collectionTime);
            } else if (message.toLowerCase().contains("pompe")) {
                processPumpStatus(message, collectionTime);
            } else {
                Log.w("MQTT", "Message inconnu : " + message);
            }
        });
    }

    private void processPumpStatus(String message, long collectionTime) {
        Map<String, Object> pumpData = new HashMap<>();
        pumpData.put("status", message);
        pumpData.put("collection_time", collectionTime);
        pumpData.put("server_time", FieldValue.serverTimestamp());

        db.collection("fermiers")
                .document(farmerEmail)
                .collection("pompe")
                .document("activation_" + collectionTime)
                .set(pumpData)
                .addOnSuccessListener(unused -> Log.d("FirestoreSuccess", "Pompe enregistrée"))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Erreur enregistrement pompe", e));
    }

    private void processPlantData(String rawMessage, long collectionTime) {
        String formattedTime = timeFormat.format(new Date(collectionTime));

        runOnUiThread(() -> {
            String currentText = textViewData.getText().toString();
            String newEntry = String.format(Locale.getDefault(),
                    "[%s] Plante: %s", formattedTime, rawMessage);
            textViewData.setText(newEntry + "\n" + currentText);
        });

        Map<String, Object> plantData = new HashMap<>();
        plantData.put("raw_data", rawMessage);
        plantData.put("collection_time", collectionTime);
        plantData.put("server_time", FieldValue.serverTimestamp());
        plantData.put("data_type", "plant_state");

        db.collection("fermiers")
                .document(farmerEmail)
                .collection("plantes")
                .document("lyuLCzNHInoRDLGQFSK0")
                .collection("plant_states")
                .document("state_" + collectionTime)
                .set(plantData)
                .addOnSuccessListener(unused -> Log.d("FirestoreSuccess", "Données plante enregistrées"))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Échec enregistrement plante", e));
    }

    private void processSensorData(String rawMessage, long collectionTime) {
        String formattedTime = timeFormat.format(new Date(collectionTime));

        runOnUiThread(() -> {
            String currentText = textViewData.getText().toString();
            String newEntry = String.format(Locale.getDefault(),
                    "[%s] Capteur: %s", formattedTime, rawMessage);
            textViewData.setText(newEntry + "\n" + currentText);
        });

        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("raw_data", rawMessage);
        sensorData.put("collection_time", collectionTime);
        sensorData.put("server_time", FieldValue.serverTimestamp());

        boolean shouldCreateTemperatureAlert = false;
        boolean shouldCreateHumidityAlert = false;
        double temperature = 0.0;
        double humidity = 100.0;

        try {
            Map<String, SensorValue> parsedMap = parseSensorData(rawMessage);
            for (Map.Entry<String, SensorValue> entry : parsedMap.entrySet()) {
                sensorData.put(entry.getKey() + "_value", entry.getValue().isText ? entry.getValue().textValue : entry.getValue().value);
                sensorData.put(entry.getKey() + "_unit", entry.getValue().isText ? "" : entry.getValue().unit);

                if ("temp".equals(entry.getKey()) && !entry.getValue().isText) {
                    temperature = entry.getValue().value;
                    if (temperature > 30.0) shouldCreateTemperatureAlert = true;
                } else if ("hum".equals(entry.getKey()) && !entry.getValue().isText) {
                    humidity = entry.getValue().value;
                    if (humidity < 50.0) shouldCreateHumidityAlert = true;
                }
            }
        } catch (Exception e) {
            Log.w("Parse", "Impossible de parser: " + rawMessage, e);
            sensorData.put("sensor_type", "unknown");
        }

        db.collection("fermiers")
                .document(farmerEmail)
                .collection("sensorData")
                .document("read_" + collectionTime)
                .set(sensorData)
                .addOnSuccessListener(unused -> Log.d("FirestoreSuccess", "Données capteur enregistrées"))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Erreur enregistrement capteur", e));

        if (shouldCreateTemperatureAlert) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "temperature");
            alert.put("value", temperature);
            alert.put("unit", "°C");
            alert.put("threshold", ">30");
            alert.put("message", "Alerte : Température élevée");
            alert.put("collection_time", collectionTime);
            alert.put("server_time", FieldValue.serverTimestamp());

            db.collection("fermiers")
                    .document(farmerEmail)
                    .collection("alertes")
                    .document("alert_temp_" + collectionTime)
                    .set(alert)
                    .addOnSuccessListener(unused -> Log.d("FirestoreSuccess", "Alerte température créée"))
                    .addOnFailureListener(e -> Log.e("FirestoreError", "Erreur création alerte température", e));
        }

        if (shouldCreateHumidityAlert) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "humidity");
            alert.put("value", humidity);
            alert.put("unit", "%");
            alert.put("threshold", "<50");
            alert.put("message", "Alerte : Humidité faible");
            alert.put("collection_time", collectionTime);
            alert.put("server_time", FieldValue.serverTimestamp());

            db.collection("fermiers")
                    .document(farmerEmail)
                    .collection("alertes")
                    .document("alert_hum_" + collectionTime)
                    .set(alert)
                    .addOnSuccessListener(unused -> Log.d("FirestoreSuccess", "Alerte humidité créée"))
                    .addOnFailureListener(e -> Log.e("FirestoreError", "Erreur création alerte humidité", e));
        }
    }

    // Parse les données brutes de capteur dans une Map
    private Map<String, SensorValue> parseSensorData(String rawData) {
        Map<String, SensorValue> result = new HashMap<>();
        String[] parts = rawData.split(" ");
        for (String part : parts) {
            if (part.contains(":")) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].toLowerCase();
                    String valueUnit = keyValue[1];

                    if (key.equals("temp") || key.equals("hum")) {
                        // ex: "22.6°C" ou "60%"
                        double val = 0.0;
                        String unit = "";
                        String strVal = valueUnit.replaceAll("[^0-9.,-]", "");
                        String unitStr = valueUnit.replaceAll("[0-9.,-]", "");
                        try {
                            val = Double.parseDouble(strVal.replace(",", "."));
                        } catch (NumberFormatException ignored) {
                        }
                        unit = unitStr;
                        result.put(key, new SensorValue(val, unit));
                    } else {
                        // Texte simple
                        result.put(key, new SensorValue(valueUnit, true));
                    }
                }
            }
        }
        return result;
    }

    // Classe auxiliaire pour valeur de capteur
    private static class SensorValue {
        final boolean isText;
        final double value;
        final String unit;
        final String textValue;

        SensorValue(double value, String unit) {
            this.value = value;
            this.unit = unit;
            this.isText = false;
            this.textValue = null;
        }

        SensorValue(String textValue, boolean isText) {
            this.textValue = textValue;
            this.isText = isText;
            this.value = 0.0;
            this.unit = null;
        }
    }

    private void calculatePumpOperatingTime() {
        db.collection("fermiers")
                .document(farmerEmail)
                .collection("pompe")
                .orderBy("collection_time", Query.Direction.ASCENDING) // du plus ancien au plus récent
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalPumpOnTime = 0L;
                    Long startTime = null;
                    boolean pumpIsOn = false;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String status = doc.getString("status");
                        Long collectionTime = doc.getLong("collection_time");

                        if (status == null || collectionTime == null) continue;

                        // Exemple: si le status contient "Pompe en cours", la pompe est ON
                        if (status.toLowerCase().contains("pompe en cours")) {
                            if (!pumpIsOn) {
                                // pompe vient de s'allumer
                                pumpIsOn = true;
                                startTime = collectionTime;
                            }
                        } else {
                            // la pompe est considérée éteinte (status différent)
                            if (pumpIsOn && startTime != null) {
                                // pompe s'éteint -> calcul intervalle
                                totalPumpOnTime += (collectionTime - startTime);
                                pumpIsOn = false;
                                startTime = null;
                            }
                        }
                    }

                    // Si la pompe est toujours ON à la fin (pas d'état OFF enregistré)
                    if (pumpIsOn && startTime != null) {
                        // calculer jusqu'à maintenant
                        totalPumpOnTime += (System.currentTimeMillis() - startTime);
                    }

                    // totalPumpOnTime est en millisecondes
                    long seconds = totalPumpOnTime / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;

                    long finalSeconds = seconds;
                    runOnUiThread(() -> {
                        String result = String.format(Locale.getDefault(),
                                "Temps total de fonctionnement pompe : %d min %d sec", minutes, finalSeconds);
                        textViewData.setText(result);
                    });

                    Log.d("PumpTime", "Total temps pompe ON en ms : " + totalPumpOnTime);
                })
                .addOnFailureListener(e -> Log.e("PumpTime", "Erreur récupération données pompe", e));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataListener != null) dataListener.remove();
        executor.shutdownNow();
    }
}