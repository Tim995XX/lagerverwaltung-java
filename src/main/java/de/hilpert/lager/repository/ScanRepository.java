package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Scan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    List<Scan> findByTransportauftragIdOrderByIdAsc(Long transportauftragId);
}
