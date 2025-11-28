package fr.clickdroit.api.config;

import fr.clickdroit.api.repository.*;
import fr.clickdroit.api.services.*;
import fr.clickdroit.api.ui.UserInterface;

/**
 * Dependency injection container for the application.
 * Manages the creation and wiring of all application components.
 * Provides a centralized location for dependency configuration.
 */
public class ApplicationContext {

    // Repositories
    private final StationRepository stationRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final LocationConfigRepository locationConfigRepository;

    // Services
    private final IStationService stationService;
    private final ILocationService locationService;
    private final IWeatherService weatherService;
    private final ITransportService transportService;
    private final IHistoriquePrixService historiquePrixService;

    // UI
    private final UserInterface userInterface;

    /**
     * Creates a new application context with default implementations.
     */
    public ApplicationContext() {
        // Create repositories
        this.stationRepository = new XmlStationRepository();
        this.priceHistoryRepository = new JsonPriceHistoryRepository();
        this.locationConfigRepository = new PropertiesLocationConfigRepository();

        // Create services with injected dependencies
        this.stationService = new StationService();
        this.locationService = new LocationService(locationConfigRepository);
        this.weatherService = new WeatherService();
        this.transportService = new TransportService();
        this.historiquePrixService = new HistoriquePrixService(priceHistoryRepository);

        // Create UI
        this.userInterface = new UserInterface();
    }

    /**
     * Constructor for custom implementations (useful for testing).
     *
     * @param stationRepository Station repository implementation
     * @param priceHistoryRepository Price history repository implementation
     * @param locationConfigRepository Location config repository implementation
     * @param stationService Station service implementation
     * @param locationService Location service implementation
     * @param weatherService Weather service implementation
     * @param transportService Transport service implementation
     * @param historiquePrixService Price history service implementation
     * @param userInterface User interface implementation
     */
    public ApplicationContext(
            StationRepository stationRepository,
            PriceHistoryRepository priceHistoryRepository,
            LocationConfigRepository locationConfigRepository,
            IStationService stationService,
            ILocationService locationService,
            IWeatherService weatherService,
            ITransportService transportService,
            IHistoriquePrixService historiquePrixService,
            UserInterface userInterface) {
        this.stationRepository = stationRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.locationConfigRepository = locationConfigRepository;
        this.stationService = stationService;
        this.locationService = locationService;
        this.weatherService = weatherService;
        this.transportService = transportService;
        this.historiquePrixService = historiquePrixService;
        this.userInterface = userInterface;
    }

    // Getters for repositories
    public StationRepository getStationRepository() {
        return stationRepository;
    }

    public PriceHistoryRepository getPriceHistoryRepository() {
        return priceHistoryRepository;
    }

    public LocationConfigRepository getLocationConfigRepository() {
        return locationConfigRepository;
    }

    // Getters for services
    public IStationService getStationService() {
        return stationService;
    }

    public ILocationService getLocationService() {
        return locationService;
    }

    public IWeatherService getWeatherService() {
        return weatherService;
    }

    public ITransportService getTransportService() {
        return transportService;
    }

    public IHistoriquePrixService getHistoriquePrixService() {
        return historiquePrixService;
    }

    // Getter for UI
    public UserInterface getUserInterface() {
        return userInterface;
    }
}
