import { screen } from "@testing-library/react";
import type { Rolle } from "../api/typen";
import { ARTIKEL, mockeApi, pruefeBarrierefreiheit, transport, zeige } from "./hilfen";

const DATEN = {
  "GET /api/transportauftraege": [
    transport({ id: 4, status: "IN_ARBEIT" }),
    transport({ id: 5, art: "AUSLAGERUNG", status: "IN_ARBEIT", vonOrt: "02-01-1", nachOrt: null }),
    transport({ id: 6, prioritaet: "HOCH" }),
  ],
  "GET /api/artikel": ARTIKEL,
  "GET /api/klaerfaelle": [
    { id: 1, grund: "PLATZ_BLOCKIERT", artikelNummer: "1001", menge: 5, beschreibung: "Platz 01-01-1 blockiert", kommissionierauftragId: null, erstelltAm: "2026-11-26T09:00:00", erledigt: false, kommentar: null },
  ],
  "GET /api/lagerplaetze": [
    { code: "01-01-1", gang: 1, regal: 1, ebene: 1, kapazitaet: 40, laengeMm: 1, breiteMm: 1, hoeheMm: 1, maxGewichtKg: 1, gesperrt: true, sperrgrund: "defekt" },
    { code: "01-01-2", gang: 1, regal: 1, ebene: 2, kapazitaet: 40, laengeMm: 1, breiteMm: 1, hoeheMm: 1, maxGewichtKg: 1, gesperrt: false, sperrgrund: null },
  ],
  "GET /api/bestaende": [{ lagerplatz: "01-01-2", artikelNummer: "1001", bezeichnung: "Mineralwasser", menge: 30, reserviert: 0, freieKapazitaet: 10 }],
};

const SEITEN: [string, Rolle | null, string][] = [
  ["/anmelden", null, "Anmelden"],
  ["/transporte", "STAPLERFAHRER", "Transportaufträge"],
  ["/transporte", "LEITSTAND", "Transportaufträge"],
  ["/wareneingang", "LEITSTAND", "Wareneingang"],
  ["/kommissionierung", "LEITSTAND", "Kommissionierung"],
  ["/klaerplatz", "LEITSTAND", "Klärplatz"],
  ["/lager", "LEITSTAND", "Lager"],
  ["/lager", "STAPLERFAHRER", "Lager"],
  ["/artikel", "LEITSTAND", "Artikel"],
  ["/gibt-es-nicht", "LEITSTAND", "Seite nicht gefunden"],
];

test.each(SEITEN)("%s als %s: keine axe-Verstöße, genau eine h1, sprechender Fenstertitel", async (pfad, rolle, titel) => {
  mockeApi(DATEN);
  const { container } = zeige(pfad, rolle);

  await screen.findByRole("heading", { level: 1, name: titel });
  // warten, bis die Daten da sind (Ladehinweis verschwunden)
  await screen.findAllByRole("heading");
  await new Promise((r) => setTimeout(r, 0));

  await pruefeBarrierefreiheit(container);
  expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
  expect(document.title).toBe(`${titel} – Lagerverwaltung`);
  expect(screen.getByRole("main")).toBeInTheDocument();
});

test("Seitenwechsel setzt den Fokus auf die neue Überschrift", async () => {
  mockeApi(DATEN);
  const { nutzer } = zeige("/transporte", "LEITSTAND");

  await nutzer.click(screen.getByRole("link", { name: "Klärplatz" }));

  expect(await screen.findByRole("heading", { level: 1, name: "Klärplatz" })).toHaveFocus();
});

test("Sprunglink führt zum Hauptinhalt", async () => {
  mockeApi(DATEN);
  const { nutzer } = zeige("/transporte", "LEITSTAND");

  await nutzer.tab();
  expect(screen.getByRole("link", { name: "Zum Inhalt springen" })).toHaveFocus();
  await nutzer.keyboard("{Enter}");
  expect(screen.getByRole("main")).toHaveFocus();
});
