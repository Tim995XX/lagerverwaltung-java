package de.hilpert.lager.repository;

import de.hilpert.lager.domain.TransportStatus;
import de.hilpert.lager.domain.Transportauftrag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface TransportauftragRepository extends JpaRepository<Transportauftrag, Long> {

    List<Transportauftrag> findByStatus(TransportStatus status);

    /** Plätze, von denen noch Ware abgeholt wird: dort steht sie physisch noch. */
    @Query("""
            select distinct t.vonOrt from Transportauftrag t
            where t.art = de.hilpert.lager.domain.TransportArt.AUSLAGERUNG
              and t.status <> de.hilpert.lager.domain.TransportStatus.ERLEDIGT
            """)
    Set<String> plaetzeMitOffenerAuslagerung();

    List<Transportauftrag> findByKommissionierauftragId(Long kommissionierauftragId);
}
