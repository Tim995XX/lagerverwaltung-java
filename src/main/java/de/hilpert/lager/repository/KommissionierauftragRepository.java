package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Kommissionierauftrag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KommissionierauftragRepository extends JpaRepository<Kommissionierauftrag, Long> {
}
