package de.htwberlin.dbtech.aufgaben.ue03;

/*
  @author Ingo Classen
 */

import de.htwberlin.dbtech.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/**
 * VersicherungJdbc
 */
public class VersicherungService implements IVersicherungService {
    private static final Logger L = LoggerFactory.getLogger(VersicherungService.class);
    private Connection connection;

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @SuppressWarnings("unused")
    private Connection useConnection() {
        if (connection == null) {
            throw new DataException("Connection not set");
        }
        return connection;
    }

    @Override
    public void createDeckung(Integer vertragsId, Integer deckungsartId, BigDecimal deckungsbetrag) {
        L.info("vertragsId: " + vertragsId);
        L.info("deckungsartId: " + deckungsartId);
        L.info("deckungsbetrag: " + deckungsbetrag);

        try {
            // 1. Validate Vertrag exists
            validateVertrag(vertragsId);

            // 2. Validate Deckungsart exists and matches Produkt
            validateDeckungsart(vertragsId, deckungsartId);

            // 3. Validate Deckungsbetrag
            validateDeckungsbetrag(deckungsartId, deckungsbetrag);

            // 4. Validate Pricing
            validateDeckungspreis(deckungsartId, deckungsbetrag, vertragsId);

            // 5. Validate Customer Age Rules
            validateKundeAlterRegel(vertragsId, deckungsartId, deckungsbetrag);

            // 6. Insert Deckung
            insertDeckung(vertragsId, deckungsartId, deckungsbetrag);

            L.info("ende");
        } catch (SQLException e) {
            throw new DataException("Database error occurred", e);
        }
    }

