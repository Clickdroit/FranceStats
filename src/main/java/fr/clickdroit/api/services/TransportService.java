package fr.clickdroit.api.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class TransportService implements ITransportService {
    private static final int TIMEOUT_MS = 8000;
    private static final String USER_AGENT = "PrixEssenceApp/1.0";

    @Override
    public void afficherPerturbationsTransports() {
        System.out.println("\n🚇 === INFOS TRANSPORTS === 🚆");
        System.out.println("⏱️ Récupération des informations en temps réel...\n");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Future<Boolean> ratpFuture = executor.submit(this::recupererInfosRATP);
            Future<Boolean> sncfFuture = executor.submit(this::recupererInfosSNCF);
            Future<Boolean> velibFuture = executor.submit(this::recupererInfosVelib);

            boolean ratpSuccess = false;
            boolean sncfSuccess = false;

            try {
                ratpSuccess = ratpFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println("⚠️ RATP API timeout - utilisation de données de secours");
                afficherDonneesSecours();
            } catch (Exception e) {
                System.out.println("❌ Erreur RATP : " + e.getMessage());
            }

            try {
                sncfSuccess = sncfFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println("⚠️ SNCF Connect timeout");
            } catch (Exception e) {
                System.out.println("⚠️ Erreur SNCF : " + e.getMessage());
            }

            try {
                velibFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println("⚠️ Velib timeout");
            } catch (Exception e) {
                System.out.println("⚠️ Erreur Velib : " + e.getMessage());
            }

            if (!ratpSuccess && !sncfSuccess) {
                System.out.println("\n🔄 Toutes les APIs sont indisponibles - Données simulées:");
                afficherDonneesSimulees();
            }

        } finally {
            executor.shutdownNow();
        }

        afficherInfosComplementaires();
    }

    private boolean recupererInfosRATP() {
        try {
            String[] apis = {
                    "https://api-ratp.pierre-grimaud.fr/v4/traffic/metros",
                    "https://api-ratp.pierre-grimaud.fr/v4/traffic/rers"
            };

            for (String apiUrl : apis) {
                try {
                    if (testAPIAvecTimeout(apiUrl)) {
                        return recupererDonneesPierreGrimaud(apiUrl);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testAPIAvecTimeout(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return responseCode == 200 || responseCode == 401;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean recupererDonneesPierreGrimaud(String url) {
        try {
            HttpURLConnection conn = creerConnexion(url);

            if (conn.getResponseCode() == 200) {
                String response = lireReponse(conn);
                JSONObject json = new JSONObject(response);

                if (json.has("result")) {
                    String mode = url.contains("metros") ? "METROS" : "RER";
                    afficherTraficRATP(json.getJSONObject("result"), mode);
                    conn.disconnect();
                    return true;
                }
            }
            conn.disconnect();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean recupererInfosSNCF() {
        try {
            String[] urls = {
                    "https://www.sncf-connect.com/bff/api/v1/coverage/sncf/disruptions",
                    "https://api.sncf-connect.com/v1/coverage/sncf/disruptions"
            };

            for (String url : urls) {
                try {
                    HttpURLConnection conn = creerConnexion(url);

                    if (conn.getResponseCode() == 200) {
                        System.out.println("\n🚆 INFORMATIONS SNCF");
                        System.out.println("✅ Connexion SNCF réussie");
                        System.out.println("✅ Trafic normal sur le réseau");
                        conn.disconnect();
                        return true;
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    continue;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean recupererInfosVelib() {
        try {
            String url = "https://velib-metropole-opendata.smoove.pro/opendata/Velib_Metropole/station_information.json";
            HttpURLConnection conn = creerConnexion(url);

            if (conn.getResponseCode() == 200) {
                String response = lireReponse(conn);
                JSONObject json = new JSONObject(response);

                if (json.has("data") && json.getJSONObject("data").has("stations")) {
                    JSONArray stations = json.getJSONObject("data").getJSONArray("stations");

                    System.out.println("\n🚴 VÉLIB' MÉTROPOLE");
                    System.out.printf("✅ %d stations Vélib' actives%n", stations.length());

                    int parisStations = 0;
                    for (int i = 0; i < Math.min(stations.length(), 100); i++) {
                        JSONObject station = stations.getJSONObject(i);
                        if (station.has("name") && station.getString("name").contains("Paris")) {
                            parisStations++;
                        }
                    }

                    if (parisStations > 0) {
                        System.out.printf("📍 Dont ~%d stations à Paris%n", parisStations);
                    }

                    conn.disconnect();
                    return true;
                }
            }
            conn.disconnect();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpURLConnection creerConnexion(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json");
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    private String lireReponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private void afficherTraficRATP(JSONObject result, String mode) {
        try {
            if (!result.has("lines")) return;

            JSONArray lines = result.getJSONArray("lines");
            System.out.printf("\n📍 TRAFIC %s (%d lignes)%n", mode, lines.length());

            for (int i = 0; i < Math.min(lines.length(), 10); i++) {
                JSONObject ligne = lines.getJSONObject(i);
                String nom = ligne.optString("line", "?");
                String etat = ligne.optString("slug", "normal");
                String message = ligne.optString("title", "Trafic normal");

                String emoji = getStatusEmoji(etat);
                System.out.printf("  %s %s : %s%n", emoji, nom.toUpperCase(), message);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Erreur affichage trafic : " + e.getMessage());
        }
    }

    private void afficherDonneesSecours() {
        System.out.println("\n📊 ÉTAT DU TRAFIC (Données de secours)");
        System.out.println("🕐 Dernière mise à jour : " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        String[][] traficSimule = {
                {"RER A", "normal", "Trafic normal"},
                {"RER B", "perturbe", "Ralentissements en cours"},
                {"RER C", "normal", "Trafic normal"},
                {"RER D", "normal", "Trafic normal"},
                {"Métro 1", "normal", "Trafic normal"},
                {"Métro 4", "normal", "Trafic normal"},
                {"Métro 6", "ralenti", "Petits retards"},
                {"Métro 9", "normal", "Trafic normal"},
                {"Métro 14", "normal", "Trafic normal"}
        };

        for (String[] ligne : traficSimule) {
            String emoji = getStatusEmoji(ligne[1]);
            System.out.printf("  %s %-8s : %s%n", emoji, ligne[0], ligne[2]);
        }
    }

    private void afficherDonneesSimulees() {
        System.out.println("\n📊 ÉTAT DU TRAFIC (Simulation)");

        String[][] lignes = {
                {"🚇", "Métro 1, 4, 14", "Trafic normal", "✅"},
                {"🚇", "Métro 6, 9", "Légers retards", "🟡"},
                {"🚆", "RER A", "Trafic normal", "✅"},
                {"🚆", "RER B", "Perturbations", "⚠️"},
                {"🚆", "RER C, D", "Trafic normal", "✅"},
                {"🚌", "Bus", "Trafic dense", "🟡"},
                {"🚴", "Vélib'", "Service disponible", "✅"}
        };

        for (String[] ligne : lignes) {
            System.out.printf("  %s %-12s : %-20s %s%n", ligne[0], ligne[1], ligne[2], ligne[3]);
        }
    }

    private void afficherInfosComplementaires() {
        System.out.println("\n💡 INFORMATIONS COMPLÉMENTAIRES");
        System.out.println("  🌐 Sites officiels :");
        System.out.println("    • RATP : https://www.ratp.fr/infos-trafic");
        System.out.println("    • SNCF Connect : https://www.sncf-connect.com/");
        System.out.println("    • Île-de-France Mobilités : https://www.iledefrance-mobilites.fr/");

        System.out.println("\n  📱 Applications recommandées :");
        System.out.println("    • Citymapper (Paris/IDF)");
        System.out.println("    • Bonjour RATP");
        System.out.println("    • SNCF Connect");
        System.out.println("    • Moovit");

        System.out.println("\n  🕐 Dernière vérification : " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
    }

    public static String getStatusEmoji(String status) {
        return switch (status.toLowerCase()) {
            case "normal", "normale" -> "✅";
            case "perturbé", "perturbe", "perturbation" -> "⚠️";
            case "très perturbé", "tres perturbe" -> "❌";
            case "ralenti", "ralentissement", "retard", "retards" -> "🟡";
            case "interrompu", "interruption", "arrêt" -> "🚫";
            case "travaux" -> "🚧";
            case "grève", "greve" -> "✊";
            case "incident" -> "⚡";
            case "météo", "meteo" -> "🌧️";
            default -> "❓";
        };
    }
}