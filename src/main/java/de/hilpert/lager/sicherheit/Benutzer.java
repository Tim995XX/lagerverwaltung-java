package de.hilpert.lager.sicherheit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Ein Benutzer mit genau einer Rolle. Das Passwort steht nur als Hash in der Datenbank. */
@Entity
public class Benutzer {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String passwortHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rolle rolle;

    private boolean aktiv = true;

    protected Benutzer() {
        // für JPA
    }

    public Benutzer(String name, String passwortHash, Rolle rolle) {
        this.name = name;
        this.passwortHash = passwortHash;
        this.rolle = rolle;
    }

    /** Statt Löschen: Der Name bleibt vergeben, eine Anmeldung ist nicht mehr möglich. */
    public void deaktivieren() {
        this.aktiv = false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPasswortHash() { return passwortHash; }
    public Rolle getRolle() { return rolle; }
    public boolean isAktiv() { return aktiv; }
}
