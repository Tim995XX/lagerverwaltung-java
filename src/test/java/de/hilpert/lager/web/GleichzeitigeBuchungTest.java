package de.hilpert.lager.web;

import de.hilpert.lager.TestDaten;
import de.hilpert.lager.PostgresTestKonfiguration;
import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.repository.ArtikelRepository;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.LagerplatzRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Zwei Wareneingänge wollen denselben leeren Platz belegen. Der Ablauf ist nicht zufällig:
 * Die erste Buchung bleibt offen, bis die zweite nachweislich in der Datenbank auf sie wartet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestKonfiguration.class)
class GleichzeitigeBuchungTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transaktionen;
    @Autowired ArtikelRepository artikelRepository;
    @Autowired LagerplatzRepository lagerplatzRepository;
    @Autowired BestandRepository bestandRepository;
    @Autowired TransportauftragRepository transportauftragRepository;

    @BeforeEach
    @AfterEach
    void leereDatenbank() {
        jdbc.execute("truncate scan, transportauftrag, klaerfall, kommissionierauftrag_positionen, "
                + "kommissionierauftrag, bestand, lagerplatz, artikel");
    }

    @Test
    void zweiteBuchungAufDenselbenPlatzBekommt409UndBuchtNichts() throws Exception {
        Artikel artikel = artikelRepository.save(TestDaten.artikel("1001"));
        Lagerplatz platz = lagerplatzRepository.save(TestDaten.platz(1, 1, 1, 40));

        CompletableFuture<MvcResult> zweite = new TransactionTemplate(transaktionen).execute(status -> {
            bestandRepository.saveAndFlush(new Bestand(artikel, platz, 10));
            CompletableFuture<MvcResult> anfrage = CompletableFuture.supplyAsync(this::wareneingang);
            warteBisDatenbankBlockiert();
            return anfrage;
        });

        MvcResult antwort = zweite.get(10, TimeUnit.SECONDS);
        assertThat(antwort.getResponse().getStatus()).isEqualTo(409);
        assertThat(antwort.getResponse().getContentAsString()).contains("Konflikt");
        assertThat(bestandRepository.findAll()).singleElement()
                .satisfies(b -> assertThat(b.getMenge()).isEqualTo(10));
        assertThat(transportauftragRepository.count()).isZero();

        // Ein neuer Versuch gelingt: Der Platz hat noch Kapazität, die Ware wird zugelagert
        assertThat(wareneingang().getResponse().getStatus()).isEqualTo(200);
        assertThat(bestandRepository.findAll()).singleElement()
                .satisfies(b -> {
                    assertThat(b.getMenge()).isEqualTo(10);
                    assertThat(b.getReserviert()).isEqualTo(5);
                });
    }

    private MvcResult wareneingang() {
        try {
            return mockMvc.perform(post("/api/wareneingaenge").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"artikelNummer": "1001", "menge": 5}""")).andReturn();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void warteBisDatenbankBlockiert() {
        Instant ende = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(ende)) {
            Integer wartend = jdbc.queryForObject("select count(*) from pg_locks where not granted", Integer.class);
            if (wartend != null && wartend > 0) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Die zweite Buchung hat nicht auf die erste gewartet");
    }
}
