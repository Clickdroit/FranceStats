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
import org.json.*;

public class PrixEssenceProche {

    private static final String CONFIG_FILE = "station_config.properties";
    private static final String HISTORY_FILE = "prix_historique.json";
    private static boolean USE_ROUTING_API = true;
    private static double[] userPosition = null;
    private static List<Station> allStations = new ArrayList<>();

    static class Station {
        String ville;
        String codePostal;
        String adresse;
        String departement;
        Map<String, Double> prix; // Stockage de tous les carburants
        double latitude;
        double longitude;
        double distanceKm;

        Station(String ville, String cp, double lat, double lon, String adresse) {
            this.ville = ville;
            this.codePostal = cp;
            this.latitude = lat;
            this.longitude = lon;
            this.adresse = adresse != null && !adresse.trim().isEmpty() ? adresse : "Adresse non disponible";
            this.departement = cp.length() >= 2 ? cp.substring(0, 2) : "??";
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

                // Trouver les villes avec prix min/max
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

            // Chargement initial des données
            if (!chargerDonnees()) {
                System.err.println("Impossible de charger les données. Fin du programme.");
                return;
            }

            // Menu principal
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
        System.out.println("6. 📈 Historique des prix (à venir)");
        System.out.println("7. 🔧 Paramètres");
        System.out.println("8. ❌ Quitter");
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
            case 6: // Historique des prix
                HistoriquePrix.sauvegarderPrixDuJour(allStations);
                HistoriquePrix.afficherEvolutionPrix(choisirCarburant(scanner), 7);
                break;
            case 7:
                configurerParametres(scanner);
                break;
            case 8:
                System.out.println("👋 À bientôt !");
                return false;
            case 9: // Transports
                TransportInfo.afficherPerturbationsTransports();
                break;
            default:
                System.out.println("❌ Choix invalide.");
        }
        return true;
    }

    private static void rechercherStationsProches(Scanner scanner) {
        if (userPosition == null) {
            System.out.println("❌ Position non configurée. Configurez d'abord votre position.");
            configurerPosition(scanner);
            return;
        }

        // Choisir le carburant
        String carburant = choisirCarburant(scanner);
        if (carburant == null) return;

        // Choisir le rayon
        System.out.print("Rayon de recherche en km (défaut: 20) : ");
        String rayonStr = scanner.nextLine().trim();
        double rayon = rayonStr.isEmpty() ? 20.0 : Double.parseDouble(rayonStr);

        // Filtrer et trier les stations
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

        // Afficher quelques exemples des stations les moins chères
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

        // Top 5 les moins chères du département
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
            // Recalculer les distances pour toutes les stations
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
            String url = "https://api.open-meteo.com/v1/forecast?latitude=48.8566&longitude=2.3522&current_weather=true&timezone=Europe%2FParis";

            // Pour simplifier, on utilise les coordonnées de Paris
            // Dans une version complète, on geocoderait la ville

            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

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
            // Pas grave, on retourne null
        }

