package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/** Protokoll eines Einlagerungs-Scans, auch der fehlgeschlagenen. */
@Entity
public class Scan {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Long transportauftragId;

    @Column(nullable = false)
    private String artikelNummer;

    @Column(nullable = false)
    private String lagerplatzCode;

    private boolean erfolgreich;

    @Column(nullable = false)
    private LocalDateTime gescanntAm;

    protected Scan() {
        // für JPA
    }

    public Scan(Long transportauftragId, String artikelNummer, String lagerplatzCode, boolean erfolgreich) {
        this.transportauftragId = transportauftragId;
        this.artikelNummer = artikelNummer;
        this.lagerplatzCode = lagerplatzCode;
        this.erfolgreich = erfolgreich;
        this.gescanntAm = LocalDateTime.now();
    }

    public String getArtikelNummer() {
        return artikelNummer;
    }

    public String getLagerplatzCode() {
        return lagerplatzCode;
    }

    public boolean isErfolgreich() {
        return erfolgreich;
    }

    public LocalDateTime getGescanntAm() {
        return gescanntAm;
    }
}
