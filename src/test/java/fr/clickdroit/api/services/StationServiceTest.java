package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.models.StatistiquesNationales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StationService.
 * Tests the business logic for station operations.
 */
class StationServiceTest {

    private IStationService stationService;
    private List<Station> testStations;

    @BeforeEach
    void setUp() {
        stationService = new StationService();
        testStations = createTestStations();
    }

    private List<Station> createTestStations() {
        List<Station> stations = new ArrayList<>();

        Station station1 = new Station("Paris", "75001", 48.8566, 2.3522, "1 Rue de Rivoli");
        station1.ajouterPrix("SP95", 1.75);
        station1.ajouterPrix("Gazole", 1.55);
        station1.setDistanceKm(5.0);
        stations.add(station1);

        Station station2 = new Station("Lyon", "69001", 45.7640, 4.8357, "1 Place Bellecour");
        station2.ajouterPrix("SP95", 1.70);
        station2.ajouterPrix("Gazole", 1.50);
        station2.setDistanceKm(10.0);
        stations.add(station2);

        Station station3 = new Station("Marseille", "13001", 43.2965, 5.3698, "1 Rue de la République");
        station3.ajouterPrix("SP95", 1.80);
        station3.ajouterPrix("E85", 0.85);
        station3.setDistanceKm(15.0);
        stations.add(station3);

        return stations;
    }

    @Test
    @DisplayName("Should find nearby stations sorted by price")
    void rechercherStationsProches_shouldReturnSortedByPrice() {
        List<Station> result = stationService.rechercherStationsProches(testStations, "SP95", 20.0, 10);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Lyon", result.get(0).getVille()); // Cheapest
        assertEquals("Paris", result.get(1).getVille());
        assertEquals("Marseille", result.get(2).getVille()); // Most expensive
    }

    @Test
    @DisplayName("Should filter stations by radius")
    void rechercherStationsProches_shouldFilterByRadius() {
        List<Station> result = stationService.rechercherStationsProches(testStations, "SP95", 12.0, 10);

        assertNotNull(result);
        assertEquals(2, result.size()); // Only Paris and Lyon within 12km
    }

    @Test
    @DisplayName("Should filter stations without requested fuel")
    void rechercherStationsProches_shouldFilterByFuel() {
        List<Station> result = stationService.rechercherStationsProches(testStations, "E85", 20.0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Marseille", result.get(0).getVille());
    }

    @Test
    @DisplayName("Should respect limit parameter")
    void rechercherStationsProches_shouldRespectLimit() {
        List<Station> result = stationService.rechercherStationsProches(testStations, "SP95", 20.0, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should return empty list for null stations")
    void rechercherStationsProches_shouldReturnEmptyForNullStations() {
        List<Station> result = stationService.rechercherStationsProches(null, "SP95", 20.0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for invalid fuel type")
    void rechercherStationsProches_shouldThrowForInvalidFuel() {
        assertThrows(IllegalArgumentException.class, () -> 
            stationService.rechercherStationsProches(testStations, "", 20.0, 10));
    }

    @Test
    @DisplayName("Should search by department")
    void rechercherParDepartement_shouldFilterByDepartment() {
        List<Station> result = stationService.rechercherParDepartement(testStations, "75", "SP95");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Paris", result.get(0).getVille());
    }

    @Test
    @DisplayName("Should get available fuel types")
    void getCarburantsDisponibles_shouldReturnAllFuelTypes() {
        Set<String> result = stationService.getCarburantsDisponibles(testStations);

        assertNotNull(result);
        assertTrue(result.contains("SP95"));
        assertTrue(result.contains("Gazole"));
        assertTrue(result.contains("E85"));
    }

    @Test
    @DisplayName("Should calculate statistics correctly")
    void calculerStatistiques_shouldCalculateCorrectly() {
        StatistiquesNationales stats = 
            stationService.calculerStatistiques(testStations, "SP95");

        assertNotNull(stats);
        assertEquals(3, stats.getNombreStations());
        assertEquals(1.70, stats.getMinimum(), 0.01);
        assertEquals(1.80, stats.getMaximum(), 0.01);
    }

    @Test
    @DisplayName("Should generate Google Maps URL correctly")
    void genererGoogleMapsUrl_shouldGenerateCorrectUrl() {
        String url = stationService.genererGoogleMapsUrl(48.8566, 2.3522, 45.7640, 4.8357);

        assertNotNull(url);
        assertTrue(url.startsWith("https://www.google.com/maps/dir/"));
        assertTrue(url.contains("48.856600"));
        assertTrue(url.contains("2.352200"));
    }

    @Test
    @DisplayName("Should parse radius with default value")
    void parseRayonAvecDefaut_shouldReturnDefaultForEmpty() {
        double result = stationService.parseRayonAvecDefaut("", 20.0);
        assertEquals(20.0, result, 0.01);
    }

    @Test
    @DisplayName("Should parse valid radius")
    void parseRayonAvecDefaut_shouldParseValidRadius() {
        double result = stationService.parseRayonAvecDefaut("15.5", 20.0);
        assertEquals(15.5, result, 0.01);
    }
}
