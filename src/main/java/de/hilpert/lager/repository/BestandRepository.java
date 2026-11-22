package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BestandRepository extends JpaRepository<Bestand, Long> {

    /** Kleinste Mengen zuerst: So werden angebrochene Plätze zuerst geleert. */
    List<Bestand> findByArtikelOrderByMengeAsc(Artikel artikel);

    List<Bestand> findAllByOrderByLagerplatzCodeAsc();
}
