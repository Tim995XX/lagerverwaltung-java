package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Prioritaet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Ohne Angabe gilt Priorität NORMAL. */
public record KommissionierauftragRequest(
        @NotEmpty List<@Valid Position> positionen,
        Prioritaet prioritaet) {

    public record Position(@NotBlank String artikelNummer, @Positive int menge) {
    }
}
