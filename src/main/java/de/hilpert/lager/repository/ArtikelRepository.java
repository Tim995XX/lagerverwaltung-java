package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Artikel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtikelRepository extends JpaRepository<Artikel, Long> {

    Optional<Artikel> findByArtikelNummer(String artikelNummer);

    boolean existsByArtikelNummer(String artikelNummer);
}
