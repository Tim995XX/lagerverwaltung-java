import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { useAnmeldung } from "../anmeldung/Anmeldung";
import { api } from "../api/client";
import {
  formatiereZeit,
  TEXTE,
  type Prioritaet,
  type Rolle,
  type Transportauftrag,
  type TransportStatus,
  type UmleitungErgebnis,
} from "../api/typen";
import { useDaten } from "../api/useDaten";
import { useAktion } from "../komponenten/Aktion";
import { AuswahlFeld, TextFeld } from "../komponenten/Formular";
import { Ladezustand } from "../komponenten/Ladezustand";
import { PrioritaetMarke, StatusMarke } from "../komponenten/Marke";
import { useRueckmeldung, type Meldung } from "../komponenten/Rueckmeldung";
import { Seite } from "../komponenten/Seite";

type Filter = "AKTIV" | "ALLE" | TransportStatus;

const FILTER: { wert: Filter; text: string }[] = [
  { wert: "AKTIV", text: "Offen und in Arbeit" },
  { wert: "OFFEN", text: "Nur offen" },
  { wert: "IN_ARBEIT", text: "Nur in Arbeit" },
  { wert: "ERLEDIGT", text: "Erledigt" },
  { wert: "ABGEBROCHEN", text: "Abgebrochen" },
  { wert: "ALLE", text: "Alle" },
];

export function TransporteSeite() {
  const { benutzer } = useAnmeldung();
  const { melde } = useRueckmeldung();
  const [filter, setFilter] = useState<Filter>("AKTIV");
  const pfad = filter === "AKTIV" || filter === "ALLE" ? "/api/transportauftraege" : `/api/transportauftraege?status=${filter}`;
  const { daten, fehler, laedt, neuLaden } = useDaten<Transportauftrag[]>(pfad);
  const auftraege = filter === "AKTIV" ? daten?.filter((a) => a.status === "OFFEN" || a.status === "IN_ARBEIT") : daten;

  // Nach einer Aktion ändern sich die Knöpfe der Karte. Damit der Fokus nicht verloren geht,
  // springt er nach dem Neuladen auf die Überschrift der Karte (oder der Liste, falls sie weg ist).
  const fokusNach = useRef<number | null>(null);
  useEffect(() => {
    if (laedt || fokusNach.current === null) return;
    const ziel = document.getElementById(`auftrag-${fokusNach.current}-titel`) ?? document.getElementById("liste-titel");
    ziel?.focus();
    fokusNach.current = null;
  }, [laedt, daten]);

  function nachAktion(id: number, meldung: Meldung) {
    melde(meldung);
    fokusNach.current = id;
    neuLaden();
  }

  const fahrer = benutzer?.rolle === "STAPLERFAHRER";
  return (
    <Seite
      titel="Transportaufträge"
      einleitung={
        fahrer
          ? "Aufträge von oben nach unten abarbeiten: Starten, hinfahren, Ware und Platz scannen."
          : "Reihenfolge: Priorität, dann ältester Auftrag zuerst."
      }
    >
      <div className="werkzeugleiste">
        <AuswahlFeld label="Anzeigen" name="filter" value={filter} onChange={(e) => setFilter(e.target.value as Filter)}>
          {FILTER.map((f) => (
            <option key={f.wert} value={f.wert}>
              {f.text}
            </option>
          ))}
        </AuswahlFeld>
        <button type="button" className="knopf-zweitrangig" onClick={neuLaden}>
          Aktualisieren
        </button>
      </div>

      <Ladezustand laedt={laedt} fehler={fehler} neuLaden={neuLaden} hatDaten={!!daten} />

      {auftraege && (
        <section aria-labelledby="liste-titel" aria-busy={laedt}>
          <h2 id="liste-titel" tabIndex={-1}>
            {auftraege.length === 1 ? "1 Auftrag" : `${auftraege.length} Aufträge`}
          </h2>
          {auftraege.length === 0 ? (
            <p>Keine Aufträge für diese Auswahl.</p>
          ) : (
            <ol className="karten">
              {auftraege.map((auftrag) => (
                <li key={auftrag.id}>
                  <AuftragKarte auftrag={auftrag} rolle={benutzer!.rolle} nachAktion={nachAktion} />
                </li>
              ))}
            </ol>
          )}
        </section>
      )}
    </Seite>
  );
}

