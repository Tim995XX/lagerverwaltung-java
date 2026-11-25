package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Umschlag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Maße und Gewicht je Stück. Ohne Angabe gilt Umschlag MITTEL. */
public record ArtikelMasseRequest(
        @Min(1) @Max(10_000) int laengeMm,
        @Min(1) @Max(10_000) int breiteMm,
        @Min(1) @Max(10_000) int hoeheMm,
        @Min(1) @Max(2_000_000) int gewichtGramm,
        Umschlag umschlag) {

    public ArtikelMasseRequest {
        umschlag = umschlag == null ? Umschlag.MITTEL : umschlag;
    }
}
