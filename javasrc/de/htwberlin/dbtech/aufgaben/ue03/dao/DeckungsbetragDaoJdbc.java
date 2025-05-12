package de.htwberlin.dbtech.aufgaben.ue03.dao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeckungsbetragDaoJdbc implements DeckungsbetragDao {
    private static final Logger L = Logger.getLogger(DeckungsbetragDaoJdbc.class.getName());
    private final java.sql.Connection connection;

    public DeckungsbetragDaoJdbc(java.sql.Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean existiert(Integer deckungsartId, BigDecimal deckungsbetrag) {
        String sql = "SELECT COUNT(*) FROM Deckungsbetrag " +
                "WHERE Deckungsart_FK = ? AND Deckungsbetrag = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            pstmt.setBigDecimal(2, deckungsbetrag);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            L.log(Level.SEVERE, "Fehler bei Deckungsbetrag-Existenzprüfung", e);
            return false;
        }
    }

    @Override
    public List<BigDecimal> getGueltigeDeckungsbetraege(Integer deckungsartId) {
        List<BigDecimal> betraege = new ArrayList<>();
        String sql = "SELECT Deckungsbetrag FROM Deckungsbetrag " +
                "WHERE Deckungsart_FK = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    betraege.add(rs.getBigDecimal("Deckungsbetrag"));
                }
            }
        } catch (SQLException e) {
            L.log(Level.SEVERE, "Fehler beim Abrufen gültiger Deckungsbeträge", e);
        }
        return betraege;
    }
}