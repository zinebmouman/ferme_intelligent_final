package com.example.ferme_intelligente;

import android.util.Log;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import java.util.function.Consumer;

public class MQTTManager {
    private Mqtt3AsyncClient mqttClient;
    private final String brokerHost = "192.168.137.47";
    private final String temperatureTopic = "topic/temperature";
    private final String plantStateTopic = "plante/etat";
    private final Consumer<String> messageCallback;

    public MQTTManager(Consumer<String> messageCallback) {
        this.messageCallback = messageCallback;
    }

    public void connect() {
        try {
            mqttClient = Mqtt3Client.builder()
                    .identifier("AndroidClient_" + System.currentTimeMillis())
                    .serverHost(brokerHost)
                    .serverPort(1883)
                    .buildAsync(); // Removed useMqttVersion3()

            mqttClient.connectWith()
                    .keepAlive(60)
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            Log.e("MQTT", "Connexion échouée", throwable);
                        } else {
                            Log.i("MQTT", "Connecté au broker");

                            mqttClient.subscribeWith()
                                    .topicFilter(temperatureTopic)
                                    .callback(this::handleMessage)
                                    .send();

                            mqttClient.subscribeWith()
                                    .topicFilter(plantStateTopic)
                                    .callback(this::handleMessage)
                                    .send();

                            mqttClient.subscribeWith()
                                    .topicFilter("topic/pompe")
                                    .callback(this::handleMessage)
                                    .send();
                        }
                    });
        } catch (Exception e) {
            Log.e("MQTT", "Erreur lors de la connexion", e);
        }
    }

    private void handleMessage(Mqtt3Publish publish) {
        String payload = new String(publish.getPayloadAsBytes());
        Log.d("MQTT", "Message reçu: " + payload);
        messageCallback.accept(payload);
    }

    public void disconnect() {
        if (mqttClient != null) {
            mqttClient.disconnect();
            Log.d("HiveMQ", "Déconnecté");
        }
    }
}
