package de.htwberlin.dbtech.aufgaben.ue03.dao;


import de.htwberlin.dbtech.exceptions.DataException;
import de.htwberlin.dbtech.exceptions.VertragExistiertNichtException;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VertragDaoJdbc implements VertragDao {
    private static final Logger L = Logger.getLogger(VertragDaoJdbc.class.getName());
    private final java.sql.Connection connection;

    public VertragDaoJdbc(java.sql.Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean existiert(Integer vertragsId) {
        String sql = "SELECT COUNT(*) FROM Vertrag WHERE ID = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            L.log(Level.SEVERE, "Fehler bei Vertrag-Existenzprüfung", e);
            return false;
        }
    }

    @Override
    public Integer getProduktId(Integer vertragsId) {
        String sql = "SELECT Produkt_FK FROM Vertrag WHERE ID = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Produkt_FK");
                }
                throw new VertragExistiertNichtException(vertragsId);
            }
        } catch (SQLException e) {
            throw new DataException("Fehler bei Produkt-ID-Abfrage", e);
        }
    }

    @Override
    public java.util.Date getKundeGeburtsdatum(Integer vertragsId) {
        String sql = "SELECT k.Geburtsdatum FROM Vertrag v " +
                "JOIN Kunde k ON v.Kunde_FK = k.ID " +
                "WHERE v.ID = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("Geburtsdatum");
                }
                throw new VertragExistiertNichtException(vertragsId);
            }
        } catch (SQLException e) {
            throw new DataException("Fehler bei Geburtsdatum-Abfrage", e);
        }
    }

    @Override
    public java.util.Date getVersicherungsbeginn(Integer vertragsId) {
        String sql = "SELECT Versicherungsbeginn FROM Vertrag WHERE ID = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("Versicherungsbeginn");
                }
                throw new VertragExistiertNichtException(vertragsId);
            }
        } catch (SQLException e) {
            throw new DataException("Fehler bei Versicherungsbeginn-Abfrage", e);
        }
    }
}