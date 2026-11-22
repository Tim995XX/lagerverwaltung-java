package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.TransportArt;
import de.hilpert.lager.domain.TransportStatus;
import de.hilpert.lager.domain.Transportauftrag;

import java.time.LocalDateTime;

public record TransportauftragResponse(
        Long id,
        TransportArt art,
        String artikelNummer,
        int menge,
        String vonOrt,
        String nachOrt,
        Prioritaet prioritaet,
        TransportStatus status,
        Long kommissionierauftragId,
        LocalDateTime erstelltAm) {

    public static TransportauftragResponse von(Transportauftrag t) {
        return new TransportauftragResponse(t.getId(), t.getArt(), t.getArtikelNummer(), t.getMenge(),
                t.getVonOrt(), t.getNachOrt(), t.getPrioritaet(), t.getStatus(), t.getKommissionierauftragId(),
                t.getErstelltAm());
    }
}
