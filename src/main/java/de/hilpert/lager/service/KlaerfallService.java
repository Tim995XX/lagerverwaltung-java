package de.hilpert.lager.service;

import de.hilpert.lager.domain.Klaerfall;
import de.hilpert.lager.fehler.NichtGefundenException;
import de.hilpert.lager.repository.KlaerfallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Der Klärplatz: offene Fälle ansehen und nach manueller Klärung abschließen. */
@Service
public class KlaerfallService {

    private final KlaerfallRepository repository;

    public KlaerfallService(KlaerfallRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Klaerfall> liste(boolean erledigt) {
        return repository.findByErledigtOrderByErstelltAmAsc(erledigt);
    }

    @Transactional
    public Klaerfall erledigen(Long id, String kommentar) {
        Klaerfall klaerfall = repository.findById(id)
                .orElseThrow(() -> new NichtGefundenException("Klärfall " + id + " nicht gefunden"));
        klaerfall.erledigen(kommentar);
        return klaerfall;
    }
}
