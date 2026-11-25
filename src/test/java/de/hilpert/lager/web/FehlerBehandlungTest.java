package de.hilpert.lager.web;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FehlerBehandlungTest {

    private final FehlerBehandlung fehlerBehandlung = new FehlerBehandlung();

    @Test
    void doppelterWertWirdZuKonflikt() {
        var e = new DataIntegrityViolationException("doppelt", new SQLException("unique", "23505"));

        assertThat(fehlerBehandlung.doppelterWert(e).getStatus()).isEqualTo(409);
    }

    @Test
    void andereVerstoesseWerdenNichtVerdeckt() {
        var e = new DataIntegrityViolationException("negativ", new SQLException("check", "23514"));

        assertThatThrownBy(() -> fehlerBehandlung.doppelterWert(e)).isSameAs(e);
    }
}
