package de.hilpert.lager.web;

import de.hilpert.lager.service.ArtikelService;
import de.hilpert.lager.web.dto.ArtikelRequest;
import de.hilpert.lager.web.dto.ArtikelResponse;
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
@RequestMapping("/api/artikel")
public class ArtikelController {

    private final ArtikelService artikelService;

    public ArtikelController(ArtikelService artikelService) {
        this.artikelService = artikelService;
    }

    @GetMapping
    public List<ArtikelResponse> alle() {
        return artikelService.alle().stream().map(ArtikelResponse::von).toList();
    }

    @GetMapping("/{artikelNummer}")
    public ArtikelResponse einer(@PathVariable String artikelNummer) {
        return ArtikelResponse.von(artikelService.finde(artikelNummer));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArtikelResponse anlegen(@Valid @RequestBody ArtikelRequest request) {
        return ArtikelResponse.von(artikelService.anlegen(request.artikelNummer(), request.bezeichnung()));
    }
}
