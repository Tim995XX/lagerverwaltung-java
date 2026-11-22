package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.KlaerGrund;
import de.hilpert.lager.domain.Klaerfall;

import java.time.LocalDateTime;

public record KlaerfallResponse(
        Long id,
        KlaerGrund grund,
        String artikelNummer,
        int menge,
        String beschreibung,
        Long kommissionierauftragId,
        LocalDateTime erstelltAm,
        boolean erledigt,
        String kommentar) {

    public static KlaerfallResponse von(Klaerfall k) {
        return new KlaerfallResponse(k.getId(), k.getGrund(), k.getArtikelNummer(), k.getMenge(),
                k.getBeschreibung(), k.getKommissionierauftragId(), k.getErstelltAm(), k.isErledigt(),
                k.getKommentar());
    }
}
