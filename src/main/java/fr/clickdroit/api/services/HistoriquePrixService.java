package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;

public class HistoriquePrixService {
    private static final String HISTORIQUE_FILE = "historique_prix.json";

    public static class EntreeHistorique {
        private String date;
        private String carburant;
        private String codePostalStation;
        private String nomStation;
        private double prix;

        public EntreeHistorique(String date, String carburant, String cp, String nom, double prix) {
            this.date = date;
            this.carburant = carburant;
            this.codePostalStation = cp;
            this.nomStation = nom;
            this.prix = prix;
        }

        // Getters
        public String getDate() { return date; }
        public String getCarburant() { return carburant; }
        public String getCodePostalStation() { return codePostalStation; }
        public String getNomStation() { return nomStation; }
        public double getPrix() { return prix; }
    }

    public static void sauvegarderPrixDuJour(List<Station> stations) {
        try {
            List<EntreeHistorique> historique = chargerHistorique();
            String dateAujourdhui = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            // Supprimer les entrées du jour (éviter les doublons)
            historique.removeIf(e -> e.getDate().equals(dateAujourdhui));

            // Ajouter les nouveaux prix
            for (Station station : stations) {
                for (Map.Entry<String, Double> entry : station.getPrix().entrySet()) {
                    historique.add(new EntreeHistorique(
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
                    LocalDate entryDate = LocalDate.parse(e.getDate());
                    return entryDate.isBefore(cutoffDate);
                } catch (Exception ex) {
                    return true; // Supprimer les entrées avec dates invalides
                }
            });

            // Sauvegarder en JSON
            JSONArray jsonArray = new JSONArray();
            for (EntreeHistorique entree : historique) {
                JSONObject obj = new JSONObject();
                obj.put("date", entree.getDate());
                obj.put("carburant", entree.getCarburant());
                obj.put("codePostal", entree.getCodePostalStation());
                obj.put("nomStation", entree.getNomStation());
                obj.put("prix", entree.getPrix());
                jsonArray.put(obj);
            }

            try (FileWriter file = new FileWriter(HISTORIQUE_FILE, StandardCharsets.UTF_8)) {
                file.write(jsonArray.toString(2));
            }

            System.out.println("✅ Historique sauvegardé (" + stations.size() + " stations)");

        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde historique : " + e.getMessage());
        }
    }

    private static List<EntreeHistorique> chargerHistorique() {
        List<EntreeHistorique> historique = new ArrayList<>();
        File file = new File(HISTORIQUE_FILE);

        if (!file.exists()) return historique;

        try {
            String content = new String(Files.readAllBytes(Paths.get(HISTORIQUE_FILE)), StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(content);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                historique.add(new EntreeHistorique(
                        obj.getString("date"),
                        obj.getString("carburant"),
                        obj.getString("codePostal"),
                        obj.getString("nomStation"),
                        obj.getDouble("prix")
                ));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lecture historique : " + e.getMessage());
        }

        return historique;
    }

    public static void afficherEvolutionPrix(String carburant, int nbJours) {
        List<EntreeHistorique> historique = chargerHistorique();

        LocalDate dateDebut = LocalDate.now().minusDays(nbJours);

        Map<String, List<EntreeHistorique>> prixParJour = historique.stream()
                .filter(e -> e.getCarburant().equals(carburant))
                .filter(e -> {
                    try {
                        LocalDate dateEntree = LocalDate.parse(e.getDate());
                        return !dateEntree.isBefore(dateDebut);
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(EntreeHistorique::getDate));

        if (prixParJour.isEmpty()) {
            System.out.println("❌ Aucune donnée historique trouvée pour " + carburant);
            return;
        }

        System.out.printf("\n📈 ÉVOLUTION %s SUR %d JOURS 📈%n", carburant, nbJours);

        prixParJour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String date = entry.getKey();
                    List<EntreeHistorique> prixJour = entry.getValue();

                    double moyenne = prixJour.stream()
                            .mapToDouble(EntreeHistorique::getPrix)
                            .average()
                            .orElse(0);

                    double min = prixJour.stream()
                            .mapToDouble(EntreeHistorique::getPrix)
                            .min()
                            .orElse(0);

                    double max = prixJour.stream()
                            .mapToDouble(EntreeHistorique::getPrix)
                            .max()
                            .orElse(0);

                    System.out.printf("%s : Moy %.3f €/L | Min %.3f €/L | Max %.3f €/L (%d stations)%n",
                            date, moyenne, min, max, prixJour.size());
                });
    }
}