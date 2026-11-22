package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    protected Artikel() {
        // für JPA
    }

    public Artikel(String artikelNummer, String bezeichnung) {
        this.artikelNummer = artikelNummer;
        this.bezeichnung = bezeichnung;
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
