package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;

import java.util.List;
import java.util.Scanner;

/**
 * Interface for location-related operations.
 */
public interface ILocationService {

    /**
     * Gets user location with configuration support.
     *
     * @param scanner Scanner for user input
     * @return Array of [latitude, longitude]
     */
    double[] getUserLocationWithConfig(Scanner scanner);

    /**
     * Gets user location through various methods.
     *
     * @param scanner Scanner for user input
     * @return Array of [latitude, longitude]
     */
    double[] getUserLocation(Scanner scanner);

    /**
     * Loads saved location from configuration.
     *
     * @return Array of [latitude, longitude] or null
     */
    double[] loadSavedLocation();

    /**
     * Saves location to configuration.
     *
     * @param location Array of [latitude, longitude]
     */
    void saveLocation(double[] location);

    /**
     * Calculates distances from user position to all stations.
     *
     * @param stations List of stations
     * @param userPosition User's position [latitude, longitude]
     */
    void calculerDistancesToutesStations(List<Station> stations, double[] userPosition);

    /**
     * Gets smart distance between two points.
     *
     * @param lat1 Starting latitude
     * @param lon1 Starting longitude
     * @param lat2 Ending latitude
     * @param lon2 Ending longitude
     * @return Distance in kilometers
     */
    double getSmartDistance(double lat1, double lon1, double lat2, double lon2);
}
