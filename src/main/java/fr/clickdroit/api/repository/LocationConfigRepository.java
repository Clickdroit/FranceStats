package fr.clickdroit.api.repository;

/**
 * Interface for location configuration data access.
 */
public interface LocationConfigRepository {

    /**
     * Loads saved location coordinates.
     *
     * @return Array of [latitude, longitude] or null if not found
     */
    double[] loadLocation();

    /**
     * Saves location coordinates.
     *
     * @param location Array of [latitude, longitude]
     */
    void saveLocation(double[] location);
}
