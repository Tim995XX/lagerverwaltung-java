import { useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router";
import { useAnmeldung } from "../anmeldung/Anmeldung";
import { MENUE } from "../Layout";
import { ApiFehler } from "../api/client";
import { TextFeld } from "../komponenten/Formular";
import { useRueckmeldung } from "../komponenten/Rueckmeldung";
import { Seite } from "../komponenten/Seite";

export function AnmeldeSeite() {
  const { benutzer, hinweis, anmelden } = useAnmeldung();
  const { melde } = useRueckmeldung();
  const zurueck = (useLocation().state as { zurueck?: string } | null)?.zurueck;
  const [laeuft, setLaeuft] = useState(false);

  // Nach erfolgreicher Anmeldung zurück zur Seite, die man ursprünglich öffnen wollte,
  // sofern sie für die eigene Rolle gedacht ist (z. B. Schichtwechsel Leitstand → Fahrer)
  if (benutzer) {
    const erlaubt = MENUE.some((m) => m.pfad === zurueck && m.rollen.includes(benutzer.rolle));
    return <Navigate to={erlaubt ? zurueck! : "/transporte"} replace />;
  }

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const daten = new FormData(e.currentTarget);
    const name = String(daten.get("name") ?? "").trim();
    const passwort = String(daten.get("passwort") ?? "");
    if (!name || !passwort) {
      melde({ art: "fehler", text: "Bitte Benutzername und Passwort eingeben." });
      return;
    }
    setLaeuft(true);
    try {
      await anmelden(name, passwort);
    } catch (fehler) {
      const text =
        fehler instanceof ApiFehler && fehler.status === 401
          ? "Benutzername oder Passwort ist falsch."
          : fehler instanceof Error
            ? fehler.message
            : "Anmeldung fehlgeschlagen.";
      melde({ art: "fehler", text });
      setLaeuft(false);
    }
  }

  return (
    <Seite titel="Anmelden">
      {hinweis && <p className="hinweis-kasten">{hinweis}</p>}
      <form className="formular schmal" onSubmit={absenden} noValidate aria-busy={laeuft}>
        <TextFeld label="Benutzername" name="name" autoComplete="username" required autoCapitalize="none" spellCheck={false} />
        <TextFeld label="Passwort" name="passwort" type="password" autoComplete="current-password" required />
        <button type="submit">{laeuft ? "Anmeldung läuft …" : "Anmelden"}</button>
      </form>
      <p className="feld-hinweis">
        Demo-Zugänge: <code>leitstand</code> / <code>leitstand</code> (darf alles) und <code>fahrer</code> /{" "}
        <code>fahrer</code> (Staplerfahrer).
      </p>
    </Seite>
  );
}
