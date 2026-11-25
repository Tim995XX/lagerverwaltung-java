package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.domain.Umschlag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.hilpert.lager.TestDaten.artikel;
import static de.hilpert.lager.TestDaten.platz;
import static org.assertj.core.api.Assertions.assertThat;

class PlatzSucheTest {

    private final PlatzSuche suche = new PlatzSuche();

    @Test
    void fassungsvermoegenDrehtDieWareUndStapelt() {
        Artikel karton = artikel("1", 300, 200, 250, 1000, Umschlag.MITTEL);

        // gedreht passen 2 x 1 nebeneinander, 2 Lagen
        assertThat(platz(1, 1, 1, 99, 400, 300, 500, 100).fassungsvermoegen(karton)).isEqualTo(4);
        // Ware bleibt aufrecht: Höhe reicht nicht
        assertThat(platz(1, 1, 1, 99, 400, 300, 249, 100).fassungsvermoegen(karton)).isZero();
    }

    @Test
    void traglastUndObergrenzeBegrenzen() {
        Artikel schwer = artikel("1", 100, 100, 100, 10_000, Umschlag.MITTEL);

        assertThat(platz(1, 1, 1, 99, 1000, 1000, 1000, 35).fassungsvermoegen(schwer)).isEqualTo(3);
        assertThat(platz(1, 1, 1, 2, 1000, 1000, 1000, 35).fassungsvermoegen(schwer)).isEqualTo(2);
    }

    @Test
    void ohneMassePasstNichts() {
        assertThat(new Lagerplatz(1, 1, 1, 99).fassungsvermoegen(artikel("1"))).isZero();
        assertThat(platz(1, 1, 1, 99).fassungsvermoegen(new Artikel("1", "alt"))).isZero();
    }

    @Test
    void schnelldreherNahBeiDenTorenLangsamdreherHinten() {
        List<Lagerplatz> plaetze = List.of(platz(3, 3, 1, 40), platz(1, 1, 1, 40), platz(2, 2, 1, 40));

        assertThat(besterCode(artikel("1", 100, 100, 100, 100, Umschlag.SCHNELL), plaetze)).isEqualTo("01-01-1");
        assertThat(besterCode(artikel("1", 100, 100, 100, 100, Umschlag.LANGSAM), plaetze)).isEqualTo("03-03-1");
    }

    @Test
    void mittlererUmschlagNimmtDenKnapperenPlatz() {
        List<Lagerplatz> plaetze = List.of(platz(1, 1, 1, 120), platz(3, 3, 1, 40));

        assertThat(besterCode(artikel("1"), plaetze)).isEqualTo("03-03-1");
    }

    @Test
    void schwereWareKommtNachUnten() {
        List<Lagerplatz> plaetze = List.of(platz(1, 1, 3, 40), platz(3, 3, 1, 40));

        assertThat(besterCode(artikel("1", 100, 100, 100, 15_000, Umschlag.SCHNELL), plaetze)).isEqualTo("03-03-1");
    }

    private String besterCode(Artikel artikel, List<Lagerplatz> plaetze) {
        return suche.besterPlatz(artikel, 10, plaetze).orElseThrow().getCode();
    }
}
