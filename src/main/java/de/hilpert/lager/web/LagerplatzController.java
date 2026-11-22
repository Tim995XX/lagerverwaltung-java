package de.hilpert.lager.web;

import de.hilpert.lager.service.LagerplatzService;
import de.hilpert.lager.web.dto.BestandResponse;
import de.hilpert.lager.web.dto.LagerplatzRequest;
import de.hilpert.lager.web.dto.LagerplatzResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LagerplatzController {

    private final LagerplatzService lagerplatzService;

    public LagerplatzController(LagerplatzService lagerplatzService) {
        this.lagerplatzService = lagerplatzService;
    }

    @GetMapping("/lagerplaetze")
    public List<LagerplatzResponse> alle() {
        return lagerplatzService.alle().stream().map(LagerplatzResponse::von).toList();
    }

    @PostMapping("/lagerplaetze")
    @ResponseStatus(HttpStatus.CREATED)
    public LagerplatzResponse anlegen(@Valid @RequestBody LagerplatzRequest r) {
        return LagerplatzResponse.von(lagerplatzService.anlegen(r.gang(), r.regal(), r.ebene(), r.kapazitaet()));
    }

    @GetMapping("/bestaende")
    public List<BestandResponse> bestaende() {
        return lagerplatzService.bestaende().stream().map(BestandResponse::von).toList();
    }
}
