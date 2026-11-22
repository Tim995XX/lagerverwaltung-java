package de.hilpert.lager.fehler;

/** Ein angefragtes Objekt existiert nicht (wird zu HTTP 404). */
public class NichtGefundenException extends RuntimeException {

    public NichtGefundenException(String message) {
        super(message);
    }
}
