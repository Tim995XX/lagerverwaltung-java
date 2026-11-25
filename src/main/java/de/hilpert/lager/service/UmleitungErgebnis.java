package de.hilpert.lager.service;

/** Ergebnis einer Umleitung: neuer Platz oder, wenn keiner passt, Klärplatz. */
public record UmleitungErgebnis(Ergebnis ergebnis, String gesperrterPlatz, String neuerPlatz, String begruendung) {

    public enum Ergebnis {
        UMGELEITET,
        KLAERPLATZ
    }
}
