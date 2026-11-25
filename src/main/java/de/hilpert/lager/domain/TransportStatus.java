package de.hilpert.lager.domain;

public enum TransportStatus {
    OFFEN,
    IN_ARBEIT,
    ERLEDIGT,
    /** Kein Ersatzplatz gefunden, die Ware steht auf dem Klärplatz. */
    ABGEBROCHEN
}
