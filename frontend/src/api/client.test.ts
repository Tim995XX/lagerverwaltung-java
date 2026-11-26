import { api, ApiFehler, loescheZugang, setzeZugang } from "./client";
import { mockeApi, problem } from "../test/hilfen";

afterEach(() => loescheZugang());

test("sendet Anmeldung als UTF-8 und markiert die Anfrage als aus der Oberfläche", async () => {
  const { anfragen } = mockeApi({ "GET /api/ich": { name: "jörg", rolle: "LEITSTAND" } });
  setzeZugang("jörg", "pässwort");

  await api("/api/ich");

  // "jörg:pässwort" in UTF-8, Base64-kodiert
  expect(anfragen[0]!.kopf.Authorization).toBe("Basic asO2cmc6cMOkc3N3b3J0");
  expect(anfragen[0]!.kopf["X-Requested-With"]).toBe("XMLHttpRequest");
});

test("ohne Anmeldung kein Authorization-Header", async () => {
  const { anfragen } = mockeApi({ "GET /api/artikel": [] });
  await api("/api/artikel");
  expect(anfragen[0]!.kopf.Authorization).toBeUndefined();
});

test("Problem Details werden zu ApiFehler mit Feldfehlern", async () => {
  mockeApi({ "POST /api/wareneingaenge": problem(400, "Ungültige Eingabe", "Bitte Felder prüfen", { menge: "muss größer als 0 sein" }) });

  const fehler = await api("/api/wareneingaenge", { methode: "POST", daten: { menge: 0 } }).catch((e: unknown) => e);

  expect(fehler).toBeInstanceOf(ApiFehler);
  expect(fehler).toMatchObject({ status: 400, titel: "Ungültige Eingabe", felder: { menge: "muss größer als 0 sein" } });
});

test("ohne Netz eine verständliche Meldung", async () => {
  vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
  await expect(api("/api/artikel")).rejects.toMatchObject({ status: 0, titel: "Keine Verbindung" });
});