function AuftragKarte({
  auftrag,
  rolle,
  nachAktion,
}: {
  auftrag: Transportauftrag;
  rolle: Rolle;
  nachAktion: (id: number, meldung: Meldung) => void;
}) {
  const titelId = `auftrag-${auftrag.id}-titel`;
  const aktiv = auftrag.status === "OFFEN" || auftrag.status === "IN_ARBEIT";
  const { ausfuehren } = useAktion();

  async function statuswechsel(schritt: "start" | "abschluss", erfolg: string) {
    const ok = await ausfuehren(() => api(`/api/transportauftraege/${auftrag.id}/${schritt}`, { methode: "POST" }));
    if (ok) nachAktion(auftrag.id, { art: "erfolg", text: erfolg });
  }

  return (
    <article className={`karte${auftrag.prioritaet === "HOCH" ? " karte-dringend" : ""}`} aria-labelledby={titelId}>
      <h3 id={titelId} tabIndex={-1}>
        Auftrag {auftrag.id}: {TEXTE.art[auftrag.art]}
      </h3>
      <dl className="eigenschaften">
        <div>
          <dt>Status</dt>
          <dd>
            <StatusMarke status={auftrag.status} />
          </dd>
        </div>
        <div>
          <dt>Priorität</dt>
          <dd>
            <PrioritaetMarke prioritaet={auftrag.prioritaet} />
          </dd>
        </div>
        <div>
          <dt>Artikel</dt>
          <dd>
            {auftrag.artikelNummer}, {auftrag.menge} Stück
          </dd>
        </div>
        <div>
          <dt>Von</dt>
          <dd>{auftrag.vonOrt ?? "Wareneingang"}</dd>
        </div>
        <div>
          <dt>Nach</dt>
          <dd className="ziel">{auftrag.nachOrt ?? "Warenausgang"}</dd>
        </div>
        <div>
          <dt>Erstellt</dt>
          <dd>{formatiereZeit(auftrag.erstelltAm)}</dd>
        </div>
      </dl>

      {aktiv && (
        <div className="karten-aktionen">
          {auftrag.status === "OFFEN" && (
            <button type="button" onClick={() => statuswechsel("start", `Auftrag ${auftrag.id} gestartet.`)}>
              Auftrag {auftrag.id} starten
            </button>
          )}
          {auftrag.status === "IN_ARBEIT" && auftrag.art === "AUSLAGERUNG" && (
            <button
              type="button"
             
              onClick={() => statuswechsel("abschluss", `Auftrag ${auftrag.id} abgeschlossen.`)}
            >
              Auftrag {auftrag.id} abschließen
            </button>
          )}
          {auftrag.status === "IN_ARBEIT" && auftrag.art === "EINLAGERUNG" && (
            <>
              <ScanFormular auftrag={auftrag} nachAktion={nachAktion} />
              <UmleitenFormular auftrag={auftrag} nachAktion={nachAktion} />
            </>
          )}
          {rolle === "LEITSTAND" && <PrioritaetFormular auftrag={auftrag} nachAktion={nachAktion} />}
        </div>
      )}
    </article>
  );
}

