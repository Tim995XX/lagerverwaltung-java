package de.hilpert.lager.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

/**
 * Menge eines Artikels auf einem Lagerplatz. Ein leerer Platz hat keinen Bestand-Datensatz:
 * Fällt die Menge auf 0, wird der Bestand gelöscht und der Platz ist wieder frei.
 */
@Entity
public class Bestand {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private Artikel artikel;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Lagerplatz lagerplatz;

    private int menge;

    /** Optimistisches Sperren: Zwei gleichzeitige Buchungen auf denselben Bestand fallen auf. */
    @Version
    private long version;

    protected Bestand() {
        // für JPA
    }

    public Bestand(Artikel artikel, Lagerplatz lagerplatz, int menge) {
        this.artikel = artikel;
        this.lagerplatz = lagerplatz;
        this.menge = menge;
    }

    public int freieKapazitaet() {
        return lagerplatz.getKapazitaet() - menge;
    }

    public void zubuchen(int zugang) {
        if (zugang > freieKapazitaet()) {
            throw new IllegalArgumentException("Platz " + lagerplatz.getCode() + " hat nicht genug freie Kapazität");
        }
        menge += zugang;
    }

    public void abbuchen(int abgang) {
        if (abgang > menge) {
            throw new IllegalArgumentException("Auf Platz " + lagerplatz.getCode() + " liegen nur " + menge + " Stück");
        }
        menge -= abgang;
    }

    public Long getId() {
        return id;
    }

    public Artikel getArtikel() {
        return artikel;
    }

    public Lagerplatz getLagerplatz() {
        return lagerplatz;
    }

    public int getMenge() {
        return menge;
    }
}
