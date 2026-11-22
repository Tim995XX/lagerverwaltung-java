package de.hilpert.lager.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BestandTest {

    private final Artikel artikel = new Artikel("1001", "Wasser");
    private final Lagerplatz platz = new Lagerplatz(1, 2, 3, 50);

    @Test
    void lagerplatzCodeIstGangRegalEbene() {
        assertThat(platz.getCode()).isEqualTo("01-02-3");
    }

    @Test
    void freieKapazitaetIstKapazitaetMinusMenge() {
        Bestand bestand = new Bestand(artikel, platz, 30);

        assertThat(bestand.freieKapazitaet()).isEqualTo(20);
    }

    @Test
    void zubuchenUeberKapazitaetIstNichtErlaubt() {
        Bestand bestand = new Bestand(artikel, platz, 30);

        assertThatThrownBy(() -> bestand.zubuchen(21)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bestand.getMenge()).isEqualTo(30);
    }

    @Test
    void abbuchenMehrAlsVorhandenIstNichtErlaubt() {
        Bestand bestand = new Bestand(artikel, platz, 30);

        assertThatThrownBy(() -> bestand.abbuchen(31)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bestand.getMenge()).isEqualTo(30);
    }
}
