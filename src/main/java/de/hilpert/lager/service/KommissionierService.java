package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Auftragsposition;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.KlaerGrund;
import de.hilpert.lager.domain.Klaerfall;
import de.hilpert.lager.domain.Kommissionierauftrag;
import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.TransportArt;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.fehler.NichtGefundenException;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.KlaerfallRepository;
import de.hilpert.lager.repository.KommissionierauftragRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kommissionierung: prüft für alle Positionen den Bestand und bucht nur aus, wenn
 * <em>alle</em> Positionen bedient werden können ("alles oder nichts").
 * Reicht der Bestand nicht, geht der Auftrag in Klärung und es wird nichts gebucht.
 */
@Service
public class KommissionierService {

    private final ArtikelService artikelService;
    private final KommissionierauftragRepository auftragRepository;
    private final BestandRepository bestandRepository;
    private final TransportauftragRepository transportauftragRepository;
    private final KlaerfallRepository klaerfallRepository;

    public KommissionierService(ArtikelService artikelService,
                                KommissionierauftragRepository auftragRepository,
                                BestandRepository bestandRepository,
                                TransportauftragRepository transportauftragRepository,
                                KlaerfallRepository klaerfallRepository) {
        this.artikelService = artikelService;
        this.auftragRepository = auftragRepository;
        this.bestandRepository = bestandRepository;
        this.transportauftragRepository = transportauftragRepository;
        this.klaerfallRepository = klaerfallRepository;
    }

    @Transactional
    public Kommissionierauftrag anlegen(List<Auftragsposition> positionen, Prioritaet prioritaet) {
        Kommissionierauftrag auftrag = auftragRepository.save(new Kommissionierauftrag(positionen, prioritaet));
        Map<String, Integer> bedarf = bedarfJeArtikel(positionen);

        // Schritt 1: nur prüfen. Erst wenn alles passt, wird gebucht – so muss nichts zurückgenommen werden.
        List<Klaerfall> fehlmengen = new ArrayList<>();
        Map<String, List<Bestand>> bestaende = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> eintrag : bedarf.entrySet()) {
            Artikel artikel = artikelService.finde(eintrag.getKey());
            List<Bestand> bestaendeDesArtikels = bestandRepository.findByArtikelOrderByMengeAsc(artikel);
            int verfuegbar = bestaendeDesArtikels.stream().mapToInt(Bestand::getMenge).sum();
            if (verfuegbar < eintrag.getValue()) {
                fehlmengen.add(new Klaerfall(KlaerGrund.BESTAND_NICHT_AUSREICHEND, eintrag.getKey(),
                        eintrag.getValue(),
                        "Benötigt " + eintrag.getValue() + " Stück, verfügbar " + verfuegbar,
                        auftrag.getId()));
            }
            bestaende.put(eintrag.getKey(), bestaendeDesArtikels);
        }

        if (!fehlmengen.isEmpty()) {
            klaerfallRepository.saveAll(fehlmengen);
            auftrag.inKlaerung();
            return auftrag;
        }

        // Schritt 2: auslagern
        for (Map.Entry<String, Integer> eintrag : bedarf.entrySet()) {
            auslagern(eintrag.getKey(), eintrag.getValue(), bestaende.get(eintrag.getKey()), auftrag);
        }
        return auftrag;
    }

    @Transactional(readOnly = true)
    public Kommissionierauftrag finde(Long id) {
        return auftragRepository.findById(id)
                .orElseThrow(() -> new NichtGefundenException("Kommissionierauftrag " + id + " nicht gefunden"));
    }

    @Transactional(readOnly = true)
    public List<Transportauftrag> transportauftraegeZu(Long auftragId) {
        return transportauftragRepository.findByKommissionierauftragId(auftragId);
    }

    /** Steht derselbe Artikel in mehreren Positionen, werden die Mengen zusammengezählt. */
    private Map<String, Integer> bedarfJeArtikel(List<Auftragsposition> positionen) {
        Map<String, Integer> bedarf = new LinkedHashMap<>();
        for (Auftragsposition position : positionen) {
            bedarf.merge(position.getArtikelNummer(), position.getMenge(), Integer::sum);
        }
        return bedarf;
    }

    /** Entnimmt die Menge von den Plätzen (kleinste Bestände zuerst) und legt je Platz einen Transport an. */
    private void auslagern(String artikelNummer, int menge, List<Bestand> bestaende, Kommissionierauftrag auftrag) {
        int rest = menge;
        for (Bestand bestand : bestaende) {
            if (rest == 0) {
                break;
            }
            int entnahme = Math.min(rest, bestand.getMenge());
            bestand.abbuchen(entnahme);
            rest -= entnahme;

            transportauftragRepository.save(new Transportauftrag(
                    TransportArt.AUSLAGERUNG, artikelNummer, entnahme,
                    bestand.getLagerplatz().getCode(), Transportauftrag.WARENAUSGANG,
                    auftrag.getPrioritaet(), auftrag.getId()));

            if (bestand.getMenge() == 0) {
                bestandRepository.delete(bestand); // Platz ist wieder frei
            }
        }
    }
}
