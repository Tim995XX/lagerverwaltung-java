import { screen, within } from "@testing-library/react";
import { erwarteAnsage, mockeApi, problem, transport, zeige } from "./hilfen";

test("Liste zeigt nur offene und laufende Aufträge in der Reihenfolge des Servers", async () => {
  mockeApi({
    "GET /api/transportauftraege": [
      transport({ id: 7, prioritaet: "HOCH", art: "AUSLAGERUNG", vonOrt: "02-01-1", nachOrt: null }),
      transport({ id: 4 }),
      transport({ id: 2, status: "ERLEDIGT" }),
    ],
  });
  zeige("/transporte", "STAPLERFAHRER");

  const liste = await screen.findByRole("region", { name: "2 Aufträge" });
  const karten = within(liste).getAllByRole("article");
  expect(karten.map((k) => within(k).getByRole("heading").textContent)).toEqual(["Auftrag 7: Auslagerung", "Auftrag 4: Einlagerung"]);
  expect(screen.getByRole("heading", { level: 2 })).toHaveTextContent("2 Aufträge");
  // Fahrer darf keine Priorität ändern: das Feld fehlt
  expect(screen.queryByLabelText("Priorität")).not.toBeInTheDocument();
});

test("Starten: Erfolg wird angesagt, Fokus landet auf der Karte, das Scanformular erscheint", async () => {
  let gestartet = false;
  const api = mockeApi({
    "GET /api/transportauftraege": () => [transport({ status: gestartet ? "IN_ARBEIT" : "OFFEN" })],
    "POST /api/transportauftraege/4/start": () => {
      gestartet = true;
      return transport({ status: "IN_ARBEIT" });
    },
  });
  const { nutzer } = zeige("/transporte", "STAPLERFAHRER");

  await nutzer.click(await screen.findByRole("button", { name: "Auftrag 4 starten" }));

  await erwarteAnsage("status", "Auftrag 4 gestartet.");
  expect(await screen.findByRole("group", { name: "Einlagerung per Scan bestätigen" })).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Auftrag 4: Einlagerung" })).toHaveFocus();
  expect(api.aenderungen()).toHaveLength(1);
});

test("Scan: Enter nach dem ersten Barcode springt zum Platzfeld, dann wird gesendet", async () => {
  const api = mockeApi({
    "GET /api/transportauftraege": [transport({ status: "IN_ARBEIT" })],
    "POST /api/transportauftraege/4/scan": transport({ status: "ERLEDIGT" }),
  });
  const { nutzer } = zeige("/transporte", "STAPLERFAHRER");

  await nutzer.type(await screen.findByLabelText(/Barcode der Ware/), "1002{Enter}");
  expect(screen.getByLabelText(/Barcode des Lagerplatzes/)).toHaveFocus();
  expect(api.aenderungen()).toHaveLength(0);

  await nutzer.type(screen.getByLabelText(/Barcode des Lagerplatzes/), "01-01-2{Enter}");

  await erwarteAnsage("status", "Auftrag 4: Scan passt, Bestand gebucht.");
  expect(api.aenderungen()[0]).toMatchObject({ daten: { artikelNummer: "1002", lagerplatzCode: "01-01-2" } });
});

test("falscher Scan: Fehler als alert, nichts geht verloren", async () => {
  mockeApi({
    "GET /api/transportauftraege": [transport({ status: "IN_ARBEIT" })],
    "POST /api/transportauftraege/4/scan": problem(409, "Falscher Scan", "Erwartet Platz 01-01-2, gescannt 01-01-3"),
  });
  const { nutzer } = zeige("/transporte", "STAPLERFAHRER");

  await nutzer.type(await screen.findByLabelText(/Barcode der Ware/), "1002{Enter}");
  await nutzer.type(screen.getByLabelText(/Barcode des Lagerplatzes/), "01-01-3{Enter}");

  await erwarteAnsage("alert", "Falscher Scan. Erwartet Platz 01-01-2, gescannt 01-01-3");
  expect(screen.getByLabelText(/Barcode des Lagerplatzes/)).toHaveValue("01-01-3");
});

test("Platz blockiert: Bereich klappt auf und meldet den neuen Platz", async () => {
  mockeApi({
    "GET /api/transportauftraege": [transport({ status: "IN_ARBEIT" })],
    "POST /api/transportauftraege/4/umleiten": { ergebnis: "UMGELEITET", gesperrterPlatz: "01-01-2", neuerPlatz: "01-02-1", begruendung: "x" },
  });
  const { nutzer } = zeige("/transporte", "STAPLERFAHRER");

  const aufklapper = await screen.findByRole("button", { name: "Platz blockiert?" });
  expect(aufklapper).toHaveAttribute("aria-expanded", "false");
  // hidden: im DOM, aber für niemanden sichtbar oder erreichbar
  expect(screen.queryByRole("textbox", { name: /Was ist mit dem Platz/ })).not.toBeInTheDocument();

  await nutzer.click(aufklapper);
  expect(aufklapper).toHaveAttribute("aria-expanded", "true");
  await nutzer.type(screen.getByLabelText(/Was ist mit dem Platz/), "Regalboden beschädigt");
  await nutzer.click(screen.getByRole("button", { name: "Platz sperren und umleiten" }));

  await erwarteAnsage("status", "Platz 01-01-2 gesperrt. Neuer Platz: 01-02-1.");
});

test("Leitstand ändert die Priorität", async () => {
  const api = mockeApi({
    "GET /api/transportauftraege": [transport()],
    "PUT /api/transportauftraege/4/prioritaet": transport({ prioritaet: "HOCH" }),
  });
  const { nutzer } = zeige("/transporte", "LEITSTAND");

  await nutzer.selectOptions(await screen.findByLabelText("Priorität"), "HOCH");
  await nutzer.click(screen.getByRole("button", { name: "Priorität ändern" }));

  await erwarteAnsage("status", "Auftrag 4: Priorität Hoch.");
  expect(api.aenderungen()[0]).toMatchObject({ methode: "PUT", daten: { prioritaet: "HOCH" } });
});

test("Filter fragt den Server mit Status ab", async () => {
  const api = mockeApi({ "GET /api/transportauftraege": [] });
  const { nutzer } = zeige("/transporte", "LEITSTAND");

  await nutzer.selectOptions(await screen.findByLabelText("Anzeigen"), "ERLEDIGT");

  expect(await screen.findByText("Keine Aufträge für diese Auswahl.")).toBeInTheDocument();
  expect(api.anfragen.at(-1)!.pfad).toBe("/api/transportauftraege?status=ERLEDIGT");
});

test("Ladefehler mit Knopf zum erneuten Laden", async () => {
  let versuch = 0;
  mockeApi({
    "GET /api/transportauftraege": () => (++versuch === 1 ? problem(500, "Fehler", "Datenbank nicht erreichbar") : []),
  });
  const { nutzer } = zeige("/transporte", "LEITSTAND");

  await erwarteAnsage("alert", "Daten konnten nicht geladen werden");
  await nutzer.click(screen.getByRole("button", { name: "Erneut laden" }));
  expect(await screen.findByText("Keine Aufträge für diese Auswahl.")).toBeInTheDocument();
});
