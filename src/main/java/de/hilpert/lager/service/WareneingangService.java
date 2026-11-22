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

import java.util.Optional;

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

    public WareneingangService(ArtikelService artikelService,
                               BestandRepository bestandRepository,
                               LagerplatzRepository lagerplatzRepository,
                               TransportauftragRepository transportauftragRepository,
                               KlaerfallRepository klaerfallRepository) {
        this.artikelService = artikelService;
        this.bestandRepository = bestandRepository;
        this.lagerplatzRepository = lagerplatzRepository;
        this.transportauftragRepository = transportauftragRepository;
        this.klaerfallRepository = klaerfallRepository;
    }

    @Transactional
    public WareneingangErgebnis einlagern(String artikelNummer, int menge) {
        Artikel artikel = artikelService.finde(artikelNummer);

        Bestand bestand;
        Optional<Bestand> zulagerung = findeZulagerung(artikel, menge);
        if (zulagerung.isPresent()) {
            bestand = zulagerung.get();
            // Innerhalb der Transaktion wird die geänderte Menge beim Commit automatisch gespeichert
            bestand.zubuchen(menge);
        } else {
            Optional<Lagerplatz> leererPlatz = findeLeerenPlatz(menge);
            if (leererPlatz.isEmpty()) {
                Klaerfall klaerfall = klaerfallRepository.save(new Klaerfall(
                        KlaerGrund.KEIN_PASSENDER_PLATZ, artikelNummer, menge,
                        "Kein Lagerplatz mit Kapazität für " + menge + " Stück gefunden", null));
                return WareneingangErgebnis.klaerplatz(klaerfall.getId());
            }
            bestand = bestandRepository.save(new Bestand(artikel, leererPlatz.get(), menge));
        }

        String platzCode = bestand.getLagerplatz().getCode();
        Transportauftrag transport = transportauftragRepository.save(new Transportauftrag(
                TransportArt.EINLAGERUNG, artikelNummer, menge,
                Transportauftrag.WARENEINGANG, platzCode, Prioritaet.NORMAL, null));
        return WareneingangErgebnis.eingelagert(platzCode, transport.getId());
    }

    /** 1. Wahl: ein Platz, auf dem der Artikel schon liegt und noch genug frei ist. */
    private Optional<Bestand> findeZulagerung(Artikel artikel, int menge) {
        return bestandRepository.findByArtikelOrderByMengeAsc(artikel).stream()
                .filter(bestand -> bestand.freieKapazitaet() >= menge)
                .findFirst();
    }

    /** 2. Wahl: der kleinste leere Platz, auf den die Menge passt. */
    private Optional<Lagerplatz> findeLeerenPlatz(int menge) {
        return lagerplatzRepository.findeLeerePlaetzeFuer(menge).stream().findFirst();
    }
}
