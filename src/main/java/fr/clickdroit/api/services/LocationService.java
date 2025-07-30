package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class LocationService {
    private static final String CONFIG_FILE = "station_config.properties";
    private static final int HTTP_TIMEOUT_MS = 10000;
    private static boolean USE_ROUTING_API = true;

    // Cache pour les distances calculées
    private final Map<String, Double> distanceCache = new ConcurrentHashMap<>();

    public double[] getUserLocationWithConfig(Scanner scanner) {
        double[] savedLocation = loadSavedLocation();
        if (savedLocation != null) {
            System.out.printf("Position sauvegardée trouvée : %.5f, %.5f%n",
                    savedLocation[0], savedLocation[1]);
            System.out.print("Utiliser cette position ? (o/n) : ");
            String response = scanner.nextLine().trim().toLowerCase();

            if (response.equals("o") || response.equals("oui") || response.equals("y") || response.equals("yes")) {
                return savedLocation;
            }
        }

        double[] newLocation = getUserLocation(scanner);
        if (newLocation != null) {
            saveLocation(newLocation);
        }
        return newLocation;
    }

    public double[] getUserLocation(Scanner scanner) {
        System.out.println("\n=== CHOIX DE LOCALISATION ===");
        System.out.println("1. Saisir manuellement les coordonnées GPS");
        System.out.println("2. Saisir une adresse/ville");
        System.out.println("3. Utiliser la géolocalisation IP (moins précis)");
        System.out.print("Votre choix (1-3) : ");

        int choix = 0;
        try {
            choix = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Choix invalide, utilisation IP par défaut");
            choix = 3;
        }

        switch (choix) {
            case 1: return saisirCoordonnees(scanner);
            case 2: return geocoderAdresse(scanner);
            case 3: return getUserLocationFromIP();
            default:
                System.out.println("Choix invalide, utilisation IP par défaut");
                return getUserLocationFromIP();
        }
    }

    private double[] saisirCoordonnees(Scanner scanner) {
        System.out.println("\n=== SAISIE COORDONNÉES GPS ===");
        System.out.println("Vous pouvez trouver vos coordonnées sur Google Maps :");
        System.out.println("1. Allez sur maps.google.com");
        System.out.println("2. Clic droit sur votre position");
        System.out.println("3. Cliquez sur les coordonnées qui s'affichent");

        try {
            System.out.print("Latitude (ex: 48.8566) : ");
            double lat = Double.parseDouble(scanner.nextLine().trim().replace(',', '.'));
            System.out.print("Longitude (ex: 2.3522) : ");
            double lon = Double.parseDouble(scanner.nextLine().trim().replace(',', '.'));

            // Validation des coordonnées
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                System.out.println("❌ Coordonnées invalides. Latitude: -90 à 90, Longitude: -180 à 180");
                return saisirCoordonnees(scanner);
            }

            return new double[]{lat, lon};
        } catch (NumberFormatException e) {
            System.out.println("❌ Format invalide. Utilisez des nombres décimaux.");
            return saisirCoordonnees(scanner);
        }
    }

    private double[] geocoderAdresse(Scanner scanner) {
        System.out.print("Entrez votre ville ou adresse : ");
        String adresse = scanner.nextLine().trim();

        if (adresse.isEmpty()) {
            System.out.println("⚠️ Aucune adresse saisie, utilisation de Paris par défaut");
            return new double[]{48.8566, 2.3522};
        }

        try {
            // Utilisation de l'API Nominatim d'OpenStreetMap (gratuite)
            String encodedAddress = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress +
                    "&format=json&countrycodes=fr&limit=1";

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "PrixEssenceApp/1.0 (contact@example.com)");

            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    JSONArray results = new JSONArray(response.toString());
                    if (results.length() > 0) {
                        JSONObject result = results.getJSONObject(0);
                        double lat = result.getDouble("lat");
                        double lon = result.getDouble("lon");
                        String displayName = result.getString("display_name");

                        System.out.printf("✅ Adresse trouvée : %s%n", displayName);
                        return new double[]{lat, lon};
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur géocodage : " + e.getMessage());
        }

        System.out.println("❌ Impossible de trouver cette adresse. Utilisation de Paris par défaut.");
        return new double[]{48.8566, 2.3522};
    }

    private double[] getUserLocationFromIP() {
        try {
            System.out.println("🌐 Tentative de géolocalisation par IP...");

            // Utilisation de l'API ipapi.co (gratuite)
            String url = "http://ipapi.co/json/";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "PrixEssenceApp/1.0");

            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    JSONObject json = new JSONObject(response.toString());
                    double lat = json.getDouble("latitude");
                    double lon = json.getDouble("longitude");
                    String city = json.optString("city", "Ville inconnue");
                    String country = json.optString("country_name", "");

                    if ("France".equals(country)) {
                        System.out.printf("✅ Position détectée : %s (%.4f, %.4f)%n", city, lat, lon);
                        return new double[]{lat, lon};
                    } else {
                        System.out.printf("⚠️ Position détectée hors France : %s, %s%n", city, country);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur géolocalisation IP : " + e.getMessage());
        }

        System.out.println("📍 Utilisation de Paris par défaut");
        return new double[]{48.8566, 2.3522};
    }

    public double[] loadSavedLocation() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(configFile)) {
            java.util.Properties props = new java.util.Properties();
            props.load(fis);

            String latStr = props.getProperty("latitude");
            String lonStr = props.getProperty("longitude");

            if (latStr != null && lonStr != null) {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lecture config : " + e.getMessage());
        }
        return null;
    }

    public void saveLocation(double[] location) {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("latitude", String.valueOf(location[0]));
            props.setProperty("longitude", String.valueOf(location[1]));
            props.setProperty("saved_date", new java.util.Date().toString());
            props.store(fos, "Configuration Station Essence");
            System.out.println("✅ Position sauvegardée !");
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde : " + e.getMessage());
        }
    }

    public void calculerDistancesToutesStations(List<Station> stations, double[] userPosition) {
        if (userPosition == null) return;

        System.out.println("🧮 Calcul des distances...");
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            station.setDistanceKm(getSmartDistance(userPosition[0], userPosition[1],
                    station.getLatitude(), station.getLongitude()));

            if (i % 1000 == 0) {
                System.out.printf("\r⚙️  Calcul... %d/%d", i, stations.size());
            }
        }
        System.out.println("\n✅ Distances calculées !");
    }

    public double getSmartDistance(double lat1, double lon1, double lat2, double lon2) {
        // Utiliser le cache pour éviter les recalculs
        String key = String.format("%.4f,%.4f-%.4f,%.4f", lat1, lon1, lat2, lon2);
        return distanceCache.computeIfAbsent(key, k -> {
            if (!USE_ROUTING_API) {
                return distanceKm(lat1, lon1, lat2, lon2) * 1.3;
            }
            // Pour une implémentation complète, utiliser une API de routing comme OSRM
            // Ici, on utilise l'approximation par défaut
            return distanceKm(lat1, lon1, lat2, lon2) * 1.3;
        });
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088;
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Getters et Setters
    public static boolean isUseRoutingApi() {
        return USE_ROUTING_API;
    }

    public static void setUseRoutingApi(boolean useRoutingApi) {
        USE_ROUTING_API = useRoutingApi;
    }
}