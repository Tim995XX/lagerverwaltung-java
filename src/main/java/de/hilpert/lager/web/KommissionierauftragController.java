package de.hilpert.lager.web;

import de.hilpert.lager.domain.Auftragsposition;
import de.hilpert.lager.domain.Kommissionierauftrag;
import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.service.KommissionierService;
import de.hilpert.lager.web.dto.KommissionierauftragRequest;
import de.hilpert.lager.web.dto.KommissionierauftragResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kommissionierauftraege")
public class KommissionierauftragController {

    private final KommissionierService kommissionierService;

    public KommissionierauftragController(KommissionierService kommissionierService) {
        this.kommissionierService = kommissionierService;
    }

    /** Legt den Auftrag an. Status FREIGEGEBEN oder IN_KLAERUNG, je nach Bestand. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KommissionierauftragResponse anlegen(@Valid @RequestBody KommissionierauftragRequest request) {
        List<Auftragsposition> positionen = request.positionen().stream()
                .map(p -> new Auftragsposition(p.artikelNummer(), p.menge()))
                .toList();
        Prioritaet prioritaet = request.prioritaet() == null ? Prioritaet.NORMAL : request.prioritaet();

        Kommissionierauftrag auftrag = kommissionierService.anlegen(positionen, prioritaet);
        return antwort(auftrag);
    }

    @GetMapping("/{id}")
    public KommissionierauftragResponse einer(@PathVariable Long id) {
        return antwort(kommissionierService.finde(id));
    }

    private KommissionierauftragResponse antwort(Kommissionierauftrag auftrag) {
        return KommissionierauftragResponse.von(auftrag, kommissionierService.transportauftraegeZu(auftrag.getId()));
    }
}