function ScanFormular({ auftrag, nachAktion }: { auftrag: Transportauftrag; nachAktion: (id: number, m: Meldung) => void }) {
  const { ausfuehren, felder } = useAktion();
  const platzFeld = useRef<HTMLInputElement>(null);
  const praefix = `scan-${auftrag.id}`;

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const daten = new FormData(e.currentTarget);
    const ok = await ausfuehren(() =>
      api(`/api/transportauftraege/${auftrag.id}/scan`, {
        methode: "POST",
        daten: { artikelNummer: daten.get("artikelNummer"), lagerplatzCode: daten.get("lagerplatzCode") },
      }),
    );
    if (ok) nachAktion(auftrag.id, { art: "erfolg", text: `Auftrag ${auftrag.id}: Scan passt, Bestand gebucht.` });
  }

  return (
    <form onSubmit={absenden} noValidate className="unterformular">
      <fieldset>
        <legend>Einlagerung per Scan bestätigen</legend>
        <TextFeld
          label="Barcode der Ware"
          name="artikelNummer"
          id={`${praefix}-artikel`}
          required
          autoComplete="off"
          fehler={felder.artikelNummer}
          // Handscanner schicken nach dem Code ein Enter: dann zum zweiten Feld statt absenden
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              platzFeld.current?.focus();
            }
          }}
        />
        <TextFeld
          label="Barcode des Lagerplatzes"
          name="lagerplatzCode"
          id={`${praefix}-platz`}
          ref={platzFeld}
          required
          autoComplete="off"
          fehler={felder.lagerplatzCode}
        />
        <button type="submit">
          Scan bestätigen
        </button>
      </fieldset>
    </form>
  );
}

function UmleitenFormular({ auftrag, nachAktion }: { auftrag: Transportauftrag; nachAktion: (id: number, m: Meldung) => void }) {
  const [offen, setOffen] = useState(false);
  const { ausfuehren, felder } = useAktion();
  const bereich = useId();

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const grund = new FormData(e.currentTarget).get("grund");
    const ergebnis = await ausfuehren(() =>
      api<UmleitungErgebnis>(`/api/transportauftraege/${auftrag.id}/umleiten`, { methode: "POST", daten: { grund } }),
    );
    if (!ergebnis) return;
    nachAktion(
      auftrag.id,
      ergebnis.ergebnis === "UMGELEITET"
        ? { art: "erfolg", text: `Platz ${ergebnis.gesperrterPlatz} gesperrt. Neuer Platz: ${ergebnis.neuerPlatz}.` }
        : { art: "hinweis", text: `Platz ${ergebnis.gesperrterPlatz} gesperrt, kein Ersatzplatz frei. Ware bitte zum Klärplatz bringen.` },
    );
  }

  return (
    <div className="aufklapper">
      <button type="button" className="knopf-zweitrangig" aria-expanded={offen} aria-controls={bereich} onClick={() => setOffen(!offen)}>
        Platz blockiert?
      </button>
      <div id={bereich} hidden={!offen}>
        <form onSubmit={absenden} noValidate className="unterformular">
          <TextFeld
            label="Was ist mit dem Platz?"
            name="grund"
            id={`umleiten-${auftrag.id}-grund`}
            required
            maxLength={200}
            hinweis="Zum Beispiel: Regalboden beschädigt. Der Platz wird gesperrt und ein neuer gesucht."
            fehler={felder.grund}
          />
          <button type="submit">
            Platz sperren und umleiten
          </button>
        </form>
      </div>
    </div>
  );
}

function PrioritaetFormular({ auftrag, nachAktion }: { auftrag: Transportauftrag; nachAktion: (id: number, m: Meldung) => void }) {
  const { ausfuehren } = useAktion();

  async function absenden(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const prioritaet = new FormData(e.currentTarget).get("prioritaet") as Prioritaet;
    const ok = await ausfuehren(() =>
      api(`/api/transportauftraege/${auftrag.id}/prioritaet`, { methode: "PUT", daten: { prioritaet } }),
    );
    if (ok) nachAktion(auftrag.id, { art: "erfolg", text: `Auftrag ${auftrag.id}: Priorität ${TEXTE.prioritaet[prioritaet]}.` });
  }

  return (
    <form onSubmit={absenden} className="unterformular inline">
      <AuswahlFeld label="Priorität" name="prioritaet" id={`prio-${auftrag.id}`} defaultValue={auftrag.prioritaet}>
        {(Object.keys(TEXTE.prioritaet) as Prioritaet[]).map((p) => (
          <option key={p} value={p}>
            {TEXTE.prioritaet[p]}
          </option>
        ))}
      </AuswahlFeld>
      <button type="submit" className="knopf-zweitrangig">
        Priorität ändern
      </button>
    </form>
  );
}
