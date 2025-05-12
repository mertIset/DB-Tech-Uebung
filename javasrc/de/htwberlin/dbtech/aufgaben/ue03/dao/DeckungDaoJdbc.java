package de.htwberlin.dbtech.aufgaben.ue03.dao;

import de.htwberlin.dbtech.exceptions.DataException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DeckungDaoJdbc implements DeckungDao {
    private static final Logger L = Logger.getLogger(DeckungDaoJdbc.class.getName());
    private final java.sql.Connection connection;

    public DeckungDaoJdbc(java.sql.Connection connection) {
        this.connection = connection;
    }

    @Override
    public void create(Integer vertragsId, Integer deckungsartId, BigDecimal deckungsbetrag) {
        String sql = "INSERT INTO Deckung (Vertrag_FK, Deckungsart_FK, Deckungsbetrag) VALUES (?, ?, ?)";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            pstmt.setInt(2, deckungsartId);
            pstmt.setBigDecimal(3, deckungsbetrag);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Fehler beim Erstellen einer Deckung", e);
        }
    }
}