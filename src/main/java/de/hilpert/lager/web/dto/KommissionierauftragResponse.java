package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.AuftragStatus;
import de.hilpert.lager.domain.Kommissionierauftrag;
import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.Transportauftrag;

import java.time.LocalDateTime;
import java.util.List;

public record KommissionierauftragResponse(
        Long id,
        AuftragStatus status,
        Prioritaet prioritaet,
        LocalDateTime erstelltAm,
        List<KommissionierauftragRequest.Position> positionen,
        List<TransportauftragResponse> transportauftraege) {

    public static KommissionierauftragResponse von(Kommissionierauftrag auftrag, List<Transportauftrag> transporte) {
        return new KommissionierauftragResponse(
                auftrag.getId(),
                auftrag.getStatus(),
                auftrag.getPrioritaet(),
                auftrag.getErstelltAm(),
                auftrag.getPositionen().stream()
                        .map(p -> new KommissionierauftragRequest.Position(p.getArtikelNummer(), p.getMenge()))
                        .toList(),
                transporte.stream().map(TransportauftragResponse::von).toList());
    }
}
