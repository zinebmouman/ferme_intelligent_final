package com.example.ferme_intelligente.Model;

public class Alerte {
    private String message;
    private long collection_time;

    public Alerte() {} // Obligatoire pour Firestore

    public Alerte(String message, long collection_time) {
        this.message = message;
        this.collection_time = collection_time;
    }

    public String getMessage() {
        return message;
    }

    public long getCollection_time() {
        return collection_time;
    }
}
