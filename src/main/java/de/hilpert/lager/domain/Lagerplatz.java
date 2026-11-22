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
