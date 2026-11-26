import { useEffect, useRef } from "react";
import { NavLink, Outlet, useLocation } from "react-router";
import { useAnmeldung } from "./anmeldung/Anmeldung";
import { TEXTE, type Rolle } from "./api/typen";
import { RueckmeldungProvider } from "./komponenten/Rueckmeldung";

/** Menü je Rolle. Der Server prüft die Rechte ohnehin; das Menü zeigt nur, was sinnvoll ist. */
export const MENUE: { pfad: string; text: string; rollen: Rolle[] }[] = [
  { pfad: "/transporte", text: "Transporte", rollen: ["LEITSTAND", "STAPLERFAHRER"] },
  { pfad: "/wareneingang", text: "Wareneingang", rollen: ["LEITSTAND"] },
  { pfad: "/kommissionierung", text: "Kommissionierung", rollen: ["LEITSTAND"] },
  { pfad: "/klaerplatz", text: "Klärplatz", rollen: ["LEITSTAND"] },
  { pfad: "/lager", text: "Lager", rollen: ["LEITSTAND", "STAPLERFAHRER"] },
  { pfad: "/artikel", text: "Artikel", rollen: ["LEITSTAND"] },
];

export function Layout() {
  const { benutzer, abmelden } = useAnmeldung();
  const { pathname } = useLocation();
  const ersterAufruf = useRef(true);

  // Nach einem Seitenwechsel den Fokus auf die Überschrift setzen. Sonst bliebe er auf dem
  // angeklickten Menüpunkt und ein Screenreader bekäme vom neuen Inhalt nichts mit.
  useEffect(() => {
    if (ersterAufruf.current) {
      ersterAufruf.current = false;
      return;
    }
    document.getElementById("seitentitel")?.focus();
  }, [pathname]);

  return (
    <RueckmeldungProvider>
      <a
        className="springlink"
        href="#hauptinhalt"
        onClick={(e) => {
          e.preventDefault();
          document.getElementById("hauptinhalt")?.focus();
        }}
      >
        Zum Inhalt springen
      </a>
      <header className="kopf">
        <div className="kopf-zeile">
          <p className="logo">Lagerverwaltung</p>
          {benutzer && (
            <div className="benutzer">
              <p>
                Angemeldet als <strong>{benutzer.name}</strong> ({TEXTE.rolle[benutzer.rolle]})
              </p>
              <button type="button" className="knopf-zweitrangig" onClick={() => abmelden()}>
                Abmelden
              </button>
            </div>
          )}
        </div>
        {benutzer && (
          <nav aria-label="Hauptmenü">
            <ul>
              {MENUE.filter((eintrag) => eintrag.rollen.includes(benutzer.rolle)).map((eintrag) => (
                <li key={eintrag.pfad}>
                  {/* NavLink setzt aria-current="page" auf den aktiven Eintrag */}
                  <NavLink to={eintrag.pfad}>{eintrag.text}</NavLink>
                </li>
              ))}
            </ul>
          </nav>
        )}
      </header>
      <main id="hauptinhalt" tabIndex={-1}>
        <Outlet />
      </main>
    </RueckmeldungProvider>
  );
}
