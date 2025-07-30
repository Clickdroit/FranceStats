package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class StationService {

    private static final Logger logger = LoggerFactory.getLogger(StationService.class);

    public List<Station> rechercherStationsProches(List<Station> stations, String carburant,
                                                   double rayon, int limite) {
        logger.debug("Recherche stations proches: carburant={}, rayon={}km, limite={}",
                carburant, rayon, limite);

        // Validation des paramètres
        if (stations == null || stations.isEmpty()) {
            logger.warn("Liste de stations vide ou null");
            return new ArrayList<>();
        }

        if (!ValidationUtils.isValidCarburant(carburant)) {
            logger.error("Carburant invalide: {}", carburant);
            throw new IllegalArgumentException("Carburant invalide: " + carburant);
        }

        if (!ValidationUtils.isValidRayon(rayon)) {
            logger.error("Rayon invalide: {}", rayon);
            throw new IllegalArgumentException("Rayon invalide: " + rayon);
        }

        if (limite <= 0 || limite > 1000) {
            logger.warn("Limite ajustée de {} à {}", limite, Math.max(1, Math.min(limite, 1000)));
            limite = Math.max(1, Math.min(limite, 1000));
        }

        List<Station> result = stations.stream()
                .filter(s -> s.hasPrix(carburant))
                .filter(s -> s.getDistanceKm() <= rayon)
                .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                .limit(limite)
                .collect(Collectors.toList());

        logger.info("Trouvé {} stations pour {} dans un rayon de {}km",
                result.size(), carburant, rayon);

        return result;
    }

    public List<Station> rechercherParDepartement(List<Station> stations, String departement,
                                                  String carburant) {
        logger.debug("Recherche par département: dept={}, carburant={}", departement, carburant);

        // Validation des paramètres
        if (stations == null || stations.isEmpty()) {
            logger.warn("Liste de stations vide ou null");
            return new ArrayList<>();
        }

        if (!ValidationUtils.isValidDepartement(departement)) {
            logger.error("Département invalide: {}", departement);
            throw new IllegalArgumentException("Département invalide: " + departement);
        }

        if (!ValidationUtils.isValidCarburant(carburant)) {
            logger.error("Carburant invalide: {}", carburant);
            throw new IllegalArgumentException("Carburant invalide: " + carburant);
        }

        List<Station> result = stations.stream()
                .filter(s -> departement.equals(s.getDepartement()))
                .filter(s -> s.hasPrix(carburant))
                .collect(Collectors.toList());

        logger.info("Trouvé {} stations pour {} dans le département {}",
                result.size(), carburant, departement);

        return result;
    }

    public Set<String> getCarburantsDisponibles(List<Station> stations) {
        logger.debug("Récupération des carburants disponibles");

        if (stations == null || stations.isEmpty()) {
            logger.warn("Liste de stations vide, aucun carburant disponible");
            return new HashSet<>();
        }

        Set<String> carburants = stations.stream()
                .flatMap(s -> s.getPrix().keySet().stream())
                .filter(ValidationUtils::isValidCarburant)
                .collect(Collectors.toSet());

        logger.info("Carburants disponibles: {}", carburants);
        return carburants;
    }

    public StatistiquesNationales calculerStatistiques(List<Station> stations, String carburant) {
        logger.debug("Calcul des statistiques pour {}", carburant);

        if (stations == null || stations.isEmpty()) {
            logger.warn("Impossible de calculer les statistiques: liste vide");
            throw new IllegalArgumentException("Liste de stations vide");
        }

        if (!ValidationUtils.isValidCarburant(carburant)) {
            logger.error("Carburant invalide pour les statistiques: {}", carburant);
            throw new IllegalArgumentException("Carburant invalide: " + carburant);
        }

        StatistiquesNationales stats = new StatistiquesNationales(carburant, stations);

        logger.info("Statistiques calculées pour {}: {} stations, moyenne {:.3f}€/L",
                carburant, stats.getNombreStations(), stats.getMoyenne());

        return stats;
    }

    public String genererGoogleMapsUrl(double fromLat, double fromLon, double toLat, double toLon) {
        logger.debug("Génération URL Google Maps: from({:.6f},{:.6f}) to({:.6f},{:.6f})",
                fromLat, fromLon, toLat, toLon);

        // Validation des coordonnées
        if (!ValidationUtils.isValidCoordinates(fromLat, fromLon) ||
                !ValidationUtils.isValidCoordinates(toLat, toLon)) {
            logger.error("Coordonnées invalides pour Google Maps");
            throw new IllegalArgumentException("Coordonnées invalides");
        }

        String url = String.format("https://www.google.com/maps/dir/%.6f,%.6f/%.6f,%.6f",
                fromLat, fromLon, toLat, toLon);

        logger.debug("URL générée: {}", url);
        return url;
    }

    public double parseRayonAvecDefaut(String rayonStr, double defaut) {
        logger.debug("Parse rayon: '{}', défaut: {}", rayonStr, defaut);

        if (rayonStr == null || rayonStr.trim().isEmpty()) {
            logger.debug("Rayon vide, utilisation de la valeur par défaut: {}", defaut);
            return defaut;
        }

        try {
            double rayon = Double.parseDouble(rayonStr.trim());

            if (!ValidationUtils.isValidRayon(rayon)) {
                logger.warn("Rayon invalide '{}', utilisation de la valeur par défaut: {}",
                        rayonStr, defaut);
                return defaut;
            }

            logger.debug("Rayon parsé avec succès: {}", rayon);
            return rayon;

        } catch (NumberFormatException e) {
            logger.warn("Impossible de parser le rayon '{}', utilisation de la valeur par défaut: {}",
                    rayonStr, defaut);
            return defaut;
        }
    }

    // Classe interne pour les statistiques (inchangée mais avec plus de validation)
    public static class StatistiquesNationales {
        private static final Logger logger = LoggerFactory.getLogger(StatistiquesNationales.class);

        private String carburant;
        private double moyenne;
        private double minimum;
        private double maximum;
        private long nombreStations;
        private String villeMin;
        private String villeMax;

        public StatistiquesNationales(String carburant, List<Station> stations) {
            logger.debug("Création des statistiques pour {} avec {} stations",
                    carburant, stations.size());

            this.carburant = carburant;

            List<Double> prixList = stations.stream()
                    .map(s -> s.getPrix(carburant))
                    .filter(Objects::nonNull)
                    .filter(ValidationUtils::isValidPrix)
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

                logger.debug("Statistiques créées: {} stations, min={}, max={}, moy={}",
                        nombreStations, minimum, maximum, moyenne);
            } else {
                logger.warn("Aucun prix valide trouvé pour {}", carburant);
            }
        }

        // Getters (inchangés)
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