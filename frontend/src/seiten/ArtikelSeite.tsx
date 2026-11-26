import { useRef, type FormEvent } from "react";
import { api } from "../api/client";
import { TEXTE, type Artikel, type Umschlag } from "../api/typen";
import { useDaten } from "../api/useDaten";
import { useAktion } from "../komponenten/Aktion";
import { AuswahlFeld, FehlerUebersicht, TextFeld } from "../komponenten/Formular";
import { Ladezustand } from "../komponenten/Ladezustand";
import { useRueckmeldung } from "../komponenten/Rueckmeldung";
import { Seite } from "../komponenten/Seite";

const BESCHRIFTUNGEN: Record<string, string> = {
  artikelNummer: "Artikelnummer",
  bezeichnung: "Bezeichnung",
  "masse.laengeMm": "Länge",
  "masse.breiteMm": "Breite",
  "masse.hoeheMm": "Höhe",
  "masse.gewichtGramm": "Gewicht",
};

const zahl = (wert: FormDataEntryValue | null) => (wert === null || wert === "" ? 0 : Number(wert));

export function ArtikelSeite() {
  const { daten, fehler, laedt, neuLaden } = useDaten<Artikel[]>("/api/artikel");
  const { ausfuehren, felder } = useAktion();
  const { melde } = useRueckmeldung();
  const formular = useRef<HTMLFormElement>(null);

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const d = new FormData(e.currentTarget);
    const neu = await ausfuehren(() =>
      api<Artikel>("/api/artikel", {
        methode: "POST",
        daten: {
          artikelNummer: d.get("artikelNummer"),
          bezeichnung: d.get("bezeichnung"),
          masse: {
            laengeMm: zahl(d.get("masse.laengeMm")),
            breiteMm: zahl(d.get("masse.breiteMm")),
            hoeheMm: zahl(d.get("masse.hoeheMm")),
            gewichtGramm: zahl(d.get("masse.gewichtGramm")),
            umschlag: d.get("masse.umschlag"),
          },
        },
      }),
    );
    if (!neu) return;
    melde({ art: "erfolg", text: `Artikel ${neu.artikelNummer} angelegt.` });
    formular.current?.reset();
    neuLaden();
  }

  return (
    <Seite titel="Artikel">
      <Ladezustand laedt={laedt} fehler={fehler} neuLaden={neuLaden} hatDaten={!!daten} />
      {daten && (
        <div className="tabellen-rahmen" role="region" aria-labelledby="artikel-tabelle-titel" tabIndex={0}>
          <table>
            <caption id="artikel-tabelle-titel">{daten.length} Artikel</caption>
            <thead>
              <tr>
                <th scope="col">Nummer</th>
                <th scope="col">Bezeichnung</th>
                <th scope="col">Maße L × B × H in mm</th>
                <th scope="col" className="zahl">Gewicht in g</th>
                <th scope="col">Umschlag</th>
              </tr>
            </thead>
            <tbody>
              {daten.map((a) => (
                <tr key={a.artikelNummer}>
                  <th scope="row">{a.artikelNummer}</th>
                  <td>{a.bezeichnung}</td>
                  <td>{a.laengeMm ? `${a.laengeMm} × ${a.breiteMm} × ${a.hoeheMm}` : "fehlen"}</td>
                  <td className="zahl">{a.gewichtGramm ?? "–"}</td>
                  <td>{a.umschlag ? TEXTE.umschlag[a.umschlag] : "–"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <section aria-labelledby="neu-titel">
        <h2 id="neu-titel">Neuen Artikel anlegen</h2>
        <form className="formular" onSubmit={absenden} noValidate ref={formular}>
          <FehlerUebersicht felder={felder} beschriftungen={BESCHRIFTUNGEN} />
          <div className="feld-gruppe">
            <TextFeld label="Artikelnummer" name="artikelNummer" required maxLength={20} autoComplete="off" fehler={felder.artikelNummer} />
            <TextFeld label="Bezeichnung" name="bezeichnung" required maxLength={100} autoComplete="off" fehler={felder.bezeichnung} />
          </div>
          <fieldset>
            <legend>Maße und Gewicht je Stück</legend>
            <div className="feld-gruppe">
              <TextFeld label="Länge in mm" name="masse.laengeMm" type="number" inputMode="numeric" min={1} required fehler={felder["masse.laengeMm"]} />
              <TextFeld label="Breite in mm" name="masse.breiteMm" type="number" inputMode="numeric" min={1} required fehler={felder["masse.breiteMm"]} />
              <TextFeld label="Höhe in mm" name="masse.hoeheMm" type="number" inputMode="numeric" min={1} required fehler={felder["masse.hoeheMm"]} />
              <TextFeld label="Gewicht in g" name="masse.gewichtGramm" type="number" inputMode="numeric" min={1} required fehler={felder["masse.gewichtGramm"]} />
            </div>
            <AuswahlFeld label="Umschlag" name="masse.umschlag" defaultValue="MITTEL" hinweis="Schnelldreher kommen nah an die Tore.">
              {(Object.keys(TEXTE.umschlag) as Umschlag[]).map((u) => (
                <option key={u} value={u}>
                  {TEXTE.umschlag[u]}
                </option>
              ))}
            </AuswahlFeld>
          </fieldset>
          <button type="submit">Artikel anlegen</button>
        </form>
      </section>
    </Seite>
  );
}
