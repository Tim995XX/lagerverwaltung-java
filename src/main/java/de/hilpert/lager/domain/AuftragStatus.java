package de.hilpert.lager.domain;

public enum AuftragStatus {
    /** Bestand ist abgebucht, Transportaufträge zur Auslagerung sind angelegt. */
    FREIGEGEBEN,
    /** Mindestens eine Position konnte nicht bedient werden, ein Klärfall ist angelegt. */
    IN_KLAERUNG
}
