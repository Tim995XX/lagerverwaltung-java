package de.hilpert.lager;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.domain.Umschlag;

/** Artikel und Plätze, bei denen Maße und Gewicht nie die engste Grenze sind. */
public final class TestDaten {

    private TestDaten() {
    }

    public static Artikel artikel(String nummer) {
        return artikel(nummer, 100, 100, 100, 100, Umschlag.MITTEL);
    }

    public static Artikel artikel(String nummer, int laenge, int breite, int hoehe, int gramm, Umschlag umschlag) {
        Artikel artikel = new Artikel(nummer, "Artikel " + nummer);
        artikel.setzeMasse(laenge, breite, hoehe, gramm, umschlag);
        return artikel;
    }

    public static Lagerplatz platz(int gang, int regal, int ebene, int kapazitaet) {
        return platz(gang, regal, ebene, kapazitaet, 2000, 2000, 2000, 1000);
    }

    public static Lagerplatz platz(int gang, int regal, int ebene, int kapazitaet,
                                   int laenge, int breite, int hoehe, int kg) {
        Lagerplatz platz = new Lagerplatz(gang, regal, ebene, kapazitaet);
        platz.setzeMasse(laenge, breite, hoehe, kg);
        return platz;
    }
}
