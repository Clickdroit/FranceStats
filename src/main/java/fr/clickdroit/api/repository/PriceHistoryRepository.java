package fr.clickdroit.api.repository;

import java.util.List;

/**
 * Interface for price history data access operations.
 */
public interface PriceHistoryRepository {

    /**
     * Represents a historical price entry.
     */
    record HistoryEntry(String date, String carburant, String codePostal, String nomStation, double prix) {}

    /**
     * Loads all history entries.
     *
     * @return List of history entries
     */
    List<HistoryEntry> findAll();

    /**
     * Saves history entries.
     *
     * @param entries List of entries to save
     */
    void saveAll(List<HistoryEntry> entries);

    /**
     * Finds entries for a specific fuel type within the last N days.
     *
     * @param carburant The fuel type
     * @param days Number of days to look back
     * @return List of matching entries
     */
    List<HistoryEntry> findByCarburantAndDays(String carburant, int days);
}
