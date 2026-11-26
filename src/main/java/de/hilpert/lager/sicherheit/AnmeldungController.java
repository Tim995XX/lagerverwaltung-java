package de.hilpert.lager.sicherheit;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Oberfläche prüft hiermit die Zugangsdaten und erfährt die Rolle, um die passenden Menüpunkte zu zeigen.
 * Was erlaubt ist, entscheidet trotzdem nur der Server bei jeder Anfrage.
 */
@RestController
public class AnmeldungController {

    public record IchResponse(String name, Rolle rolle) {
    }

    @GetMapping("/api/ich")
    public IchResponse ich(Authentication anmeldung) {
        Rolle rolle = anmeldung.getAuthorities().stream()
                .map(recht -> recht.getAuthority().replaceFirst("^ROLE_", ""))
                .map(Rolle::valueOf)
                .findFirst()
                .orElseThrow();
        return new IchResponse(anmeldung.getName(), rolle);
    }
}
