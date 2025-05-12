package de.htwberlin.dbtech.aufgaben.ue03.dao;

/**
 * Data Access Object (DAO) Interface für Vertrag-bezogene Operationen.
 */
public interface VertragDao {
    /**
     * Prüft, ob ein Vertrag existiert.
     * @param vertragsId Die ID des zu prüfenden Vertrags
     * @return true, wenn der Vertrag existiert, sonst false
     */
    boolean existiert(Integer vertragsId);

    /**
     * Holt den Produkttyp für einen gegebenen Vertrag.
     * @param vertragsId Die ID des Vertrags
     * @return Die Produkt-ID
     * @throws Exception wenn der Vertrag nicht existiert
     */
    Integer getProduktId(Integer vertragsId);

    /**
     * Holt das Geburtsdatum des Kunden für einen Vertrag.
     * @param vertragsId Die ID des Vertrags
     * @return Das Geburtsdatum als java.util.Date
     * @throws Exception wenn der Vertrag nicht existiert
     */
    java.util.Date getKundeGeburtsdatum(Integer vertragsId);

    /**
     * Holt das Versicherungsbeginn-Datum für einen Vertrag.
     * @param vertragsId Die ID des Vertrags
     * @return Das Versicherungsbeginn-Datum
     * @throws Exception wenn der Vertrag nicht existiert
     */
    java.util.Date getVersicherungsbeginn(Integer vertragsId);
}