package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Eine Zeile eines Kommissionierauftrags: welcher Artikel in welcher Menge. */
@Embeddable
public class Auftragsposition {

    @Column(nullable = false)
    private String artikelNummer;

    private int menge;

    protected Auftragsposition() {
        // für JPA
    }

    public Auftragsposition(String artikelNummer, int menge) {
        this.artikelNummer = artikelNummer;
        this.menge = menge;
    }

    public String getArtikelNummer() {
        return artikelNummer;
    }

    public int getMenge() {
        return menge;
    }
}
