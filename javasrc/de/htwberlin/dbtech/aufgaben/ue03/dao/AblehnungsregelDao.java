package de.htwberlin.dbtech.aufgaben.ue03.dao;

import java.math.BigDecimal;

/**
 * Data Access Object (DAO) Interface für Ablehnungsregeln.
 */
public interface AblehnungsregelDao {
    /**
     * Prüft die Ablehnungsregeln für eine Deckungsart.
     * @param deckungsartId Die ID der Deckungsart
     * @param deckungsbetrag Der Deckungsbetrag
     * @param alter Das Alter der Person
     * @return true, wenn die Regeln eingehalten werden, sonst false
     */
    boolean istRegelkonform(Integer deckungsartId, BigDecimal deckungsbetrag, int alter);
}