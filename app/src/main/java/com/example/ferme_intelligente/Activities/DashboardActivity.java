package com.example.ferme_intelligente.Activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.ferme_intelligente.MQTTManager;
import com.example.ferme_intelligente.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.example.ferme_intelligente.views.ThermometerView;

public class DashboardActivity extends BaseActivity {

    private static final String TAG = "DashboardActivity";

    private LineChart tempChart, humidityChart, soilChart;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String farmerEmail;
    private MQTTManager mqttManager;

    private TextView tvRecordingTime, tvWaterVolume;
    private TextView tvLastTemp, tvLastHumidity, tvLastSoil, tvLastPumpStatus, tvLastWaterVolume, tvLastUpdateTime;

    private ThermometerView thermometerView;
    private PieChart pieHumidity, pieHumiditysol;

    private final List<Entry> tempEntries = new ArrayList<>();
    private final List<Entry> humidityEntries = new ArrayList<>();
    private final List<Entry> soilEntries = new ArrayList<>();

    private View dashboardView, dataView;
    private Button btnDashboard, btnData;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflater le contenu dans le fragment_container de BaseActivity
        FrameLayout container = findViewById(R.id.fragment_container);
        LayoutInflater.from(this).inflate(R.layout.activity_dashboard, container, true);

        // Set the selected item in bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Log.e(TAG, "Utilisateur non authentifié");
            finish();
            return;
        }

        farmerEmail = user.getEmail();

        // Initialisation des vues
        tempChart = findViewById(R.id.tempChart);
        humidityChart = findViewById(R.id.humidityChart);
        soilChart = findViewById(R.id.soilChart);

        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvWaterVolume = findViewById(R.id.tvWaterVolume);

        // Vues pour les dernières valeurs
        tvLastTemp = findViewById(R.id.tvLastTemp);
        tvLastHumidity = findViewById(R.id.tvLastHumidity);
        tvLastSoil = findViewById(R.id.tvLastSoil);
        tvLastWaterVolume = findViewById(R.id.tvLastWaterVolume);
        tvLastUpdateTime = findViewById(R.id.tvLastUpdateTime);
        thermometerView = findViewById(R.id.thermometerView);
        pieHumidity = findViewById(R.id.pieHumidity);
        pieHumiditysol = findViewById(R.id.pieHumiditysol);
        setupPieChart();

        // Initialisation des onglets
        dashboardView = findViewById(R.id.dashboardView);
        dataView = findViewById(R.id.dataView);
        btnDashboard = findViewById(R.id.btnDashboard);
        btnData = findViewById(R.id.btnData);

        setupCharts();

        // Exécution des tâches lourdes dans des threads séparés
        new FetchSensorDataTask().execute();
        new FetchPumpDataTask().execute();
        new FetchLastValuesTask().execute();

        mqttManager = new MQTTManager(this::onNewMqttData);
        mqttManager.connect();

        // Gestion des clics sur les onglets
        btnDashboard.setOnClickListener(v -> showDashboardView());
        btnData.setOnClickListener(v -> showDataView());
    }

    private class FetchSensorDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            fetchSensorData();
            return null;
        }
    }

    private class FetchPumpDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            fetchPumpData();
            return null;
        }
    }

    private class FetchLastValuesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            fetchLastValues();
            return null;
        }
    }

    private void showDashboardView() {
        runOnUiThread(() -> {
            dashboardView.setVisibility(View.VISIBLE);
            dataView.setVisibility(View.GONE);
            btnDashboard.setBackgroundResource(R.drawable.tab_selected);
            btnData.setBackgroundResource(R.drawable.tab_unselected);
        });
    }

    private void showDataView() {
        runOnUiThread(() -> {
            dashboardView.setVisibility(View.GONE);
            dataView.setVisibility(View.VISIBLE);
            btnDashboard.setBackgroundResource(R.drawable.tab_unselected);
            btnData.setBackgroundResource(R.drawable.tab_selected);
            updateLastValuesUI();
        });
    }

    private void setupCharts() {
        runOnUiThread(() -> {
            for (LineChart chart : new LineChart[]{tempChart, humidityChart, soilChart}) {
                chart.setNoDataText("Chargement des données...");
                chart.setNoDataTextColor(Color.GRAY);
                chart.getDescription().setEnabled(true);
                chart.setTouchEnabled(true);
                chart.setDragEnabled(true);
                chart.setScaleEnabled(true);
                chart.setDrawGridBackground(false);
                chart.setPinchZoom(true);
                chart.getLegend().setEnabled(true);

                XAxis xAxis = chart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setGranularity(1f);
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return timeFormat.format(new Date((long) value));
                    }
                });

                chart.getAxisLeft().setGranularity(1f);
                chart.getAxisRight().setEnabled(false);
            }

            tempChart.getDescription().setText("Évolution de la température (°C)");
            humidityChart.getDescription().setText("Évolution de l'humidité (%)");
            soilChart.getDescription().setText("Humidité du sol (%)");
        });
    }

    private void fetchSensorData() {
        db.collection("fermiers")
                .document(farmerEmail)
                .collection("sensorData")
                .orderBy("collection_time", Query.Direction.ASCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        addSensorData(doc);
                    }
                    runOnUiThread(() -> {
                        updateAllCharts();
                        updateLastValuesUI();
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erreur récupération Firestore", e));
    }

    private void fetchLastValues() {
        db.collection("fermiers")
                .document(farmerEmail)
                .collection("sensorData")
                .orderBy("collection_time", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        addSensorData(doc);
                        updateLastValuesUI();
                    }
                });
    }

    private void fetchPumpData() {
        db.collection("fermiers")
                .document(farmerEmail)
                .collection("pompe")
                .orderBy("collection_time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> pumpDocs = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if ("Pompe en cours".equals(doc.getString("status"))) {
                            pumpDocs.add(doc);
                        }
                    }

                    if (pumpDocs.size() < 2) {
                        String message = "Pompe : données insuffisantes";
                        runOnUiThread(() -> {
                            tvRecordingTime.setText(message);
                            tvWaterVolume.setText("Volume : -- L");
                            tvLastWaterVolume.setText("Volume d'eau : -- L");
                            tvLastUpdateTime.setText("Dernière mise à jour : --");
                        });
                        return;
                    }

                    long totalDurationMillis = 0;
                    long thresholdMillis = 2 * 60 * 1000;

                    for (int i = 1; i < pumpDocs.size(); i++) {
                        long currentTime = pumpDocs.get(i).getLong("collection_time");
                        long prevTime = pumpDocs.get(i - 1).getLong("collection_time");
                        long delta = currentTime - prevTime;

                        if (delta <= thresholdMillis) {
                            totalDurationMillis += delta;
                        }
                    }

                    float totalMinutes = totalDurationMillis / 60000f;
                    float volume = totalMinutes * 2f;

                    String timeText = String.format(Locale.getDefault(), "Pompe activée : %.1f minutes", totalMinutes);
                    String volumeText = String.format(Locale.getDefault(), "Volume d'eau : %.2f L", volume);

                    long end = pumpDocs.get(pumpDocs.size() - 1).getLong("collection_time");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

                    runOnUiThread(() -> {
                        tvRecordingTime.setText(timeText);
                        tvWaterVolume.setText(volumeText);
                        tvLastWaterVolume.setText("Volume d'eau : " + String.format(Locale.getDefault(), "%.2f L", volume));
                        tvLastUpdateTime.setText("Dernière mise à jour : " + dateFormat.format(new Date(end)));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur récupération pompe", e);
                    runOnUiThread(() -> {
                        tvRecordingTime.setText("Erreur chargement pompe");
                        tvWaterVolume.setText("--");
                        tvLastWaterVolume.setText("--");
                        tvLastUpdateTime.setText("--");
                    });
                });
    }

    private void addSensorData(DocumentSnapshot doc) {
        if (doc == null) return;

        try {
            String rawData = doc.getString("raw_data");
            Long collectionTime = doc.getLong("collection_time");
            if (rawData == null || collectionTime == null) return;

            Double tempValue = doc.getDouble("temp_value");
            float temperature = tempValue != null ? tempValue.floatValue() : 0f;

            Long humValueLong = doc.getLong("hum_value");
            float humidity = humValueLong != null ? humValueLong.floatValue() : 0f;

            Long rawValueLong = doc.getLong("raw_value");
            float soilHumidity = rawValueLong != null ? rawValueLong.floatValue() : 0f;

            float time = collectionTime.floatValue();

            tempEntries.add(new Entry(time, temperature));
            humidityEntries.add(new Entry(time, humidity));
            soilEntries.add(new Entry(time, soilHumidity));

            runOnUiThread(() -> {
                thermometerView.setTemperature(temperature);
                updatePieHumidity(humidity);
                tvLastSoil.setText(String.format(Locale.getDefault(), "Humidité sol : %.0f%%", soilHumidity));
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout des données capteurs", e);
        }
    }

    private void updatePieHumidity(float humidityValue) {
        runOnUiThread(() -> {
            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry(humidityValue, "Humidité"));
            entries.add(new PieEntry(100 - humidityValue, "Sec"));

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(Color.BLUE, Color.LTGRAY);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.BLACK);

            PieData data = new PieData(dataSet);
            pieHumidity.setData(data);
            pieHumidity.invalidate();
        });
    }

    private void updatePieHumiditySoil(float soilHumidity) {
        runOnUiThread(() -> {
            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry(soilHumidity, "Sol humide"));
            entries.add(new PieEntry(100 - soilHumidity, "Sec"));

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(Color.GREEN, Color.LTGRAY);
            PieData data = new PieData(dataSet);
            data.setDrawValues(false);

            pieHumiditysol.setData(data);
            pieHumiditysol.invalidate();
        });
    }

    private void updateAllCharts() {
        runOnUiThread(() -> {
            updateChart(tempChart, tempEntries, "Température (°C)", Color.RED);
            updateChart(humidityChart, humidityEntries, "Humidité (%)", Color.BLUE);
            updateSoilChart(soilChart, soilEntries, "Humidité du sol (%)", Color.GREEN);
        });
    }

    private void setupPieChart() {
        runOnUiThread(() -> {
            pieHumidity.setUsePercentValues(true);
            pieHumidity.getDescription().setEnabled(false);
            pieHumidity.setDrawHoleEnabled(true);
            pieHumidity.setHoleColor(Color.WHITE);
            pieHumidity.setTransparentCircleRadius(61f);
            pieHumiditysol.setTransparentCircleRadius(61f);
            if (pieHumiditysol != null) {
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(60f, "Sol humide"));
                entries.add(new PieEntry(40f, "Sol sec"));

                PieDataSet dataSet = new PieDataSet(entries, "Humidité du sol");
                dataSet.setColors(Color.GREEN, Color.GRAY);
                dataSet.setValueTextColor(Color.BLACK);
                dataSet.setValueTextSize(16f);

                PieData data = new PieData(dataSet);
                pieHumiditysol.setData(data);
                pieHumiditysol.invalidate();
            }
        });
    }

    private void updateChart(LineChart chart, List<Entry> entries, String label, int color) {
        if (entries.isEmpty()) return;

        entries.sort(Comparator.comparing(Entry::getX));

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(color);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void updateSoilChart(LineChart chart, List<Entry> entries, String label, int color) {
        if (entries.isEmpty()) return;

        entries.sort(Comparator.comparing(Entry::getX));

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(color);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setAxisMaximum(100f);
        chart.getDescription().setText(label);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void updateLastValuesUI() {
        runOnUiThread(() -> {
            if (!tempEntries.isEmpty()) {
                Entry lastTemp = tempEntries.get(tempEntries.size() - 1);
                tvLastTemp.setText(String.format(Locale.getDefault(), "Température : %.1f°C", lastTemp.getY()));
            }

            if (!humidityEntries.isEmpty()) {
                Entry lastHum = humidityEntries.get(humidityEntries.size() - 1);
                tvLastHumidity.setText(String.format(Locale.getDefault(), "Humidité : %.0f%%", lastHum.getY()));
            }

            if (!soilEntries.isEmpty()) {
                Entry lastSoil = soilEntries.get(soilEntries.size() - 1);
                tvLastSoil.setText(String.format(Locale.getDefault(), "Humidité sol : %.0f%%", lastSoil.getY()));
            }
        });
    }

    private void onNewMqttData(String rawData) {
        new ProcessMqttDataTask().execute(rawData);
    }

    private class ProcessMqttDataTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            String rawData = strings[0];
            try {
                long now = System.currentTimeMillis();

                float temperature = 0;
                float humidity = 0;
                float soilHumidity = 0;

                String[] parts = rawData.split(",");
                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();

                        switch (key) {
                            case "Temp":
                                temperature = Float.parseFloat(value.replace("C", ""));
                                break;
                            case "Hum":
                                humidity = Float.parseFloat(value.replace("%", ""));
                                break;
                            case "Raw":
                                soilHumidity = Float.parseFloat(value);
                                break;
                        }
                    }
                }

                float time = now;
                tempEntries.add(new Entry(time, temperature));
                humidityEntries.add(new Entry(time, humidity));
                soilEntries.add(new Entry(time, soilHumidity));

                float finalTemperature = temperature;
                float finalHumidity = humidity;
                float finalSoilHumidity = soilHumidity;
                runOnUiThread(() -> {
                    updateAllCharts();
                    updateLastValuesUI();
                    thermometerView.setTemperature(finalTemperature);
                    updatePieHumidity(finalHumidity);
                    updatePieHumiditySoil(finalSoilHumidity);
                });

            } catch (Exception e) {
                Log.e(TAG, "Erreur parsing MQTT data", e);
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }
}