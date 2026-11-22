package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Artikel;

public record ArtikelResponse(String artikelNummer, String bezeichnung) {

    public static ArtikelResponse von(Artikel artikel) {
        return new ArtikelResponse(artikel.getArtikelNummer(), artikel.getBezeichnung());
    }
}
