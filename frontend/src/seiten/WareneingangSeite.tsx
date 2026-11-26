import { useState, type FormEvent } from "react";
import { Link } from "react-router";
import { api } from "../api/client";
import type { Artikel, WareneingangErgebnis } from "../api/typen";
import { useDaten } from "../api/useDaten";
import { useAktion } from "../komponenten/Aktion";
import { AuswahlFeld, FehlerUebersicht, TextFeld } from "../komponenten/Formular";
import { Ladezustand } from "../komponenten/Ladezustand";
import { useRueckmeldung } from "../komponenten/Rueckmeldung";
import { Seite } from "../komponenten/Seite";

const BESCHRIFTUNGEN = { artikelNummer: "Artikel", menge: "Menge" };

export function WareneingangSeite() {
  const artikel = useDaten<Artikel[]>("/api/artikel");
  const { ausfuehren, felder } = useAktion();
  const { melde } = useRueckmeldung();
  const [ergebnis, setErgebnis] = useState<WareneingangErgebnis | null>(null);

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const daten = new FormData(e.currentTarget);
    setErgebnis(null);
    const antwort = await ausfuehren(() =>
      api<WareneingangErgebnis>("/api/wareneingaenge", {
        methode: "POST",
        daten: { artikelNummer: daten.get("artikelNummer"), menge: Number(daten.get("menge")) },
      }),
    );
    if (!antwort) return;
    setErgebnis(antwort);
    melde(
      antwort.ergebnis === "PLATZ_RESERVIERT"
        ? { art: "erfolg", text: `Platz ${antwort.lagerplatz} reserviert, Transportauftrag ${antwort.transportauftragId} angelegt.` }
        : { art: "hinweis", text: `Kein passender Platz. Ware auf den Klärplatz, Klärfall ${antwort.klaerfallId} angelegt.` },
    );
  }

  return (
    <Seite titel="Wareneingang" einleitung="Das System sucht einen passenden Platz und legt einen Transportauftrag an. Gebucht wird erst beim Scan am Platz.">
      <Ladezustand {...artikel} hatDaten={!!artikel.daten} />
      {artikel.daten && (
        <form className="formular schmal" onSubmit={absenden} noValidate>
          <FehlerUebersicht felder={felder} beschriftungen={BESCHRIFTUNGEN} />
          <AuswahlFeld label="Artikel" name="artikelNummer" required fehler={felder.artikelNummer}>
            {artikel.daten.map((a) => (
              <option key={a.artikelNummer} value={a.artikelNummer}>
                {a.artikelNummer} – {a.bezeichnung}
              </option>
            ))}
          </AuswahlFeld>
          <TextFeld label="Menge in Stück" name="menge" type="number" inputMode="numeric" min={1} required fehler={felder.menge} />
          <button type="submit">Einlagern</button>
        </form>
      )}

      {ergebnis && (
        <section aria-labelledby="ergebnis-titel" className="ergebnis">
          <h2 id="ergebnis-titel">Ergebnis</h2>
          {ergebnis.ergebnis === "PLATZ_RESERVIERT" ? (
            <dl className="eigenschaften">
              <div>
                <dt>Lagerplatz</dt>
                <dd className="ziel">{ergebnis.lagerplatz}</dd>
              </div>
              <div>
                <dt>Transportauftrag</dt>
                <dd>{ergebnis.transportauftragId}</dd>
              </div>
              <div>
                <dt>Begründung</dt>
                <dd>{ergebnis.begruendung}</dd>
              </div>
            </dl>
          ) : (
            <p>
              Klärfall {ergebnis.klaerfallId} wurde angelegt. <Link to="/klaerplatz">Zum Klärplatz</Link>
            </p>
          )}
        </section>
      )}
    </Seite>
  );
}
