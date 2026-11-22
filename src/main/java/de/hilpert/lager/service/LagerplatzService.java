package de.hilpert.lager.service;

import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.fehler.KonfliktException;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.LagerplatzRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lagerplätze anlegen und den aktuellen Bestand je Platz abfragen. */
@Service
public class LagerplatzService {

    private final LagerplatzRepository lagerplatzRepository;
    private final BestandRepository bestandRepository;

    public LagerplatzService(LagerplatzRepository lagerplatzRepository, BestandRepository bestandRepository) {
        this.lagerplatzRepository = lagerplatzRepository;
        this.bestandRepository = bestandRepository;
    }

    @Transactional
    public Lagerplatz anlegen(int gang, int regal, int ebene, int kapazitaet) {
        String code = Lagerplatz.codeFuer(gang, regal, ebene);
        if (lagerplatzRepository.existsByCode(code)) {
            throw new KonfliktException("Lagerplatz " + code + " existiert bereits");
        }
        return lagerplatzRepository.save(new Lagerplatz(gang, regal, ebene, kapazitaet));
    }

    @Transactional(readOnly = true)
    public List<Lagerplatz> alle() {
        return lagerplatzRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<Bestand> bestaende() {
        return bestandRepository.findAllByOrderByLagerplatzCodeAsc();
    }
}
