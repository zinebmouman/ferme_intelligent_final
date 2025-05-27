package com.example.ferme_intelligente.Activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;

import com.example.ferme_intelligente.R;

public class MainActivity extends AppCompatActivity {
    private MqttClient mqttClient;
    private final String brokerUrl = "tcp://10.1.8.140:1883";  private final String clientId = MqttClient.generateClientId();
    private final String topic = "capteurs/sol";  // Sujet où tu publies les données

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Créer une instance de client MQTT
            mqttClient = new MqttClient(brokerUrl, clientId, null);

            // Configurer les options de connexion
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // Connecter au broker MQTT
            mqttClient.connect(options);

            // S'abonner au sujet MQTT
            mqttClient.subscribe(topic, 1);
// Configurer le callback pour recevoir les messages
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());

                    // Trouver les TextView
                    TextView tempTextView = findViewById(R.id.textViewTemperature);
                    TextView humidityTextView = findViewById(R.id.textViewHumidity);
                    TextView solTextView = findViewById(R.id.textViewSol);

                    // Mettre à jour les TextView avec les nouvelles valeurs
                    tempTextView.setText("Température: " + payload.split(",")[0].split(":")[1]);
                    humidityTextView.setText("Humidité: " + payload.split(",")[1].split(":")[1]);
                    solTextView.setText("Sol: " + payload.split(",")[2].split(":")[1]);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Connexion perdue : " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Message livré !");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}

