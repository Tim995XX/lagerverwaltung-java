package de.hilpert.lager.repository;

import de.hilpert.lager.PostgresTestKonfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** V2 muss auf einer Datenbank mit V1-Daten laufen, nicht nur auf einer leeren. */
@DataJpaTest
@Import(PostgresTestKonfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MigrationMitAltdatenTest {

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @Test
    void alteDatenBleibenUndAlteEinlagerungenGeltenAlsGebucht() {
        try {
            migriere("1");
            jdbc.execute("insert into altdaten.artikel values (1, '1001', 'Wasser')");
            jdbc.execute("insert into altdaten.lagerplatz values (1, '01-01-1', 1, 1, 1, 40)");
            jdbc.execute("insert into altdaten.bestand values (1, 1, 1, 10, 0)");
            jdbc.execute("insert into altdaten.transportauftrag values (1, 'EINLAGERUNG', '1001', 10, "
                    + "'WE-ZONE', '01-01-1', 'NORMAL', 'OFFEN', null, now())");

            migriere("2");

            assertThat(jdbc.queryForObject("select menge from altdaten.bestand", Integer.class)).isEqualTo(10);
            assertThat(jdbc.queryForObject("select reserviert from altdaten.bestand", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "select bestand_gebucht from altdaten.transportauftrag", Boolean.class)).isTrue();
        } finally {
            jdbc.execute("drop schema if exists altdaten cascade");
        }
    }

    private void migriere(String version) {
        Flyway.configure().dataSource(dataSource).schemas("altdaten").target(version).load().migrate();
    }
}
