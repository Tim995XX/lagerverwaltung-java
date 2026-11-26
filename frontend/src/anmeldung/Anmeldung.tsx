import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, beiAbmeldungDurchServer, loescheZugang, setzeZugang } from "../api/client";
import type { Ich } from "../api/typen";

interface AnmeldungWert {
  benutzer: Ich | null;
  /** Grund der letzten Abmeldung, wird auf der Anmeldeseite angezeigt */
  hinweis: string | null;
  anmelden: (name: string, passwort: string) => Promise<Ich>;
  abmelden: (hinweis?: string) => void;
}

const AnmeldungKontext = createContext<AnmeldungWert | null>(null);

/** startBenutzer ist nur für Tests gedacht, damit nicht jeder Test das Anmeldeformular ausfüllen muss. */
export function AnmeldungProvider({ children, startBenutzer = null }: { children: ReactNode; startBenutzer?: Ich | null }) {
  const [benutzer, setBenutzer] = useState<Ich | null>(startBenutzer);
  const [hinweis, setHinweis] = useState<string | null>(null);

  const anmelden = useCallback(async (name: string, passwort: string) => {
    setzeZugang(name, passwort);
    try {
      const ich = await api<Ich>("/api/ich");
      setHinweis(null);
      setBenutzer(ich);
      return ich;
    } catch (fehler) {
      loescheZugang();
      throw fehler;
    }
  }, []);

  // Kein navigate() hier: Ohne Benutzer leitet NurAngemeldet selbst zur Anmeldung um.
  const abmelden = useCallback((grund = "Sie wurden abgemeldet.") => {
    loescheZugang();
    setHinweis(grund);
    setBenutzer(null);
  }, []);

  useEffect(() => {
    beiAbmeldungDurchServer(() => abmelden("Die Anmeldung ist nicht mehr gültig. Bitte erneut anmelden."));
    return () => beiAbmeldungDurchServer(null);
  }, [abmelden]);

  const wert = useMemo(() => ({ benutzer, hinweis, anmelden, abmelden }), [benutzer, hinweis, anmelden, abmelden]);
  return <AnmeldungKontext.Provider value={wert}>{children}</AnmeldungKontext.Provider>;
}

export function useAnmeldung(): AnmeldungWert {
  const wert = useContext(AnmeldungKontext);
  if (!wert) throw new Error("useAnmeldung nur innerhalb von AnmeldungProvider verwenden");
  return wert;
}
