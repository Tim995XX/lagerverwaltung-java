package de.hilpert.lager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Ein Lagerplatz im Regal, adressiert über Gang, Regal und Ebene.
 * Vereinfachung: Auf einem Platz liegt immer nur ein Artikel ("artikelreiner Platz"),
 * die Kapazität wird in Stück (Einheiten) angegeben.
 */
@Entity
public class Lagerplatz {

    @Id
    @GeneratedValue
    private Long id;

    /** Lesbare Adresse, z. B. "01-03-2" für Gang 1, Regal 3, Ebene 2. */
    @Column(nullable = false, unique = true)
    private String code;

    private int gang;
    private int regal;
    private int ebene;
    private int kapazitaet;

    // Innenmaße und Traglast; null bei Plätzen, die vor der Platzsuche angelegt wurden
    private Integer laengeMm;
    private Integer breiteMm;
    private Integer hoeheMm;
    private Integer maxGewichtKg;

    protected Lagerplatz() {
        // für JPA
    }

    public Lagerplatz(int gang, int regal, int ebene, int kapazitaet) {
        this.gang = gang;
        this.regal = regal;
        this.ebene = ebene;
        this.kapazitaet = kapazitaet;
        this.code = codeFuer(gang, regal, ebene);
    }

    public static String codeFuer(int gang, int regal, int ebene) {
        return String.format("%02d-%02d-%d", gang, regal, ebene);
    }

    public void setzeMasse(int laengeMm, int breiteMm, int hoeheMm, int maxGewichtKg) {
        this.laengeMm = laengeMm;
        this.breiteMm = breiteMm;
        this.hoeheMm = hoeheMm;
        this.maxGewichtKg = maxGewichtKg;
    }

    public boolean hatMasse() {
        return laengeMm != null && breiteMm != null && hoeheMm != null && maxGewichtKg != null;
    }

    /**
     * Wie viele Stück des Artikels passen auf den leeren Platz? Es zählt die engste Grenze aus
     * Maßen, Traglast und fester Obergrenze. Die Ware bleibt aufrecht, darf aber um 90° gedreht
     * und gestapelt werden. Fehlen Maße, ist die Antwort 0.
     */
    public int fassungsvermoegen(Artikel artikel) {
        if (!hatMasse() || !artikel.hatMasse()) {
            return 0;
        }
        long laengs = (long) (laengeMm / artikel.getLaengeMm()) * (breiteMm / artikel.getBreiteMm());
        long quer = (long) (laengeMm / artikel.getBreiteMm()) * (breiteMm / artikel.getLaengeMm());
        long nachMassen = Math.max(laengs, quer) * (hoeheMm / artikel.getHoeheMm());
        long nachGewicht = maxGewichtKg * 1000L / artikel.getGewichtGramm();
        return (int) Math.min(kapazitaet, Math.min(nachMassen, nachGewicht));
    }

    /** Einfaches Maß für den Weg: Gang 1, Regal 1 liegt am nächsten an den Toren. */
    public int entfernung() {
        return gang + regal;
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

    public Integer getMaxGewichtKg() {
        return maxGewichtKg;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public int getGang() {
        return gang;
    }

    public int getRegal() {
        return regal;
    }

    public int getEbene() {
        return ebene;
    }

    public int getKapazitaet() {
        return kapazitaet;
    }
}
