package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Umschlag;
import de.hilpert.lager.fehler.KonfliktException;
import de.hilpert.lager.fehler.NichtGefundenException;
import de.hilpert.lager.repository.ArtikelRepository;
import de.hilpert.lager.repository.BestandRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArtikelService {

    private final ArtikelRepository artikelRepository;
    private final BestandRepository bestandRepository;

    public ArtikelService(ArtikelRepository artikelRepository, BestandRepository bestandRepository) {
        this.artikelRepository = artikelRepository;
        this.bestandRepository = bestandRepository;
    }

    @Transactional
    public Artikel anlegen(String artikelNummer, String bezeichnung, int laengeMm, int breiteMm, int hoeheMm,
                           int gewichtGramm, Umschlag umschlag) {
        if (artikelRepository.existsByArtikelNummer(artikelNummer)) {
            throw new KonfliktException("Artikel " + artikelNummer + " existiert bereits");
        }
        Artikel artikel = new Artikel(artikelNummer, bezeichnung);
        artikel.setzeMasse(laengeMm, breiteMm, hoeheMm, gewichtGramm, umschlag);
        return artikelRepository.save(artikel);
    }

    /** Maße nachtragen. Liegt der Artikel schon mit Maßen im Lager, würden Reservierungen nicht mehr stimmen. */
    @Transactional
    public Artikel setzeMasse(String artikelNummer, int laengeMm, int breiteMm, int hoeheMm,
                              int gewichtGramm, Umschlag umschlag) {
        Artikel artikel = finde(artikelNummer);
        if (artikel.hatMasse() && bestandRepository.existsByArtikel(artikel)) {
            throw new KonfliktException("Artikel " + artikelNummer + " liegt im Lager, Maße sind gesperrt");
        }
        artikel.setzeMasse(laengeMm, breiteMm, hoeheMm, gewichtGramm, umschlag);
        return artikel;
    }

    @Transactional(readOnly = true)
    public List<Artikel> alle() {
        return artikelRepository.findAll(Sort.by("artikelNummer"));
    }

    @Transactional(readOnly = true)
    public Artikel finde(String artikelNummer) {
        return artikelRepository.findByArtikelNummer(artikelNummer)
                .orElseThrow(() -> new NichtGefundenException("Artikel " + artikelNummer + " nicht gefunden"));
    }
}
