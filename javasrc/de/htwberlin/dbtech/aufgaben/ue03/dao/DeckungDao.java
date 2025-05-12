package de.htwberlin.dbtech.aufgaben.ue03.dao;
import java.math.BigDecimal;

/**
 * Data Access Object (DAO) Interface für Deckung-bezogene Operationen.
 */
public interface DeckungDao {
    /**
     * Fügt eine neue Deckung in die Datenbank ein.
     * @param vertragsId Die ID des Vertrags
     * @param deckungsartId Die ID der Deckungsart
     * @param deckungsbetrag Der Deckungsbetrag
     * @throws Exception wenn das Einfügen fehlschlägt
     */
    void create(Integer vertragsId, Integer deckungsartId, BigDecimal deckungsbetrag);
}