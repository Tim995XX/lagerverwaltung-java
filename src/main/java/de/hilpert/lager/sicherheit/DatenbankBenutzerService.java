package de.hilpert.lager.sicherheit;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Liefert Spring Security den Benutzer aus der Datenbank. Den Passwortvergleich macht Spring selbst
 * (DaoAuthenticationProvider), auch für unbekannte Namen mit gleicher Laufzeit wie für bekannte.
 */
@Service
public class DatenbankBenutzerService implements UserDetailsService {

    private final BenutzerRepository benutzerRepository;

    public DatenbankBenutzerService(BenutzerRepository benutzerRepository) {
        this.benutzerRepository = benutzerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String name) {
        Benutzer benutzer = benutzerRepository.findByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("Unbekannter Benutzer"));
        return User.withUsername(benutzer.getName())
                .password(benutzer.getPasswortHash())
                .roles(benutzer.getRolle().name())
                .disabled(!benutzer.isAktiv())
                .build();
    }
}