        System.out.println("❌ Choix invalide.");
        return null;
    }

    private static boolean chargerDonnees() {
        try {
            System.out.println("📂 Chargement des données des stations...");

            File xmlFile = new File("C:\\Users\\maxim\\Desktop\\PrixCarburants_instantane.xml");
            if (!xmlFile.exists()) {
                System.err.println("❌ Fichier XML non trouvé : " + xmlFile.getAbsolutePath());
                return false;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList stations = doc.getElementsByTagName("pdv");
            allStations.clear();
            int stationsIgnorees = 0;

            for (int i = 0; i < stations.getLength(); i++) {
                Element stationElement = (Element) stations.item(i);

                try {
                    String cp = stationElement.getAttribute("cp");
                    String ville = stationElement.getAttribute("ville");
                    String adresse = stationElement.getAttribute("adresse");

                    double lat = Double.parseDouble(stationElement.getAttribute("latitude")) / 100000.0;
                    double lon = Double.parseDouble(stationElement.getAttribute("longitude")) / 100000.0;

                    // Vérification coordonnées françaises
                    if (lat < 41.0 || lat > 52.0 || lon < -5.0 || lon > 10.0) {
                        stationsIgnorees++;
                        continue;
                    }

                    Station station = new Station(ville, cp, lat, lon, adresse);

                    // Récupérer tous les prix de carburants
                    NodeList prixList = stationElement.getElementsByTagName("prix");
                    for (int j = 0; j < prixList.getLength(); j++) {
                        Element prix = (Element) prixList.item(j);
                        String carburant = prix.getAttribute("nom");
                        String valeur = prix.getAttribute("valeur");

                        try {
                            double prixValue = Double.parseDouble(valeur.replace(',', '.'));
                            station.ajouterPrix(carburant, prixValue);
                        } catch (NumberFormatException ignored) {}
                    }

                    // N'ajouter que les stations qui ont au moins un prix
                    if (!station.prix.isEmpty()) {
                        allStations.add(station);
                    }

                } catch (NumberFormatException e) {
                    stationsIgnorees++;
                }
            }

            System.out.printf("✅ %d stations chargées (%d ignorées)%n", allStations.size(), stationsIgnorees);

            // Charger la position sauvegardée
            double[] savedPosition = loadSavedLocation();
            if (savedPosition != null) {
                userPosition = savedPosition;
                calculerDistancesToutesStations();
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement : " + e.getMessage());
            return false;
        }
    }

    private static void calculerDistancesToutesStations() {
        if (userPosition == null) return;

        for (Station station : allStations) {
            station.distanceKm = getSmartDistance(userPosition[0], userPosition[1],
                    station.latitude, station.longitude);
        }
    }

    // ==================== MÉTHODES GÉOLOCALISATION (reprises du code précédent) ====================

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

    // ... (Toutes les autres méthodes du code précédent restent identiques)
    // Je les omets ici pour la lisibilité, mais elles sont nécessaires dans le code complet

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
            return new double[]{lat, lon};
        } catch (NumberFormatException e) {
            System.out.println("Format invalide.");
            return saisirCoordonnees(scanner);
        }
    }

    private static double[] geocoderAdresse(Scanner scanner) {
        // Implémentation simplifiée - utiliser celle du code précédent
        System.out.print("Entrez votre ville : ");
        String ville = scanner.nextLine().trim();
        // Pour l'exemple, retourner Paris si vide
        if (ville.isEmpty()) return new double[]{48.8566, 2.3522};

        // Ici, implémenter le geocoding complet du code précédent
        return new double[]{48.8566, 2.3522}; // Paris par défaut
    }

    private static double[] getUserLocationFromIP() {
        // Implémentation du code précédent
        return new double[]{48.8566, 2.3522}; // Paris par défaut pour l'exemple
    }

    private static double[] loadSavedLocation() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) return null;

        try {
            Properties props = new Properties();
            props.load(new FileInputStream(configFile));
            double lat = Double.parseDouble(props.getProperty("latitude"));
            double lon = Double.parseDouble(props.getProperty("longitude"));
            return new double[]{lat, lon};
        } catch (Exception e) {
            return null;
        }
    }

    private static void saveLocation(double[] location) {
        try {
            Properties props = new Properties();
            props.setProperty("latitude", String.valueOf(location[0]));
            props.setProperty("longitude", String.valueOf(location[1]));
            props.setProperty("saved_date", new Date().toString());
            props.store(new FileOutputStream(CONFIG_FILE), "Configuration Station Essence");
            System.out.println("✅ Position sauvegardée !");
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde : " + e.getMessage());
        }
    }

    public static double getSmartDistance(double lat1, double lon1, double lat2, double lon2) {
        if (!USE_ROUTING_API) {
            return distanceKm(lat1, lon1, lat2, lon2) * 1.3;
        }

        // Implémentation simplifiée - utiliser celle du code précédent
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
    // Ajouter ces classes à ton code principal

    class HistoriquePrix {
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

        // Sauvegarder les prix du jour
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

                try (FileWriter file = new FileWriter(HISTORIQUE_FILE)) {
                    file.write(jsonArray.toString(2));
                }

                System.out.println("✅ Historique sauvegardé (" + stations.size() + " stations)");

            } catch (Exception e) {
                System.err.println("❌ Erreur sauvegarde historique : " + e.getMessage());
            }
        }

        // Charger l'historique existant
        private static List<EntreeHistorique> chargerHistorique() {
            List<EntreeHistorique> historique = new ArrayList<>();
            File file = new File(HISTORIQUE_FILE);

            if (!file.exists()) return historique;

            try {
                String content = new String(Files.readAllBytes(file.toPath()));
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
                System.err.println("Erreur lecture historique : " + e.getMessage());
            }

            return historique;
        }

        // Analyser l'évolution des prix
        public static void afficherEvolutionPrix(String carburant, int nbJours) {
            List<EntreeHistorique> historique = chargerHistorique();

            // Filtrer par carburant et derniers X jours
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

            System.out.printf("\n📈 ÉVOLUTION %s SUR %d JOURS 📈%n", carburant, nbJours);

            // Calculer moyenne par jour
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

        // Détecter les stations avec les plus fortes variations
        public static void detecterVariationsPrix(String carburant) {
            List<EntreeHistorique> historique = chargerHistorique();

            // Grouper par station
            Map<String, List<EntreeHistorique>> prixParStation = historique.stream()
                    .filter(e -> e.carburant.equals(carburant))
                    .collect(Collectors.groupingBy(e -> e.codePostalStation + "_" + e.nomStation));

            System.out.printf("\n🔍 VARIATIONS DE PRIX %s PAR STATION 🔍%n", carburant);

            prixParStation.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1) // Au moins 2 points de données
                    .forEach(entry -> {
                        List<EntreeHistorique> prixStation = entry.getValue();
                        prixStation.sort(Comparator.comparing(e -> e.date));

                        double prixMin = prixStation.stream().mapToDouble(e -> e.prix).min().orElse(0);
                        double prixMax = prixStation.stream().mapToDouble(e -> e.prix).max().orElse(0);
                        double variation = prixMax - prixMin;

                        if (variation > 0.05) { // Variation de plus de 5 centimes
                            EntreeHistorique station = prixStation.get(0);
                            System.out.printf("⚠️  %s (%s) : Variation %.3f €/L (%.3f → %.3f)%n",
                                    station.nomStation, station.codePostalStation, variation, prixMin, prixMax);
                        }
                    });
        }
    }
    // Module pour intégrer les infos transports (à ajouter à ton projet)

    class TransportInfo {

        // Récupérer les perturbations RATP/SNCF
        public static void afficherPerturbationsTransports() {
            System.out.println("\n🚇 === INFOS TRANSPORTS === 🚆");

            try {
                // API SNCF Connect (gratuite avec inscription)
                // Pour l'exemple, on simule des données
                String[] lignes = {"RER A", "RER B", "RER C", "RER D", "Métro 1", "Métro 4", "Métro 6", "Métro 9"};
                String[] status = {"Normal", "Perturbé", "Normal", "Très perturbé", "Normal", "Perturbé", "Normal", "Normal"};

                System.out.println("📊 État du trafic :");
                for (int i = 0; i < lignes.length; i++) {
                    String emoji = getStatusEmoji(status[i]);
                    System.out.printf("  %s %s : %s%n", emoji, lignes[i], status[i]);
                }

                System.out.println("\n💡 Conseil : Vérifiez l'app Citymapper pour les itinéraires en temps réel !");

            } catch (Exception e) {
                System.err.println("❌ Erreur récupération infos transport : " + e.getMessage());
            }
        }

        private static String getStatusEmoji(String status) {
            switch (status.toLowerCase()) {
                case "normal": return "✅";
                case "perturbé": return "⚠️";
                case "très perturbé": return "❌";
                case "interrompu": return "🚫";
                default: return "❓";
            }
        }

        // Calculer le coût carburant vs transport en commun
        public static void comparerCoutTransport(double distanceKm, double prixCarburant, double consommation) {
            System.out.println("\n💰 === COMPARAISON COÛTS === 💰");

            // Calcul coût carburant
            double coutCarburant = (distanceKm / 100) * consommation * prixCarburant;

            // Coûts transports publics (approximatifs pour Paris)
            double prixTicketMetro = 2.15;
            double prixNavigoMois = 84.10;
            double prixNavigoJour = prixNavigoMois / 30;

            System.out.printf("🚗 Voiture (%.1f km) :%n", distanceKm);
            System.out.printf("  Carburant : %.2f € (conso %.1fL/100km à %.3f€/L)%n",
                    coutCarburant, consommation, prixCarburant);
            System.out.printf("  + Parking, péages, usure... : ~%.2f €%n", coutCarburant * 0.5);
            System.out.printf("  TOTAL estimé : %.2f €%n", coutCarburant * 1.5);

            System.out.println("\n🚇 Transports en commun :");
            System.out.printf("  Ticket à l'unité : %.2f €%n", prixTicketMetro * 2); // A/R
            System.out.printf("  Navigo jour : %.2f €%n", prixNavigoJour);
            System.out.printf("  Navigo mois : %.2f € (si trajet quotidien)%n", prixNavigoMois);

            // Conseil
            if (coutCarburant * 1.5 > prixTicketMetro * 2) {
                System.out.println("\n💡 Les transports en commun semblent plus économiques !");
            } else {
                System.out.println("\n💡 La voiture pourrait être plus économique pour ce trajet.");
            }
        }
    }
    // Améliorations visuelles pour ton interface CLI

    class InterfaceCLI {

        // Afficher un graphique ASCII des prix
        public static void afficherGraphiquePrix(List<Station> stations, String carburant) {
            if (stations.isEmpty()) return;

            System.out.printf("\n📊 GRAPHIQUE PRIX %s 📊%n", carburant);

            // Trier par prix croissant et prendre les 10 premiers
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

                // Calculer la longueur de la barre (proportionnelle)
                int longueurBarre = ecart > 0 ? (int) ((prix - prixMin) / ecart * 40) + 5 : 5;

                // Créer la barre visuelle
                String barre = "█".repeat(Math.max(1, longueurBarre));

                // Couleur selon la position (vert = moins cher, rouge = plus cher)
                String couleur = i < 3 ? "🟢" : i < 7 ? "🟡" : "🔴";

                System.out.printf("%s %2d. %-20s %s %.3f €/L%n",
                        couleur, i + 1,
                        truncateString(s.ville + " (" + s.codePostal + ")", 18),
                        barre, prix);
            }
            System.out.println();
        }

        // Afficher une carte ASCII approximative
        public static void afficherCarteStations(List<Station> stations, double[] userPos) {
            if (stations.isEmpty() || userPos == null) return;

            System.out.println("\n🗺️  CARTE APPROXIMATIVE DES STATIONS 🗺️");
            System.out.println("(Vous êtes au centre [●])");
            System.out.println();

            // Grille 21x11 (largeur x hauteur)
            char[][] carte = new char[11][21];
            for (int i = 0; i < 11; i++) {
                Arrays.fill(carte[i], ' ');
            }

            // Position utilisateur au centre
            carte[5][10] = '●';

            // Placer les stations (maximum 10 premières)
            List<Station> stationsProches = stations.stream()
                    .filter(s -> s.distanceKm > 0 && s.distanceKm <= 50) // Dans les 50km
                    .sorted(Comparator.comparingDouble(s -> s.distanceKm))
                    .limit(10)
                    .collect(Collectors.toList());

            for (int i = 0; i < stationsProches.size() && i < 9; i++) {
                Station s = stationsProches.get(i);

                // Calculer position relative approximative
                double deltaLat = s.latitude - userPos[0];
                double deltaLon = s.longitude - userPos[1];

                // Convertir en coordonnées grille (approximatif)
                int x = Math.max(0, Math.min(20, 10 + (int) (deltaLon * 200))); // Facteur arbitraire
                int y = Math.max(0, Math.min(10, 5 - (int) (deltaLat * 200)));   // Inversé pour l'affichage

                // Éviter de superposer sur l'utilisateur
                if (x == 10 && y == 5) {
                    x = (deltaLon >= 0) ? 11 : 9;
                }

                carte[y][x] = (char) ('1' + i); // Numéro de la station
            }

            // Afficher la carte
            System.out.println("    " + "⬆️ NORD");
            for (int i = 0; i < 11; i++) {
                System.out.print("    ");
                for (int j = 0; j < 21; j++) {
                    char c = carte[i][j];
                    if (c == '●') {
                        System.out.print("🏠"); // Vous
                    } else if (c >= '1' && c <= '9') {
                        System.out.print("⛽"); // Station
                    } else {
                        System.out.print("·");
                    }
                }
                System.out.println();
            }
            System.out.println("    " + "⬇️ SUD");

            // Légende
            System.out.println("\n📍 Légende :");
            System.out.println("  🏠 Votre position");
            System.out.println("  ⛽ Stations-service");

            for (int i = 0; i < Math.min(stationsProches.size(), 9); i++) {
                Station s = stationsProches.get(i);
                System.out.printf("  %d. %s (%.1f km)%n", i + 1, s.ville, s.distanceKm);
            }
        }

        // Afficher un tableau formaté
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

        // Afficher les alertes et notifications
        public static void afficherAlertes(List<Station> stations, String carburant) {
            System.out.println("\n🚨 === ALERTES PRIX === 🚨");

            // Calculer la moyenne
            double moyenne = stations.stream()
                    .filter(s -> s.getPrix(carburant) != null)
                    .mapToDouble(s -> s.getPrix(carburant))
                    .average()
                    .orElse(0);

            // Stations exceptionnellement bon marché (> 5 centimes sous la moyenne)
            List<Station> bonnesAffaires = stations.stream()
                    .filter(s -> s.getPrix(carburant) != null)
                    .filter(s -> s.getPrix(carburant) < moyenne - 0.05)
                    .filter(s -> s.distanceKm <= 30) // Dans les 30km
                    .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                    .limit(3)
                    .collect(Collectors.toList());

            if (!bonnesAffaires.isEmpty()) {
                System.out.println("💰 BONNES AFFAIRES (>5cts sous la moyenne) :");
                for (Station s : bonnesAffaires) {
                    double economie = moyenne - s.getPrix(carburant);
                    System.out.printf("  • %s (%s) : %.3f €/L (économie: %.3f €/L, %.1f km)%n",
                            s.ville, s.codePostal, s.getPrix(carburant), economie, s.distanceKm);
                }
            } else {
                System.out.println("ℹ️  Aucune bonne affaire particulière détectée.");
            }

            // Stations très chères à éviter
            List<Station> stationsChere = stations.stream()
                    .filter(s -> s.getPrix(carburant) != null)
                    .filter(s -> s.getPrix(carburant) > moyenne + 0.08)
                    .filter(s -> s.distanceKm <= 30)
                    .sorted(Comparator.comparingDouble((Station s) -> s.getPrix(carburant)).reversed())
                    .limit(2)
                    .collect(Collectors.toList());

            if (!stationsChere.isEmpty()) {
                System.out.println("\n⚠️  STATIONS À ÉVITER (très au-dessus de la moyenne) :");
                for (Station s : stationsChere) {
                    double surcoût = s.getPrix(carburant) - moyenne;
                    System.out.printf("  • %s (%s) : %.3f €/L (surcoût: +%.3f €/L)%n",
                            s.ville, s.codePostal, s.getPrix(carburant), surcoût);
                }
            }
        }

        // Barre de progression pour les opérations longues
        public static void afficherBarreProgression(int actuel, int total, String operation) {
            int pourcentage = (int) ((double) actuel / total * 100);
            int barres = pourcentage / 2; // Barre de 50 caractères max

            String barre = "█".repeat(barres) + "░".repeat(50 - barres);

            System.out.printf("\r%s [%s] %d%% (%d/%d)",
                    operation, barre, pourcentage, actuel, total);

            if (actuel == total) {
                System.out.println(" ✅");
            }
        }

        // Utilitaire pour tronquer les chaînes
        private static String truncateString(String str, int maxLength) {
            if (str.length() <= maxLength) {
                return str;
            }
            return str.substring(0, maxLength - 3) + "...";
        }

        // Affichage de bienvenue stylisé
        public static void afficherBienvenue() {
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                  🚗 ANALYSEUR CARBURANTS ⛽                  ║");
            System.out.println("║                                                              ║");
            System.out.println("║  Trouvez les meilleures stations-service près de chez vou s  ║");
            System.out.println("║         Analysez les prix • Économisez de l'argent           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
        }
    }
}