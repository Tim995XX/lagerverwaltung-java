package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.KlaerGrund;
import de.hilpert.lager.domain.Klaerfall;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.TransportArt;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.KlaerfallRepository;
import de.hilpert.lager.repository.LagerplatzRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Wareneingang: sucht für eine angelieferte Menge einen Lagerplatz, bucht den Bestand
 * und legt einen Transportauftrag für die physische Einlagerung an.
 * <p>
 * Vereinfachung: Der Bestand wird sofort gebucht, nicht erst bei Quittierung des Transports.
 * Eine Anlieferung wird nicht auf mehrere Plätze aufgeteilt.
 */
@Service
public class WareneingangService {

    private final ArtikelService artikelService;
    private final BestandRepository bestandRepository;
    private final LagerplatzRepository lagerplatzRepository;
    private final TransportauftragRepository transportauftragRepository;
    private final KlaerfallRepository klaerfallRepository;
    private final PlatzSuche platzSuche;

    public WareneingangService(ArtikelService artikelService,
                               BestandRepository bestandRepository,
                               LagerplatzRepository lagerplatzRepository,
                               TransportauftragRepository transportauftragRepository,
                               KlaerfallRepository klaerfallRepository,
                               PlatzSuche platzSuche) {
        this.artikelService = artikelService;
        this.bestandRepository = bestandRepository;
        this.lagerplatzRepository = lagerplatzRepository;
        this.transportauftragRepository = transportauftragRepository;
        this.klaerfallRepository = klaerfallRepository;
        this.platzSuche = platzSuche;
    }

    @Transactional
    public WareneingangErgebnis einlagern(String artikelNummer, int menge) {
        Artikel artikel = artikelService.finde(artikelNummer);
        if (!artikel.hatMasse()) {
            return klaerplatz(KlaerGrund.MASSE_FEHLEN, artikelNummer, menge,
                    "Maße oder Gewicht von Artikel " + artikelNummer + " fehlen");
        }

        // Dort steht noch Ware, die erst abgeholt werden muss
        Set<String> gesperrt = transportauftragRepository.plaetzeMitOffenerAuslagerung();

        Bestand bestand;
        String begruendung;
        Optional<Bestand> zulagerung = findeZulagerung(artikel, menge, gesperrt);
        if (zulagerung.isPresent()) {
            bestand = zulagerung.get();
            bestand.reservieren(menge);
            begruendung = "Artikel liegt dort bereits und es ist genug Platz";
        } else {
            Optional<Lagerplatz> leererPlatz = findeLeerenPlatz(artikel, menge, gesperrt);
            if (leererPlatz.isEmpty()) {
                return klaerplatz(KlaerGrund.KEIN_PASSENDER_PLATZ, artikelNummer, menge,
                        "Kein freier Lagerplatz passt nach Maßen und Traglast für " + menge + " Stück");
            }
            bestand = bestandRepository.save(Bestand.reservierung(artikel, leererPlatz.get(), menge));
            begruendung = platzSuche.begruendung(artikel);
        }

        String platzCode = bestand.getLagerplatz().getCode();
        Transportauftrag transport = transportauftragRepository.save(new Transportauftrag(
                TransportArt.EINLAGERUNG, artikelNummer, menge,
                Transportauftrag.WARENEINGANG, platzCode, Prioritaet.NORMAL, null));
        return WareneingangErgebnis.reserviert(platzCode, transport.getId(), begruendung);
    }

    private WareneingangErgebnis klaerplatz(KlaerGrund grund, String artikelNummer, int menge, String beschreibung) {
        Klaerfall klaerfall = klaerfallRepository.save(new Klaerfall(grund, artikelNummer, menge, beschreibung, null));
        return WareneingangErgebnis.klaerplatz(klaerfall.getId());
    }

    /** 1. Wahl: ein Platz, auf dem der Artikel schon liegt und noch genug frei ist. */
    private Optional<Bestand> findeZulagerung(Artikel artikel, int menge, Set<String> gesperrt) {
        return bestandRepository.findByArtikelOrderByMengeAsc(artikel).stream()
                .filter(bestand -> !gesperrt.contains(bestand.getLagerplatz().getCode()))
                .filter(bestand -> bestand.freieKapazitaet() >= menge)
                .findFirst();
    }

    /** 2. Wahl: der sinnvollste leere Platz. */
    private Optional<Lagerplatz> findeLeerenPlatz(Artikel artikel, int menge, Set<String> gesperrt) {
        List<Lagerplatz> frei = lagerplatzRepository.findeLeerePlaetzeFuer(menge).stream()
                .filter(platz -> !gesperrt.contains(platz.getCode()))
                .toList();
        return platzSuche.besterPlatz(artikel, menge, frei);
    }
}
