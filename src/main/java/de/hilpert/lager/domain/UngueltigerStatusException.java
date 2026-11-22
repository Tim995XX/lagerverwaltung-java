package de.hilpert.lager.domain;

/** Ein Statuswechsel ist im aktuellen Zustand nicht erlaubt (wird zu HTTP 409). */
public class UngueltigerStatusException extends RuntimeException {

    public UngueltigerStatusException(String message) {
        super(message);
    }
}
