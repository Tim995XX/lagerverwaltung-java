import { screen, within } from "@testing-library/react";
import { erwarteAnsage, mockeApi, problem, zeige } from "./hilfen";

test("falsches Passwort: Fehlermeldung, Anmeldung bleibt offen", async () => {
  mockeApi({ "GET /api/ich": problem(401, "Nicht angemeldet", "Bitte anmelden") });
  const { nutzer } = zeige("/anmelden", null);

  await nutzer.type(screen.getByLabelText(/Benutzername/), "fahrer");
  await nutzer.type(screen.getByLabelText(/Passwort/), "falsch");
  await nutzer.click(screen.getByRole("button", { name: "Anmelden" }));

  await erwarteAnsage("alert", "Benutzername oder Passwort ist falsch.");
  expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Anmelden");
});

test("Fahrer meldet sich an und sieht nur seine Menüpunkte", async () => {
  mockeApi({ "GET /api/ich": { name: "fahrer", rolle: "STAPLERFAHRER" }, "GET /api/transportauftraege": [] });
  const { nutzer } = zeige("/anmelden", null);

  await nutzer.type(screen.getByLabelText(/Benutzername/), "fahrer");
  await nutzer.type(screen.getByLabelText(/Passwort/), "fahrer{Enter}");

  expect(await screen.findByRole("heading", { level: 1, name: "Transportaufträge" })).toBeInTheDocument();
  const menue = screen.getByRole("navigation", { name: "Hauptmenü" });
  expect(within(menue).getAllByRole("link").map((l) => l.textContent)).toEqual(["Transporte", "Lager"]);
  expect(within(menue).getByRole("link", { name: "Transporte" })).toHaveAttribute("aria-current", "page");
  expect(screen.getByText(/Angemeldet als/)).toHaveTextContent("Angemeldet als fahrer (Staplerfahrer)");
});

test("ohne Anmeldung geht es zur Anmeldung und danach zurück zur gewünschten Seite", async () => {
  mockeApi({ "GET /api/ich": { name: "leitstand", rolle: "LEITSTAND" }, "GET /api/klaerfaelle": [] });
  const { nutzer } = zeige("/klaerplatz", null);

  expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Anmelden");
  await nutzer.type(screen.getByLabelText(/Benutzername/), "leitstand");
  await nutzer.type(screen.getByLabelText(/Passwort/), "leitstand{Enter}");

  expect(await screen.findByRole("heading", { level: 1, name: "Klärplatz" })).toBeInTheDocument();
});

test("nach Abmeldung des Leitstands landet ein Fahrer nicht auf einer Leitstand-Seite", async () => {
  mockeApi({ "GET /api/ich": { name: "fahrer", rolle: "STAPLERFAHRER" }, "GET /api/klaerfaelle": [], "GET /api/transportauftraege": [] });
  const { nutzer } = zeige("/klaerplatz", "LEITSTAND");

  await nutzer.click(screen.getByRole("button", { name: "Abmelden" }));
  await nutzer.type(screen.getByLabelText(/Benutzername/), "fahrer");
  await nutzer.type(screen.getByLabelText(/Passwort/), "fahrer{Enter}");

  expect(await screen.findByRole("heading", { level: 1, name: "Transportaufträge" })).toBeInTheDocument();
});

test("Fahrer auf einer Leitstand-Seite sieht einen Hinweis statt des Formulars", () => {
  mockeApi({});
  zeige("/wareneingang", "STAPLERFAHRER");
  expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Keine Berechtigung");
});

test("lehnt der Server die Anmeldung später ab, geht es zurück zur Anmeldung", async () => {
  mockeApi({ "GET /api/ich": { name: "fahrer", rolle: "STAPLERFAHRER" }, "GET /api/transportauftraege": problem(401, "Nicht angemeldet", "x") });
  const { nutzer } = zeige("/anmelden", null);

  await nutzer.type(screen.getByLabelText(/Benutzername/), "fahrer");
  await nutzer.type(screen.getByLabelText(/Passwort/), "fahrer{Enter}");

  expect(await screen.findByText(/Anmeldung ist nicht mehr gültig/)).toBeInTheDocument();
  expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Anmelden");
});

test("Abmelden", async () => {
  mockeApi({ "GET /api/transportauftraege": [] });
  const { nutzer } = zeige("/transporte", "LEITSTAND");

  await nutzer.click(screen.getByRole("button", { name: "Abmelden" }));

  expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Anmelden");
  expect(screen.getByText("Sie wurden abgemeldet.")).toBeInTheDocument();
  expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
});
