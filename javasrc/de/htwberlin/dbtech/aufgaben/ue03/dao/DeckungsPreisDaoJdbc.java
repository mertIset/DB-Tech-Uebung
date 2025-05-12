package de.htwberlin.dbtech.aufgaben.ue03.dao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeckungsPreisDaoJdbc implements DeckungsPreisDao {
    private static final Logger L = Logger.getLogger(DeckungsPreisDaoJdbc.class.getName());
    private final java.sql.Connection connection;

    public DeckungsPreisDaoJdbc(java.sql.Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean existiert(Integer deckungsartId, BigDecimal deckungsbetrag, java.util.Date versicherungsbeginn) {
        String sql = "SELECT COUNT(*) FROM Deckungsbetrag db " +
                "JOIN Deckungspreis dp ON dp.Deckungsbetrag_FK = db.ID " +
                "WHERE db.Deckungsart_FK = ? AND db.Deckungsbetrag = ? " +
                "AND dp.Gueltig_Von <= ? AND dp.Gueltig_Bis >= ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            pstmt.setBigDecimal(2, deckungsbetrag);
            pstmt.setDate(3, new java.sql.Date(versicherungsbeginn.getTime()));
            pstmt.setDate(4, new java.sql.Date(versicherungsbeginn.getTime()));
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            L.log(Level.SEVERE, "Fehler bei Deckungspreis-Existenzprüfung", e);
            return false;
        }
    }
}