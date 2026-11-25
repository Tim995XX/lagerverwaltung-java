package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Lagerplatz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LagerplatzRepository extends JpaRepository<Lagerplatz, Long> {

    List<Lagerplatz> findAllByOrderByCodeAsc();

    Optional<Lagerplatz> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Vorauswahl: leere, nicht reservierte und nicht gesperrte Plätze mit ausreichender Obergrenze.
     * Maße, Gewicht und Reihenfolge prüft danach die Platzsuche.
     */
    @Query("""
            select p from Lagerplatz p
            where p.kapazitaet >= :menge
              and p.gesperrt = false
              and not exists (select b from Bestand b where b.lagerplatz = p)
            order by p.kapazitaet asc, p.code asc
            """)
    List<Lagerplatz> findeLeerePlaetzeFuer(int menge);
}
