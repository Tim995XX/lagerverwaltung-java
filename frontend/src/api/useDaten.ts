import { useCallback, useEffect, useState } from "react";
import { api, ApiFehler } from "./client";

interface Ergebnis<T> {
  schluessel: string;
  daten?: T;
  fehler?: ApiFehler;
}

/**
 * Lädt Daten per GET und lädt neu, wenn sich der Pfad ändert oder neuLaden() aufgerufen wird.
 * Während des Neuladens bleiben die alten Daten sichtbar, damit die Liste nicht flackert.
 */
export function useDaten<T>(pfad: string) {
  const [version, setVersion] = useState(0);
  const [ergebnis, setErgebnis] = useState<Ergebnis<T>>({ schluessel: "" });
  const schluessel = `${pfad}#${version}`;

  useEffect(() => {
    let aktuell = true;
    api<T>(pfad).then(
      (daten) => aktuell && setErgebnis({ schluessel, daten }),
      (fehler: ApiFehler) => aktuell && setErgebnis((alt) => ({ schluessel, daten: alt.daten, fehler })),
    );
    // Antwort einer veralteten Anfrage ignorieren (z. B. Filter schnell gewechselt)
    return () => {
      aktuell = false;
    };
  }, [pfad, schluessel]);

  const neuLaden = useCallback(() => setVersion((v) => v + 1), []);

  return {
    daten: ergebnis.daten,
    fehler: ergebnis.schluessel === schluessel ? ergebnis.fehler : undefined,
    laedt: ergebnis.schluessel !== schluessel,
    neuLaden,
  };
}
