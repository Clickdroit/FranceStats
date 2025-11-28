package fr.clickdroit.api.services;

/**
 * Interface for weather-related operations.
 */
public interface IWeatherService {

    /**
     * Displays weather information for a city.
     *
     * @param ville City name
     */
    void afficherMeteo(String ville);
}
