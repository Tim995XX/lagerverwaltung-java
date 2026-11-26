import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axe from "axe-core";
import { MemoryRouter } from "react-router";
import { vi } from "vitest";
import { App } from "../App";
import type { Rolle, Transportauftrag } from "../api/typen";

/** Eine aufgezeichnete Anfrage an die (simulierte) API. */
export interface Anfrage {
  methode: string;
  pfad: string;
  daten: unknown;
  kopf: Record<string, string>;
}

const FEHLER = Symbol("fehler");
type Fehlerantwort = { [FEHLER]: true; status: number; title: string; detail: string; felder?: Record<string, string> };
type Antwort = unknown;

export function problem(status: number, title: string, detail: string, felder?: Record<string, string>): Fehlerantwort {
  return { [FEHLER]: true, status, title, detail, felder };
}

/**
 * Ersetzt fetch durch eine Tabelle "METHODE /pfad" → Antwort.
 * Eine Funktion als Antwort bekommt die Anfrage und kann z. B. beim zweiten Aufruf etwas anderes liefern.
 */
export function mockeApi(routen: Record<string, Antwort | ((anfrage: Anfrage) => Antwort)>) {
  const anfragen: Anfrage[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (pfad: string, init: RequestInit = {}) => {
      const anfrage: Anfrage = {
        methode: init.method ?? "GET",
        pfad,
        daten: init.body ? JSON.parse(String(init.body)) : undefined,
        kopf: (init.headers ?? {}) as Record<string, string>,
      };
      anfragen.push(anfrage);
      const eintrag = routen[`${anfrage.methode} ${pfad}`] ?? routen[`${anfrage.methode} ${pfad.split("?")[0]}`];
      if (eintrag === undefined) {
        return new Response(JSON.stringify({ title: "Nicht gefunden", detail: `Kein Mock für ${anfrage.methode} ${pfad}` }), { status: 404 });
      }
      const antwort = typeof eintrag === "function" ? eintrag(anfrage) : eintrag;
      if (antwort && typeof antwort === "object" && FEHLER in antwort) {
        const { status, title, detail, felder } = antwort as Fehlerantwort;
        return new Response(JSON.stringify({ status, title, detail, felder }), {
          status,
          headers: { "Content-Type": "application/problem+json" },
        });
      }
      return new Response(antwort === undefined ? "" : JSON.stringify(antwort), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }),
  );
  return {
    anfragen,
    /** Schreibende Anfragen, z. B. um zu prüfen, dass etwas gesendet wurde */
    aenderungen: () => anfragen.filter((a) => a.methode !== "GET"),
  };
}

/** App unter einer Adresse starten, standardmäßig schon angemeldet. */
export function zeige(pfad: string, rolle: Rolle | null = "LEITSTAND") {
  const benutzer = rolle && { name: rolle === "LEITSTAND" ? "leitstand" : "fahrer", rolle };
  return {
    nutzer: userEvent.setup(),
    ...render(
      <MemoryRouter initialEntries={[pfad]}>
        <App startBenutzer={benutzer} />
      </MemoryRouter>,
    ),
  };
}

/**
 * Prüft die Seite mit axe-core (dieselbe Prüfung wie in den Browser-Werkzeugen).
 * Farbkontraste kann jsdom nicht berechnen, die sind im Stylesheet von Hand geprüft.
 */
export async function pruefeBarrierefreiheit(bereich: HTMLElement) {
  const ergebnis = await axe.run(bereich, { rules: { "color-contrast": { enabled: false } } });
  const verstoesse = ergebnis.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(" ")).join(", ")})`);
  if (verstoesse.length > 0) throw new Error("Barrierefreiheit verletzt:\n" + verstoesse.join("\n"));
}

export function transport(teil: Partial<Transportauftrag> = {}): Transportauftrag {
  return {
    id: 4,
    art: "EINLAGERUNG",
    artikelNummer: "1002",
    menge: 20,
    vonOrt: null,
    nachOrt: "01-01-2",
    prioritaet: "NORMAL",
    status: "OFFEN",
    kommissionierauftragId: null,
    erstelltAm: "2026-11-26T10:00:00",
    ...teil,
  };
}

export const ARTIKEL = [
  { artikelNummer: "1001", bezeichnung: "Mineralwasser", laengeMm: 270, breiteMm: 180, hoeheMm: 330, gewichtGramm: 9300, umschlag: "SCHNELL" },
  { artikelNummer: "1002", bezeichnung: "Apfelsaft", laengeMm: null, breiteMm: null, hoeheMm: null, gewichtGramm: null, umschlag: null },
];

/**
 * Wartet, bis eine Live-Region den Text enthält. "alert" für Fehler, "status" für Erfolg und Hinweise.
 * So prüft der Test, dass ein Screenreader die Meldung auch vorlesen würde, nicht nur, dass sie sichtbar ist.
 */
export async function erwarteAnsage(rolle: "alert" | "status", text: string) {
  await waitFor(() => {
    const regionen = screen.getAllByRole(rolle).map((r) => r.textContent ?? "");
    expect(regionen.some((t) => t.includes(text)), `"${text}" in ${rolle}: ${JSON.stringify(regionen)}`).toBe(true);
  });
}
