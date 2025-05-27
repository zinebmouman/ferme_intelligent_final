package com.example.ferme_intelligente.Models;

public class Fermier {
    private String id;
    private String nom;
    private String prenom;
    private String contact;
    private String localisation;
    private String email;

    // Constructeur par défaut (obligatoire pour Firebase Firestore)
    public Fermier() {
    }

    // Constructeur avec paramètres
    public Fermier(String id, String nom, String prenom, String contact, String localisation, String email) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.contact = contact;
        this.localisation = localisation;
        this.email = email;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Fermier{" +
                "id='" + id + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", contact='" + contact + '\'' +
                ", localisation='" + localisation + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
