package com.example.ferme_intelligente.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Alerte {
    private String id;
    private String type;
    private String message;
    private long timestamp;
    private Map<String, Object> data = new HashMap<>();

    // Constructeur vide pour Firestore
    public Alerte() {}

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    // Méthode pour créer une Alerte à partir d'un DocumentSnapshot
    public static Alerte fromFirestore(DocumentSnapshot doc) {
        Alerte alerte = new Alerte();
        alerte.setId(doc.getId());
        alerte.setType(doc.getString("type"));
        alerte.setMessage(doc.getString("message"));

        // Gestion du timestamp ou collection_time
        Object tsObj = null;
        if (doc.contains("timestamp")) {
            tsObj = doc.get("timestamp");
        } else if (doc.contains("collection_time")) {
            tsObj = doc.get("collection_time");
        }

        if (tsObj instanceof Long) {
            alerte.setTimestamp((Long) tsObj);
        } else if (tsObj instanceof Timestamp) {
            // Firestore Timestamp => convertir en millisecondes
            alerte.setTimestamp(((Timestamp) tsObj).toDate().getTime());
        } else if (tsObj instanceof String) {
            // Si c'est une chaîne, on peut essayer de parser la date si nécessaire
            // Ici on met à 0 pour éviter l'erreur
            alerte.setTimestamp(0L);
        } else {
            // Valeur par défaut si absent ou type inconnu
            alerte.setTimestamp(System.currentTimeMillis());
        }

        // Stockage de tous les champs supplémentaires (hors type, message, timestamp, collection_time)
        Map<String, Object> data = new HashMap<>();
        if (doc.getData() != null) {
            for (String key : doc.getData().keySet()) {
                if (!key.equals("type") && !key.equals("message") && !key.equals("timestamp") && !key.equals("collection_time")) {
                    data.put(key, doc.get(key));
                }
            }
        }
        alerte.setData(data);

        return alerte;
    }

    // Méthode pour formater le message selon le type
    public String getFormattedMessage() {
        switch (type) {
            case "temperature":
                return String.format(Locale.getDefault(),
                        "Alerte Température: %s\nValeur: %.1f%s | Seuil: %s",
                        message,
                        data.containsKey("value") ? ((data.get("value") instanceof Number) ? ((Number) data.get("value")).doubleValue() : 0.0) : 0.0,
                        data.getOrDefault("unit", ""),
                        data.getOrDefault("threshold", "")
                );

            case "maladie":
                return String.format(Locale.getDefault(),
                        "Alerte Maladie: %s\nType: %s\nPlante ID: %s",
                        message,
                        data.getOrDefault("maladie", "Inconnue"),
                        data.getOrDefault("plante_id", "Inconnu")
                );

            case "pompe":
                return String.format("Alerte Pompe: %s", message);

            default:
                return message;
        }
    }
}
