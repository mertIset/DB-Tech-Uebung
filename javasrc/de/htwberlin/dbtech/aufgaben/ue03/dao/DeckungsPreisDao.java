package de.htwberlin.dbtech.aufgaben.ue03.dao;

import java.math.BigDecimal;

/**
 * Data Access Object (DAO) Interface für Deckungspreis-bezogene Operationen.
 */
public interface DeckungsPreisDao {
    /**
     * Prüft, ob ein Deckungspreis für einen bestimmten Deckungsbetrag und Zeitraum existiert.
     * @param deckungsartId Die ID der Deckungsart
     * @param deckungsbetrag Der Deckungsbetrag
     * @param versicherungsbeginn Das Datum des Versicherungsbeginns
     * @return true, wenn ein Deckungspreis existiert, sonst false
     */
    boolean existiert(Integer deckungsartId, BigDecimal deckungsbetrag, java.util.Date versicherungsbeginn);
}
