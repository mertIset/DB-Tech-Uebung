package de.htwberlin.dbtech.aufgaben.ue03.dao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AblehnungsregelDaoJdbc implements AblehnungsregelDao {
    private static final Logger L = Logger.getLogger(AblehnungsregelDaoJdbc.class.getName());
    private final java.sql.Connection connection;

    public AblehnungsregelDaoJdbc(java.sql.Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean istRegelkonform(Integer deckungsartId, BigDecimal deckungsbetrag, int alter) {
        String sql = "SELECT R_Betrag, R_Alter FROM Ablehnungsregel " +
                "WHERE Deckungsart_FK = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, deckungsartId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String rBetrag = rs.getString("R_Betrag");
                    String rAlter = rs.getString("R_Alter");

                    if (istRegelVerletzt(rBetrag, rAlter, deckungsbetrag, alter)) {
                        return false;
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            L.log(Level.SEVERE, "Fehler bei Regelkonformitätsprüfung", e);
            return false;
        }
    }

    private boolean istRegelVerletzt(String rBetrag, String rAlter, BigDecimal deckungsbetrag, int alter) {
        // Altersregel prüfen
        boolean alterRegelVerletzt = false;
        if (!rAlter.equals("- -")) {
            if (rAlter.startsWith("<")) {
                int alterGrenze = Integer.parseInt(rAlter.substring(2));
                alterRegelVerletzt = alter < alterGrenze;
            } else if (rAlter.startsWith(">")) {
                int alterGrenze = Integer.parseInt(rAlter.substring(2));
                alterRegelVerletzt = alter > alterGrenze;
            }
        }

        // Betragsregel prüfen
        boolean betragRegelVerletzt = false;
        if (!rBetrag.equals("- -")) {
            BigDecimal betragGrenze = new BigDecimal(rBetrag.substring(2).trim());
            if (rBetrag.startsWith(">=")) {
                betragRegelVerletzt = deckungsbetrag.compareTo(betragGrenze) >= 0;
            } else if (rBetrag.startsWith(">")) {
                betragRegelVerletzt = deckungsbetrag.compareTo(betragGrenze) > 0;
            }
        }

        return alterRegelVerletzt && (rBetrag.equals("- -") || betragRegelVerletzt);
    }
}