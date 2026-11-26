import { useEffect, useRef, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { formatiereZeit, TEXTE, type Klaerfall } from "../api/typen";
import { useDaten } from "../api/useDaten";
import { useAktion } from "../komponenten/Aktion";
import { AuswahlFeld, TextFeld } from "../komponenten/Formular";
import { Ladezustand } from "../komponenten/Ladezustand";
import { useRueckmeldung } from "../komponenten/Rueckmeldung";
import { Seite } from "../komponenten/Seite";

export function KlaerplatzSeite() {
  const [erledigt, setErledigt] = useState(false);
  const { daten, fehler, laedt, neuLaden } = useDaten<Klaerfall[]>(`/api/klaerfaelle?erledigt=${erledigt}`);
  const { melde } = useRueckmeldung();

  // Ein erledigter Fall verschwindet aus der Liste: Fokus auf die Listenüberschrift
  const fokusSetzen = useRef(false);
  useEffect(() => {
    if (!laedt && fokusSetzen.current) {
      document.getElementById("klaerfaelle-titel")?.focus();
      fokusSetzen.current = false;
    }
  }, [laedt, daten]);

  function nachErledigen(id: number) {
    melde({ art: "erfolg", text: `Klärfall ${id} erledigt.` });
    fokusSetzen.current = true;
    neuLaden();
  }

  return (
    <Seite titel="Klärplatz" einleitung="Buchungen, die das System nicht automatisch ausführen konnte. Nach der Klärung mit Kommentar abschließen.">
      <div className="werkzeugleiste">
        <AuswahlFeld label="Anzeigen" name="erledigt" value={String(erledigt)} onChange={(e) => setErledigt(e.target.value === "true")}>
          <option value="false">Offene Klärfälle</option>
          <option value="true">Erledigte Klärfälle</option>
        </AuswahlFeld>
      </div>
      <Ladezustand laedt={laedt} fehler={fehler} neuLaden={neuLaden} hatDaten={!!daten} />
      {daten && (
        <section aria-labelledby="klaerfaelle-titel" aria-busy={laedt}>
          <h2 id="klaerfaelle-titel" tabIndex={-1}>
            {daten.length === 1 ? "1 Klärfall" : `${daten.length} Klärfälle`}
          </h2>
          {daten.length === 0 ? (
            <p>{erledigt ? "Noch keine erledigten Klärfälle." : "Keine offenen Klärfälle."}</p>
          ) : (
            <ul className="karten">
              {daten.map((fall) => (
                <li key={fall.id}>
                  <KlaerfallKarte fall={fall} nachErledigen={nachErledigen} />
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </Seite>
  );
}

function KlaerfallKarte({ fall, nachErledigen }: { fall: Klaerfall; nachErledigen: (id: number) => void }) {
  const { ausfuehren, felder } = useAktion();
  const titelId = `klaerfall-${fall.id}-titel`;

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const kommentar = new FormData(e.currentTarget).get("kommentar");
    const ok = await ausfuehren(() => api(`/api/klaerfaelle/${fall.id}/erledigen`, { methode: "POST", daten: { kommentar } }));
    if (ok) nachErledigen(fall.id);
  }

  return (
    <article className="karte" aria-labelledby={titelId}>
      <h3 id={titelId}>
        Klärfall {fall.id}: {TEXTE.klaerGrund[fall.grund]}
      </h3>
      <p>{fall.beschreibung}</p>
      <dl className="eigenschaften">
        <div>
          <dt>Artikel</dt>
          <dd>
            {fall.artikelNummer}, {fall.menge} Stück
          </dd>
        </div>
        {fall.kommissionierauftragId && (
          <div>
            <dt>Kommissionierauftrag</dt>
            <dd>{fall.kommissionierauftragId}</dd>
          </div>
        )}
        <div>
          <dt>Erstellt</dt>
          <dd>{formatiereZeit(fall.erstelltAm)}</dd>
        </div>
        {fall.erledigt && (
          <div>
            <dt>Kommentar</dt>
            <dd>{fall.kommentar}</dd>
          </div>
        )}
      </dl>
      {!fall.erledigt && (
        <form onSubmit={absenden} noValidate className="unterformular">
          <TextFeld
            label="Was wurde getan?"
            name="kommentar"
            id={`klaerfall-${fall.id}-kommentar`}
            required
            maxLength={500}
            fehler={felder.kommentar}
          />
          <button type="submit">Klärfall {fall.id} erledigen</button>
        </form>
      )}
    </article>
  );
}
