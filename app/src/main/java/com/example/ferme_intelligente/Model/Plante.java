package com.example.ferme_intelligente.Model;

public class Plante {
    private String id;
    private String nom;
    private String type;
    private String description;
    private String periodePlantation;
    private String besoinsEau;
    private String besoinsSoleil;
    private String image;

    // Constructeur par défaut requis pour Firebase
    public Plante() {
    }

    // Constructeur avec paramètres
    public Plante(String id, String nom, String type, String description, String periodePlantation, String besoinsEau, String besoinsSoleil, String imageUrl) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.description = description;
        this.periodePlantation = periodePlantation;
        this.besoinsEau = besoinsEau;
        this.besoinsSoleil = besoinsSoleil;
        this.image = image;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPeriodePlantation() {
        return periodePlantation;
    }

    public void setPeriodePlantation(String periodePlantation) {
        this.periodePlantation = periodePlantation;
    }

    public String getBesoinsEau() {
        return besoinsEau;
    }

    public void setBesoinsEau(String besoinsEau) {
        this.besoinsEau = besoinsEau;
    }

    public String getBesoinsSoleil() {
        return besoinsSoleil;
    }

    public void setBesoinsSoleil(String besoinsSoleil) {
        this.besoinsSoleil = besoinsSoleil;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
