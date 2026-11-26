/**
 * Dünne Schicht über fetch: hängt die Anmeldung an, liest Fehler im Format
 * "Problem Details" (RFC 9457) und wirft sie als ApiFehler.
 *
 * Die Zugangsdaten liegen nur in dieser Variablen, nicht im localStorage und nicht in einem Cookie.
 * Nach einem Neuladen der Seite muss man sich deshalb neu anmelden.
 */

export class ApiFehler extends Error {
  readonly status: number;
  readonly titel: string;
  /** Feldfehler aus der Bean Validation, z. B. { "menge": "muss größer als 0 sein" } */
  readonly felder: Record<string, string>;

  constructor(status: number, titel: string, detail: string, felder: Record<string, string> = {}) {
    super(detail);
    this.status = status;
    this.titel = titel;
    this.felder = felder;
  }
}

let zugang: string | null = null;
let beiAbgelaufenerAnmeldung: (() => void) | null = null;

export function setzeZugang(name: string, passwort: string): void {
  // UTF-8 statt btoa direkt, damit auch Umlaute im Passwort funktionieren
  const bytes = new TextEncoder().encode(`${name}:${passwort}`);
  zugang = "Basic " + btoa(String.fromCharCode(...bytes));
}

export function loescheZugang(): void {
  zugang = null;
}

/** Wird aufgerufen, wenn der Server eine bestehende Anmeldung nicht mehr annimmt (z. B. Benutzer deaktiviert). */
export function beiAbmeldungDurchServer(aktion: (() => void) | null): void {
  beiAbgelaufenerAnmeldung = aktion;
}

interface Optionen {
  methode?: "GET" | "POST" | "PUT" | "DELETE";
  daten?: unknown;
}

export async function api<T>(pfad: string, { methode = "GET", daten }: Optionen = {}): Promise<T> {
  const kopf: Record<string, string> = {
    Accept: "application/json",
    // sonst öffnet der Browser bei 401 seinen eigenen Anmeldedialog
    "X-Requested-With": "XMLHttpRequest",
  };
  if (zugang) kopf.Authorization = zugang;
  if (daten !== undefined) kopf["Content-Type"] = "application/json";

  let antwort: Response;
  try {
    antwort = await fetch(pfad, {
      method: methode,
      headers: kopf,
      body: daten === undefined ? undefined : JSON.stringify(daten),
    });
  } catch {
    throw new ApiFehler(0, "Keine Verbindung", "Der Server ist nicht erreichbar. Bitte Netzwerk prüfen und erneut versuchen.");
  }

  if (antwort.ok) {
    const text = await antwort.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }

  const fehler = await alsApiFehler(antwort);
  if (fehler.status === 401 && zugang) {
    beiAbgelaufenerAnmeldung?.();
  }
  throw fehler;
}

async function alsApiFehler(antwort: Response): Promise<ApiFehler> {
  try {
    const problem = (await antwort.json()) as { title?: string; detail?: string; felder?: Record<string, string> };
    return new ApiFehler(
      antwort.status,
      problem.title ?? "Fehler",
      problem.detail ?? `Der Server meldet Fehler ${antwort.status}.`,
      problem.felder ?? {},
    );
  } catch {
    return new ApiFehler(antwort.status, "Fehler", `Der Server meldet Fehler ${antwort.status}.`);
  }
}
