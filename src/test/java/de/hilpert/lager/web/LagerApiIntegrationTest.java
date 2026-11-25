package de.hilpert.lager.web;

import de.hilpert.lager.PostgresTestKonfiguration;
import de.hilpert.lager.repository.ArtikelRepository;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.KlaerfallRepository;
import de.hilpert.lager.repository.KommissionierauftragRepository;
import de.hilpert.lager.repository.LagerplatzRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests: kompletter Spring-Kontext mit PostgreSQL (Testcontainers),
 * Aufrufe laufen über MockMvc durch Controller, Service und Repository.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestKonfiguration.class)
class LagerApiIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Autowired TransportauftragRepository transportauftragRepository;
    @Autowired KlaerfallRepository klaerfallRepository;
    @Autowired KommissionierauftragRepository kommissionierauftragRepository;
    @Autowired BestandRepository bestandRepository;
    @Autowired LagerplatzRepository lagerplatzRepository;
    @Autowired ArtikelRepository artikelRepository;

    @BeforeEach
    void leereDatenbankMitEinemArtikelUndZweiPlaetzen() throws Exception {
        transportauftragRepository.deleteAll();
        klaerfallRepository.deleteAll();
        kommissionierauftragRepository.deleteAll();
        bestandRepository.deleteAll();
        lagerplatzRepository.deleteAll();
        artikelRepository.deleteAll();

        postJson("/api/artikel", """
                {"artikelNummer": "1001", "bezeichnung": "Mineralwasser"}""").andExpect(status().isCreated());
        postJson("/api/lagerplaetze", """
                {"gang": 1, "regal": 1, "ebene": 1, "kapazitaet": 40}""").andExpect(status().isCreated());
        postJson("/api/lagerplaetze", """
                {"gang": 2, "regal": 1, "ebene": 1, "kapazitaet": 120}""").andExpect(status().isCreated());
    }

    @Test
    void ablaufWareneingangKommissionierungTransport() throws Exception {
        // Wareneingang: 30 Stück passen auf den kleinen Platz
        postJson("/api/wareneingaenge", """
                {"artikelNummer": "1001", "menge": 30}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ergebnis").value("EINGELAGERT"))
                .andExpect(jsonPath("$.lagerplatz").value("01-01-1"));

        // Kommissionierung mit hoher Priorität
        postJson("/api/kommissionierauftraege", """
                {"positionen": [{"artikelNummer": "1001", "menge": 12}], "prioritaet": "HOCH"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FREIGEGEBEN"))
                .andExpect(jsonPath("$.transportauftraege", hasSize(1)))
                .andExpect(jsonPath("$.transportauftraege[0].vonOrt").value("01-01-1"));

        mockMvc.perform(get("/api/bestaende"))
                .andExpect(jsonPath("$[0].menge").value(18));

        // Die Auslagerung (HOCH) steht vor der älteren Einlagerung (NORMAL)
        mockMvc.perform(get("/api/transportauftraege").param("status", "OFFEN"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].art").value("AUSLAGERUNG"))
                .andExpect(jsonPath("$[1].art").value("EINLAGERUNG"));
    }

    @Test
    void transportStatuswechselUndUngueltigerWechsel() throws Exception {
        postJson("/api/wareneingaenge", """
                {"artikelNummer": "1001", "menge": 10}""").andExpect(status().isOk());
        long id = transportauftragRepository.findAll().getFirst().getId();

        mockMvc.perform(post("/api/transportauftraege/{id}/abschluss", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Konflikt"));

        mockMvc.perform(post("/api/transportauftraege/{id}/start", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_ARBEIT"));
        mockMvc.perform(post("/api/transportauftraege/{id}/abschluss", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERLEDIGT"));

        mockMvc.perform(put("/api/transportauftraege/{id}/prioritaet", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prioritaet": "HOCH"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void fehlmengeLandetAufDemKlaerplatzUndKannErledigtWerden() throws Exception {
        postJson("/api/kommissionierauftraege", """
                {"positionen": [{"artikelNummer": "1001", "menge": 5}]}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_KLAERUNG"))
                .andExpect(jsonPath("$.prioritaet").value("NORMAL"));

        mockMvc.perform(get("/api/klaerfaelle"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].grund").value("BESTAND_NICHT_AUSREICHEND"));
        long id = klaerfallRepository.findAll().getFirst().getId();

        postJson("/api/klaerfaelle/" + id + "/erledigen", """
                {"kommentar": "Nachschub bestellt"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.erledigt").value(true));

        mockMvc.perform(get("/api/klaerfaelle")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void zuGrosseAnlieferungKommtAufDenKlaerplatz() throws Exception {
        postJson("/api/wareneingaenge", """
                {"artikelNummer": "1001", "menge": 500}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ergebnis").value("KLAERPLATZ"))
                .andExpect(jsonPath("$.klaerfallId").isNumber());
    }

    @Test
    void ungueltigeEingabenGebenSinnvolleStatuscodes() throws Exception {
        postJson("/api/wareneingaenge", """
                {"artikelNummer": "", "menge": 0}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.felder.artikelNummer").exists())
                .andExpect(jsonPath("$.felder.menge").exists());

        postJson("/api/wareneingaenge", """
                {"artikelNummer": "9999", "menge": 1}""")
                .andExpect(status().isNotFound());

        postJson("/api/artikel", """
                {"artikelNummer": "1001", "bezeichnung": "doppelt"}""")
                .andExpect(status().isConflict());

        postJson("/api/kommissionierauftraege", """
                {"positionen": [{"artikelNummer": "1001", "menge": 1}], "prioritaet": "DRINGEND"}""")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/transportauftraege").param("status", "FALSCH"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
