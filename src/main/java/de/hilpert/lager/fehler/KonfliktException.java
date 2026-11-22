package de.hilpert.lager.fehler;

/** Die Anfrage widerspricht dem aktuellen Datenstand, z. B. doppelte Artikelnummer (wird zu HTTP 409). */
public class KonfliktException extends RuntimeException {

    public KonfliktException(String message) {
        super(message);
    }
}
