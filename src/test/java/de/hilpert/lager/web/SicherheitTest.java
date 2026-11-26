package de.hilpert.lager.web;

import de.hilpert.lager.PostgresTestKonfiguration;
import de.hilpert.lager.sicherheit.Benutzer;
import de.hilpert.lager.sicherheit.BenutzerRepository;
import de.hilpert.lager.sicherheit.Rolle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Anmeldung und Rollen mit echten Benutzern aus PostgreSQL (kein @WithMockUser),
 * damit auch Passwortprüfung und das Laden aus der Datenbank mitgetestet werden.
 * Ein 404 oder 400 heißt hier: Die Rechteprüfung wurde bestanden, erst danach ging etwas schief.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestKonfiguration.class)
class SicherheitTest {

    private static final RequestPostProcessor LEITSTAND = httpBasic("leitstand", "leitstand-geheim");
    private static final RequestPostProcessor FAHRER = httpBasic("fahrer", "fahrer-geheim");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired BenutzerRepository benutzerRepository;
    @Autowired PasswordEncoder passwortEncoder;

    @BeforeEach
    void benutzerAnlegen() {
        benutzerRepository.deleteAll();
        benutzerRepository.save(new Benutzer("leitstand", passwortEncoder.encode("leitstand-geheim"), Rolle.LEITSTAND));
        benutzerRepository.save(new Benutzer("fahrer", passwortEncoder.encode("fahrer-geheim"), Rolle.STAPLERFAHRER));
        Benutzer ehemalig = new Benutzer("ehemalig", passwortEncoder.encode("ehemalig-geheim"), Rolle.LEITSTAND);
        ehemalig.deaktivieren();
        benutzerRepository.save(ehemalig);
    }

    @Test
    void ohneAnmeldungGibt401AlsProblemDetails() throws Exception {
        mockMvc.perform(get("/api/artikel"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"lagerverwaltung\""))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Nicht angemeldet"));
    }

    @Test
    void falschesPasswortUnbekannterUndDeaktivierterBenutzerGeben401() throws Exception {
        mockMvc.perform(get("/api/artikel").with(httpBasic("fahrer", "falsch"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/artikel").with(httpBasic("niemand", "egal"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/artikel").with(httpBasic("ehemalig", "ehemalig-geheim")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthUndSwaggerSindOhneAnmeldungErreichbar() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void unbekannterPfadIstGesperrt() throws Exception {
        mockMvc.perform(get("/intern/irgendwas").with(LEITSTAND)).andExpect(status().isForbidden());
    }

    @Test
    void fahrerDarfLesen() throws Exception {
        mockMvc.perform(get("/api/transportauftraege").with(FAHRER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/bestaende").with(FAHRER)).andExpect(status().isOk());
        mockMvc.perform(get("/api/klaerfaelle").with(FAHRER)).andExpect(status().isOk());
    }

    @Test
    void fahrerDarfTransportauftraegeAbarbeiten() throws Exception {
        // 404, nicht 403: Der Fahrer kommt durch, nur den Auftrag 999 gibt es nicht
        mockMvc.perform(post("/api/transportauftraege/999/start").with(FAHRER)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/transportauftraege/999/abschluss").with(FAHRER)).andExpect(status().isNotFound());
        mockMvc.perform(json(post("/api/transportauftraege/999/scan"),
                        "{\"artikelNummer\": \"1001\", \"lagerplatzCode\": \"01-01-1\"}").with(FAHRER))
                .andExpect(status().isNotFound());
        mockMvc.perform(json(post("/api/transportauftraege/999/umleiten"), "{\"grund\": \"Regal beschädigt\"}")
                        .with(FAHRER))
                .andExpect(status().isNotFound());
    }

    @Test
    void fahrerDarfKeineStammdatenPrioritaetenOderKlaerfaelleAendern() throws Exception {
        String artikel = """
                {"artikelNummer": "9999", "bezeichnung": "Test"}""";
        erwarte403(json(post("/api/artikel"), artikel));
        erwarte403(json(post("/api/wareneingaenge"), "{\"artikelNummer\": \"1001\", \"menge\": 5}"));
        erwarte403(json(put("/api/transportauftraege/1/prioritaet"), "{\"prioritaet\": \"HOCH\"}"));
        erwarte403(json(post("/api/klaerfaelle/1/erledigen"), "{\"kommentar\": \"erledigt\"}"));
        erwarte403(json(put("/api/lagerplaetze/01-01-1/sperre"), "{\"grund\": \"defekt\"}"));
    }

    @Test
    void leitstandDarfAendern() throws Exception {
        // 400 statt 403: Die Rechte passen, nur der leere Auftrag ist ungültig
        mockMvc.perform(json(post("/api/kommissionierauftraege"), "{}").with(LEITSTAND))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(put("/api/transportauftraege/999/prioritaet"), "{\"prioritaet\": \"HOCH\"}")
                        .with(LEITSTAND))
                .andExpect(status().isNotFound());
    }

    @Test
    void ichNenntNameUndRolle() throws Exception {
        mockMvc.perform(get("/api/ich").with(FAHRER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("fahrer"))
                .andExpect(jsonPath("$.rolle").value("STAPLERFAHRER"));
        mockMvc.perform(get("/api/ich").with(LEITSTAND)).andExpect(jsonPath("$.rolle").value("LEITSTAND"));
    }

    @Test
    void anfrageAusDerOberflaecheOeffnetKeinenBrowserDialog() throws Exception {
        mockMvc.perform(get("/api/ich").header("X-Requested-With", "XMLHttpRequest")
                        .with(httpBasic("fahrer", "falsch")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void seitenDerOberflaecheSindOhneAnmeldungErreichbarUndHabenCsp() throws Exception {
        // Im Test gibt es keine gebaute index.html, deshalb zählt nur: keine 401/403
        int status = mockMvc.perform(get("/transporte")).andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
        mockMvc.perform(get("/api/ich").with(FAHRER))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
    }

    @Test
    void passwortStehtNurAlsBcryptHashInDerDatenbank() {
        String hash = jdbc.queryForObject("select passwort_hash from benutzer where name = 'fahrer'", String.class);

        assertThat(hash).startsWith("{bcrypt}$2").doesNotContain("fahrer-geheim");
    }

    private void erwarte403(MockHttpServletRequestBuilder anfrage) throws Exception {
        mockMvc.perform(anfrage.with(FAHRER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Keine Berechtigung"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder anfrage, String body) {
        return anfrage.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
