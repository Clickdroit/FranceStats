package fr.clickdroit.api.repository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON file-based implementation of PriceHistoryRepository.
 */
public class JsonPriceHistoryRepository implements PriceHistoryRepository {

    private static final String HISTORIQUE_FILE = "historique_prix.json";

    @Override
    public List<HistoryEntry> findAll() {
        List<HistoryEntry> historique = new ArrayList<>();
        File file = new File(HISTORIQUE_FILE);

        if (!file.exists()) return historique;

        try {
            String content = new String(Files.readAllBytes(Paths.get(HISTORIQUE_FILE)), StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(content);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                historique.add(new HistoryEntry(
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

    @Override
    public void saveAll(List<HistoryEntry> entries) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (HistoryEntry entry : entries) {
                JSONObject obj = new JSONObject();
                obj.put("date", entry.date());
                obj.put("carburant", entry.carburant());
                obj.put("codePostal", entry.codePostal());
                obj.put("nomStation", entry.nomStation());
                obj.put("prix", entry.prix());
                jsonArray.put(obj);
            }

            try (FileWriter file = new FileWriter(HISTORIQUE_FILE, StandardCharsets.UTF_8)) {
                file.write(jsonArray.toString(2));
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde historique : " + e.getMessage());
        }
    }

    @Override
    public List<HistoryEntry> findByCarburantAndDays(String carburant, int days) {
        LocalDate dateDebut = LocalDate.now().minusDays(days);

        return findAll().stream()
                .filter(e -> e.carburant().equals(carburant))
                .filter(e -> {
                    try {
                        LocalDate dateEntree = LocalDate.parse(e.date());
                        return !dateEntree.isBefore(dateDebut);
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
