package fr.clickdroit.api.models;

import java.util.HashMap;
import java.util.Map;

public class Station {
    private String ville;
    private String codePostal;
    private String adresse;
    private String departement;
    private Map<String, Double> prix;
    private double latitude;
    private double longitude;
    private double distanceKm;

    public Station(String ville, String codePostal, double latitude, double longitude, String adresse) {
        this.ville = ville != null ? ville : "Ville inconnue";
        this.codePostal = codePostal != null ? codePostal : "00000";
        this.latitude = latitude;
        this.longitude = longitude;
        this.adresse = adresse != null && !adresse.trim().isEmpty() ? adresse : "Adresse non disponible";
        this.departement = this.codePostal.length() >= 2 ? this.codePostal.substring(0, 2) : "??";
        this.prix = new HashMap<>();
        this.distanceKm = 0.0;
    }

    // Méthodes pour gérer les prix
    public void ajouterPrix(String carburant, double prixValue) {
        prix.put(carburant, prixValue);
    }

    public Double getPrix(String carburant) {
        return prix.get(carburant);
    }

    public boolean hasPrix(String carburant) {
        return prix.containsKey(carburant) && prix.get(carburant) != null;
    }

    // Getters et Setters
    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    public Map<String, Double> getPrix() {
        return new HashMap<>(prix); // Retourne une copie pour éviter les modifications externes
    }

    public void setPrix(Map<String, Double> prix) {
        this.prix = new HashMap<>(prix);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Station à %s (%s)", ville, codePostal));
        if (distanceKm > 0) {
            sb.append(String.format(" - Distance : %.2f km", distanceKm));
        }
        sb.append("\n   📍 ").append(adresse);
        sb.append("\n   💰 Carburants : ");

        for (Map.Entry<String, Double> entry : prix.entrySet()) {
            sb.append(String.format("%s: %.3f€/L  ", entry.getKey(), entry.getValue()));
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Station station = (Station) o;

        if (Double.compare(station.latitude, latitude) != 0) return false;
        if (Double.compare(station.longitude, longitude) != 0) return false;
        if (!ville.equals(station.ville)) return false;
        return codePostal.equals(station.codePostal);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = ville.hashCode();
        result = 31 * result + codePostal.hashCode();
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}