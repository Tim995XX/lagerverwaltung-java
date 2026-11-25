package de.hilpert.lager.service;

import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.fehler.KonfliktException;
import de.hilpert.lager.fehler.NichtGefundenException;
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
    public Lagerplatz anlegen(int gang, int regal, int ebene, int kapazitaet,
                              int laengeMm, int breiteMm, int hoeheMm, int maxGewichtKg) {
        String code = Lagerplatz.codeFuer(gang, regal, ebene);
        if (lagerplatzRepository.existsByCode(code)) {
            throw new KonfliktException("Lagerplatz " + code + " existiert bereits");
        }
        Lagerplatz platz = new Lagerplatz(gang, regal, ebene, kapazitaet);
        platz.setzeMasse(laengeMm, breiteMm, hoeheMm, maxGewichtKg);
        return lagerplatzRepository.save(platz);
    }

    /** Maße nachtragen, solange der Platz noch keine hat oder nicht belegt bzw. reserviert ist. */
    @Transactional
    public Lagerplatz setzeMasse(String code, int laengeMm, int breiteMm, int hoeheMm, int maxGewichtKg) {
        Lagerplatz platz = finde(code);
        if (platz.hatMasse() && bestandRepository.findByLagerplatzCode(code).isPresent()) {
            throw new KonfliktException("Lagerplatz " + code + " ist belegt, Maße sind gesperrt");
        }
        platz.setzeMasse(laengeMm, breiteMm, hoeheMm, maxGewichtKg);
        return platz;
    }

    @Transactional
    public Lagerplatz sperren(String code, String grund) {
        Lagerplatz platz = finde(code);
        platz.sperren(grund);
        return platz;
    }

    @Transactional
    public Lagerplatz freigeben(String code) {
        Lagerplatz platz = finde(code);
        platz.freigeben();
        return platz;
    }

    private Lagerplatz finde(String code) {
        return lagerplatzRepository.findByCode(code)
                .orElseThrow(() -> new NichtGefundenException("Lagerplatz " + code + " nicht gefunden"));
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
