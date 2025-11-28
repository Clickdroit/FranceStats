package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;
import fr.clickdroit.api.repository.StationRepository;
import fr.clickdroit.api.repository.XmlStationRepository;

import java.util.List;

/**
 * Legacy DataLoader class that delegates to StationRepository.
 * Maintained for backward compatibility.
 * 
 * @deprecated Use {@link StationRepository} directly with dependency injection.
 */
@Deprecated
public class DataLoader {

    private final StationRepository stationRepository;

    /**
     * Default constructor.
     */
    public DataLoader() {
        this.stationRepository = new XmlStationRepository();
    }

    /**
     * Constructor with dependency injection.
     *
     * @param stationRepository Station repository implementation
     */
    public DataLoader(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    /**
     * Loads station data.
     *
     * @return List of stations
     * @throws DataLoadException if loading fails
     */
    public List<Station> chargerDonnees() throws DataLoadException {
        try {
            return stationRepository.findAll();
        } catch (StationRepository.DataAccessException e) {
            throw new DataLoadException(e.getMessage(), e);
        }
    }

    /**
     * Forces a refresh of the data.
     */
    public void forceRefreshData() {
        stationRepository.refreshData();
    }

    /**
     * Exception for data loading errors.
     */
    public static class DataLoadException extends Exception {
        public DataLoadException(String message) {
            super(message);
        }

        public DataLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}