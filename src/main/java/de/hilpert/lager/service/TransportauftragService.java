package de.hilpert.lager.service;

import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.FalscherScanException;
import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.Scan;
import de.hilpert.lager.domain.TransportStatus;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.fehler.NichtGefundenException;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.ScanRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Transportaufträge für den Leitstand: abarbeiten in der richtigen Reihenfolge, umpriorisieren. */
@Service
public class TransportauftragService {

    /**
     * Abarbeitungsreihenfolge: Priorität (HOCH zuerst), dann der älteste Auftrag.
     * Sortiert wird in Java statt in SQL, weil die Priorität als Text gespeichert ist –
     * alphabetisch käme HOCH, NIEDRIG, NORMAL heraus.
     */
    static final Comparator<Transportauftrag> REIHENFOLGE =
            Comparator.comparing(Transportauftrag::getPrioritaet)
                    .thenComparing(Transportauftrag::getErstelltAm)
                    .thenComparing(Transportauftrag::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final TransportauftragRepository repository;
    private final BestandRepository bestandRepository;
    private final ScanRepository scanRepository;

    public TransportauftragService(TransportauftragRepository repository, BestandRepository bestandRepository,
                                   ScanRepository scanRepository) {
        this.repository = repository;
        this.bestandRepository = bestandRepository;
        this.scanRepository = scanRepository;
    }

    /** Alle Aufträge oder nur die mit einem bestimmten Status, in Abarbeitungsreihenfolge. */
    @Transactional(readOnly = true)
    public List<Transportauftrag> liste(TransportStatus status) {
        List<Transportauftrag> auftraege = status == null ? repository.findAll() : repository.findByStatus(status);
        return auftraege.stream().sorted(REIHENFOLGE).toList();
    }

    @Transactional
    public Transportauftrag starten(Long id) {
        Transportauftrag auftrag = finde(id);
        auftrag.starten();
        return auftrag;
    }

    @Transactional
    public Transportauftrag abschliessen(Long id) {
        Transportauftrag auftrag = finde(id);
        auftrag.abschliessen();
        return auftrag;
    }

    /**
     * Ware steht am Platz: Artikel und Platz müssen zum Auftrag passen, erst dann wird Bestand gebucht.
     * Ein falscher Scan bucht nichts, sein Protokolleintrag bleibt aber erhalten (deshalb noRollbackFor).
     */
    @Transactional(noRollbackFor = FalscherScanException.class)
    public Transportauftrag einlagerungScannen(Long id, String artikelNummer, String lagerplatzCode) {
        Transportauftrag auftrag = finde(id);
        auftrag.pruefeBereitZumScan();

        String artikel = artikelNummer.strip();
        String platz = lagerplatzCode.strip();
        boolean passt = auftrag.passtZuScan(artikel, platz);
        scanRepository.save(new Scan(id, artikel, platz, passt));
        if (!passt) {
            throw new FalscherScanException("Erwartet: Artikel " + auftrag.getArtikelNummer() + " auf Platz "
                    + auftrag.getNachOrt() + ", gescannt: Artikel " + artikel + " auf Platz " + platz);
        }

        if (!auftrag.isBestandGebucht()) {
            Bestand bestand = bestandRepository.findByLagerplatzCode(platz)
                    .orElseThrow(() -> new IllegalStateException("Keine Reservierung auf Platz " + platz));
            bestand.einbuchen(auftrag.getMenge());
        }
        auftrag.eingelagert();
        return auftrag;
    }

    @Transactional(readOnly = true)
    public List<Scan> scans(Long id) {
        finde(id);
        return scanRepository.findByTransportauftragIdOrderByIdAsc(id);
    }

    @Transactional
    public Transportauftrag aenderePrioritaet(Long id, Prioritaet prioritaet) {
        Transportauftrag auftrag = finde(id);
        auftrag.aenderePrioritaet(prioritaet);
        return auftrag;
    }

    private Transportauftrag finde(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NichtGefundenException("Transportauftrag " + id + " nicht gefunden"));
    }
}
