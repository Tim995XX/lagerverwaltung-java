package de.hilpert.lager.web;

import de.hilpert.lager.domain.TransportStatus;
import de.hilpert.lager.service.TransportauftragService;
import de.hilpert.lager.web.dto.PrioritaetRequest;
import de.hilpert.lager.web.dto.ScanRequest;
import de.hilpert.lager.web.dto.ScanResponse;
import de.hilpert.lager.web.dto.TransportauftragResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transportauftraege")
public class TransportauftragController {

    private final TransportauftragService transportauftragService;

    public TransportauftragController(TransportauftragService transportauftragService) {
        this.transportauftragService = transportauftragService;
    }

    /** Liste in Abarbeitungsreihenfolge, optional gefiltert, z. B. ?status=OFFEN. */
    @GetMapping
    public List<TransportauftragResponse> liste(@RequestParam(required = false) TransportStatus status) {
        return transportauftragService.liste(status).stream().map(TransportauftragResponse::von).toList();
    }

    @PostMapping("/{id}/start")
    public TransportauftragResponse starten(@PathVariable Long id) {
        return TransportauftragResponse.von(transportauftragService.starten(id));
    }

    @PostMapping("/{id}/abschluss")
    public TransportauftragResponse abschliessen(@PathVariable Long id) {
        return TransportauftragResponse.von(transportauftragService.abschliessen(id));
    }

    /** Einlagerung bestätigen: Barcode der Ware und Barcode des Platzes. */
    @PostMapping("/{id}/scan")
    public TransportauftragResponse scannen(@PathVariable Long id, @Valid @RequestBody ScanRequest request) {
        return TransportauftragResponse.von(transportauftragService.einlagerungScannen(
                id, request.artikelNummer(), request.lagerplatzCode()));
    }

    @GetMapping("/{id}/scans")
    public List<ScanResponse> scans(@PathVariable Long id) {
        return transportauftragService.scans(id).stream().map(ScanResponse::von).toList();
    }

    @PutMapping("/{id}/prioritaet")
    public TransportauftragResponse prioritaet(@PathVariable Long id, @Valid @RequestBody PrioritaetRequest request) {
        return TransportauftragResponse.von(transportauftragService.aenderePrioritaet(id, request.prioritaet()));
    }
}
