package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Ein Artikel aus den Stammdaten, z. B. "4711 – Mineralwasser 6x1,5 l". */
@Entity
public class Artikel {

    @Id
    @GeneratedValue
    private Long id;

    /** Fachlicher Schlüssel, über den alle Buchungen laufen. */
    @Column(nullable = false, unique = true)
    private String artikelNummer;

    @Column(nullable = false)
    private String bezeichnung;

    // Maße und Gewicht je Stück; null bei Artikeln, die vor der Platzsuche angelegt wurden
    private Integer laengeMm;
    private Integer breiteMm;
    private Integer hoeheMm;
    private Integer gewichtGramm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Umschlag umschlag = Umschlag.MITTEL;

    protected Artikel() {
        // für JPA
    }

    public Artikel(String artikelNummer, String bezeichnung) {
        this.artikelNummer = artikelNummer;
        this.bezeichnung = bezeichnung;
    }

    public void setzeMasse(int laengeMm, int breiteMm, int hoeheMm, int gewichtGramm, Umschlag umschlag) {
        this.laengeMm = laengeMm;
        this.breiteMm = breiteMm;
        this.hoeheMm = hoeheMm;
        this.gewichtGramm = gewichtGramm;
        this.umschlag = umschlag;
    }

    public boolean hatMasse() {
        return laengeMm != null && breiteMm != null && hoeheMm != null && gewichtGramm != null;
    }

    public Integer getLaengeMm() {
        return laengeMm;
    }

    public Integer getBreiteMm() {
        return breiteMm;
    }

    public Integer getHoeheMm() {
        return hoeheMm;
    }

    public Integer getGewichtGramm() {
        return gewichtGramm;
    }

    public Umschlag getUmschlag() {
        return umschlag;
    }

    public Long getId() {
        return id;
    }

    public String getArtikelNummer() {
        return artikelNummer;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }
}
