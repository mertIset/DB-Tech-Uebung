package de.htwberlin.dbtech.aufgaben.ue03;

import de.htwberlin.dbtech.aufgaben.ue03.dao.*;
import de.htwberlin.dbtech.exceptions.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * VersicherungServiceDao - Implementierung mit dem Table Data Gateway Pattern
 */
public class VersicherungServiceDao implements IVersicherungService {
    private static final Logger L = Logger.getLogger(VersicherungServiceDao.class.getName());

    private java.sql.Connection connection;
    private VertragDao vertragDao;
    private DeckungsartDao deckungsartDao;
    private DeckungsbetragDao deckungsbetragDao;
    private DeckungsPreisDao deckungspreisDao;
    private AblehnungsregelDao ablehnungsregelDao;
    private DeckungDao deckungDao;

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;

        // DAO-Instanzen mit der neuen Connection initialisieren
        this.vertragDao = new VertragDaoJdbc(connection);
        this.deckungsartDao = new DeckungsartDaoJdbc(connection);
        this.deckungsbetragDao = new DeckungsbetragDaoJdbc(connection);
        this.deckungspreisDao = new DeckungsPreisDaoJdbc(connection);
        this.ablehnungsregelDao = new AblehnungsregelDaoJdbc(connection);
        this.deckungDao = new DeckungDaoJdbc(connection);
    }

    @Override
    public void createDeckung(Integer vertragsId, Integer deckungsartId, BigDecimal deckungsbetrag) {
        L.info("vertragsId: " + vertragsId);
        L.info("deckungsartId: " + deckungsartId);
        L.info("deckungsbetrag: " + deckungsbetrag);

        // 1. Prüfen, ob Vertrag existiert
        if (!vertragDao.existiert(vertragsId)) {
            throw new VertragExistiertNichtException(vertragsId);
        }

        // 2. Prüfen, ob Deckungsart existiert
        if (!deckungsartDao.existiert(deckungsartId)) {
            throw new DeckungsartExistiertNichtException(deckungsartId);
        }

        // 3. Prüfen, ob Deckungsart zum Produkt passt
        Integer produktId = vertragDao.getProduktId(vertragsId);
        if (!deckungsartDao.passtZuProdukt(deckungsartId, produktId)) {
            throw new DeckungsartPasstNichtZuProduktException(deckungsartId, vertragsId);
        }

        // 4. Prüfen, ob Deckungsbetrag gültig ist
        if (!deckungsbetragDao.existiert(deckungsartId, deckungsbetrag)) {
            throw new UngueltigerDeckungsbetragException(deckungsbetrag);
        }

        // 5. Prüfen, ob ein Deckungspreis existiert
        Date versicherungsbeginn = vertragDao.getVersicherungsbeginn(vertragsId);
        if (!deckungspreisDao.existiert(deckungsartId, deckungsbetrag, versicherungsbeginn)) {
            throw new DeckungspreisNichtVorhandenException(deckungsbetrag);
        }

        // 6. Alter des Kunden berechnen und Regelkonformität prüfen
        Date geburtsdatum = vertragDao.getKundeGeburtsdatum(vertragsId);
        int alter = berechneAlter(geburtsdatum, versicherungsbeginn);

        if (!ablehnungsregelDao.istRegelkonform(deckungsartId, deckungsbetrag, alter)) {
            throw new DeckungsartNichtRegelkonformException(deckungsartId);
        }

        // 7. Deckung erstellen
        deckungDao.create(vertragsId, deckungsartId, deckungsbetrag);

        L.info("ende");
    }

    /**
     * Berechnet das Alter einer Person zu einem bestimmten Stichtag.
     *
     * @param geburtsdatum Das Geburtsdatum
     * @param stichtag Der Stichtag für die Altersberechnung
     * @return Das Alter in Jahren
     */
    private int berechneAlter(Date geburtsdatum, Date stichtag) {
        Calendar geburtstag = Calendar.getInstance();
        geburtstag.setTime(geburtsdatum);

        Calendar stichtagKalender = Calendar.getInstance();
        stichtagKalender.setTime(stichtag);

        int alter = stichtagKalender.get(Calendar.YEAR) - geburtstag.get(Calendar.YEAR);

        // Prüfen, ob der Geburtstag in diesem Jahr schon war
        if (geburtstag.get(Calendar.MONTH) > stichtagKalender.get(Calendar.MONTH) ||
                (geburtstag.get(Calendar.MONTH) == stichtagKalender.get(Calendar.MONTH) &&
                        geburtstag.get(Calendar.DAY_OF_MONTH) > stichtagKalender.get(Calendar.DAY_OF_MONTH))) {
            alter--;
        }

        return alter;
    }
}