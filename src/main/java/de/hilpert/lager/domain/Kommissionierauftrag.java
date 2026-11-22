package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Ein Kundenauftrag: mehrere Positionen, die aus dem Lager geholt werden sollen. */
@Entity
public class Kommissionierauftrag {

    @Id
    @GeneratedValue
    private Long id;

    // Positionen gehören fest zum Auftrag und haben keine eigene Identität → ElementCollection
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Auftragsposition> positionen = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioritaet prioritaet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuftragStatus status;

    @Column(nullable = false)
    private LocalDateTime erstelltAm;

    protected Kommissionierauftrag() {
        // für JPA
    }

    public Kommissionierauftrag(List<Auftragsposition> positionen, Prioritaet prioritaet) {
        this.positionen = new ArrayList<>(positionen);
        this.prioritaet = prioritaet;
        this.status = AuftragStatus.FREIGEGEBEN;
        this.erstelltAm = LocalDateTime.now();
    }

    public void inKlaerung() {
        this.status = AuftragStatus.IN_KLAERUNG;
    }

    public Long getId() {
        return id;
    }

    public List<Auftragsposition> getPositionen() {
        return List.copyOf(positionen);
    }

    public Prioritaet getPrioritaet() {
        return prioritaet;
    }

    public AuftragStatus getStatus() {
        return status;
    }

    public LocalDateTime getErstelltAm() {
        return erstelltAm;
    }
}
