package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.KlaerGrund;
import de.hilpert.lager.domain.Klaerfall;
import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.TransportArt;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.repository.KlaerfallRepository;
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
    private final TransportauftragRepository transportauftragRepository;
    private final KlaerfallRepository klaerfallRepository;
    private final EinlagerPlanung einlagerPlanung;

    public WareneingangService(ArtikelService artikelService,
                               TransportauftragRepository transportauftragRepository,
                               KlaerfallRepository klaerfallRepository,
                               EinlagerPlanung einlagerPlanung) {
        this.artikelService = artikelService;
        this.transportauftragRepository = transportauftragRepository;
        this.klaerfallRepository = klaerfallRepository;
        this.einlagerPlanung = einlagerPlanung;
    }

    @Transactional
    public WareneingangErgebnis einlagern(String artikelNummer, int menge) {
        Artikel artikel = artikelService.finde(artikelNummer);
        if (!artikel.hatMasse()) {
            return klaerplatz(KlaerGrund.MASSE_FEHLEN, artikelNummer, menge,
                    "Maße oder Gewicht von Artikel " + artikelNummer + " fehlen");
        }

        Optional<EinlagerPlanung.Reservierung> gefunden = einlagerPlanung.reserviere(artikel, menge);
        if (gefunden.isEmpty()) {
            return klaerplatz(KlaerGrund.KEIN_PASSENDER_PLATZ, artikelNummer, menge,
                    "Kein freier Lagerplatz passt nach Maßen und Traglast für " + menge + " Stück");
        }

        EinlagerPlanung.Reservierung reservierung = gefunden.get();
        Transportauftrag transport = transportauftragRepository.save(new Transportauftrag(
                TransportArt.EINLAGERUNG, artikelNummer, menge,
                Transportauftrag.WARENEINGANG, reservierung.platzCode(), Prioritaet.NORMAL, null));
        return WareneingangErgebnis.reserviert(reservierung.platzCode(), transport.getId(),
                reservierung.begruendung());
    }

    private WareneingangErgebnis klaerplatz(KlaerGrund grund, String artikelNummer, int menge, String beschreibung) {
        Klaerfall klaerfall = klaerfallRepository.save(new Klaerfall(grund, artikelNummer, menge, beschreibung, null));
        return WareneingangErgebnis.klaerplatz(klaerfall.getId());
    }
}
