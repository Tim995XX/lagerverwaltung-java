package de.hilpert.lager.sicherheit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {

    Optional<Benutzer> findByName(String name);
}
