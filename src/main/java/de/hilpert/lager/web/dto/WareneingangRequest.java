package de.hilpert.lager.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record WareneingangRequest(@NotBlank String artikelNummer, @Positive int menge) {
}
