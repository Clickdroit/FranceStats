package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import java.util.*;
import java.util.stream.Collectors;

public class StationService {

    public List<Station> rechercherStationsProches(List<Station> stations, String carburant,
                                                   double rayon, int limite) {
        return stations.stream()
                .filter(s -> s.hasPrix(carburant))
                .filter(s -> s.getDistanceKm() <= rayon)
                .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                .limit(limite)
                .collect(Collectors.toList());
    }

    public List<Station> rechercherParDepartement(List<Station> stations, String departement,
                                                  String carburant) {
        return stations.stream()
                .filter(s -> s.getDepartement().equals(departement))
                .filter(s -> s.hasPrix(carburant))
                .collect(Collectors.toList());
    }

    public Set<String> getCarburantsDisponibles(List<Station> stations) {
        return stations.stream()
                .flatMap(s -> s.getPrix().keySet().stream())
                .collect(Collectors.toSet());
    }

    public StatistiquesNationales calculerStatistiques(List<Station> stations, String carburant) {
        return new StatistiquesNationales(carburant, stations);
    }

    public String genererGoogleMapsUrl(double fromLat, double fromLon, double toLat, double toLon) {
        return String.format("https://www.google.com/maps/dir/%.6f,%.6f/%.6f,%.6f",
                fromLat, fromLon, toLat, toLon);
    }

    public double parseRayonAvecDefaut(String rayonStr, double defaut) {
        try {
            double rayon = Double.parseDouble(rayonStr);
            if (rayon <= 0) {
                System.out.println("⚠️ Le rayon doit être positif, utilisation de la valeur par défaut");
                return defaut;
            }
            return rayon;
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Valeur invalide, utilisation du rayon par défaut (" + defaut + " km)");
            return defaut;
        }
    }

    // Classe interne pour les statistiques
    public static class StatistiquesNationales {
        private String carburant;
        private double moyenne;
        private double minimum;
        private double maximum;
        private long nombreStations;
        private String villeMin;
        private String villeMax;

        public StatistiquesNationales(String carburant, List<Station> stations) {
            this.carburant = carburant;

            List<Double> prixList = stations.stream()
                    .map(s -> s.getPrix(carburant))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!prixList.isEmpty()) {
                this.nombreStations = prixList.size();
                this.moyenne = prixList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                this.minimum = prixList.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                this.maximum = prixList.stream().mapToDouble(Double::doubleValue).max().orElse(0);

                this.villeMin = stations.stream()
                        .filter(s -> Objects.equals(s.getPrix(carburant), minimum))
                        .findFirst()
                        .map(s -> s.getVille() + " (" + s.getCodePostal() + ")")
                        .orElse("Inconnue");

                this.villeMax = stations.stream()
                        .filter(s -> Objects.equals(s.getPrix(carburant), maximum))
                        .findFirst()
                        .map(s -> s.getVille() + " (" + s.getCodePostal() + ")")
                        .orElse("Inconnue");
            }
        }

        // Getters
        public String getCarburant() { return carburant; }
        public double getMoyenne() { return moyenne; }
        public double getMinimum() { return minimum; }
        public double getMaximum() { return maximum; }
        public long getNombreStations() { return nombreStations; }
        public String getVilleMin() { return villeMin; }
        public String getVilleMax() { return villeMax; }

        @Override
        public String toString() {
            return String.format(
                    "📊 STATISTIQUES %s 📊\n" +
                            "  Moyenne nationale : %.3f €/L\n" +
                            "  Prix minimum : %.3f €/L (à %s)\n" +
                            "  Prix maximum : %.3f €/L (à %s)\n" +
                            "  Nombre de stations : %d\n" +
                            "  Écart min-max : %.3f €/L",
                    carburant, moyenne, minimum, villeMin, maximum, villeMax, nombreStations, maximum - minimum
            );
        }
    }
}