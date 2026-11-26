import { useEffect, useRef, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { TEXTE, type Artikel, type Kommissionierauftrag, type Prioritaet } from "../api/typen";
import { useDaten } from "../api/useDaten";
import { useAktion } from "../komponenten/Aktion";
import { AuswahlFeld, FehlerUebersicht, TextFeld } from "../komponenten/Formular";
import { Ladezustand } from "../komponenten/Ladezustand";
import { useRueckmeldung } from "../komponenten/Rueckmeldung";
import { Seite } from "../komponenten/Seite";

interface Position {
  schluessel: number;
  artikelNummer: string;
  menge: string;
}

export function KommissionierSeite() {
  const artikel = useDaten<Artikel[]>("/api/artikel");
  const { ausfuehren, felder } = useAktion();
  const { melde } = useRueckmeldung();
  const naechsterSchluessel = useRef(1);
  const [positionen, setPositionen] = useState<Position[]>([{ schluessel: 0, artikelNummer: "", menge: "" }]);
  const [auftrag, setAuftrag] = useState<Kommissionierauftrag | null>(null);

  // Fokus nach Hinzufügen/Entfernen gezielt setzen, sonst landet er im Nichts
  const fokusZiel = useRef<string | null>(null);
  useEffect(() => {
    if (fokusZiel.current) {
      document.getElementById(fokusZiel.current)?.focus();
      fokusZiel.current = null;
    }
  }, [positionen]);

  function hinzufuegen() {
    const schluessel = naechsterSchluessel.current++;
    setPositionen([...positionen, { schluessel, artikelNummer: "", menge: "" }]);
    fokusZiel.current = `position-${schluessel}-artikel`;
  }

  function entfernen(schluessel: number) {
    setPositionen(positionen.filter((p) => p.schluessel !== schluessel));
    fokusZiel.current = "position-hinzufuegen";
  }

  function aendern(schluessel: number, feld: "artikelNummer" | "menge", wert: string) {
    setPositionen(positionen.map((p) => (p.schluessel === schluessel ? { ...p, [feld]: wert } : p)));
  }

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const prioritaet = new FormData(e.currentTarget).get("prioritaet") as Prioritaet;
    setAuftrag(null);
    const antwort = await ausfuehren(() =>
      api<Kommissionierauftrag>("/api/kommissionierauftraege", {
        methode: "POST",
        daten: {
          prioritaet,
          positionen: positionen.map((p) => ({ artikelNummer: p.artikelNummer, menge: Number(p.menge) })),
        },
      }),
    );
    if (!antwort) return;
    setAuftrag(antwort);
    melde(
      antwort.status === "FREIGEGEBEN"
        ? { art: "erfolg", text: `Auftrag ${antwort.id} freigegeben, ${antwort.transportauftraege.length} Transportaufträge angelegt.` }
        : { art: "hinweis", text: `Auftrag ${antwort.id} ist in Klärung: Der Bestand reicht nicht. Nichts wurde gebucht.` },
    );
    naechsterSchluessel.current++;
    setPositionen([{ schluessel: naechsterSchluessel.current, artikelNummer: "", menge: "" }]);
  }

  // Feldnamen der API ("positionen[0].menge") in lesbare Beschriftungen übersetzen
  const beschriftungen: Record<string, string> = { positionen: "Positionen", prioritaet: "Priorität" };
  positionen.forEach((_, i) => {
    beschriftungen[`positionen[${i}].artikelNummer`] = `Position ${i + 1}, Artikel`;
    beschriftungen[`positionen[${i}].menge`] = `Position ${i + 1}, Menge`;
  });

  return (
    <Seite titel="Kommissionierung" einleitung="Gebucht wird nur, wenn der Bestand für alle Positionen reicht. Sonst geht der Auftrag in Klärung.">
      <Ladezustand {...artikel} hatDaten={!!artikel.daten} />
      {artikel.daten && (
        <form className="formular" onSubmit={absenden} noValidate>
          <FehlerUebersicht felder={felder} beschriftungen={beschriftungen} />
          {positionen.map((p, i) => (
            <fieldset key={p.schluessel} className="position">
              <legend>Position {i + 1}</legend>
              <AuswahlFeld
                label="Artikel"
                name={`positionen[${i}].artikelNummer`}
                id={`position-${p.schluessel}-artikel`}
                required
                value={p.artikelNummer}
                onChange={(e) => aendern(p.schluessel, "artikelNummer", e.target.value)}
                fehler={felder[`positionen[${i}].artikelNummer`]}
              >
                <option value="">Bitte wählen</option>
                {artikel.daten!.map((a) => (
                  <option key={a.artikelNummer} value={a.artikelNummer}>
                    {a.artikelNummer} – {a.bezeichnung}
                  </option>
                ))}
              </AuswahlFeld>
              <TextFeld
                label="Menge in Stück"
                name={`positionen[${i}].menge`}
                id={`position-${p.schluessel}-menge`}
                type="number"
                inputMode="numeric"
                min={1}
                required
                value={p.menge}
                onChange={(e) => aendern(p.schluessel, "menge", e.target.value)}
                fehler={felder[`positionen[${i}].menge`]}
              />
              {positionen.length > 1 && (
                <button type="button" className="knopf-zweitrangig" onClick={() => entfernen(p.schluessel)}>
                  Position {i + 1} entfernen
                </button>
              )}
            </fieldset>
          ))}
          <button type="button" id="position-hinzufuegen" className="knopf-zweitrangig" onClick={hinzufuegen}>
            Position hinzufügen
          </button>
          <AuswahlFeld label="Priorität" name="prioritaet" defaultValue="NORMAL">
            {(Object.keys(TEXTE.prioritaet) as Prioritaet[]).map((wert) => (
              <option key={wert} value={wert}>
                {TEXTE.prioritaet[wert]}
              </option>
            ))}
          </AuswahlFeld>
          <button type="submit">Auftrag anlegen</button>
        </form>
      )}

      {auftrag && (
        <section aria-labelledby="ergebnis-titel" className="ergebnis">
          <h2 id="ergebnis-titel">
            Auftrag {auftrag.id}: {TEXTE.auftragStatus[auftrag.status]}
          </h2>
          {auftrag.transportauftraege.length > 0 ? (
            <ul>
              {auftrag.transportauftraege.map((t) => (
                <li key={t.id}>
                  Transport {t.id}: {t.menge} × {t.artikelNummer} von Platz {t.vonOrt}
                </li>
              ))}
            </ul>
          ) : (
            <p>Keine Transporte angelegt. Der Klärfall steht auf dem Klärplatz.</p>
          )}
        </section>
      )}
    </Seite>
  );
}
