package de.hilpert.lager.repository;

import de.hilpert.lager.domain.Klaerfall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KlaerfallRepository extends JpaRepository<Klaerfall, Long> {

    List<Klaerfall> findByErledigtOrderByErstelltAmAsc(boolean erledigt);
}
