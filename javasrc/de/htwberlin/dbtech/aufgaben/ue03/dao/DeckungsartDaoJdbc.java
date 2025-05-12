package de.htwberlin.dbtech.aufgaben.ue03.dao;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeckungsartDaoJdbc implements DeckungsartDao {
    private static final Logger L = Logger.getLogger(DeckungsartDaoJdbc.class.getName());
    private final java.sql.Connection connection;

    public DeckungsartDaoJdbc(java.sql.Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean existiert(Integer deckungsartId) {
        String sql = "SELECT COUNT(*) FROM Deckungsart WHERE ID = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            L.log(Level.SEVERE, "Fehler bei Deckungsart-Existenzprüfung", e);
            return false;
        }
    }

    @Override
    public boolean passtZuProdukt(Integer deckungsartId, Integer produktId) {
        String sql = "SELECT COUNT(*) FROM Deckungsart " +
                "WHERE ID = ? AND Produkt_FK = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            pstmt.setInt(2, produktId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            L.log(Level.SEVERE, "Fehler bei Deckungsart-Produkt-Prüfung", e);
            return false;
        }
    }
}