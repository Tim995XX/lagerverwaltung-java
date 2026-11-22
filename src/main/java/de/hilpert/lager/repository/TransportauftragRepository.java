package de.hilpert.lager.repository;

import de.hilpert.lager.domain.TransportStatus;
import de.hilpert.lager.domain.Transportauftrag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportauftragRepository extends JpaRepository<Transportauftrag, Long> {

    List<Transportauftrag> findByStatus(TransportStatus status);

    List<Transportauftrag> findByKommissionierauftragId(Long kommissionierauftragId);
}
