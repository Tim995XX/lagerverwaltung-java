package de.hilpert.lager.web;

import de.hilpert.lager.service.ArtikelService;
import de.hilpert.lager.web.dto.ArtikelMasseRequest;
import de.hilpert.lager.web.dto.ArtikelRequest;
import de.hilpert.lager.web.dto.ArtikelResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
        ArtikelMasseRequest m = request.masse();
        return ArtikelResponse.von(artikelService.anlegen(request.artikelNummer(), request.bezeichnung(),
                m.laengeMm(), m.breiteMm(), m.hoeheMm(), m.gewichtGramm(), m.umschlag()));
    }

    @PutMapping("/{artikelNummer}/masse")
    public ArtikelResponse masse(@PathVariable String artikelNummer, @Valid @RequestBody ArtikelMasseRequest m) {
        return ArtikelResponse.von(artikelService.setzeMasse(artikelNummer,
                m.laengeMm(), m.breiteMm(), m.hoeheMm(), m.gewichtGramm(), m.umschlag()));
    }
}
