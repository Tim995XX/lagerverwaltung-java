package de.hilpert.lager.repository;

import de.hilpert.lager.PostgresTestKonfiguration;
import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.Lagerplatz;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prüft, dass Flyway gelaufen ist und die Datenbank falsche Daten auch selbst ablehnt. */
@DataJpaTest
@Import(PostgresTestKonfiguration.class)
class DatenbankSchemaTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired LagerplatzRepository lagerplatzRepository;
    @Autowired ArtikelRepository artikelRepository;
    @Autowired BestandRepository bestandRepository;

    @Test
    void laeuftAufPostgresMitErfolgreicherMigration() {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL 17");
        assertThat(jdbc.queryForList(
                "select version from flyway_schema_history where success", String.class)).contains("1");
    }

    @Test
    void negativerBestandWirdAbgelehnt() {
        Lagerplatz platz = lagerplatzRepository.save(new Lagerplatz(1, 1, 1, 40));
        Artikel artikel = artikelRepository.save(new Artikel("1001", "Wasser"));

        assertThatThrownBy(() -> bestandRepository.saveAndFlush(new Bestand(artikel, platz, -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void zweiBestaendeAufEinemPlatzWerdenAbgelehnt() {
        Lagerplatz platz = lagerplatzRepository.save(new Lagerplatz(1, 1, 1, 40));
        Artikel artikel = artikelRepository.save(new Artikel("1001", "Wasser"));
        bestandRepository.saveAndFlush(new Bestand(artikel, platz, 5));

        assertThatThrownBy(() -> bestandRepository.saveAndFlush(new Bestand(artikel, platz, 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
