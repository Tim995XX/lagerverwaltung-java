package de.hilpert.lager.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LagerplatzRequest(
        @Min(1) @Max(99) int gang,
        @Min(1) @Max(99) int regal,
        @Min(1) @Max(9) int ebene,
        @Min(1) @Max(10_000) int kapazitaet,
        @NotNull @Valid LagerplatzMasseRequest masse) {
}
