package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Scan;

import java.time.LocalDateTime;

public record ScanResponse(String artikelNummer, String lagerplatzCode, boolean erfolgreich,
                           LocalDateTime gescanntAm) {

    public static ScanResponse von(Scan scan) {
        return new ScanResponse(scan.getArtikelNummer(), scan.getLagerplatzCode(), scan.isErfolgreich(),
                scan.getGescanntAm());
    }
}
