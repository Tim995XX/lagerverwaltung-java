package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Umschlag;

public record ArtikelResponse(String artikelNummer, String bezeichnung, Integer laengeMm, Integer breiteMm,
                              Integer hoeheMm, Integer gewichtGramm, Umschlag umschlag) {

    public static ArtikelResponse von(Artikel artikel) {
        return new ArtikelResponse(artikel.getArtikelNummer(), artikel.getBezeichnung(), artikel.getLaengeMm(),
                artikel.getBreiteMm(), artikel.getHoeheMm(), artikel.getGewichtGramm(), artikel.getUmschlag());
    }
}
