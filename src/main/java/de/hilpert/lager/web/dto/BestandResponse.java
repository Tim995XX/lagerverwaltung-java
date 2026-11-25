package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Bestand;

public record BestandResponse(String lagerplatz, String artikelNummer, String bezeichnung, int menge, int reserviert,
                              int freieKapazitaet) {

    public static BestandResponse von(Bestand bestand) {
        return new BestandResponse(
                bestand.getLagerplatz().getCode(),
                bestand.getArtikel().getArtikelNummer(),
                bestand.getArtikel().getBezeichnung(),
                bestand.getMenge(),
                bestand.getReserviert(),
                bestand.freieKapazitaet());
    }
}
