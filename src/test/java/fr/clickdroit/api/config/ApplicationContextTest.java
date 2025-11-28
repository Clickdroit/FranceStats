package fr.clickdroit.api.config;

import fr.clickdroit.api.repository.*;
import fr.clickdroit.api.services.*;
import fr.clickdroit.api.ui.UserInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApplicationContext dependency injection container.
 */
class ApplicationContextTest {

    @Test
    @DisplayName("Should create default context with all components")
    void defaultConstructor_shouldCreateAllComponents() {
        ApplicationContext context = new ApplicationContext();

        assertNotNull(context.getStationRepository());
        assertNotNull(context.getPriceHistoryRepository());
        assertNotNull(context.getLocationConfigRepository());
        assertNotNull(context.getStationService());
        assertNotNull(context.getLocationService());
        assertNotNull(context.getWeatherService());
        assertNotNull(context.getTransportService());
        assertNotNull(context.getHistoriquePrixService());
        assertNotNull(context.getUserInterface());
    }

    @Test
    @DisplayName("Should create repositories with correct implementations")
    void defaultConstructor_shouldCreateCorrectRepositoryImplementations() {
        ApplicationContext context = new ApplicationContext();

        assertTrue(context.getStationRepository() instanceof XmlStationRepository);
        assertTrue(context.getPriceHistoryRepository() instanceof JsonPriceHistoryRepository);
        assertTrue(context.getLocationConfigRepository() instanceof PropertiesLocationConfigRepository);
    }

    @Test
    @DisplayName("Should create services with correct implementations")
    void defaultConstructor_shouldCreateCorrectServiceImplementations() {
        ApplicationContext context = new ApplicationContext();

        assertTrue(context.getStationService() instanceof StationService);
        assertTrue(context.getLocationService() instanceof LocationService);
        assertTrue(context.getWeatherService() instanceof WeatherService);
        assertTrue(context.getTransportService() instanceof TransportService);
        assertTrue(context.getHistoriquePrixService() instanceof HistoriquePrixService);
    }

    @Test
    @DisplayName("Should return interface types for services")
    void getters_shouldReturnInterfaceTypes() {
        ApplicationContext context = new ApplicationContext();

        // Verify return types are interfaces
        IStationService stationService = context.getStationService();
        ILocationService locationService = context.getLocationService();
        IWeatherService weatherService = context.getWeatherService();
        ITransportService transportService = context.getTransportService();
        IHistoriquePrixService historiquePrixService = context.getHistoriquePrixService();

        assertNotNull(stationService);
        assertNotNull(locationService);
        assertNotNull(weatherService);
        assertNotNull(transportService);
        assertNotNull(historiquePrixService);
    }
}
