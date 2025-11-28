package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.models.StatistiquesNationales;

import java.util.List;
import java.util.Set;

/**
 * Interface for station-related business operations.
 */
public interface IStationService {

    /**
     * Searches for nearby stations that have a specific fuel type.
     *
     * @param stations List of all stations
     * @param carburant Fuel type to search for
     * @param rayon Maximum distance in kilometers
     * @param limite Maximum number of results
     * @return List of matching stations sorted by price
     */
    List<Station> rechercherStationsProches(List<Station> stations, String carburant, double rayon, int limite);

    /**
     * Searches for stations in a specific department.
     *
     * @param stations List of all stations
     * @param departement Department code
     * @param carburant Fuel type to search for
     * @return List of matching stations
     */
    List<Station> rechercherParDepartement(List<Station> stations, String departement, String carburant);

    /**
     * Gets all available fuel types from the stations.
     *
     * @param stations List of stations
     * @return Set of available fuel types
     */
    Set<String> getCarburantsDisponibles(List<Station> stations);

    /**
     * Calculates statistics for a specific fuel type.
     *
     * @param stations List of stations
     * @param carburant Fuel type
     * @return Statistics object
     */
    StatistiquesNationales calculerStatistiques(List<Station> stations, String carburant);

    /**
     * Generates a Google Maps URL for directions.
     *
     * @param fromLat Starting latitude
     * @param fromLon Starting longitude
     * @param toLat Destination latitude
     * @param toLon Destination longitude
     * @return Google Maps URL
     */
    String genererGoogleMapsUrl(double fromLat, double fromLon, double toLat, double toLon);

    /**
     * Parses a radius string with a default value.
     *
     * @param rayonStr Radius string
     * @param defaut Default value
     * @return Parsed radius or default
     */
    double parseRayonAvecDefaut(String rayonStr, double defaut);
}
