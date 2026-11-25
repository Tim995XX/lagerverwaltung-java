package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Lagerplatz;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Wählt den sinnvollsten leeren Platz. Muss-Regeln (Maße, Traglast) schließen Plätze aus,
 * danach entscheidet die Reihenfolge:
 * schwere Ware nach unten, Schnelldreher nah an die Tore, sonst der knappste passende Platz.
 */
@Component
public class PlatzSuche {

    /** Ab diesem Stückgewicht zählt zuerst eine niedrige Ebene. */
    static final int SCHWER_AB_GRAMM = 15_000;

    public Optional<Lagerplatz> besterPlatz(Artikel artikel, int menge, Collection<Lagerplatz> leerePlaetze) {
        return leerePlaetze.stream()
                .filter(platz -> platz.fassungsvermoegen(artikel) >= menge)
                .min(reihenfolge(artikel));
    }

    public String begruendung(Artikel artikel) {
        String weg = switch (artikel.getUmschlag()) {
            case SCHNELL -> "Schnelldreher: kurzer Weg vor knapper Platzgröße";
            case MITTEL -> "kleinster passender Platz, dann kurzer Weg";
            case LANGSAM -> "Langsamdreher: kleinster passender Platz, dann weiter hinten";
        };
        return istSchwer(artikel) ? "Schwere Ware: niedrige Ebene zuerst; " + weg : weg;
    }

    private static boolean istSchwer(Artikel artikel) {
        return artikel.getGewichtGramm() >= SCHWER_AB_GRAMM;
    }

    private static Comparator<Lagerplatz> reihenfolge(Artikel artikel) {
        Comparator<Lagerplatz> ebene = Comparator.comparingInt(Lagerplatz::getEbene);
        Comparator<Lagerplatz> weg = Comparator.comparingInt(Lagerplatz::entfernung);
        Comparator<Lagerplatz> groesse = Comparator.comparingInt(platz -> platz.fassungsvermoegen(artikel));

        Comparator<Lagerplatz> reihenfolge = switch (artikel.getUmschlag()) {
            case SCHNELL -> weg.thenComparing(ebene).thenComparing(groesse);
            case MITTEL -> groesse.thenComparing(weg);
            case LANGSAM -> groesse.thenComparing(weg.reversed());
        };
        if (istSchwer(artikel)) {
            reihenfolge = ebene.thenComparing(reihenfolge);
        }
        // Bei Gleichstand immer dasselbe Ergebnis
        return reihenfolge.thenComparing(Lagerplatz::getCode);
    }
}
