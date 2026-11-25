package de.hilpert.lager.domain;

/** Gescannter Artikel oder Platz passt nicht zum Auftrag (wird zu HTTP 409). */
public class FalscherScanException extends RuntimeException {

    public FalscherScanException(String message) {
        super(message);
    }
}
