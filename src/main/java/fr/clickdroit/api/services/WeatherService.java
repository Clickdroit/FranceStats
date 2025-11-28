package fr.clickdroit.api.services;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WeatherService implements IWeatherService {
    private static final int HTTP_TIMEOUT_MS = 10000;

    @Override
    public void afficherMeteo(String ville) {
        try {
            // Utilisation d'Open-Meteo (gratuit, pas de clé API requise)
            // Coordonnées par défaut pour Paris, dans un vrai cas il faudrait géocoder la ville
            String url = "https://api.open-meteo.com/v1/forecast?latitude=48.8566&longitude=2.3522&current_weather=true&timezone=Europe%2FParis";

            URL weatherUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) weatherUrl.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(HTTP_TIMEOUT_MS);
            con.setReadTimeout(HTTP_TIMEOUT_MS);
            con.setRequestProperty("User-Agent", "PrixEssenceApp/1.0");

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                System.err.println("❌ Erreur météo : Code " + responseCode);
                return;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                JSONObject response = new JSONObject(sb.toString());
                JSONObject currentWeather = response.getJSONObject("current_weather");

                double temperature = currentWeather.getDouble("temperature");
                double windSpeed = currentWeather.getDouble("windspeed");

                System.out.printf("\n🌡️ MÉTÉO À %s 🌡️%n", ville.toUpperCase());
                System.out.printf("  Température : %.1f°C%n", temperature);
                System.out.printf("  Vitesse du vent : %.1f km/h%n", windSpeed);
                System.out.printf("  Heure : %s%n", currentWeather.getString("time"));
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération météo : " + e.getMessage());
        }
    }
}