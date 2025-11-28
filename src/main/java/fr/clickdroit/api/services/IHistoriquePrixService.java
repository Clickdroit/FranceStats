package fr.clickdroit.api.services;

import fr.clickdroit.api.models.Station;

import java.util.List;

/**
 * Interface for price history operations.
 */
public interface IHistoriquePrixService {

    /**
     * Saves today's prices to history.
     *
     * @param stations List of stations with current prices
     */
    void saveTodayPrices(List<Station> stations);

    /**
     * Displays price evolution for a fuel type.
     *
     * @param carburant Fuel type
     * @param nbJours Number of days to display
     */
    void displayPriceEvolution(String carburant, int nbJours);
}
