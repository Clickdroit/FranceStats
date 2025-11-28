package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.repository.PriceHistoryRepository;
import fr.clickdroit.api.repository.JsonPriceHistoryRepository;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class HistoriquePrixService implements IHistoriquePrixService {

    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * Constructor with dependency injection.
     *
     * @param priceHistoryRepository Repository for price history data
     */
    public HistoriquePrixService(PriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * Default constructor for backward compatibility.
     */
    public HistoriquePrixService() {
        this(new JsonPriceHistoryRepository());
    }

    @Override
    public void saveTodayPrices(List<Station> stations) {
        try {
            List<PriceHistoryRepository.HistoryEntry> historique = new ArrayList<>(priceHistoryRepository.findAll());
            String dateAujourdhui = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            // Supprimer les entrées du jour (éviter les doublons)
            historique.removeIf(e -> e.date().equals(dateAujourdhui));

            // Ajouter les nouveaux prix
            for (Station station : stations) {
                for (Map.Entry<String, Double> entry : station.getPrix().entrySet()) {
                    historique.add(new PriceHistoryRepository.HistoryEntry(
                            dateAujourdhui,
                            entry.getKey(),
                            station.getCodePostal(),
                            station.getVille(),
                            entry.getValue()
                    ));
                }
            }

            // Nettoyer l'historique (garder seulement les 30 derniers jours)
            LocalDate cutoffDate = LocalDate.now().minusDays(30);
            historique.removeIf(e -> {
                try {
                    LocalDate entryDate = LocalDate.parse(e.date());
                    return entryDate.isBefore(cutoffDate);
                } catch (Exception ex) {
                    return true;
                }
            });

            priceHistoryRepository.saveAll(historique);
            System.out.println("✅ Historique sauvegardé (" + stations.size() + " stations)");

        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde historique : " + e.getMessage());
        }
    }

    @Override
    public void displayPriceEvolution(String carburant, int nbJours) {
        List<PriceHistoryRepository.HistoryEntry> historique = priceHistoryRepository.findByCarburantAndDays(carburant, nbJours);

        Map<String, List<PriceHistoryRepository.HistoryEntry>> prixParJour = historique.stream()
                .collect(Collectors.groupingBy(PriceHistoryRepository.HistoryEntry::date));

        if (prixParJour.isEmpty()) {
            System.out.println("❌ Aucune donnée historique trouvée pour " + carburant);
            return;
        }

        System.out.printf("\n📈 ÉVOLUTION %s SUR %d JOURS 📈%n", carburant, nbJours);

        prixParJour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String date = entry.getKey();
                    List<PriceHistoryRepository.HistoryEntry> prixJour = entry.getValue();

                    double moyenne = prixJour.stream()
                            .mapToDouble(PriceHistoryRepository.HistoryEntry::prix)
                            .average()
                            .orElse(0);

                    double min = prixJour.stream()
                            .mapToDouble(PriceHistoryRepository.HistoryEntry::prix)
                            .min()
                            .orElse(0);

                    double max = prixJour.stream()
                            .mapToDouble(PriceHistoryRepository.HistoryEntry::prix)
                            .max()
                            .orElse(0);

                    System.out.printf("%s : Moy %.3f €/L | Min %.3f €/L | Max %.3f €/L (%d stations)%n",
                            date, moyenne, min, max, prixJour.size());
                });
    }

    /**
     * Static method for backward compatibility.
     * @param stations List of stations
     * @deprecated since 1.1, for removal in 2.0. Use {@link #saveTodayPrices(List)} instead.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public static void sauvegarderPrixDuJour(List<Station> stations) {
        new HistoriquePrixService().saveTodayPrices(stations);
    }

    /**
     * Static method for backward compatibility.
     * @param carburant Fuel type
     * @param nbJours Number of days
     * @deprecated since 1.1, for removal in 2.0. Use {@link #displayPriceEvolution(String, int)} instead.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public static void afficherEvolutionPrix(String carburant, int nbJours) {
        new HistoriquePrixService().displayPriceEvolution(carburant, nbJours);
    }
}