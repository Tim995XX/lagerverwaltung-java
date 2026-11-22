package de.hilpert.lager.service;

/**
 * Ergebnis einer Einlagerung. Entweder wurde ein Platz gefunden (lagerplatz + transportauftragId)
 * oder die Ware steht auf dem Klärplatz (klaerfallId).
 */
public record WareneingangErgebnis(
        Ergebnis ergebnis,
        String lagerplatz,
        Long transportauftragId,
        Long klaerfallId) {

    public enum Ergebnis {
        EINGELAGERT,
        KLAERPLATZ
    }

    static WareneingangErgebnis eingelagert(String lagerplatz, Long transportauftragId) {
        return new WareneingangErgebnis(Ergebnis.EINGELAGERT, lagerplatz, transportauftragId, null);
    }

    static WareneingangErgebnis klaerplatz(Long klaerfallId) {
        return new WareneingangErgebnis(Ergebnis.KLAERPLATZ, null, null, klaerfallId);
    }
}
