package de.hilpert.lager.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Die React-Oberfläche hat eigene Adressen (z. B. /transporte). Lädt man so eine Adresse direkt
 * oder neu, liefert der Server die index.html aus; welche Seite erscheint, entscheidet dann React.
 */
@Controller
public class OberflaecheController {

    /** Muss zu den Routen in frontend/src/App.tsx passen. */
    public static final String[] SEITEN = {
            "/anmelden", "/transporte", "/wareneingang", "/kommissionierung", "/klaerplatz", "/lager", "/artikel"};

    @GetMapping({"/anmelden", "/transporte", "/wareneingang", "/kommissionierung", "/klaerplatz", "/lager",
            "/artikel"})
    public String seite() {
        return "forward:/index.html";
    }
}
