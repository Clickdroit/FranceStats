package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.repository.LocationConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationService with mocked dependencies.
 */
class LocationServiceTest {

    @Mock
    private LocationConfigRepository locationConfigRepository;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        locationService = new LocationService(locationConfigRepository);
    }

    @Test
    @DisplayName("Should load saved location from repository")
    void loadSavedLocation_shouldDelegateToRepository() {
        double[] expectedLocation = new double[]{48.8566, 2.3522};
        when(locationConfigRepository.loadLocation()).thenReturn(expectedLocation);

        double[] result = locationService.loadSavedLocation();

        assertNotNull(result);
        assertEquals(48.8566, result[0], 0.0001);
        assertEquals(2.3522, result[1], 0.0001);
        verify(locationConfigRepository).loadLocation();
    }

    @Test
    @DisplayName("Should return null when no saved location exists")
    void loadSavedLocation_shouldReturnNullWhenNoSavedLocation() {
        when(locationConfigRepository.loadLocation()).thenReturn(null);

        double[] result = locationService.loadSavedLocation();

        assertNull(result);
        verify(locationConfigRepository).loadLocation();
    }

    @Test
    @DisplayName("Should save location to repository")
    void saveLocation_shouldDelegateToRepository() {
        double[] location = new double[]{48.8566, 2.3522};

        locationService.saveLocation(location);

        verify(locationConfigRepository).saveLocation(location);
    }

    @Test
    @DisplayName("Should calculate distances for all stations")
    void calculerDistancesToutesStations_shouldSetDistanceOnAllStations() {
        List<Station> stations = new ArrayList<>();
        Station station1 = new Station("Paris", "75001", 48.8566, 2.3522, "Test");
        Station station2 = new Station("Lyon", "69001", 45.7640, 4.8357, "Test");
        stations.add(station1);
        stations.add(station2);

        double[] userPosition = new double[]{48.8566, 2.3522};

        locationService.calculerDistancesToutesStations(stations, userPosition);

        assertTrue(station1.getDistanceKm() >= 0);
        assertTrue(station2.getDistanceKm() > 0); // Lyon is far from Paris
    }

    @Test
    @DisplayName("Should not calculate distances when user position is null")
    void calculerDistancesToutesStations_shouldNotCalculateWhenPositionNull() {
        List<Station> stations = new ArrayList<>();
        Station station = new Station("Paris", "75001", 48.8566, 2.3522, "Test");
        stations.add(station);

        locationService.calculerDistancesToutesStations(stations, null);

        assertEquals(0.0, station.getDistanceKm(), 0.01);
    }

    @Test
    @DisplayName("Should calculate smart distance correctly")
    void getSmartDistance_shouldCalculateDistance() {
        double distance = locationService.getSmartDistance(48.8566, 2.3522, 45.7640, 4.8357);

        // Paris to Lyon is approximately 392km as the crow flies
        // With 1.3x factor for road distance
        assertTrue(distance > 300 && distance < 700);
    }

    @Test
    @DisplayName("Should calculate exact distance using Haversine formula")
    void distanceKm_shouldCalculateHaversineDistance() {
        double distance = LocationService.distanceKm(48.8566, 2.3522, 45.7640, 4.8357);

        // Paris to Lyon is approximately 392km as the crow flies
        assertTrue(distance > 380 && distance < 410);
    }

    @Test
    @DisplayName("Should return zero distance for same coordinates")
    void distanceKm_shouldReturnZeroForSameCoordinates() {
        double distance = LocationService.distanceKm(48.8566, 2.3522, 48.8566, 2.3522);

        assertEquals(0.0, distance, 0.001);
    }

    @Test
    @DisplayName("Should manage routing API setting")
    void routingApiSetting_shouldBeManageable() {
        boolean initial = LocationService.isUseRoutingApi();

        LocationService.setUseRoutingApi(!initial);
        assertEquals(!initial, LocationService.isUseRoutingApi());

        // Restore
        LocationService.setUseRoutingApi(initial);
        assertEquals(initial, LocationService.isUseRoutingApi());
    }
}
