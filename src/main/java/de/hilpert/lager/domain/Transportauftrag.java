package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Auftrag an einen Staplerfahrer bzw. eine Fördertechnik, Ware von A nach B zu bringen.
 * Erlaubte Statuswechsel: OFFEN → IN_ARBEIT → ERLEDIGT.
 */
@Entity
public class Transportauftrag {

    /** Feste Orte außerhalb der Regale. */
    public static final String WARENEINGANG = "WE-ZONE";
    public static final String WARENAUSGANG = "WA-ZONE";

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportArt art;

    @Column(nullable = false)
    private String artikelNummer;

    private int menge;

    @Column(nullable = false)
    private String vonOrt;

    @Column(nullable = false)
    private String nachOrt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioritaet prioritaet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportStatus status;

    /** Nur bei Auslagerungen gesetzt. */
    private Long kommissionierauftragId;

    @Column(nullable = false)
    private LocalDateTime erstelltAm;

    protected Transportauftrag() {
        // für JPA
    }

    public Transportauftrag(TransportArt art, String artikelNummer, int menge, String vonOrt, String nachOrt,
                            Prioritaet prioritaet, Long kommissionierauftragId) {
        this.art = art;
        this.artikelNummer = artikelNummer;
        this.menge = menge;
        this.vonOrt = vonOrt;
        this.nachOrt = nachOrt;
        this.prioritaet = prioritaet;
        this.kommissionierauftragId = kommissionierauftragId;
        this.status = TransportStatus.OFFEN;
        this.erstelltAm = LocalDateTime.now();
    }

    public void starten() {
        wechsleStatus(TransportStatus.OFFEN, TransportStatus.IN_ARBEIT);
    }

    public void abschliessen() {
        wechsleStatus(TransportStatus.IN_ARBEIT, TransportStatus.ERLEDIGT);
    }

    /** Der Leitstand darf nur Aufträge umpriorisieren, die noch nicht erledigt sind. */
    public void aenderePrioritaet(Prioritaet neuePrioritaet) {
        if (status == TransportStatus.ERLEDIGT) {
            throw new UngueltigerStatusException("Erledigte Transportaufträge können nicht umpriorisiert werden");
        }
        this.prioritaet = neuePrioritaet;
    }

    private void wechsleStatus(TransportStatus erwartet, TransportStatus neu) {
        if (status != erwartet) {
            throw new UngueltigerStatusException(
                    "Transportauftrag " + id + " ist " + status + ", erwartet war " + erwartet);
        }
        status = neu;
    }

    public Long getId() {
        return id;
    }

    public TransportArt getArt() {
        return art;
    }

    public String getArtikelNummer() {
        return artikelNummer;
    }

    public int getMenge() {
        return menge;
    }

    public String getVonOrt() {
        return vonOrt;
    }

    public String getNachOrt() {
        return nachOrt;
    }

    public Prioritaet getPrioritaet() {
        return prioritaet;
    }

    public TransportStatus getStatus() {
        return status;
    }

    public Long getKommissionierauftragId() {
        return kommissionierauftragId;
    }

    public LocalDateTime getErstelltAm() {
        return erstelltAm;
    }
}
