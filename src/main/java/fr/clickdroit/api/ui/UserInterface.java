package fr.clickdroit.api.ui;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.services.StationService;

import java.util.*;
import java.util.stream.Collectors;

public class UserInterface {
    private final Scanner scanner;

    public UserInterface() {
        this.scanner = new Scanner(System.in);
    }

    public void afficherBienvenue() {
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

    public void afficherMenuPrincipal() {
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

    public int lireChoixMenu() {
        System.out.print("Votre choix : ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("❌ Choix invalide. Entrez un nombre entre 0 et 9.");
            return -1; // Valeur invalide
        }
    }

    public String choisirCarburant(List<Station> stations) {
        Set<String> carburantsDisponibles = stations.stream()
                .flatMap(s -> s.getPrix().keySet().stream())
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

    public String lireDepartement() {
        System.out.print("Entrez un numéro de département (ex: 75, 69, 13) : ");
        return scanner.nextLine().trim();
    }

    public double lireRayon(double defaut) {
        System.out.print("Rayon de recherche en km (défaut: " + defaut + ") : ");
        String rayonStr = scanner.nextLine().trim();

        if (rayonStr.isEmpty()) {
            return defaut;
        }

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

    public void afficherStationsProches(List<Station> stations, String carburant, double rayon, double[] userPosition) {
        if (stations.isEmpty()) {
            System.out.printf("❌ Aucune station avec %s trouvée dans un rayon de %.0f km.%n", carburant, rayon);
            return;
        }

        System.out.printf("\n🎯 TOP %d STATIONS %s DANS UN RAYON DE %.0f KM :\n",
                Math.min(stations.size(), 10), carburant, rayon);

        for (int i = 0; i < Math.min(stations.size(), 10); i++) {
            Station s = stations.get(i);
            System.out.printf("%d. %s (%s) - %s: %.3f €/L - %.2f km%n   📍 %s%n",
                    i + 1, s.getVille(), s.getCodePostal(), carburant,
                    s.getPrix(carburant), s.getDistanceKm(), s.getAdresse());

            if (i < 3 && userPosition != null) {
                String googleMapsUrl = String.format("https://www.google.com/maps/dir/%.6f,%.6f/%.6f,%.6f",
                        userPosition[0], userPosition[1], s.getLatitude(), s.getLongitude());
                System.out.printf("   🗺️ Itinéraire: %s%n", googleMapsUrl);
            }
            System.out.println();
        }
    }

    public void afficherStatistiques(StationService.StatistiquesNationales stats, List<Station> stations) {
        System.out.println("\n" + stats);

        List<Station> stationsPasCher = stations.stream()
                .filter(s -> s.getPrix(stats.getCarburant()) != null)
                .filter(s -> s.getPrix(stats.getCarburant()).equals(stats.getMinimum()))
                .limit(5)
                .collect(Collectors.toList());

        if (!stationsPasCher.isEmpty()) {
            System.out.println("\n🏆 Stations au prix minimum :");
            for (Station s : stationsPasCher) {
                System.out.printf("  • %s (%s) - %s%n", s.getVille(), s.getCodePostal(), s.getAdresse());
            }
        }
    }

    public void afficherStatistiquesDepartement(String departement, String carburant,
                                                StationService.StatistiquesNationales stats,
                                                List<Station> top5) {
        System.out.printf("\n📊 DÉPARTEMENT %s - %s 📊%n", departement, carburant);
        System.out.printf("  Moyenne : %.3f €/L%n", stats.getMoyenne());
        System.out.printf("  Prix minimum : %.3f €/L (à %s)%n", stats.getMinimum(), stats.getVilleMin());
        System.out.printf("  Prix maximum : %.3f €/L (à %s)%n", stats.getMaximum(), stats.getVilleMax());
        System.out.printf("  Nombre de stations : %d%n", stats.getNombreStations());

        System.out.printf("\n🏆 TOP 5 MOINS CHÈRES DU DÉPARTEMENT %s :%n", departement);
        for (int i = 0; i < top5.size(); i++) {
            Station s = top5.get(i);
            System.out.printf("%d. %s (%s) - %.3f €/L%n   📍 %s%n",
                    i + 1, s.getVille(), s.getCodePostal(), s.getPrix(carburant), s.getAdresse());
        }
    }

    public void afficherGraphiquePrix(List<Station> stations, String carburant) {
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
                    truncateString(s.getVille() + " (" + s.getCodePostal() + ")", 18),
                    barre, prix);
        }
        System.out.println();
    }

    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    public String lireVille(String defaut) {
        System.out.print("Ville pour la météo (défaut: " + defaut + ") : ");
        String ville = scanner.nextLine().trim();
        return ville.isEmpty() ? defaut : ville;
    }

    public boolean confirmerAction(String message) {
        System.out.print(message + " (o/n) : ");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("o") || response.equals("oui") || response.equals("y") || response.equals("yes");
    }

    public void afficherMessage(String message) {
        System.out.println(message);
    }

    public void afficherErreur(String erreur) {
        System.err.println("❌ " + erreur);
    }

    public void afficherSucces(String message) {
        System.out.println("✅ " + message);
    }

    public void afficherSeparateur() {
        System.out.println("\n" + "=".repeat(50));
    }

    public void fermer() {
        if (scanner != null) {
            scanner.close();
        }
    }
}