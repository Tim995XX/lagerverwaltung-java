package de.hilpert.lager.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ArtikelRequest(
        @NotBlank @Size(max = 20) String artikelNummer,
        @NotBlank @Size(max = 100) String bezeichnung,
        @NotNull @Valid ArtikelMasseRequest masse) {
}
