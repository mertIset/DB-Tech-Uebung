package de.htwberlin.dbtech.aufgaben.ue03.dao;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Access Object (DAO) Interface für Deckungsbetrag-bezogene Operationen.
 */
public interface DeckungsbetragDao {
    /**
     * Prüft, ob ein Deckungsbetrag für eine bestimmte Deckungsart gültig ist.
     * @param deckungsartId Die ID der Deckungsart
     * @param deckungsbetrag Der zu prüfende Deckungsbetrag
     * @return true, wenn der Deckungsbetrag gültig ist, sonst false
     */
    boolean existiert(Integer deckungsartId, BigDecimal deckungsbetrag);

    /**
     * Holt alle gültigen Deckungsbeträge für eine Deckungsart.
     * @param deckungsartId Die ID der Deckungsart
     * @return Liste der gültigen Deckungsbeträge
     */
    List<BigDecimal> getGueltigeDeckungsbetraege(Integer deckungsartId);
}