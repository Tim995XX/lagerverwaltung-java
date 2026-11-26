import { screen, within } from "@testing-library/react";
import { ARTIKEL, erwarteAnsage, mockeApi, problem, zeige } from "./hilfen";

test("Wareneingang: Feldfehler vom Server erscheinen am Feld und in einer Übersicht mit Fokus", async () => {
  mockeApi({
    "GET /api/artikel": ARTIKEL,
    "POST /api/wareneingaenge": problem(400, "Ungültige Eingabe", "Bitte Felder prüfen", { menge: "muss größer als 0 sein" }),
  });
  const { nutzer } = zeige("/wareneingang");

  await nutzer.click(await screen.findByRole("button", { name: "Einlagern" }));

  const uebersicht = await screen.findByRole("heading", { name: "Bitte Eingaben prüfen" });
  expect(uebersicht.parentElement).toHaveFocus();
  const menge = screen.getByLabelText(/Menge in Stück/);
  expect(menge).toHaveAttribute("aria-invalid", "true");
  expect(menge).toHaveAccessibleDescription("Fehler: muss größer als 0 sein");

  // Link in der Übersicht springt zum Feld
  await nutzer.click(screen.getByRole("link", { name: "Menge: muss größer als 0 sein" }));
  expect(menge).toHaveFocus();
});

test("Wareneingang: reservierter Platz wird angesagt und angezeigt", async () => {
  const api = mockeApi({
    "GET /api/artikel": ARTIKEL,
    "POST /api/wareneingaenge": { ergebnis: "PLATZ_RESERVIERT", lagerplatz: "01-01-2", transportauftragId: 4, klaerfallId: null, begruendung: "kurzer Weg" },
  });
  const { nutzer } = zeige("/wareneingang");

  await nutzer.selectOptions(await screen.findByLabelText(/Artikel/), "1002");
  await nutzer.type(screen.getByLabelText(/Menge/), "20");
  await nutzer.click(screen.getByRole("button", { name: "Einlagern" }));

  await erwarteAnsage("status", "Platz 01-01-2 reserviert, Transportauftrag 4 angelegt.");
  expect(api.aenderungen()[0]!.daten).toEqual({ artikelNummer: "1002", menge: 20 });
  expect(within(screen.getByRole("region", { name: "Ergebnis" })).getByText("kurzer Weg")).toBeInTheDocument();
});

test("Kommissionierung: Fokus folgt beim Hinzufügen und Entfernen von Positionen", async () => {
  mockeApi({ "GET /api/artikel": ARTIKEL });
  const { nutzer } = zeige("/kommissionierung");

  await nutzer.click(await screen.findByRole("button", { name: "Position hinzufügen" }));
  const position2 = screen.getByRole("group", { name: "Position 2" });
  expect(within(position2).getByLabelText(/Artikel/)).toHaveFocus();

  await nutzer.click(screen.getByRole("button", { name: "Position 2 entfernen" }));
  expect(screen.queryByRole("group", { name: "Position 2" })).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Position hinzufügen" })).toHaveFocus();
});

test("Kommissionierung: sendet alle Positionen, Klärung wird als Hinweis angesagt", async () => {
  const api = mockeApi({
    "GET /api/artikel": ARTIKEL,
    "POST /api/kommissionierauftraege": {
      id: 9, status: "IN_KLAERUNG", prioritaet: "HOCH", erstelltAm: "2026-11-26T10:00:00",
      positionen: [{ artikelNummer: "1001", menge: 500 }], transportauftraege: [],
    },
  });
  const { nutzer } = zeige("/kommissionierung");

  await nutzer.selectOptions(await screen.findByLabelText(/Artikel/), "1001");
  await nutzer.type(screen.getByLabelText(/Menge/), "500");
  await nutzer.selectOptions(screen.getByLabelText("Priorität"), "HOCH");
  await nutzer.click(screen.getByRole("button", { name: "Auftrag anlegen" }));

  await erwarteAnsage("status", "Auftrag 9 ist in Klärung");
  expect(api.aenderungen()[0]!.daten).toEqual({ prioritaet: "HOCH", positionen: [{ artikelNummer: "1001", menge: 500 }] });
});

test("Klärplatz: Fall mit Kommentar erledigen", async () => {
  let erledigt = false;
  const fall = { id: 1, grund: "KEIN_PASSENDER_PLATZ", artikelNummer: "1001", menge: 999, beschreibung: "Zu groß", kommissionierauftragId: null, erstelltAm: "2026-11-26T09:00:00", erledigt: false, kommentar: null };
  const api = mockeApi({
    "GET /api/klaerfaelle": () => (erledigt ? [] : [fall]),
    "POST /api/klaerfaelle/1/erledigen": () => {
      erledigt = true;
      return { ...fall, erledigt: true };
    },
  });
  const { nutzer } = zeige("/klaerplatz");

  await nutzer.type(await screen.findByLabelText(/Was wurde getan/), "Auf zwei Plätze verteilt");
  await nutzer.click(screen.getByRole("button", { name: "Klärfall 1 erledigen" }));

  await erwarteAnsage("status", "Klärfall 1 erledigt.");
  expect(await screen.findByText("Keine offenen Klärfälle.")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "0 Klärfälle" })).toHaveFocus();
  expect(api.aenderungen()[0]!.daten).toEqual({ kommentar: "Auf zwei Plätze verteilt" });
});

test("Lager: Leitstand sperrt einen Platz, Fahrer sieht keine Aktion", async () => {
  const platz = { code: "01-01-1", gang: 1, regal: 1, ebene: 1, kapazitaet: 40, laengeMm: 1200, breiteMm: 800, hoeheMm: 700, maxGewichtKg: 500, gesperrt: false, sperrgrund: null };
  const api = mockeApi({
    "GET /api/lagerplaetze": [platz],
    "GET /api/bestaende": [{ lagerplatz: "01-01-1", artikelNummer: "1001", bezeichnung: "Mineralwasser", menge: 30, reserviert: 0, freieKapazitaet: 10 }],
    "PUT /api/lagerplaetze/01-01-1/sperre": { ...platz, gesperrt: true, sperrgrund: "defekt" },
  });
  const { nutzer, unmount } = zeige("/lager");

  const zeile = (await screen.findByRole("rowheader", { name: "01-01-1" })).closest("tr")!;
  expect(within(zeile).getByText("1001 – Mineralwasser")).toBeInTheDocument();
  await nutzer.click(within(zeile).getByRole("button", { name: "Sperren Platz 01-01-1" }));
  await nutzer.type(within(zeile).getByLabelText(/Grund/), "defekt");
  await nutzer.click(within(zeile).getByRole("button", { name: "Sperren" }));
  await erwarteAnsage("status", "Platz 01-01-1 gesperrt.");
  expect(api.aenderungen()[0]).toMatchObject({ methode: "PUT", daten: { grund: "defekt" } });

  unmount();
  zeige("/lager", "STAPLERFAHRER");
  await screen.findByRole("rowheader", { name: "01-01-1" });
  expect(screen.queryByRole("button", { name: /sperren/i })).not.toBeInTheDocument();
});
