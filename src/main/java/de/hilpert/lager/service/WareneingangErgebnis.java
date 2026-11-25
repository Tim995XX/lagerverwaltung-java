package de.hilpert.lager.service;

/**
 * Ergebnis einer Einlagerung. Entweder wurde ein Platz gefunden (lagerplatz + transportauftragId)
 * oder die Ware steht auf dem Klärplatz (klaerfallId).
 */
public record WareneingangErgebnis(
        Ergebnis ergebnis,
        String lagerplatz,
        Long transportauftragId,
        Long klaerfallId,
        String begruendung) {

    public enum Ergebnis {
        /** Platz reserviert; Bestand entsteht erst durch den Scan am Platz. */
        PLATZ_RESERVIERT,
        KLAERPLATZ
    }

    static WareneingangErgebnis reserviert(String lagerplatz, Long transportauftragId, String begruendung) {
        return new WareneingangErgebnis(Ergebnis.PLATZ_RESERVIERT, lagerplatz, transportauftragId, null, begruendung);
    }

    static WareneingangErgebnis klaerplatz(Long klaerfallId) {
        return new WareneingangErgebnis(Ergebnis.KLAERPLATZ, null, null, klaerfallId, null);
    }
}
