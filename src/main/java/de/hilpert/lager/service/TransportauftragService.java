package de.hilpert.lager.service;

import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.TransportStatus;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.fehler.NichtGefundenException;
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

    public TransportauftragService(TransportauftragRepository repository) {
        this.repository = repository;
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
