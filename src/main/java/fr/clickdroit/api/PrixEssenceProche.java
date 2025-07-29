package fr.clickdroit.api;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class PrixEssenceProche {

    private static final String CONFIG_FILE = "station_config.properties";
    private static final String HISTORY_FILE = "prix_historique.json";
    private static final String XML_URL = "https://donnees.roulez-eco.fr/opendata/instantane";
    private static final String CACHE_FILE = "prix_carburants_cache.xml";
    private static boolean USE_ROUTING_API = true;
    private static double[] userPosition = null;
    private static List<Station> allStations = new ArrayList<>();

    static class Station {
        String ville;
        String codePostal;
        String adresse;
        String departement;
        Map<String, Double> prix;
        double latitude;
        double longitude;
        double distanceKm;

        Station(String ville, String cp, double lat, double lon, String adresse) {
            this.ville = ville != null ? ville : "Ville inconnue";
            this.codePostal = cp != null ? cp : "00000";
            this.latitude = lat;
            this.longitude = lon;
            this.adresse = adresse != null && !adresse.trim().isEmpty() ? adresse : "Adresse non disponible";
            this.departement = this.codePostal.length() >= 2 ? this.codePostal.substring(0, 2) : "??";
            this.prix = new HashMap<>();
        }

        void ajouterPrix(String carburant, double prixValue) {
            prix.put(carburant, prixValue);
        }

        Double getPrix(String carburant) {
            return prix.get(carburant);
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
    }

    static class StatistiquesNationales {
        String carburant;
        double moyenne;
        double minimum;
        double maximum;
        long nombreStations;
        String villeMin;
        String villeMax;

        StatistiquesNationales(String carburant, List<Station> stations) {
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
                        .map(s -> s.ville + " (" + s.codePostal + ")")
                        .orElse("Inconnue");

                this.villeMax = stations.stream()
                        .filter(s -> Objects.equals(s.getPrix(carburant), maximum))
                        .findFirst()
                        .map(s -> s.ville + " (" + s.codePostal + ")")
                        .orElse("Inconnue");
            }
        }

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

    public static void main(String[] args) {
        InterfaceCLI.afficherBienvenue();
        try {
            System.out.println("🚗 === ANALYSEUR PRIX CARBURANTS === ⛽");

            if (!chargerDonnees()) {
                System.err.println("Impossible de charger les données. Fin du programme.");
                return;
            }

            Scanner scanner = new Scanner(System.in);
            boolean continuer = true;

            while (continuer) {
                afficherMenuPrincipal();
                System.out.print("Votre choix : ");

                try {
                    int choix = Integer.parseInt(scanner.nextLine().trim());
                    continuer = traiterChoixMenu(choix, scanner);
                } catch (NumberFormatException e) {
                    System.out.println("❌ Choix invalide. Entrez un nombre entre 1 et 8.");
                }

                if (continuer) {
                    System.out.println("\n" + "=".repeat(50));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void afficherMenuPrincipal() {
        System.out.println("\n🏠 === MENU PRINCIPAL ===");
        System.out.println("1. 🎯 Stations les moins chères près de moi");
        System.out.println("2. 📊 Statistiques nationales par carburant");
        System.out.println("3. 🗺️ Recherche par département/région");
        System.out.println("4. ⚙️ Configurer ma position");
        System.out.println("5. 🌡️ Météo du jour");
        System.out.println("6. 📈 Historique des prix");
        System.out.println("7. 🔧 Paramètres");
        System.out.println("8. 🚇 Infos transports");
        System.out.println("9. 🔄 Actualiser les données");
        System.out.println("0. ❌ Quitter");
    }

    private static boolean traiterChoixMenu(int choix, Scanner scanner) {
        switch (choix) {
            case 1:
                rechercherStationsProches(scanner);
                break;
            case 2:
                afficherStatistiquesNationales(scanner);
                break;
            case 3:
                rechercherParDepartement(scanner);
                break;
            case 4:
                configurerPosition(scanner);
                break;
            case 5:
                afficherMeteo(scanner);
                break;
            case 6:
                HistoriquePrix.sauvegarderPrixDuJour(allStations);
                String carburant = choisirCarburant(scanner);
                if (carburant != null) {
                    HistoriquePrix.afficherEvolutionPrix(carburant, 7);
                }
                break;
            case 7:
                configurerParametres(scanner);
                break;
            case 8:
                TransportInfo.afficherPerturbationsTransports();
                break;
            case 9:
                System.out.println("🔄 Actualisation des données...");
                forceDownloadData();
                chargerDonnees();
                break;
            case 0:
                System.out.println("👋 À bientôt !");
                return false;
            default:
                System.out.println("❌ Choix invalide.");
        }
        return true;
    }

    private static void rechercherStationsProches(Scanner scanner) {
        if (userPosition == null) {
            System.out.println("❌ Position non configurée. Configurez d'abord votre position.");
            configurerPosition(scanner);
            if (userPosition == null) return;
        }

        String carburant = choisirCarburant(scanner);
        if (carburant == null) return;

        System.out.print("Rayon de recherche en km (défaut: 20) : ");
        String rayonStr = scanner.nextLine().trim();
        final double rayon = rayonStr.isEmpty() ? 20.0 : parseRayonAvecDefaut(rayonStr, 20.0);

        List<Station> stationsAvecCarburant = allStations.stream()
                .filter(s -> s.getPrix(carburant) != null)
                .filter(s -> s.distanceKm <= rayon)
                .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                .limit(10)
                .collect(Collectors.toList());

        if (stationsAvecCarburant.isEmpty()) {
            System.out.printf("❌ Aucune station avec %s trouvée dans un rayon de %.0f km.%n", carburant, rayon);
            return;
        }

        System.out.printf("\n🎯 TOP 10 STATIONS %s DANS UN RAYON DE %.0f KM :\n", carburant, rayon);
        for (int i = 0; i < stationsAvecCarburant.size(); i++) {
            Station s = stationsAvecCarburant.get(i);
            System.out.printf("%d. %s (%s) - %s: %.3f €/L - %.2f km%n   📍 %s%n",
                    i + 1, s.ville, s.codePostal, carburant, s.getPrix(carburant), s.distanceKm, s.adresse);

            if (i < 3) {
                String googleMapsUrl = String.format("https://www.google.com/maps/dir/%.6f,%.6f/%.6f,%.6f",
                        userPosition[0], userPosition[1], s.latitude, s.longitude);
                System.out.printf("   🗺️ Itinéraire: %s%n", googleMapsUrl);
            }
            System.out.println();
        }
    }

    private static void afficherStatistiquesNationales(Scanner scanner) {
        String carburant = choisirCarburant(scanner);
        if (carburant == null) return;

        StatistiquesNationales stats = new StatistiquesNationales(carburant, allStations);
        System.out.println("\n" + stats);

        List<Station> stationsPasCher = allStations.stream()
                .filter(s -> s.getPrix(carburant) != null)
                .filter(s -> s.getPrix(carburant).equals(stats.minimum))
                .limit(5)
                .collect(Collectors.toList());

        if (!stationsPasCher.isEmpty()) {
            System.out.println("\n🏆 Stations au prix minimum :");
            for (Station s : stationsPasCher) {
                System.out.printf("  • %s (%s) - %s%n", s.ville, s.codePostal, s.adresse);
            }
        }
    }

    private static void rechercherParDepartement(Scanner scanner) {
        System.out.print("Entrez un numéro de département (ex: 75, 69, 13) : ");
        String dept = scanner.nextLine().trim();

        if (dept.length() < 2) {
            System.out.println("❌ Numéro de département invalide.");
            return;
        }

        String carburant = choisirCarburant(scanner);
        if (carburant == null) return;

        List<Station> stationsDept = allStations.stream()
                .filter(s -> s.departement.equals(dept))
                .filter(s -> s.getPrix(carburant) != null)
                .collect(Collectors.toList());

        if (stationsDept.isEmpty()) {
            System.out.printf("❌ Aucune station trouvée dans le département %s avec %s.%n", dept, carburant);
            return;
        }

        StatistiquesNationales statsDept = new StatistiquesNationales(carburant, stationsDept);
        System.out.printf("\n📊 DÉPARTEMENT %s - %s 📊%n", dept, carburant);
        System.out.printf("  Moyenne : %.3f €/L%n", statsDept.moyenne);
        System.out.printf("  Prix minimum : %.3f €/L (à %s)%n", statsDept.minimum, statsDept.villeMin);
        System.out.printf("  Prix maximum : %.3f €/L (à %s)%n", statsDept.maximum, statsDept.villeMax);
        System.out.printf("  Nombre de stations : %d%n", statsDept.nombreStations);

        List<Station> top5Dept = stationsDept.stream()
                .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                .limit(5)
                .collect(Collectors.toList());

        System.out.printf("\n🏆 TOP 5 MOINS CHÈRES DU DÉPARTEMENT %s :%n", dept);
        for (int i = 0; i < top5Dept.size(); i++) {
            Station s = top5Dept.get(i);
            System.out.printf("%d. %s (%s) - %.3f €/L%n   📍 %s%n",
                    i + 1, s.ville, s.codePostal, s.getPrix(carburant), s.adresse);
        }
    }

    private static void configurerPosition(Scanner scanner) {
        userPosition = getUserLocationWithConfig(scanner);
        if (userPosition != null) {
            System.out.println("🔄 Recalcul des distances...");
            calculerDistancesToutesStations();
            System.out.println("✅ Position configurée et distances calculées !");
        }
    }

    private static void afficherMeteo(Scanner scanner) {
        System.out.print("Ville pour la météo (défaut: Paris) : ");
        String ville = scanner.nextLine().trim();
        if (ville.isEmpty()) ville = "Paris";

        try {
            // Utilisation d'Open-Meteo (gratuit, pas de clé API requise)
            String url = "https://api.open-meteo.com/v1/forecast?latitude=48.8566&longitude=2.3522&current_weather=true&timezone=Europe%2FParis";

            URL weatherUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) weatherUrl.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent", "PrixEssenceApp/1.0");

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                System.err.println("❌ Erreur météo : Code " + responseCode);
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

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

    private static void configurerParametres(Scanner scanner) {
        System.out.println("\n⚙️ === PARAMÈTRES ===");
        System.out.println("1. Calcul de distance : " + (USE_ROUTING_API ? "Par la route (précis)" : "Vol d'oiseau × 1.3 (rapide)"));
        System.out.print("Changer le mode de calcul de distance ? (o/n) : ");
        String change = scanner.nextLine().trim().toLowerCase();

        if (change.equals("o") || change.equals("oui")) {
            USE_ROUTING_API = !USE_ROUTING_API;
            System.out.println("✅ Mode changé vers : " +
                    (USE_ROUTING_API ? "Par la route (précis)" : "Vol d'oiseau × 1.3 (rapide)"));

            if (userPosition != null) {
                System.out.print("Recalculer les distances avec le nouveau mode ? (o/n) : ");
                String recalc = scanner.nextLine().trim().toLowerCase();
                if (recalc.equals("o") || recalc.equals("oui")) {
                    System.out.println("🔄 Recalcul en cours...");
                    calculerDistancesToutesStations();
                    System.out.println("✅ Distances recalculées !");
                }
            }
        }
    }

    private static String choisirCarburant(Scanner scanner) {
        Set<String> carburantsDisponibles = allStations.stream()
                .flatMap(s -> s.prix.keySet().stream())
                .collect(Collectors.toSet());

        if (carburantsDisponibles.isEmpty()) {
            System.out.println("❌ Aucun carburant trouvé dans les données.");
            return null;
        }

        System.out.println("\n⛽ Carburants disponibles :");
        List<String> carburantsList = new ArrayList<>(carburantsDisponibles);
        Collections.sort(carburantsList); // Tri alphabétique

        for (int i = 0; i < carburantsList.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, carburantsList.get(i));
        }

        System.out.print("Choisissez un carburant (1-" + carburantsList.size() + ") : ");
        try {
            int choix = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choix >= 0 && choix < carburantsList.size()) {
                return carburantsList.get(choix);
            }
        } catch (NumberFormatException e) {
            // Continue vers l'erreur
        }

        System.out.println("❌ Choix invalide.");
        return null;
    }

    // ========== TÉLÉCHARGEMENT DES DONNÉES ==========

    private static boolean chargerDonnees() {
        try {
            System.out.println("📂 Chargement des données des stations...");

            File xmlFile = new File(CACHE_FILE);

            // Vérifier si le fichier cache existe et n'est pas trop ancien (plus de 6 heures)
            boolean needDownload = !xmlFile.exists() ||
                    (System.currentTimeMillis() - xmlFile.lastModified()) > 6 * 60 * 60 * 1000;

            if (needDownload || !isValidXMLFile(xmlFile)) {
                System.out.println("🌐 Téléchargement des données depuis internet...");
                if (!downloadDataFromInternet()) {
                    System.err.println("❌ Échec du téléchargement. Tentative avec fichier cache...");
                    if (!xmlFile.exists() || !isValidXMLFile(xmlFile)) {
                        System.err.println("❌ Aucun fichier XML valide disponible.");
                        return false;
                    }
                }
            } else {
                System.out.println("📁 Utilisation du cache local (récent)");
            }

            return parseXMLFile(xmlFile);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement : " + e.getMessage());
            return false;
        }
    }

    private static boolean downloadDataFromInternet() {
        try {
            URL url = new URL(XML_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("User-Agent", "PrixEssenceApp/1.0");
            connection.setRequestProperty("Accept", "*/*");
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("❌ Erreur HTTP : " + responseCode);
                return false;
            }

            // Télécharger et extraire le ZIP
            try (ZipInputStream zipIn = new ZipInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(CACHE_FILE)) {

                ZipEntry entry = zipIn.getNextEntry();
                if (entry != null && entry.getName().endsWith(".xml")) {
                    System.out.println("📦 Extraction du fichier : " + entry.getName());

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;

                    while ((bytesRead = zipIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;

                        if (totalBytes % (1024 * 1024) == 0) { // Chaque MB
                            System.out.printf("\r📥 Extraction... %.1f MB", totalBytes / 1024.0 / 1024.0);
                        }
                    }
                    System.out.println("\n✅ Extraction terminée !");
                    zipIn.closeEntry();
                    return true;
                } else {
                    System.err.println("❌ Aucun fichier XML trouvé dans l'archive");
                    return false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur téléchargement : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void forceDownloadData() {
        File cacheFile = new File(CACHE_FILE);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    private static boolean parseXMLFile(File xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList stations = doc.getElementsByTagName("pdv");
            allStations.clear();
            int stationsIgnorees = 0;

            System.out.println("🔄 Analyse du fichier XML...");

            for (int i = 0; i < stations.getLength(); i++) {
                if (i % 1000 == 0) {
                    System.out.printf("\r⚙️  Traitement... %d/%d stations", i, stations.getLength());
                }

                Element stationElement = (Element) stations.item(i);

                try {
                    String cp = stationElement.getAttribute("cp");
                    String ville = stationElement.getAttribute("ville");
                    String adresse = stationElement.getAttribute("adresse");

                    String latStr = stationElement.getAttribute("latitude");
                    String lonStr = stationElement.getAttribute("longitude");

                    if (latStr.isEmpty() || lonStr.isEmpty()) {
                        stationsIgnorees++;
                        continue;
                    }

                    double lat = Double.parseDouble(latStr) / 100000.0;
                    double lon = Double.parseDouble(lonStr) / 100000.0;

                    // Vérification coordonnées françaises (incluant DOM-TOM)
                    if (lat < -22.0 || lat > 52.0 || lon < -63.0 || lon > 56.0) {
                        stationsIgnorees++;
                        continue;
                    }

                    Station station = new Station(ville, cp, lat, lon, adresse);

                    NodeList prixList = stationElement.getElementsByTagName("prix");
                    for (int j = 0; j < prixList.getLength(); j++) {
                        Element prix = (Element) prixList.item(j);
                        String carburant = prix.getAttribute("nom");
                        String valeur = prix.getAttribute("valeur");

                        if (!valeur.isEmpty()) {
                            try {
                                double prixValue = Double.parseDouble(valeur.replace(',', '.'));
                                if (prixValue > 0 && prixValue < 10) { // Filtrage prix aberrants
                                    station.ajouterPrix(carburant, prixValue);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    if (!station.prix.isEmpty()) {
                        allStations.add(station);
                    } else {
                        stationsIgnorees++;
                    }

                } catch (NumberFormatException e) {
                    stationsIgnorees++;
                }
            }

            System.out.printf("\n✅ %d stations chargées (%d ignorées)%n", allStations.size(), stationsIgnorees);

            // Charger la position sauvegardée
            double[] savedPosition = loadSavedLocation();
            if (savedPosition != null) {
                userPosition = savedPosition;
                calculerDistancesToutesStations();
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du parsing XML : " + e.getMessage());
            return false;
        }
    }

    private static void calculerDistancesToutesStations() {
        if (userPosition == null) return;

        System.out.println("🧮 Calcul des distances...");
        for (int i = 0; i < allStations.size(); i++) {
            Station station = allStations.get(i);
            station.distanceKm = getSmartDistance(userPosition[0], userPosition[1],
                    station.latitude, station.longitude);

            if (i % 1000 == 0) {
                System.out.printf("\r⚙️  Calcul... %d/%d", i, allStations.size());
            }
        }
        System.out.println("\n✅ Distances calculées !");
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private static double parseRayonAvecDefaut(String rayonStr, double defaut) {
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

    // ==================== MÉTHODES GÉOLOCALISATION ====================

    private static double[] getUserLocationWithConfig(Scanner scanner) {
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

    private static double[] getUserLocation(Scanner scanner) {
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

    private static double[] saisirCoordonnees(Scanner scanner) {
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

    private static double[] geocoderAdresse(Scanner scanner) {
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
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
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

    private static double[] getUserLocationFromIP() {
        try {
            System.out.println("🌐 Tentative de géolocalisation par IP...");

            // Utilisation de l'API ipapi.co (gratuite)
            String url = "http://ipapi.co/json/";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
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

    private static double[] loadSavedLocation() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Properties props = new Properties();
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

    private static void saveLocation(double[] location) {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("latitude", String.valueOf(location[0]));
            props.setProperty("longitude", String.valueOf(location[1]));
            props.setProperty("saved_date", new Date().toString());
            props.store(fos, "Configuration Station Essence");
            System.out.println("✅ Position sauvegardée !");
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde : " + e.getMessage());
        }
    }

    public static double getSmartDistance(double lat1, double lon1, double lat2, double lon2) {
        if (!USE_ROUTING_API) {
            return distanceKm(lat1, lon1, lat2, lon2) * 1.3;
        }

        // Pour une implémentation complète, utiliser une API de routing comme OSRM
        // Ici, on utilise l'approximation par défaut
        return distanceKm(lat1, lon1, lat2, lon2) * 1.3;
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

    // ==================== CLASSES INTERNES ====================

    static class HistoriquePrix {
        private static final String HISTORIQUE_FILE = "historique_prix.json";

        static class EntreeHistorique {
            String date;
            String carburant;
            String codePostalStation;
            String nomStation;
            double prix;

            EntreeHistorique(String date, String carburant, String cp, String nom, double prix) {
                this.date = date;
                this.carburant = carburant;
                this.codePostalStation = cp;
                this.nomStation = nom;
                this.prix = prix;
            }
        }

        public static void sauvegarderPrixDuJour(List<Station> stations) {
            try {
                List<EntreeHistorique> historique = chargerHistorique();
                String dateAujourdhui = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                // Supprimer les entrées du jour (éviter les doublons)
                historique.removeIf(e -> e.date.equals(dateAujourdhui));

                // Ajouter les nouveaux prix
                for (Station station : stations) {
                    for (Map.Entry<String, Double> entry : station.prix.entrySet()) {
                        historique.add(new EntreeHistorique(
                                dateAujourdhui,
                                entry.getKey(),
                                station.codePostal,
                                station.ville,
                                entry.getValue()
                        ));
                    }
                }

                // Nettoyer l'historique (garder seulement les 30 derniers jours)
                LocalDate cutoffDate = LocalDate.now().minusDays(30);
                historique.removeIf(e -> {
                    try {
                        LocalDate entryDate = LocalDate.parse(e.date);
                        return entryDate.isBefore(cutoffDate);
                    } catch (Exception ex) {
                        return true; // Supprimer les entrées avec dates invalides
                    }
                });

                // Sauvegarder en JSON
                JSONArray jsonArray = new JSONArray();
                for (EntreeHistorique entree : historique) {
                    JSONObject obj = new JSONObject();
                    obj.put("date", entree.date);
                    obj.put("carburant", entree.carburant);
                    obj.put("codePostal", entree.codePostalStation);
                    obj.put("nomStation", entree.nomStation);
                    obj.put("prix", entree.prix);
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
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
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
                    .filter(e -> e.carburant.equals(carburant))
                    .filter(e -> {
                        try {
                            LocalDate dateEntree = LocalDate.parse(e.date);
                            return !dateEntree.isBefore(dateDebut);
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .collect(Collectors.groupingBy(e -> e.date));

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
                                .mapToDouble(e -> e.prix)
                                .average()
                                .orElse(0);

                        double min = prixJour.stream()
                                .mapToDouble(e -> e.prix)
                                .min()
                                .orElse(0);

                        double max = prixJour.stream()
                                .mapToDouble(e -> e.prix)
                                .max()
                                .orElse(0);

                        System.out.printf("%s : Moy %.3f €/L | Min %.3f €/L | Max %.3f €/L (%d stations)%n",
                                date, moyenne, min, max, prixJour.size());
                    });
        }
    }
    public class TransportInfo {

        public static void afficherPerturbationsTransports() {
            System.out.println("\n🚇 === INFOS TRANSPORTS === 🚆");

            recupererPerturbationsRATP();

            System.out.println("\n💡 Pour plus d'infos en temps réel :");
            System.out.println("  🌐 SNCF Connect : https://www.sncf-connect.com/");
            System.out.println("  📱 App Citymapper ou RATP");
            System.out.println("  📞 3635 (SNCF) ou 3424 (RATP)");
        }

        private static void recupererPerturbationsRATP() {
            try {
                String[] modes = {"rers", "metros"}; // Ajoute "tramways", "bus" si besoin

                for (String mode : modes) {
                    String url = "https://api-ratp.pierre-grimaud.fr/v4/traffic/" + mode;
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "JavaApp");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        JSONObject json = new JSONObject(response.toString());
                        JSONArray lines = json.getJSONObject("result").getJSONArray("lines");

                        System.out.println("\n📍 État du trafic " + mode.toUpperCase());

                        for (int i = 0; i < lines.length(); i++) {
                            JSONObject ligne = lines.getJSONObject(i);
                            String nom = ligne.getString("line");
                            String etat = ligne.getString("slug");
                            String message = ligne.getString("title");

                            String emoji = getStatusEmoji(etat);
                            System.out.printf("  %s %s : %s%n", emoji, nom.toUpperCase(), message);
                        }
                    } else {
                        System.out.println("⚠️ Erreur API RATP (" + mode + ") : " + conn.getResponseCode());
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Erreur API RATP : " + e.getMessage());
            }
        }

        private static void afficherPerturbationsJSON(JSONObject json) {
            try {
                JSONArray disruptions = json.getJSONArray("disruptions");

                System.out.println("🚆 PERTURBATIONS SNCF EN TEMPS RÉEL :");

                if (disruptions.length() == 0) {
                    System.out.println("✅ Aucune perturbation majeure signalée");
                    return;
                }

                for (int i = 0; i < Math.min(disruptions.length(), 10); i++) {
                    JSONObject disruption = disruptions.getJSONObject(i);

                    String severity = disruption.optString("severity", "unknown").toLowerCase();
                    String cause = disruption.optString("cause", "Non spécifiée");

                    JSONArray impactedObjects = disruption.optJSONArray("impacted_objects");
                    if (impactedObjects != null && impactedObjects.length() > 0) {
                        JSONObject impact = impactedObjects.getJSONObject(0);
                        JSONObject line = impact.optJSONObject("pt_object");

                        if (line != null) {
                            String lineName = line.optString("name", "Ligne inconnue");
                            String emoji = getSeverityEmoji(severity);

                            System.out.printf("  %s %s : %s (%s)%n", emoji, lineName, cause, severity);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Erreur parsing perturbations : " + e.getMessage());
                afficherDonneesSimulees();
            }
        }

        private static void afficherDonneesSimulees() {
            System.out.println("📊 État du trafic (données simulées) :");
            String[] lignes = {"RER A", "RER B", "RER C", "RER D", "Métro 1", "Métro 4", "Métro 14"};
            String[] status = {"Normal", "Perturbé", "Normal", "Ralenti", "Normal", "Normal", "Normal"};

            for (int i = 0; i < lignes.length; i++) {
                String emoji = getStatusEmoji(status[i]);
                System.out.printf("  %s %s : %s%n", emoji, lignes[i], status[i]);
            }
        }

        private static String getSeverityEmoji(String severity) {
            return switch (severity) {
                case "information" -> "ℹ️";
                case "warning" -> "⚠️";
                case "blocking" -> "🚫";
                case "reduced_service" -> "🟡";
                case "significant_delays" -> "🔴";
                case "detour" -> "🔄";
                default -> "❓";
            };
        }

        public static String getStatusEmoji(String status) {
            return switch (status.toLowerCase()) {
                case "normal" -> "✅";
                case "perturbé" -> "⚠️";
                case "très perturbé" -> "❌";
                case "ralenti", "retard" -> "🟡";
                case "interrompu" -> "🚫";
                default -> "❓";
            };
        }

        public static void comparerCoutTransport(double distanceKm, double prixCarburant, double consommation) {
            System.out.println("\n💰 === COMPARAISON COÛTS === 💰");

            double coutCarburant = (distanceKm / 100) * consommation * prixCarburant;
            double prixTicketMetro = 2.15;
            double prixNavigoMois = 84.10;
            double prixNavigoJour = prixNavigoMois / 30;

            System.out.printf("🚗 Voiture (%.1f km) :%n", distanceKm);
            System.out.printf("  Carburant : %.2f € (conso %.1fL/100km à %.3f€/L)%n",
                    coutCarburant, consommation, prixCarburant);
            System.out.printf("  + Parking, péages, usure... : ~%.2f €%n", coutCarburant * 0.5);
            System.out.printf("  TOTAL estimé : %.2f €%n", coutCarburant * 1.5);

            System.out.println("\n🚇 Transports en commun :");
            System.out.printf("  Ticket à l'unité : %.2f € (A/R)%n", prixTicketMetro * 2);
            System.out.printf("  Navigo jour : %.2f €%n", prixNavigoJour);
            System.out.printf("  Navigo mois : %.2f € (si trajet quotidien)%n", prixNavigoMois);

            if (coutCarburant * 1.5 > prixTicketMetro * 2) {
                System.out.println("\n💡 Les transports en commun semblent plus économiques !");
            } else {
                System.out.println("\n💡 La voiture pourrait être plus économique pour ce trajet.");
            }
        }
    }

    static class InterfaceCLI {

        public static void afficherGraphiquePrix(List<Station> stations, String carburant) {
            if (stations.isEmpty()) return;

            System.out.printf("\n📊 GRAPHIQUE PRIX %s 📊%n", carburant);

            List<Station> top10 = stations.stream()
                    .filter(s -> s.getPrix(carburant) != null)
                    .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                    .limit(10)
                    .collect(Collectors.toList());

            if (top10.isEmpty()) return;

            double prixMin = top10.get(0).getPrix(carburant);
            double prixMax = top10.get(top10.size() - 1).getPrix(carburant);
            double ecart = prixMax - prixMin;

            System.out.printf("Prix min: %.3f €/L | Prix max: %.3f €/L | Écart: %.3f €/L%n%n",
                    prixMin, prixMax, ecart);

            for (int i = 0; i < top10.size(); i++) {
                Station s = top10.get(i);
                double prix = s.getPrix(carburant);

                int longueurBarre = ecart > 0 ? (int) ((prix - prixMin) / ecart * 40) + 5 : 5;
                String barre = "█".repeat(Math.max(1, longueurBarre));
                String couleur = i < 3 ? "🟢" : i < 7 ? "🟡" : "🔴";

                System.out.printf("%s %2d. %-20s %s %.3f €/L%n",
                        couleur, i + 1,
                        truncateString(s.ville + " (" + s.codePostal + ")", 18),
                        barre, prix);
            }
            System.out.println();
        }

        public static void afficherTableauComparaison(List<Station> stations, String carburant) {
            if (stations.isEmpty()) return;

            System.out.printf("\n📋 TABLEAU COMPARATIF %s 📋%n", carburant);
            System.out.println("┌────┬─────────────────────┬───────────┬──────────┬─────────────┐");
            System.out.println("│ #  │ Ville              │ CP        │ Prix €/L │ Distance km │");
            System.out.println("├────┼─────────────────────┼───────────┼──────────┼─────────────┤");

            List<Station> top10 = stations.stream()
                    .filter(s -> s.getPrix(carburant) != null)
                    .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                    .limit(10)
                    .collect(Collectors.toList());

            for (int i = 0; i < top10.size(); i++) {
                Station s = top10.get(i);
                System.out.printf("│%3d │ %-19s │ %-9s │ %8.3f │ %11.2f │%n",
                        i + 1,
                        truncateString(s.ville, 19),
                        s.codePostal,
                        s.getPrix(carburant),
                        s.distanceKm
                );
            }

            System.out.println("└────┴─────────────────────┴───────────┴──────────┴─────────────┘");
        }

        private static String truncateString(String str, int maxLength) {
            if (str.length() <= maxLength) {
                return str;
            }
            return str.substring(0, maxLength - 3) + "...";
        }

        public static void afficherBienvenue() {
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                  🚗 ANALYSEUR CARBURANTS ⛽                  ║");
            System.out.println("║                                                              ║");
            System.out.println("║  Trouvez les meilleures stations-service près de chez vous   ║");
            System.out.println("║         Analysez les prix • Économisez de l'argent           ║");
            System.out.println("║                                                              ║");
            System.out.println("║  📊 Données en temps réel depuis data.gouv.fr                ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
        }
    }

    private static boolean isValidXMLFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) return false;

            // Vérifier que ça commence par <?xml ou <pdv_liste
            firstLine = firstLine.trim();
            return firstLine.startsWith("<?xml") || firstLine.startsWith("<pdv_liste");
        } catch (Exception e) {
            return false;
        }
    }
}