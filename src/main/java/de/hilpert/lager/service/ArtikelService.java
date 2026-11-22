package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.fehler.KonfliktException;
import de.hilpert.lager.fehler.NichtGefundenException;
import de.hilpert.lager.repository.ArtikelRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArtikelService {

    private final ArtikelRepository artikelRepository;

    public ArtikelService(ArtikelRepository artikelRepository) {
        this.artikelRepository = artikelRepository;
    }

    @Transactional
    public Artikel anlegen(String artikelNummer, String bezeichnung) {
        if (artikelRepository.existsByArtikelNummer(artikelNummer)) {
            throw new KonfliktException("Artikel " + artikelNummer + " existiert bereits");
        }
        return artikelRepository.save(new Artikel(artikelNummer, bezeichnung));
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
