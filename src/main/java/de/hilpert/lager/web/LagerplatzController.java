package de.hilpert.lager.web;

import de.hilpert.lager.service.LagerplatzService;
import de.hilpert.lager.web.dto.BestandResponse;
import de.hilpert.lager.web.dto.LagerplatzMasseRequest;
import de.hilpert.lager.web.dto.LagerplatzRequest;
import de.hilpert.lager.web.dto.LagerplatzResponse;
import de.hilpert.lager.web.dto.SperrRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        LagerplatzMasseRequest m = r.masse();
        return LagerplatzResponse.von(lagerplatzService.anlegen(r.gang(), r.regal(), r.ebene(), r.kapazitaet(),
                m.laengeMm(), m.breiteMm(), m.hoeheMm(), m.maxGewichtKg()));
    }

    @PutMapping("/lagerplaetze/{code}/masse")
    public LagerplatzResponse masse(@PathVariable String code, @Valid @RequestBody LagerplatzMasseRequest m) {
        return LagerplatzResponse.von(lagerplatzService.setzeMasse(code,
                m.laengeMm(), m.breiteMm(), m.hoeheMm(), m.maxGewichtKg()));
    }

    @PutMapping("/lagerplaetze/{code}/sperre")
    public LagerplatzResponse sperren(@PathVariable String code, @Valid @RequestBody SperrRequest request) {
        return LagerplatzResponse.von(lagerplatzService.sperren(code, request.grund()));
    }

    @DeleteMapping("/lagerplaetze/{code}/sperre")
    public LagerplatzResponse freigeben(@PathVariable String code) {
        return LagerplatzResponse.von(lagerplatzService.freigeben(code));
    }

    @GetMapping("/bestaende")
    public List<BestandResponse> bestaende() {
        return lagerplatzService.bestaende().stream().map(BestandResponse::von).toList();
    }
}
