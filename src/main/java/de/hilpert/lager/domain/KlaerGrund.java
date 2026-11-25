package de.hilpert.lager.domain;

public enum KlaerGrund {
    /** Wareneingang: kein Platz mit genug freier Kapazität gefunden. */
    KEIN_PASSENDER_PLATZ,
    /** Wareneingang: Maße oder Gewicht des Artikels sind nicht gepflegt. */
    MASSE_FEHLEN,
    /** Einlagerung: Platz war beschädigt oder belegt und wurde gesperrt. */
    PLATZ_BLOCKIERT,
    /** Kommissionierung: Gesamtbestand reicht für eine Position nicht aus. */
    BESTAND_NICHT_AUSREICHEND
}
