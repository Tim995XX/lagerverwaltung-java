package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.Lagerplatz;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Prüft die eigene JPQL-Abfrage gegen eine echte (In-Memory-)H2-Datenbank. */
@DataJpaTest
class LagerplatzRepositoryTest {

    @Autowired LagerplatzRepository lagerplatzRepository;
    @Autowired ArtikelRepository artikelRepository;
    @Autowired BestandRepository bestandRepository;

    @Test
    void findetNurLeerePlaetzeMitGenugKapazitaetKleinsterZuerst() {
        Lagerplatz gross = lagerplatzRepository.save(new Lagerplatz(2, 1, 1, 120));
        Lagerplatz klein = lagerplatzRepository.save(new Lagerplatz(1, 1, 1, 40));
        Lagerplatz zuKlein = lagerplatzRepository.save(new Lagerplatz(1, 1, 2, 10));
        Lagerplatz belegt = lagerplatzRepository.save(new Lagerplatz(1, 2, 1, 40));
        Artikel artikel = artikelRepository.save(new Artikel("1001", "Wasser"));
        bestandRepository.save(new Bestand(artikel, belegt, 5));

        assertThat(lagerplatzRepository.findeLeerePlaetzeFuer(30))
                .containsExactly(klein, gross)
                .doesNotContain(zuKlein, belegt);
    }
}
