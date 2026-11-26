import { useEffect, useRef, type ComponentProps, type ReactNode } from "react";

/** Aus dem Feldnamen der API ("masse.laengeMm", "positionen[0].menge") eine gültige HTML-ID machen. */
export function feldId(name: string): string {
  return "feld-" + name.replace(/[^A-Za-z0-9]+/g, "-");
}

interface FeldRahmen {
  label: string;
  name: string;
  id?: string;
  fehler?: string;
  hinweis?: ReactNode;
}

/**
 * Doppelklicks verhindert useAktion; Knöpfe werden bewusst nicht "disabled",
 * weil ein deaktivierter Knopf den Tastaturfokus verliert.
 *
 * Beschriftung, Hinweis und Fehler hängen über aria-describedby am Eingabefeld,
 * damit ein Screenreader sie zusammen mit dem Feld vorliest.
 */
function useFeldAttribute({ name, id, fehler, hinweis }: FeldRahmen) {
  const feld = id ?? feldId(name);
  const beschreibung = [hinweis ? `${feld}-hinweis` : null, fehler ? `${feld}-fehler` : null].filter(Boolean).join(" ");
  return {
    feld,
    attribute: {
      id: feld,
      name,
      "aria-invalid": fehler ? true : undefined,
      "aria-describedby": beschreibung || undefined,
    },
  };
}

function Zusatz({ feld, fehler, hinweis }: { feld: string; fehler?: string; hinweis?: ReactNode }) {
  return (
    <>
      {hinweis && (
        <p id={`${feld}-hinweis`} className="feld-hinweis">
          {hinweis}
        </p>
      )}
      {fehler && (
        <p id={`${feld}-fehler`} className="feld-fehler">
          <span className="sichtbar-nur-sr">Fehler:</span> {fehler}
        </p>
      )}
    </>
  );
}

export function TextFeld({
  label,
  fehler,
  hinweis,
  ...eingabe
}: FeldRahmen & Omit<ComponentProps<"input">, "id" | "name">) {
  const { feld, attribute } = useFeldAttribute({ label, fehler, hinweis, ...eingabe });
  return (
    <div className="feld">
      <label htmlFor={feld}>
        {label}
        {eingabe.required && <span className="pflicht"> (Pflichtfeld)</span>}
      </label>
      <Zusatz feld={feld} fehler={fehler} hinweis={hinweis} />
      <input {...eingabe} {...attribute} />
    </div>
  );
}

export function AuswahlFeld({
  label,
  fehler,
  hinweis,
  children,
  ...auswahl
}: FeldRahmen & Omit<ComponentProps<"select">, "id" | "name">) {
  const { feld, attribute } = useFeldAttribute({ label, fehler, hinweis, ...auswahl });
  return (
    <div className="feld">
      <label htmlFor={feld}>
        {label}
        {auswahl.required && <span className="pflicht"> (Pflichtfeld)</span>}
      </label>
      <Zusatz feld={feld} fehler={fehler} hinweis={hinweis} />
      <select {...auswahl} {...attribute}>
        {children}
      </select>
    </div>
  );
}

/**
 * Fehlerübersicht über dem Formular: bekommt den Fokus, nennt alle Fehler
 * und springt per Link zum jeweiligen Feld (Muster aus dem GOV.UK Design System).
 */
export function FehlerUebersicht({ felder, beschriftungen }: { felder: Record<string, string>; beschriftungen: Record<string, string> }) {
  const kasten = useRef<HTMLDivElement>(null);
  const eintraege = Object.entries(felder);

  useEffect(() => {
    if (eintraege.length > 0) kasten.current?.focus();
  }, [eintraege.length, felder]);

  if (eintraege.length === 0) return null;
  return (
    <div className="fehler-uebersicht" tabIndex={-1} ref={kasten} aria-labelledby="fehler-uebersicht-titel">
      <h2 id="fehler-uebersicht-titel">Bitte Eingaben prüfen</h2>
      <ul>
        {eintraege.map(([name, text]) => (
          <li key={name}>
            <a
              href={`#${feldId(name)}`}
              onClick={(e) => {
                e.preventDefault();
                document.getElementById(feldId(name))?.focus();
              }}
            >
              {beschriftungen[name] ?? name}: {text}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
