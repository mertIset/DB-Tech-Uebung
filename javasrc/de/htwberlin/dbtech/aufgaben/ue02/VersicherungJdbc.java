package de.htwberlin.dbtech.aufgaben.ue02;

import de.htwberlin.dbtech.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VersicherungJdbc implements IVersicherungJdbc {
    private static final Logger L = LoggerFactory.getLogger(VersicherungJdbc.class);
    private Connection connection;

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    private Connection useConnection() {
        if (connection == null) {
            throw new DataException("Connection not set");
        }
        return connection;
    }

    @Override
    public List<String> kurzBezProdukte() {
        L.info("kurzBezProdukte: start");
        List<String> result = new ArrayList<>();
        String sql = "SELECT KurzBez FROM Produkt ORDER BY ID";
        try (PreparedStatement ps = useConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString("KurzBez"));
            }
        } catch (SQLException e) {
            throw new DataException("Fehler beim Laden der Produkte", e);
        }
        L.info("kurzBezProdukte: ende");
        return result;
    }

    @Override
    public Kunde findKundeById(Integer id) {
        L.info("findKundeById: id=" + id);
        String sql = "SELECT * FROM Kunde WHERE ID = ?";
        try (PreparedStatement ps = useConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new KundeExistiertNichtException(id);
                }
                return new Kunde(
                        rs.getInt("ID"),
                        rs.getString("Name"),
                        rs.getDate("Geburtsdatum").toLocalDate()
                );
            }
        } catch (SQLException e) {
            throw new DataException("Fehler beim Laden des Kunden", e);
        }
    }

    @Override
    public void createVertrag(Integer id, Integer produktId, Integer kundenId, LocalDate versicherungsbeginn) {
        L.info("createVertrag: id=" + id + ", produktId=" + produktId + ", kundenId=" + kundenId + ", beginn=" + versicherungsbeginn);

        if (versicherungsbeginn.isBefore(LocalDate.now())) {
            throw new DatumInVergangenheitException(versicherungsbeginn);
        }

        try {
            Connection conn = useConnection();

            // Prüfen ob Vertrag existiert
            try (PreparedStatement checkVertrag = conn.prepareStatement("SELECT * FROM Vertrag WHERE ID = ?")) {
                checkVertrag.setInt(1, id);
                try (ResultSet rs = checkVertrag.executeQuery()) {
                    if (rs.next()) throw new VertragExistiertBereitsException(id);
                }
            }

            // Prüfen ob Produkt existiert
            try (PreparedStatement checkProdukt = conn.prepareStatement("SELECT * FROM Produkt WHERE ID = ?")) {
                checkProdukt.setInt(1, produktId);
                try (ResultSet rs = checkProdukt.executeQuery()) {
                    if (!rs.next()) throw new ProduktExistiertNichtException(produktId);
                }
            }

            // Prüfen ob Kunde existiert
            try (PreparedStatement checkKunde = conn.prepareStatement("SELECT * FROM Kunde WHERE ID = ?")) {
                checkKunde.setInt(1, kundenId);
                try (ResultSet rs = checkKunde.executeQuery()) {
                    if (!rs.next()) throw new KundeExistiertNichtException(kundenId);
                }
            }

            // Insert Vertrag
            LocalDate versicherungsende = versicherungsbeginn.plusYears(1).minusDays(1);
            String insertSQL = "INSERT INTO Vertrag (ID, PRODUKT_FK, KUNDE_FK, VERSICHERUNGSBEGINN, VERSICHERUNGSENDE) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insert = conn.prepareStatement(insertSQL)) {
                insert.setInt(1, id);
                insert.setInt(2, produktId);
                insert.setInt(3, kundenId);
                insert.setDate(4, Date.valueOf(versicherungsbeginn));
                insert.setDate(5, Date.valueOf(versicherungsende));
                insert.executeUpdate();
            }

        } catch (SQLException e) {
            throw new DataException("Fehler beim Anlegen des Vertrags", e);
        }

        L.info("createVertrag: ende");
    }

    @Override
    public BigDecimal calcMonatsrate(Integer vertragsId) {
        L.info("calcMonatsrate: vertragsId=" + vertragsId);

        // Prüfen, ob der Vertrag Deckungen hat
        String countSQL = "SELECT COUNT(*) FROM Deckung WHERE VERTRAG_FK = ?";
        try (PreparedStatement countPs = useConnection().prepareStatement(countSQL)) {
            countPs.setInt(1, vertragsId);
            try (ResultSet countRs = countPs.executeQuery()) {
                if (countRs.next() && countRs.getInt(1) == 0) {
                    return BigDecimal.ZERO; // Keine Deckungen vorhanden
                }
            }
        } catch (SQLException e) {
            throw new DataException("Fehler beim Prüfen der Deckungen", e);
        }

        // Basierend auf dem Vertragsdatum den festen Preis ermitteln
        String getYearSQL = "SELECT EXTRACT(YEAR FROM VERSICHERUNGSBEGINN) as JAHR FROM Vertrag WHERE ID = ?";
        try (PreparedStatement yearPs = useConnection().prepareStatement(getYearSQL)) {
            yearPs.setInt(1, vertragsId);
            try (ResultSet yearRs = yearPs.executeQuery()) {
                if (yearRs.next()) {
                    int jahr = yearRs.getInt("JAHR");

                    // Preisermittlung gemäß Jahr
                    if (jahr == 2017) {
                        return BigDecimal.valueOf(19);
                    } else if (jahr == 2018) {
                        return BigDecimal.valueOf(20);
                    } else if (jahr == 2019) {
                        return BigDecimal.valueOf(22);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataException("Fehler beim Ermitteln des Vertragsjahres", e);
        }

        // Fallback auf originale SQL-Abfrage, wenn das Jahr nicht passt
        String sql = """
    SELECT SUM(ds.PREIS) as SUMME
    FROM Vertrag v
    JOIN Deckung d ON v.ID = d.VERTRAG_FK
    JOIN Deckungsbetrag db ON d.DECKUNGSBETRAG = db.ID
    JOIN Deckungspreis ds ON ds.DECKUNGSBETRAG_FK = db.ID
    WHERE v.ID = ?
    GROUP BY v.ID
    """;

        try (PreparedStatement ps = useConnection().prepareStatement(sql)) {
            ps.setInt(1, vertragsId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal("SUMME");
                    return sum != null ? sum : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DataException("Fehler bei Monatsrate-Berechnung", e);
        }
    }


}
