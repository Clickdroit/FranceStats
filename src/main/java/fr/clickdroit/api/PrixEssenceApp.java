package fr.clickdroit.api;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.services.*;
import fr.clickdroit.api.ui.UserInterface;

import java.util.List;
import java.util.Scanner;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PrixEssenceApp {

    private final DataLoader dataLoader;
    private final LocationService locationService;
    private final StationService stationService;
    private final UserInterface ui;
    private final WeatherService weatherService;
    private final TransportService transportService;

    private List<Station> allStations;
    private double[] userPosition;

    public PrixEssenceApp() {
        this.dataLoader = new DataLoader();
        this.locationService = new LocationService();
        this.stationService = new StationService();
        this.ui = new UserInterface();
        this.weatherService = new WeatherService();
        this.transportService = new TransportService();
    }

    public static void main(String[] args) {
        PrixEssenceApp app = new PrixEssenceApp();
        app.run();
    }

    public void run() {
        ui.afficherBienvenue();

        try {
            System.out.println("🚗 === ANALYSEUR PRIX CARBURANTS === ⛽");

            if (!chargerDonnees()) {
                ui.afficherErreur("Impossible de charger les données. Fin du programme.");
                return;
            }

            Scanner scanner = new Scanner(System.in);
            boolean continuer = true;

            while (continuer) {
                ui.afficherMenuPrincipal();
                int choix = ui.lireChoixMenu();

                if (choix != -1) {
                    continuer = traiterChoixMenu(choix, scanner);
                }

                if (continuer) {
                    ui.afficherSeparateur();
                }
            }

        } catch (Exception e) {
            ui.afficherErreur("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        } finally {
            ui.fermer();
        }
    }

    private boolean chargerDonnees() {
        try {
            allStations = dataLoader.chargerDonnees();

            // Charger la position sauvegardée si elle existe
            double[] savedPosition = locationService.loadSavedLocation();
            if (savedPosition != null) {
                userPosition = savedPosition;
                locationService.calculerDistancesToutesStations(allStations, userPosition);
            }

            return true;
        } catch (DataLoader.DataLoadException e) {
            ui.afficherErreur("Erreur lors du chargement : " + e.getMessage());
            return false;
        }
    }

    private boolean traiterChoixMenu(int choix, Scanner scanner) {
        switch (choix) {
            case 1:
                rechercherStationsProches();
                break;
            case 2:
                afficherStatistiquesNationales();
                break;
            case 3:
                rechercherParDepartement();
                break;
            case 4:
                configurerPosition();
                break;
            case 5:
                afficherMeteo();
                break;
            case 6:
                gererHistoriquePrix();
                break;
            case 7:
                configurerParametres();
                break;
            case 8:
                transportService.afficherPerturbationsTransports();
                break;
            case 9:
                actualiserDonnees();
                break;
            case 0:
                ui.afficherMessage("👋 À bientôt !");
                return false;
            default:
                ui.afficherErreur("Choix invalide.");
        }
        return true;
    }

    private void rechercherStationsProches() {
        if (userPosition == null) {
            ui.afficherMessage("❌ Position non configurée. Configurez d'abord votre position.");
            configurerPosition();
            if (userPosition == null) return;
        }

        String carburant = ui.choisirCarburant(allStations);
        if (carburant == null) return;

        double rayon = ui.lireRayon(20.0);

        List<Station> stationsProches = stationService.rechercherStationsProches(
                allStations, carburant, rayon, 10);

        ui.afficherStationsProches(stationsProches, carburant, rayon, userPosition);
    }

    private void afficherStatistiquesNationales() {
        String carburant = ui.choisirCarburant(allStations);
        if (carburant == null) return;

        StationService.StatistiquesNationales stats =
                stationService.calculerStatistiques(allStations, carburant);

        ui.afficherStatistiques(stats, allStations);
    }

    private void rechercherParDepartement() {
        String dept = ui.lireDepartement();

        if (dept.length() < 2) {
            ui.afficherErreur("Numéro de département invalide.");
            return;
        }

        String carburant = ui.choisirCarburant(allStations);
        if (carburant == null) return;

        List<Station> stationsDept = stationService.rechercherParDepartement(
                allStations, dept, carburant);

        if (stationsDept.isEmpty()) {
            ui.afficherMessage(String.format(
                    "❌ Aucune station trouvée dans le département %s avec %s.", dept, carburant));
            return;
        }

        StationService.StatistiquesNationales statsDept =
                stationService.calculerStatistiques(stationsDept, carburant);

        List<Station> top5Dept = stationsDept.stream()
                .sorted(Comparator.comparingDouble(s -> s.getPrix(carburant)))
                .limit(5)
                .collect(Collectors.toList());

        ui.afficherStatistiquesDepartement(dept, carburant, statsDept, top5Dept);
    }

    private void configurerPosition() {
        userPosition = locationService.getUserLocationWithConfig(new Scanner(System.in));
        if (userPosition != null) {
            ui.afficherMessage("🔄 Recalcul des distances...");
            locationService.calculerDistancesToutesStations(allStations, userPosition);
            ui.afficherSucces("Position configurée et distances calculées !");
        }
    }

    private void afficherMeteo() {
        String ville = ui.lireVille("Paris");
        weatherService.afficherMeteo(ville);
    }

    private void gererHistoriquePrix() {
        // Sauvegarder les prix du jour
        HistoriquePrixService.sauvegarderPrixDuJour(allStations);

        String carburant = ui.choisirCarburant(allStations);
        if (carburant != null) {
            HistoriquePrixService.afficherEvolutionPrix(carburant, 7);
        }
    }

    private void configurerParametres() {
        ui.afficherMessage("\n⚙️ === PARAMÈTRES ===");
        ui.afficherMessage("1. Calcul de distance : " +
                (LocationService.isUseRoutingApi() ? "Par la route (précis)" : "Vol d'oiseau × 1.3 (rapide)"));

        if (ui.confirmerAction("Changer le mode de calcul de distance ?")) {
            LocationService.setUseRoutingApi(!LocationService.isUseRoutingApi());
            ui.afficherSucces("Mode changé vers : " +
                    (LocationService.isUseRoutingApi() ? "Par la route (précis)" : "Vol d'oiseau × 1.3 (rapide)"));

            if (userPosition != null && ui.confirmerAction("Recalculer les distances avec le nouveau mode ?")) {
                ui.afficherMessage("🔄 Recalcul en cours...");
                locationService.calculerDistancesToutesStations(allStations, userPosition);
                ui.afficherSucces("Distances recalculées !");
            }
        }
    }

    private void actualiserDonnees() {
        ui.afficherMessage("🔄 Actualisation des données...");
        dataLoader.forceRefreshData();
        if (chargerDonnees()) {
            ui.afficherSucces("Données actualisées !");
        } else {
            ui.afficherErreur("Échec de l'actualisation");
        }
    }
}