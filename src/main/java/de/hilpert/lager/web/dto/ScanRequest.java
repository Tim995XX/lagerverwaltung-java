package de.hilpert.lager.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Beide Barcodes: Etikett der Ware und Etikett am Lagerplatz. */
public record ScanRequest(
        @NotBlank @Size(max = 50) String artikelNummer,
        @NotBlank @Size(max = 50) String lagerplatzCode) {
}
