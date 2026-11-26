/** Typen passend zu den Response-Records im Paket web.dto des Backends. */

export type Rolle = "LEITSTAND" | "STAPLERFAHRER";
export type TransportArt = "EINLAGERUNG" | "AUSLAGERUNG";
export type TransportStatus = "OFFEN" | "IN_ARBEIT" | "ERLEDIGT" | "ABGEBROCHEN";
export type Prioritaet = "HOCH" | "NORMAL" | "NIEDRIG";
export type KlaerGrund = "KEIN_PASSENDER_PLATZ" | "MASSE_FEHLEN" | "PLATZ_BLOCKIERT" | "BESTAND_NICHT_AUSREICHEND";
export type Umschlag = "SCHNELL" | "MITTEL" | "LANGSAM";
export type AuftragStatus = "FREIGEGEBEN" | "IN_KLAERUNG";

export interface Ich {
  name: string;
  rolle: Rolle;
}

export interface Transportauftrag {
  id: number;
  art: TransportArt;
  artikelNummer: string;
  menge: number;
  vonOrt: string | null;
  nachOrt: string | null;
  prioritaet: Prioritaet;
  status: TransportStatus;
  kommissionierauftragId: number | null;
  erstelltAm: string;
}

export interface Artikel {
  artikelNummer: string;
  bezeichnung: string;
  laengeMm: number | null;
  breiteMm: number | null;
  hoeheMm: number | null;
  gewichtGramm: number | null;
  umschlag: Umschlag | null;
}

export interface Lagerplatz {
  code: string;
  gang: number;
  regal: number;
  ebene: number;
  kapazitaet: number;
  laengeMm: number | null;
  breiteMm: number | null;
  hoeheMm: number | null;
  maxGewichtKg: number | null;
  gesperrt: boolean;
  sperrgrund: string | null;
}

export interface Bestand {
  lagerplatz: string;
  artikelNummer: string;
  bezeichnung: string;
  menge: number;
  reserviert: number;
  freieKapazitaet: number;
}

export interface Klaerfall {
  id: number;
  grund: KlaerGrund;
  artikelNummer: string;
  menge: number;
  beschreibung: string;
  kommissionierauftragId: number | null;
  erstelltAm: string;
  erledigt: boolean;
  kommentar: string | null;
}

export interface WareneingangErgebnis {
  ergebnis: "PLATZ_RESERVIERT" | "KLAERPLATZ";
  lagerplatz: string | null;
  transportauftragId: number | null;
  klaerfallId: number | null;
  begruendung: string | null;
}

export interface UmleitungErgebnis {
  ergebnis: "UMGELEITET" | "KLAERPLATZ";
  gesperrterPlatz: string;
  neuerPlatz: string | null;
  begruendung: string | null;
}

export interface Kommissionierauftrag {
  id: number;
  status: AuftragStatus;
  prioritaet: Prioritaet;
  erstelltAm: string;
  positionen: { artikelNummer: string; menge: number }[];
  transportauftraege: Transportauftrag[];
}

/** Lesbare Bezeichnungen statt der Enum-Namen aus der API. */
export const TEXTE = {
  rolle: { LEITSTAND: "Leitstand", STAPLERFAHRER: "Staplerfahrer" },
  art: { EINLAGERUNG: "Einlagerung", AUSLAGERUNG: "Auslagerung" },
  status: { OFFEN: "Offen", IN_ARBEIT: "In Arbeit", ERLEDIGT: "Erledigt", ABGEBROCHEN: "Abgebrochen" },
  prioritaet: { HOCH: "Hoch", NORMAL: "Normal", NIEDRIG: "Niedrig" },
  klaerGrund: {
    KEIN_PASSENDER_PLATZ: "Kein passender Platz",
    MASSE_FEHLEN: "Maße fehlen",
    PLATZ_BLOCKIERT: "Platz blockiert",
    BESTAND_NICHT_AUSREICHEND: "Bestand reicht nicht",
  },
  umschlag: { SCHNELL: "Schnelldreher", MITTEL: "Mittel", LANGSAM: "Langsamdreher" },
  auftragStatus: { FREIGEGEBEN: "Freigegeben", IN_KLAERUNG: "In Klärung" },
} as const;

const datumsFormat = new Intl.DateTimeFormat("de-DE", { dateStyle: "short", timeStyle: "short" });

export function formatiereZeit(iso: string): string {
  return datumsFormat.format(new Date(iso));
}
