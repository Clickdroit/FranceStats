package fr.clickdroit.api.repository;

import fr.clickdroit.api.models.Station;

import java.util.List;

/**
 * Interface for Station data access operations.
 * Separates data access concerns from business logic.
 */
public interface StationRepository {

    /**
     * Loads all stations from the data source.
     *
     * @return List of all stations
     * @throws DataAccessException if data cannot be loaded
     */
    List<Station> findAll() throws DataAccessException;

    /**
     * Forces a refresh of the data from the remote source.
     */
    void refreshData();

    /**
     * Exception for data access errors.
     */
    class DataAccessException extends Exception {
        public DataAccessException(String message) {
            super(message);
        }

        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
