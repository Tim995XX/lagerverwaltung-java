package de.hilpert.lager.web;

import de.hilpert.lager.service.KlaerfallService;
import de.hilpert.lager.web.dto.KlaerfallErledigenRequest;
import de.hilpert.lager.web.dto.KlaerfallResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/klaerfaelle")
public class KlaerfallController {

    private final KlaerfallService klaerfallService;

    public KlaerfallController(KlaerfallService klaerfallService) {
        this.klaerfallService = klaerfallService;
    }

    /** Standard: nur offene Fälle (?erledigt=true zeigt die abgeschlossenen). */
    @GetMapping
    public List<KlaerfallResponse> liste(@RequestParam(defaultValue = "false") boolean erledigt) {
        return klaerfallService.liste(erledigt).stream().map(KlaerfallResponse::von).toList();
    }

    @PostMapping("/{id}/erledigen")
    public KlaerfallResponse erledigen(@PathVariable Long id, @Valid @RequestBody KlaerfallErledigenRequest request) {
        return KlaerfallResponse.von(klaerfallService.erledigen(id, request.kommentar()));
    }
}
