package de.hilpert.lager.sicherheit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Legt je Rolle einen Beispielbenutzer an, solange es noch keine Benutzer gibt.
 * Getrennt von DemoDaten, damit auch eine bestehende Datenbank mit Lagerplätzen Benutzer bekommt.
 */
@Component
@ConditionalOnProperty(name = "lager.demodaten", havingValue = "true")
public class DemoBenutzer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoBenutzer.class);

    private final BenutzerRepository benutzerRepository;
    private final PasswordEncoder passwortEncoder;
    private final String passwortLeitstand;
    private final String passwortFahrer;

    public DemoBenutzer(BenutzerRepository benutzerRepository, PasswordEncoder passwortEncoder,
                        @Value("${lager.demo.passwort-leitstand}") String passwortLeitstand,
                        @Value("${lager.demo.passwort-fahrer}") String passwortFahrer) {
        this.benutzerRepository = benutzerRepository;
        this.passwortEncoder = passwortEncoder;
        this.passwortLeitstand = passwortLeitstand;
        this.passwortFahrer = passwortFahrer;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (benutzerRepository.count() > 0) {
            return;
        }
        benutzerRepository.save(new Benutzer("leitstand", passwortEncoder.encode(passwortLeitstand), Rolle.LEITSTAND));
        benutzerRepository.save(new Benutzer("fahrer", passwortEncoder.encode(passwortFahrer), Rolle.STAPLERFAHRER));
        log.info("Demobenutzer angelegt: leitstand (LEITSTAND), fahrer (STAPLERFAHRER)");
    }
}
