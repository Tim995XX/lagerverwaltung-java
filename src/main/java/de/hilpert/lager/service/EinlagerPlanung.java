package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.LagerplatzRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Sucht einen Platz und reserviert ihn: für neue Ware und beim Umleiten. */
@Component
public class EinlagerPlanung {

    private final BestandRepository bestandRepository;
    private final LagerplatzRepository lagerplatzRepository;
    private final TransportauftragRepository transportauftragRepository;
    private final PlatzSuche platzSuche;

    public EinlagerPlanung(BestandRepository bestandRepository, LagerplatzRepository lagerplatzRepository,
                           TransportauftragRepository transportauftragRepository, PlatzSuche platzSuche) {
        this.bestandRepository = bestandRepository;
        this.lagerplatzRepository = lagerplatzRepository;
        this.transportauftragRepository = transportauftragRepository;
        this.platzSuche = platzSuche;
    }

    public record Reservierung(String platzCode, String begruendung) {
    }

    public Optional<Reservierung> reserviere(Artikel artikel, int menge) {
        // Dort steht noch Ware, die erst abgeholt werden muss
        Set<String> nochBelegt = transportauftragRepository.plaetzeMitOffenerAuslagerung();

        Optional<Bestand> zulagerung = findeZulagerung(artikel, menge, nochBelegt);
        if (zulagerung.isPresent()) {
            zulagerung.get().reservieren(menge);
            return Optional.of(new Reservierung(zulagerung.get().getLagerplatz().getCode(),
                    "Artikel liegt dort bereits und es ist genug Platz"));
        }
        return findeLeerenPlatz(artikel, menge, nochBelegt).map(platz -> {
            bestandRepository.save(Bestand.reservierung(artikel, platz, menge));
            return new Reservierung(platz.getCode(), platzSuche.begruendung(artikel));
        });
    }

    /** 1. Wahl: ein Platz, auf dem der Artikel schon liegt und noch genug frei ist. */
    private Optional<Bestand> findeZulagerung(Artikel artikel, int menge, Set<String> nochBelegt) {
        return bestandRepository.findByArtikelOrderByMengeAsc(artikel).stream()
                .filter(bestand -> !bestand.getLagerplatz().isGesperrt())
                .filter(bestand -> !nochBelegt.contains(bestand.getLagerplatz().getCode()))
                .filter(bestand -> bestand.freieKapazitaet() >= menge)
                .findFirst();
    }

    /** 2. Wahl: der sinnvollste leere Platz. */
    private Optional<Lagerplatz> findeLeerenPlatz(Artikel artikel, int menge, Set<String> nochBelegt) {
        List<Lagerplatz> frei = lagerplatzRepository.findeLeerePlaetzeFuer(menge).stream()
                .filter(platz -> !nochBelegt.contains(platz.getCode()))
                .toList();
        return platzSuche.besterPlatz(artikel, menge, frei);
    }
}
