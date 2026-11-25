package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.KlaerGrund;
import de.hilpert.lager.domain.Klaerfall;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.domain.UngueltigerStatusException;
import de.hilpert.lager.fehler.NichtGefundenException;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.KlaerfallRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Der Fahrer steht vor einem beschädigten oder belegten Platz: Platz sperren, Reservierung lösen,
 * neuen Platz suchen. Alles in einer Transaktion, damit die Ware nie ohne Ziel dasteht.
 */
@Service
public class UmleitungService {

    private final TransportauftragRepository transportauftragRepository;
    private final BestandRepository bestandRepository;
    private final KlaerfallRepository klaerfallRepository;
    private final EinlagerPlanung einlagerPlanung;

    public UmleitungService(TransportauftragRepository transportauftragRepository,
                            BestandRepository bestandRepository, KlaerfallRepository klaerfallRepository,
                            EinlagerPlanung einlagerPlanung) {
        this.transportauftragRepository = transportauftragRepository;
        this.bestandRepository = bestandRepository;
        this.klaerfallRepository = klaerfallRepository;
        this.einlagerPlanung = einlagerPlanung;
    }

    @Transactional
    public UmleitungErgebnis umleiten(Long transportId, String grund) {
        Transportauftrag auftrag = transportauftragRepository.findById(transportId)
                .orElseThrow(() -> new NichtGefundenException("Transportauftrag " + transportId + " nicht gefunden"));
        auftrag.pruefeInArbeitUndEinlagerung();
        if (auftrag.isBestandGebucht()) {
            // vor Einführung der Scans angelegt: der Bestand steht schon auf dem Platz
            throw new UngueltigerStatusException("Alte Einlagerung " + transportId
                    + " ist bereits gebucht und muss im Leitstand geklärt werden");
        }

        String alterPlatz = auftrag.getNachOrt();
        Bestand bestand = bestandRepository.findByLagerplatzCode(alterPlatz)
                .orElseThrow(() -> new IllegalStateException("Keine Reservierung auf Platz " + alterPlatz));
        Artikel artikel = bestand.getArtikel();
        int menge = auftrag.getMenge();

        sperre(bestand.getLagerplatz(), grund, auftrag);
        bestand.reservierungFreigeben(menge);
        if (bestand.istLeer()) {
            bestandRepository.delete(bestand);
        }

        Optional<EinlagerPlanung.Reservierung> neu = einlagerPlanung.reserviere(artikel, menge);
        if (neu.isEmpty()) {
            auftrag.abbrechen();
            klaerfallRepository.save(new Klaerfall(KlaerGrund.KEIN_PASSENDER_PLATZ, auftrag.getArtikelNummer(), menge,
                    "Platz " + alterPlatz + " blockiert und kein Ersatzplatz gefunden", null));
            return new UmleitungErgebnis(UmleitungErgebnis.Ergebnis.KLAERPLATZ, alterPlatz, null, null);
        }
        auftrag.umleitenNach(neu.get().platzCode());
        return new UmleitungErgebnis(UmleitungErgebnis.Ergebnis.UMGELEITET, alterPlatz,
                neu.get().platzCode(), neu.get().begruendung());
    }

    /** Ein Klärfall pro Sperre genügt, auch wenn mehrere Fahrer denselben Platz melden. */
    private void sperre(Lagerplatz platz, String grund, Transportauftrag auftrag) {
        if (platz.isGesperrt()) {
            return;
        }
        platz.sperren(grund);
        klaerfallRepository.save(new Klaerfall(KlaerGrund.PLATZ_BLOCKIERT, auftrag.getArtikelNummer(),
                auftrag.getMenge(), "Platz " + platz.getCode() + " gesperrt: " + grund, null));
    }
}
