package de.hilpert.lager.web.dto;

import de.hilpert.lager.domain.Prioritaet;
import jakarta.validation.constraints.NotNull;

public record PrioritaetRequest(@NotNull Prioritaet prioritaet) {
}
