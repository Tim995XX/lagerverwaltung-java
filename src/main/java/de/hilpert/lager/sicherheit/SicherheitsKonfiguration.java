package de.hilpert.lager.sicherheit;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

/**
 * Wer darf was: alle Regeln an einer Stelle, nach URL und HTTP-Methode.
 * Anmeldung per HTTP Basic, ohne Session: Jede Anfrage bringt ihre Zugangsdaten selbst mit.
 */
@Configuration
public class SicherheitsKonfiguration {

    private static final String LEITSTAND = Rolle.LEITSTAND.name();
    private static final String STAPLERFAHRER = Rolle.STAPLERFAHRER.name();

    @Bean
    SecurityFilterChain sicherheitsRegeln(HttpSecurity http) throws Exception {
        http
                // Kein Login-Formular und kein Session-Cookie: Der Browser schickt nichts automatisch mit,
                // deshalb ist CSRF-Schutz hier nicht nötig (anders bei einer späteren Oberfläche mit Cookies).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint((anfrage, antwort, fehler) ->
                        nichtAngemeldet(antwort)))
                .exceptionHandling(fehler -> fehler
                        .authenticationEntryPoint((anfrage, antwort, e) -> nichtAngemeldet(antwort))
                        .accessDeniedHandler((anfrage, antwort, e) -> problem(antwort, 403, "Keine Berechtigung",
                                "Diese Aktion ist für die eigene Rolle nicht freigegeben")))
                .authorizeHttpRequests(regeln -> regeln
                        .requestMatchers("/actuator/health", "/actuator/health/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        // Lesen dürfen beide Rollen
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(LEITSTAND, STAPLERFAHRER)
                        // Der Fahrer arbeitet seine Transportaufträge ab
                        .requestMatchers(HttpMethod.POST,
                                "/api/transportauftraege/*/start",
                                "/api/transportauftraege/*/scan",
                                "/api/transportauftraege/*/abschluss",
                                "/api/transportauftraege/*/umleiten").hasAnyRole(LEITSTAND, STAPLERFAHRER)
                        // Alles andere unter /api ändert Stammdaten, Prioritäten oder Klärfälle
                        .requestMatchers("/api/**").hasRole(LEITSTAND)
                        // Unbekannte Pfade sind gesperrt statt versehentlich offen
                        .anyRequest().denyAll());
        return http.build();
    }

    /** Speichert neue Passwörter mit BCrypt und schreibt das Verfahren als Präfix davor, z. B. "{bcrypt}". */
    @Bean
    PasswordEncoder passwortEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static void nichtAngemeldet(HttpServletResponse antwort) throws IOException {
        antwort.setHeader("WWW-Authenticate", "Basic realm=\"lagerverwaltung\"");
        problem(antwort, 401, "Nicht angemeldet", "Bitte mit Benutzername und Passwort anmelden");
    }

    /** Gleiches Format wie FehlerBehandlung (Problem Details), nur ohne Spring MVC, weil der Filter vorher greift. */
    private static void problem(HttpServletResponse antwort, int status, String titel, String detail)
            throws IOException {
        antwort.setStatus(status);
        antwort.setContentType("application/problem+json");
        antwort.setCharacterEncoding("UTF-8");
        antwort.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titel + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\"}");
    }
}
