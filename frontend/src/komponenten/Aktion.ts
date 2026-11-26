import { useCallback, useRef, useState } from "react";
import { ApiFehler } from "../api/client";
import { useRueckmeldung } from "./Rueckmeldung";

/**
 * Führt eine schreibende Anfrage aus: verhindert Doppelklicks, meldet Fehler
 * und gibt Feldfehler an das Formular weiter, statt sie als allgemeine Meldung zu zeigen.
 */
export function useAktion() {
  const { melde } = useRueckmeldung();
  const [laeuft, setLaeuft] = useState(false);
  const [felder, setFelder] = useState<Record<string, string>>({});
  const sperre = useRef(false);

  const ausfuehren = useCallback(
    async <T>(anfrage: () => Promise<T>): Promise<T | undefined> => {
      if (sperre.current) return undefined;
      sperre.current = true;
      setLaeuft(true);
      setFelder({});
      melde(null);
      try {
        return await anfrage();
      } catch (fehler) {
        if (fehler instanceof ApiFehler && Object.keys(fehler.felder).length > 0) {
          setFelder(fehler.felder);
        } else if (fehler instanceof ApiFehler) {
          melde({ art: "fehler", text: `${fehler.titel}. ${fehler.message}` });
        } else {
          melde({ art: "fehler", text: "Unerwarteter Fehler. Bitte Seite neu laden." });
        }
        return undefined;
      } finally {
        sperre.current = false;
        setLaeuft(false);
      }
    },
    [melde],
  );

  return { ausfuehren, laeuft, felder };
}