    private void validateVertrag(Integer vertragsId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Vertrag WHERE ID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new VertragExistiertNichtException(vertragsId);
                }
            }
        }
    }

    private void validateDeckungsart(Integer vertragsId, Integer deckungsartId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Deckungsart d " +
                "JOIN Vertrag v ON v.Produkt_FK = d.Produkt_FK " +
                "WHERE v.ID = ? AND d.ID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            pstmt.setInt(2, deckungsartId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getInt(1) == 0) {
                        // Check if Deckungsart exists first
                        checkDeckungsartExists(deckungsartId);

                        // If exists but doesn't match Produkt
                        throw new DeckungsartPasstNichtZuProduktException(deckungsartId, vertragsId);
                    }
                }
            }
        }
    }

    private void checkDeckungsartExists(Integer deckungsartId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Deckungsart WHERE ID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new DeckungsartExistiertNichtException(deckungsartId);
                }
            }
        }
    }

    private void validateDeckungsbetrag(Integer deckungsartId, BigDecimal deckungsbetrag) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Deckungsbetrag " +
                "WHERE Deckungsart_FK = ? AND Deckungsbetrag = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            pstmt.setBigDecimal(2, deckungsbetrag);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new UngueltigerDeckungsbetragException(deckungsbetrag);
                }
            }
        }
    }

    private void validateDeckungspreis(Integer deckungsartId, BigDecimal deckungsbetrag, Integer vertragsId) throws SQLException {
        // Find the Vertrag's Versicherungsbeginn
        LocalDate versicherungsbeginn = getVersicherungsbeginn(vertragsId);

        String sql = "SELECT COUNT(*) FROM Deckungsbetrag db " +
                "JOIN Deckungspreis dp ON dp.Deckungsbetrag_FK = db.ID " +
                "WHERE db.Deckungsart_FK = ? AND db.Deckungsbetrag = ? " +
                "AND dp.Gueltig_Von <= ? AND dp.Gueltig_Bis >= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            pstmt.setBigDecimal(2, deckungsbetrag);
            pstmt.setDate(3, Date.valueOf(versicherungsbeginn));
            pstmt.setDate(4, Date.valueOf(versicherungsbeginn));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new DeckungspreisNichtVorhandenException(deckungsbetrag);
                }
            }
        }
    }

    private void validateKundeAlterRegel(Integer vertragsId, Integer deckungsartId, BigDecimal deckungsbetrag) throws SQLException {
        // Get Customer's age at Versicherungsbeginn
        String ageCheckSql = "SELECT k.Geburtsdatum, v.Versicherungsbeginn " +
                "FROM Vertrag v " +
                "JOIN Kunde k ON v.Kunde_FK = k.ID " +
                "WHERE v.ID = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(ageCheckSql)) {
            pstmt.setInt(1, vertragsId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Date geburtsdatum = rs.getDate("Geburtsdatum");
                    Date versicherungsbeginn = rs.getDate("Versicherungsbeginn");

                    // Calculate age at insurance begin
                    int age = calculateAge(geburtsdatum, versicherungsbeginn);

                    // Check Ablehnungsregeln
                    checkAblehnungsregeln(deckungsartId, deckungsbetrag, age, vertragsId);
                }
            }
        }
    }

    private void checkAblehnungsregeln(Integer deckungsartId, BigDecimal deckungsbetrag, int age, Integer vertragsId) throws SQLException {
        String rulesSql = "SELECT R_Betrag, R_Alter FROM Ablehnungsregel " +
                "WHERE Deckungsart_FK = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(rulesSql)) {
            pstmt.setInt(1, deckungsartId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String rBetrag = rs.getString("R_Betrag");
                    String rAlter = rs.getString("R_Alter");

                    if (isRuleViolated(rBetrag, rAlter, deckungsbetrag, age)) {
                        throw new DeckungsartNichtRegelkonformException(deckungsartId);
                    }
                }
            }
        }
    }

    private boolean isRuleViolated(String rBetrag, String rAlter, BigDecimal deckungsbetrag, int age) {
        // Age rule check
        boolean ageRuleViolated = false;
        if (!rAlter.equals("- -")) {
            if (rAlter.startsWith("<")) {
                int limitAge = Integer.parseInt(rAlter.substring(2));
                ageRuleViolated = age < limitAge;
            } else if (rAlter.startsWith(">")) {
                int limitAge = Integer.parseInt(rAlter.substring(2));
                ageRuleViolated = age > limitAge;
            }
        }

        // Amount rule check
        boolean amountRuleViolated = false;
        if (!rBetrag.equals("- -")) {
            BigDecimal limitAmount = extractLimitAmount(rBetrag);
            if (rBetrag.startsWith(">=")) {
                amountRuleViolated = deckungsbetrag.compareTo(limitAmount) >= 0;
            } else if (rBetrag.startsWith(">")) {
                amountRuleViolated = deckungsbetrag.compareTo(limitAmount) > 0;
            }
        }

        return ageRuleViolated && (rBetrag.equals("- -") || amountRuleViolated);
    }

    private BigDecimal extractLimitAmount(String rBetrag) {
        return new BigDecimal(rBetrag.substring(2).trim());
    }

    private int calculateAge(Date geburtsdatum, Date versicherungsbeginn) {
        LocalDate birthDate = geburtsdatum.toLocalDate();
        LocalDate insuranceBegin = versicherungsbeginn.toLocalDate();
        return insuranceBegin.getYear() - birthDate.getYear() -
                (insuranceBegin.getMonthValue() < birthDate.getMonthValue() ||
                        (insuranceBegin.getMonthValue() == birthDate.getMonthValue() &&
                                insuranceBegin.getDayOfMonth() < birthDate.getDayOfMonth()) ? 1 : 0);
    }

    private LocalDate getVersicherungsbeginn(Integer vertragsId) throws SQLException {
        String sql = "SELECT Versicherungsbeginn FROM Vertrag WHERE ID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("Versicherungsbeginn").toLocalDate();
                }
                throw new VertragExistiertNichtException(vertragsId);
            }
        }
    }

    private void insertDeckung(Integer vertragsId, Integer deckungsartId, BigDecimal deckungsbetrag) throws SQLException {
        String sql = "INSERT INTO Deckung (Vertrag_FK, Deckungsart_FK, Deckungsbetrag) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, vertragsId);
            pstmt.setInt(2, deckungsartId);
            pstmt.setBigDecimal(3, deckungsbetrag);
            pstmt.executeUpdate();
        }
    }
}



