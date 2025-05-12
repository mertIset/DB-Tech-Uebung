package de.htwberlin.dbtech.aufgaben.ue03.dao;

/**
 * Data Access Object (DAO) Interface für Deckungsart-bezogene Operationen.
 */
public interface DeckungsartDao {
    /**
     * Prüft, ob eine Deckungsart existiert.
     * @param deckungsartId Die ID der zu prüfenden Deckungsart
     * @return true, wenn die Deckungsart existiert, sonst false
     */
    boolean existiert(Integer deckungsartId);

    /**
     * Prüft, ob eine Deckungsart zu einem bestimmten Produkt passt.
     * @param deckungsartId Die ID der Deckungsart
     * @param produktId Die ID des Produkts
     * @return true, wenn die Deckungsart zum Produkt passt, sonst false
     */
    boolean passtZuProdukt(Integer deckungsartId, Integer produktId);
}
