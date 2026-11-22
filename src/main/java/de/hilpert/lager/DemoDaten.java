package de.hilpert.lager;

import de.hilpert.lager.service.ArtikelService;
import de.hilpert.lager.service.LagerplatzService;
import de.hilpert.lager.service.WareneingangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Legt beim ersten Start ein kleines Beispiellager an, damit man die API sofort ausprobieren kann.
 * Läuft nur, wenn lager.demodaten=true ist und noch keine Lagerplätze existieren.
 * Der Bestand wird über den normalen Wareneingang gebucht, nicht direkt in die Datenbank geschrieben.
 */
@Component
@ConditionalOnProperty(name = "lager.demodaten", havingValue = "true")
public class DemoDaten implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDaten.class);

    private final ArtikelService artikelService;
    private final LagerplatzService lagerplatzService;
    private final WareneingangService wareneingangService;

    public DemoDaten(ArtikelService artikelService, LagerplatzService lagerplatzService,
                     WareneingangService wareneingangService) {
        this.artikelService = artikelService;
        this.lagerplatzService = lagerplatzService;
        this.wareneingangService = wareneingangService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!lagerplatzService.alle().isEmpty()) {
            return;
        }

        artikelService.anlegen("1001", "Mineralwasser 6x1,5 l");
        artikelService.anlegen("1002", "Apfelsaft 6x1 l");
        artikelService.anlegen("2001", "Haferflocken 500 g");

        // Gang 1: kleine Plätze (40 Stück), Gang 2: große Plätze (120 Stück)
        for (int regal = 1; regal <= 3; regal++) {
            for (int ebene = 1; ebene <= 2; ebene++) {
                lagerplatzService.anlegen(1, regal, ebene, 40);
                lagerplatzService.anlegen(2, regal, ebene, 120);
            }
        }

        wareneingangService.einlagern("1001", 100);
        wareneingangService.einlagern("1002", 30);
        wareneingangService.einlagern("2001", 25);
        log.info("Demodaten angelegt: 3 Artikel, 12 Lagerplätze, 3 Wareneingänge");
    }
}
