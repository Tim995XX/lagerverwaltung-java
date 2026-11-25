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
 * Sind Menge und Reservierung 0, wird der Bestand gelöscht und der Platz ist wieder frei.
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

    /** Unterwegs zum Platz, noch nicht eingescannt: belegt Kapazität, ist aber nicht verfügbar. */
    private int reserviert;

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

    /** Reserviert einen leeren Platz für Ware, die noch unterwegs ist. */
    public static Bestand reservierung(Artikel artikel, Lagerplatz lagerplatz, int menge) {
        Bestand bestand = new Bestand(artikel, lagerplatz, 0);
        bestand.reservieren(menge);
        return bestand;
    }

    public int freieKapazitaet() {
        return Math.max(0, lagerplatz.fassungsvermoegen(artikel) - menge - reserviert);
    }

    public void reservieren(int zugang) {
        if (zugang > freieKapazitaet()) {
            throw new IllegalArgumentException("Platz " + lagerplatz.getCode() + " hat nicht genug freie Kapazität");
        }
        reserviert += zugang;
    }

    /** Die Ware steht auf dem Platz und wurde gescannt: aus der Reservierung wird Bestand. */
    public void einbuchen(int zugang) {
        if (zugang > reserviert) {
            throw new IllegalArgumentException("Auf Platz " + lagerplatz.getCode() + " sind nur "
                    + reserviert + " Stück reserviert");
        }
        reserviert -= zugang;
        menge += zugang;
    }

    public boolean istLeer() {
        return menge == 0 && reserviert == 0;
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

    public int getReserviert() {
        return reserviert;
    }

    public int getMenge() {
        return menge;
    }
}
