package de.hilpert.lager.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Warum ist der Platz nicht nutzbar? z. B. "Regalboden beschädigt". */
public record SperrRequest(@NotBlank @Size(max = 200) String grund) {
}
