package de.hilpert.lager.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Was wurde getan? z. B. "Nachschub gebucht, Auftrag neu angelegt". */
public record KlaerfallErledigenRequest(@NotBlank @Size(max = 500) String kommentar) {
}
