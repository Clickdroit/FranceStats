package fr.clickdroit.api;

import fr.clickdroit.api.config.ApplicationContext;
import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.models.StatistiquesNationales;
import fr.clickdroit.api.repository.StationRepository;
import fr.clickdroit.api.services.*;
import fr.clickdroit.api.ui.UserInterface;

import java.util.List;
import java.util.Scanner;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Main application class for the fuel price analyzer.
 * Uses dependency injection for all services and repositories.
 */
public class PrixEssenceApp {

    private final StationRepository stationRepository;
    private final ILocationService locationService;
    private final IStationService stationService;
    private final UserInterface ui;
    private final IWeatherService weatherService;
    private final ITransportService transportService;
    private final IHistoriquePrixService historiquePrixService;

    private List<Station> allStations;
    private double[] userPosition;

    /**
     * Constructor with dependency injection.
     *
     * @param stationRepository Repository for station data
     * @param locationService Service for location operations
     * @param stationService Service for station operations
     * @param ui User interface
     * @param weatherService Service for weather operations
     * @param transportService Service for transport operations
     * @param historiquePrixService Service for price history operations
     */
    public PrixEssenceApp(
            StationRepository stationRepository,
            ILocationService locationService,
            IStationService stationService,
            UserInterface ui,
            IWeatherService weatherService,
            ITransportService transportService,
            IHistoriquePrixService historiquePrixService) {
        this.stationRepository = stationRepository;
        this.locationService = locationService;
        this.stationService = stationService;
        this.ui = ui;
        this.weatherService = weatherService;
        this.transportService = transportService;
        this.historiquePrixService = historiquePrixService;
    }

    /**
     * Default constructor using ApplicationContext for dependency injection.
     */
    public PrixEssenceApp() {
        ApplicationContext context = new ApplicationContext();
        this.stationRepository = context.getStationRepository();
        this.locationService = context.getLocationService();
        this.stationService = context.getStationService();
        this.ui = context.getUserInterface();
        this.weatherService = context.getWeatherService();
        this.transportService = context.getTransportService();
        this.historiquePrixService = context.getHistoriquePrixService();
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
            allStations = stationRepository.findAll();

            // Charger la position sauvegardée si elle existe
            double[] savedPosition = locationService.loadSavedLocation();
            if (savedPosition != null) {
                userPosition = savedPosition;
                locationService.calculerDistancesToutesStations(allStations, userPosition);
            }

            return true;
        } catch (StationRepository.DataAccessException e) {
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

        StatistiquesNationales stats =
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

        StatistiquesNationales statsDept =
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
        historiquePrixService.saveTodayPrices(allStations);

        String carburant = ui.choisirCarburant(allStations);
        if (carburant != null) {
            historiquePrixService.displayPriceEvolution(carburant, 7);
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
        stationRepository.refreshData();
        if (chargerDonnees()) {
            ui.afficherSucces("Données actualisées !");
        } else {
            ui.afficherErreur("Échec de l'actualisation");
        }
    }
}