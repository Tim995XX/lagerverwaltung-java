import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { useLocation } from "react-router";

export interface Meldung {
  art: "erfolg" | "hinweis" | "fehler";
  text: string;
}

interface RueckmeldungWert {
  meldung: Meldung | null;
  melde: (meldung: Meldung | null) => void;
}

const RueckmeldungKontext = createContext<RueckmeldungWert | null>(null);

/** Eine Meldung für die ganze Seite; beim Seitenwechsel verschwindet sie. */
export function RueckmeldungProvider({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const [eintrag, setEintrag] = useState<{ pfad: string; meldung: Meldung } | null>(null);
  const melde = useCallback((meldung: Meldung | null) => setEintrag(meldung && { pfad: pathname, meldung }), [pathname]);
  const meldung = eintrag && eintrag.pfad === pathname ? eintrag.meldung : null;
  const wert = useMemo(() => ({ meldung, melde }), [meldung, melde]);
  return <RueckmeldungKontext.Provider value={wert}>{children}</RueckmeldungKontext.Provider>;
}

export function useRueckmeldung(): RueckmeldungWert {
  const wert = useContext(RueckmeldungKontext);
  if (!wert) throw new Error("useRueckmeldung nur innerhalb von RueckmeldungProvider verwenden");
  return wert;
}

/**
 * Zwei Live-Regionen, die immer im DOM stehen: Screenreader lesen nur Änderungen vor,
 * eine erst mit dem Text eingefügte Region würde oft überhört.
 * Fehler kommen in die "alert"-Region und unterbrechen die laufende Ansage, alles andere wartet höflich.
 */
export function RueckmeldungAnzeige() {
  const { meldung, melde } = useRueckmeldung();
  const istFehler = meldung?.art === "fehler";
  const inhalt = (art: Meldung["art"]) =>
    meldung && meldung.art === art ? (
      <div className={`meldung meldung-${art}`}>
        <p>
          <span className="meldung-art">{{ erfolg: "Erledigt:", hinweis: "Hinweis:", fehler: "Fehler:" }[art]}</span>{" "}
          {meldung.text}
        </p>
        <button type="button" className="knopf-schlicht" onClick={() => melde(null)}>
          Meldung schließen
        </button>
      </div>
    ) : null;
  return (
    <>
      <div role="status">{!istFehler && (inhalt("erfolg") ?? inhalt("hinweis"))}</div>
      <div role="alert">{istFehler && inhalt("fehler")}</div>
    </>
  );
}
