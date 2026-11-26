import type { ApiFehler } from "../api/client";

/** Laden und Ladefehler einheitlich anzeigen; beim Fehler mit Knopf zum erneuten Versuch. */
export function Ladezustand({ laedt, fehler, neuLaden, hatDaten }: { laedt: boolean; fehler?: ApiFehler; neuLaden: () => void; hatDaten: boolean }) {
  if (fehler) {
    return (
      <div className="meldung meldung-fehler" role="alert">
        <p>
          <span className="meldung-art">Fehler:</span> Daten konnten nicht geladen werden. {fehler.message}
        </p>
        <button type="button" onClick={neuLaden}>
          Erneut laden
        </button>
      </div>
    );
  }
  return (
    <p role="status" className="lade-hinweis">
      {laedt && !hatDaten ? "Wird geladen …" : ""}
    </p>
  );
}
