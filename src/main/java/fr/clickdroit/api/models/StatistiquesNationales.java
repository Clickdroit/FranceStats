package fr.clickdroit.api.models;

import fr.clickdroit.api.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Statistics data class for fuel price analysis.
 * Contains national or regional statistics for a specific fuel type.
 */
public class StatistiquesNationales {

    private static final Logger logger = LoggerFactory.getLogger(StatistiquesNationales.class);

    private final String carburant;
    private final double moyenne;
    private final double minimum;
    private final double maximum;
    private final long nombreStations;
    private final String villeMin;
    private final String villeMax;

    /**
     * Creates statistics from a list of stations for a specific fuel type.
     *
     * @param carburant Fuel type
     * @param stations List of stations to analyze
     */
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
            this.nombreStations = 0;
            this.moyenne = 0;
            this.minimum = 0;
            this.maximum = 0;
            this.villeMin = "Inconnue";
            this.villeMax = "Inconnue";
            logger.warn("Aucun prix valide trouvé pour {}", carburant);
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
