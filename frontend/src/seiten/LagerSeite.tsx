import { useId, useState, type FormEvent } from "react";
import { useAnmeldung } from "../anmeldung/Anmeldung";
import { api } from "../api/client";
import type { Bestand, Lagerplatz } from "../api/typen";
import { useDaten } from "../api/useDaten";
import { useAktion } from "../komponenten/Aktion";
import { TextFeld } from "../komponenten/Formular";
import { Ladezustand } from "../komponenten/Ladezustand";
import { useRueckmeldung } from "../komponenten/Rueckmeldung";
import { Seite } from "../komponenten/Seite";

export function LagerSeite() {
  const { benutzer } = useAnmeldung();
  const plaetze = useDaten<Lagerplatz[]>("/api/lagerplaetze");
  const bestaende = useDaten<Bestand[]>("/api/bestaende");
  const { melde } = useRueckmeldung();
  const [nurBelegt, setNurBelegt] = useState(false);
  const leitstand = benutzer?.rolle === "LEITSTAND";

  const bestandJePlatz = new Map(bestaende.daten?.map((b) => [b.lagerplatz, b]));
  const zeilen = plaetze.daten?.filter((p) => !nurBelegt || bestandJePlatz.has(p.code));

  function nachAenderung(text: string) {
    melde({ art: "erfolg", text });
    plaetze.neuLaden();
  }

  return (
    <Seite titel="Lager" einleitung="Lagerplätze mit Bestand. Platzadresse: Gang-Regal-Ebene.">
      <div className="werkzeugleiste">
        <div className="feld feld-ankreuz">
          <input id="nur-belegt" type="checkbox" checked={nurBelegt} onChange={(e) => setNurBelegt(e.target.checked)} />
          <label htmlFor="nur-belegt">Nur belegte Plätze zeigen</label>
        </div>
      </div>
      <Ladezustand {...plaetze} hatDaten={!!plaetze.daten} />
      <Ladezustand {...bestaende} hatDaten={!!bestaende.daten} />
      {zeilen && (
        // Auf schmalen Bildschirmen scrollt nur die Tabelle; per Tastatur erreichbar (tabIndex) und benannt
        <div className="tabellen-rahmen" role="region" aria-labelledby="lager-tabelle-titel" tabIndex={0}>
          <table>
            <caption id="lager-tabelle-titel">
              {zeilen.length} Lagerplätze{nurBelegt ? " mit Bestand" : ""}
            </caption>
            <thead>
              <tr>
                <th scope="col">Platz</th>
                <th scope="col">Artikel</th>
                <th scope="col" className="zahl">Menge</th>
                <th scope="col" className="zahl">Reserviert</th>
                <th scope="col" className="zahl">Frei / Kapazität</th>
                <th scope="col">Zustand</th>
                {leitstand && <th scope="col">Aktion</th>}
              </tr>
            </thead>
            <tbody>
              {zeilen.map((platz) => {
                const bestand = bestandJePlatz.get(platz.code);
                return (
                  <tr key={platz.code}>
                    <th scope="row">{platz.code}</th>
                    <td>{bestand ? `${bestand.artikelNummer} – ${bestand.bezeichnung}` : "leer"}</td>
                    <td className="zahl">{bestand?.menge ?? 0}</td>
                    <td className="zahl">{bestand?.reserviert ?? 0}</td>
                    <td className="zahl">
                      {bestand?.freieKapazitaet ?? platz.kapazitaet} / {platz.kapazitaet}
                    </td>
                    <td>
                      {platz.gesperrt ? (
                        <span className="marke marke-gesperrt">Gesperrt: {platz.sperrgrund}</span>
                      ) : (
                        "Frei nutzbar"
                      )}
                    </td>
                    {leitstand && (
                      <td>
                        <SperrAktion platz={platz} nachAenderung={nachAenderung} />
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Seite>
  );
}

function SperrAktion({ platz, nachAenderung }: { platz: Lagerplatz; nachAenderung: (text: string) => void }) {
  const [offen, setOffen] = useState(false);
  const { ausfuehren, felder } = useAktion();
  const bereich = useId();

  async function freigeben() {
    const ok = await ausfuehren(() => api(`/api/lagerplaetze/${platz.code}/sperre`, { methode: "DELETE" }));
    if (ok) nachAenderung(`Platz ${platz.code} freigegeben.`);
  }

  async function sperren(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const grund = new FormData(e.currentTarget).get("grund");
    const ok = await ausfuehren(() => api(`/api/lagerplaetze/${platz.code}/sperre`, { methode: "PUT", daten: { grund } }));
    if (ok) {
      setOffen(false);
      nachAenderung(`Platz ${platz.code} gesperrt.`);
    }
  }

  if (platz.gesperrt) {
    return (
      <button type="button" className="knopf-zweitrangig knopf-klein" onClick={freigeben}>
        Freigeben <span className="sichtbar-nur-sr">Platz {platz.code}</span>
      </button>
    );
  }
  return (
    <div className="aufklapper">
      <button type="button" className="knopf-zweitrangig knopf-klein" aria-expanded={offen} aria-controls={bereich} onClick={() => setOffen(!offen)}>
        {/* Sichtbar kurz, vorgelesen mit Platz: Das sichtbare Wort steht vorn (WCAG 2.5.3) */}
        Sperren <span className="sichtbar-nur-sr">Platz {platz.code}</span>
      </button>
      <div id={bereich} hidden={!offen}>
        <form onSubmit={sperren} noValidate className="unterformular">
          <TextFeld label="Grund" name="grund" id={`sperre-${platz.code}-grund`} required maxLength={200} fehler={felder.grund} />
          <button type="submit">Sperren</button>
        </form>
      </div>
    </div>
  );
}
