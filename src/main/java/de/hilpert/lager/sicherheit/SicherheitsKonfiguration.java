package de.hilpert.lager.sicherheit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import de.hilpert.lager.web.OberflaecheController;
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

    /**
     * Content Security Policy: Skripte und Styles nur vom eigenen Server, keine fremden Seiten im Frame.
     * Bremst eingeschleustes JavaScript (XSS), falls doch einmal Benutzereingaben ungefiltert im HTML landen.
     */
    private static final String INHALTS_REGELN = "default-src 'self'; img-src 'self' data:; "
            + "style-src 'self' 'unsafe-inline'; script-src 'self'; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    SecurityFilterChain sicherheitsRegeln(HttpSecurity http) throws Exception {
        http
                // Kein Session-Cookie: Die Oberfläche hält die Zugangsdaten nur im Speicher und setzt den
                // Authorization-Header selbst. Der Browser schickt also nichts automatisch mit, CSRF entfällt.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint((anfrage, antwort, fehler) ->
                        nichtAngemeldet(anfrage, antwort)))
                .exceptionHandling(fehler -> fehler
                        .authenticationEntryPoint((anfrage, antwort, e) -> nichtAngemeldet(anfrage, antwort))
                        .accessDeniedHandler((anfrage, antwort, e) -> problem(antwort, 403, "Keine Berechtigung",
                                "Diese Aktion ist für die eigene Rolle nicht freigegeben")))
                .headers(kopf -> kopf.contentSecurityPolicy(csp -> csp.policyDirectives(INHALTS_REGELN)))
                .authorizeHttpRequests(regeln -> regeln
                        .requestMatchers("/actuator/health", "/actuator/health/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        // Die Oberfläche selbst ist öffentlich, ihre Daten kommen nur mit Anmeldung
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/favicon.svg")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, OberflaecheController.SEITEN).permitAll()
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

    /**
     * Mit WWW-Authenticate öffnet der Browser seinen eigenen Anmeldedialog. Die Oberfläche hat ein eigenes
     * Formular und kennzeichnet ihre Anfragen mit X-Requested-With, dann fehlt der Header.
     */
    private static void nichtAngemeldet(HttpServletRequest anfrage, HttpServletResponse antwort) throws IOException {
        if (!"XMLHttpRequest".equals(anfrage.getHeader("X-Requested-With"))) {
            antwort.setHeader("WWW-Authenticate", "Basic realm=\"lagerverwaltung\"");
        }
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
