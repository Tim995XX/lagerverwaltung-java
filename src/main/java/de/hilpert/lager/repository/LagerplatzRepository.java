package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Lagerplatz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LagerplatzRepository extends JpaRepository<Lagerplatz, Long> {

    List<Lagerplatz> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);

    /**
     * Leere Plätze, auf die die Menge passt – der kleinste passende zuerst ("Best Fit"),
     * damit große Plätze für große Mengen frei bleiben.
     */
    @Query("""
            select p from Lagerplatz p
            where p.kapazitaet >= :menge
              and not exists (select b from Bestand b where b.lagerplatz = p)
            order by p.kapazitaet asc, p.code asc
            """)
    List<Lagerplatz> findeLeerePlaetzeFuer(int menge);
}
