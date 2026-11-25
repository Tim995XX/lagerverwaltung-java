package de.hilpert.lager.domain;

import de.hilpert.lager.TestDaten;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BestandTest {

    private final Artikel artikel = TestDaten.artikel("1001");
    private final Lagerplatz platz = TestDaten.platz(1, 2, 3, 50);

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
    void reservierenUeberKapazitaetIstNichtErlaubt() {
        Bestand bestand = new Bestand(artikel, platz, 30);

        assertThatThrownBy(() -> bestand.reservieren(21)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bestand.getMenge()).isEqualTo(30);
    }

    @Test
    void abbuchenMehrAlsVorhandenIstNichtErlaubt() {
        Bestand bestand = new Bestand(artikel, platz, 30);

        assertThatThrownBy(() -> bestand.abbuchen(31)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bestand.getMenge()).isEqualTo(30);
    }

    @Test
    void reservierteWareBelegtPlatzUndWirdErstDurchEinbuchenZuBestand() {
        Bestand bestand = Bestand.reservierung(artikel, platz, 20);
        assertThat(bestand.getMenge()).isZero();
        assertThat(bestand.freieKapazitaet()).isEqualTo(30);
        assertThat(bestand.istLeer()).isFalse();

        bestand.einbuchen(20);

        assertThat(bestand.getMenge()).isEqualTo(20);
        assertThat(bestand.getReserviert()).isZero();
        assertThatThrownBy(() -> bestand.einbuchen(1)).isInstanceOf(IllegalArgumentException.class);
    }
}
