package de.hilpert.lager.domain;

public enum TransportArt {
    /** Vom Wareneingang auf einen Lagerplatz. */
    EINLAGERUNG,
    /** Von einem Lagerplatz zum Warenausgang (für einen Kommissionierauftrag). */
    AUSLAGERUNG
}
