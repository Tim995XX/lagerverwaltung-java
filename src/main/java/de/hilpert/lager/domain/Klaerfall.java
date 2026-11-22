package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Eine Buchung, die das System nicht automatisch ausführen konnte und die ein Mensch
 * im Leitstand klären muss ("Klärplatz"). Ware im Wareneingang landet dann physisch
 * auf dem Klärplatz, ein Kommissionierauftrag bleibt stehen.
 */
@Entity
public class Klaerfall {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KlaerGrund grund;

    @Column(nullable = false)
    private String artikelNummer;

    private int menge;

    @Column(nullable = false, length = 500)
    private String beschreibung;

    /** Gesetzt, wenn der Klärfall bei einem Kommissionierauftrag entstanden ist. */
    private Long kommissionierauftragId;

    @Column(nullable = false)
    private LocalDateTime erstelltAm;

    private boolean erledigt;

    @Column(length = 500)
    private String kommentar;

    protected Klaerfall() {
        // für JPA
    }

    public Klaerfall(KlaerGrund grund, String artikelNummer, int menge, String beschreibung,
                     Long kommissionierauftragId) {
        this.grund = grund;
        this.artikelNummer = artikelNummer;
        this.menge = menge;
        this.beschreibung = beschreibung;
        this.kommissionierauftragId = kommissionierauftragId;
        this.erstelltAm = LocalDateTime.now();
    }

    public void erledigen(String kommentar) {
        if (erledigt) {
            throw new UngueltigerStatusException("Klärfall " + id + " ist bereits erledigt");
        }
        this.erledigt = true;
        this.kommentar = kommentar;
    }

    public Long getId() {
        return id;
    }

    public KlaerGrund getGrund() {
        return grund;
    }

    public String getArtikelNummer() {
        return artikelNummer;
    }

    public int getMenge() {
        return menge;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public Long getKommissionierauftragId() {
        return kommissionierauftragId;
    }

    public LocalDateTime getErstelltAm() {
        return erstelltAm;
    }

    public boolean isErledigt() {
        return erledigt;
    }

    public String getKommentar() {
        return kommentar;
    }
}
