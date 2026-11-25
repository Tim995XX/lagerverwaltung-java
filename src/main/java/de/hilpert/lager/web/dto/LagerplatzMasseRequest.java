package de.hilpert.lager.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Innenmaße und Traglast des Platzes. */
public record LagerplatzMasseRequest(
        @Min(1) @Max(20_000) int laengeMm,
        @Min(1) @Max(20_000) int breiteMm,
        @Min(1) @Max(20_000) int hoeheMm,
        @Min(1) @Max(50_000) int maxGewichtKg) {
}
