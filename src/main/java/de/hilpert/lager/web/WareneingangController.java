package de.hilpert.lager.web;

import de.hilpert.lager.service.WareneingangErgebnis;
import de.hilpert.lager.service.WareneingangService;
import de.hilpert.lager.web.dto.WareneingangRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WareneingangController {

    private final WareneingangService wareneingangService;

    public WareneingangController(WareneingangService wareneingangService) {
        this.wareneingangService = wareneingangService;
    }

    /**
     * Antwortet immer mit 200: Auch "Ware auf Klärplatz" ist ein fachlich gültiges Ergebnis
     * und kein Fehler der Anfrage. Das Feld "ergebnis" sagt, was passiert ist.
     */
    @PostMapping("/api/wareneingaenge")
    public WareneingangErgebnis einlagern(@Valid @RequestBody WareneingangRequest request) {
        return wareneingangService.einlagern(request.artikelNummer(), request.menge());
    }
}
