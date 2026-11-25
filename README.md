# lagerverwaltung-java

[![CI](https://github.com/Tim995XX/lagerverwaltung-java/actions/workflows/ci.yml/badge.svg)](https://github.com/Tim995XX/lagerverwaltung-java/actions/workflows/ci.yml)

Eine kleine Lagerverwaltung als REST-API mit **Java 21** und **Spring Boot**: Artikel und
Lagerplätze anlegen, Ware einlagern, Kommissionieraufträge auslagern, Transportaufträge nach
Priorität abarbeiten – und alles, was das System nicht automatisch buchen kann, landet auf dem
**Klärplatz**, wo ein Mensch es klärt.

## Motivation

Vor meiner Umschulung habe ich vier Jahre im Leitstand eines Lagers gearbeitet, mit einem
Lagerverwaltungssystem und SAP. Ein großer Teil der Arbeit waren die Fälle, in denen die
Automatik nicht weiterkam: Klärplatzmeldungen, Buchungsfehler, Transportaufträge, die
umpriorisiert werden mussten, weil ein Lkw wartet.

Dieses Projekt ist eine eigene, stark vereinfachte Nachbildung dieser Konzepte. Es enthält
keinen Code und keine Daten eines echten Systems. Mir ging es darum, die Abläufe, die ich als
Anwender kenne, einmal selbst als Software umzusetzen – und dabei Java und Spring Boot sauber
zu lernen.

## Fachlicher Ablauf

```
Wareneingang ──▶ passender Platz?  ── ja ──▶ Bestand buchen + Transportauftrag EINLAGERUNG
                       │
                       └── nein ──▶ Klärfall KEIN_PASSENDER_PLATZ

Kommissionierauftrag ──▶ Bestand reicht für alle Positionen?
                       │
                       ├── ja ──▶ ausbuchen (kleinste Bestände zuerst) + Transportaufträge AUSLAGERUNG
                       └── nein ──▶ nichts buchen, Auftrag IN_KLAERUNG + Klärfall BESTAND_NICHT_AUSREICHEND

Transportauftrag:  OFFEN ──start──▶ IN_ARBEIT ──abschluss──▶ ERLEDIGT
                   Reihenfolge: Priorität (HOCH, NORMAL, NIEDRIG), dann ältester zuerst
```

## Features

- **Stammdaten:** Artikel (eindeutige Artikelnummer) und Lagerplätze mit Adresse
  Gang-Regal-Ebene (z. B. `01-03-2`) und Kapazität in Stück.
- **Wareneingang:** sucht zuerst einen Platz, auf dem der Artikel schon liegt und noch genug
  frei ist (Zulagerung), sonst den **kleinsten leeren Platz, auf den die Menge passt**
  (Best Fit, damit große Plätze für große Mengen frei bleiben).
- **Kommissionierung:** mehrere Positionen pro Auftrag, Mengen desselben Artikels werden
  zusammengezählt. Gebucht wird nur, wenn **alle** Positionen bedient werden können.
  Ausgelagert wird von den kleinsten Beständen zuerst, damit angebrochene Plätze leer werden.
- **Transportaufträge:** Liste in Abarbeitungsreihenfolge, Statuswechsel mit Prüfung,
  Umpriorisieren durch den Leitstand.
- **Klärplatz:** offene Klärfälle mit Grund und Beschreibung, Abschluss mit Kommentar.
- **Fehlerbehandlung:** zentral per `@RestControllerAdvice`, Antworten im Format
  „Problem Details“ (RFC 9457) mit passenden HTTP-Codes (400, 404, 409).
- **Bean Validation** auf allen Eingaben, **OpenAPI/Swagger-UI**, Health-Endpunkt.
- **Demodaten:** Beim ersten Start werden 3 Artikel, 12 Plätze und 3 Wareneingänge angelegt.

## Architektur

Klassische Schichten: Controller → Service → Repository → Datenbank.

| Paket | Aufgabe |
|---|---|
| `web` | REST-Controller, `FehlerBehandlung` (Exceptions → HTTP) |
| `web.dto` | Request-/Response-Records mit Validierungs-Annotationen |
| `service` | Geschäftslogik und Transaktionen (`@Transactional`) |
| `repository` | Spring-Data-JPA-Interfaces, eine eigene JPQL-Abfrage für leere Plätze |
| `domain` | JPA-Entities und Enums; Regeln, die nur ein Objekt betreffen (z. B. Statuswechsel), stehen direkt in der Entity |
| `fehler` | Fachliche Exceptions (`NichtGefundenException`, `KonfliktException`) |

Datenbank ist eine **Datei-H2** unter `./data/`, das Schema erzeugt Hibernate aus den Entities.

### Entscheidungen und bewusste Vereinfachungen

- **Entities werden nie direkt als JSON ausgegeben**, sondern über Records im Paket `web.dto`.
  So bleibt die API stabil, wenn sich das Datenmodell ändert.
- **Erst prüfen, dann buchen:** Die Kommissionierung prüft alle Positionen, bevor sie etwas
  bucht. Ein Klärfall ist ein gültiges Ergebnis, kein Fehler – würde ich stattdessen eine
  Exception werfen, rollte die Transaktion zurück und der Klärfall wäre auch weg.
- **Klärplatz statt Fehlermeldung:** Ein Wareneingang ohne passenden Platz antwortet mit
  `200` und `"ergebnis": "KLAERPLATZ"`. Die Ware ist ja physisch da, sie muss nur geklärt werden.
- **Sortierung der Transportaufträge in Java:** Die Priorität ist als Text gespeichert
  (lesbar in der Datenbank), alphabetisch käme aber HOCH, NIEDRIG, NORMAL heraus.
- **Optimistisches Sperren** (`@Version`) auf dem Bestand: Ändern zwei Anfragen gleichzeitig
  denselben Bestand, bekommt die zweite `409` statt einen falschen Bestand zu erzeugen.
- **Vereinfachungen:** Ein Platz hält nur einen Artikel; eine Anlieferung wird nicht auf
  mehrere Plätze verteilt; der Bestand wird sofort gebucht und nicht erst, wenn der
  Transport quittiert ist; keine Chargen, kein MHD, keine Benutzer.

## Starten

**Lokal** (JDK 21; Maven muss nicht installiert sein, der Wrapper lädt es):

```bash
./mvnw spring-boot:run
```

**Mit Docker:**

```bash
docker compose up --build -d
docker compose down          # Daten bleiben im Volume "lagerdaten"
```

Danach:

- API: <http://localhost:8080/api/...>
- Swagger-UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

## Beispiele mit curl

```bash
# Bestand je Lagerplatz (Demodaten)
curl -s localhost:8080/api/bestaende

# Wareneingang: 20 Stück Apfelsaft
curl -s -X POST localhost:8080/api/wareneingaenge \
  -H 'Content-Type: application/json' \
  -d '{"artikelNummer": "1002", "menge": 20}'
# → {"ergebnis":"EINGELAGERT","lagerplatz":"01-02-1","transportauftragId":4,"klaerfallId":null}

# Zu groß für jeden Platz → Klärplatz
curl -s -X POST localhost:8080/api/wareneingaenge \
  -H 'Content-Type: application/json' \
  -d '{"artikelNummer": "1001", "menge": 999}'
# → {"ergebnis":"KLAERPLATZ","lagerplatz":null,"transportauftragId":null,"klaerfallId":1}

# Kommissionierauftrag mit hoher Priorität
curl -s -X POST localhost:8080/api/kommissionierauftraege \
  -H 'Content-Type: application/json' \
  -d '{"positionen": [{"artikelNummer": "1001", "menge": 30}], "prioritaet": "HOCH"}'

# Offene Transportaufträge in Abarbeitungsreihenfolge
curl -s 'localhost:8080/api/transportauftraege?status=OFFEN'

# Transport starten, abschließen, umpriorisieren
curl -s -X POST localhost:8080/api/transportauftraege/1/start
curl -s -X POST localhost:8080/api/transportauftraege/1/abschluss
curl -s -X PUT  localhost:8080/api/transportauftraege/2/prioritaet \
  -H 'Content-Type: application/json' -d '{"prioritaet": "HOCH"}'

# Klärplatz ansehen und einen Fall abschließen
curl -s localhost:8080/api/klaerfaelle
curl -s -X POST localhost:8080/api/klaerfaelle/1/erledigen \
  -H 'Content-Type: application/json' -d '{"kommentar": "Ware auf zwei Plätze verteilt"}'
```

Fehler kommen einheitlich als Problem Details, z. B. ein unerlaubter Statuswechsel:

```json
{"status": 409, "title": "Konflikt", "detail": "Transportauftrag 1 ist OFFEN, erwartet war IN_ARBEIT"}
```

| Endpunkt | Methode | Zweck |
|---|---|---|
| `/api/artikel`, `/api/artikel/{nr}` | GET, POST | Artikel |
| `/api/lagerplaetze` | GET, POST | Lagerplätze |
| `/api/bestaende` | GET | Bestand je Platz |
| `/api/wareneingaenge` | POST | Einlagerung |
| `/api/kommissionierauftraege`, `/{id}` | POST, GET | Kommissionierung |
| `/api/transportauftraege` | GET (`?status=`) | Liste in Abarbeitungsreihenfolge |
| `/api/transportauftraege/{id}/start` \| `/abschluss` | POST | Statuswechsel |
| `/api/transportauftraege/{id}/prioritaet` | PUT | Umpriorisieren |
| `/api/klaerfaelle`, `/{id}/erledigen` | GET (`?erledigt=`), POST | Klärplatz |

## Tests

```bash
./mvnw verify
```

22 Tests, alle grün:

- **Unit-Tests ohne Spring** (JUnit 5, Mockito, AssertJ): Statuswechsel und Kapazitätsregeln
  in den Entities; Platzwahl im Wareneingang; Kommissionierung über mehrere Plätze,
  Fehlmenge → Klärfall ohne Buchung, zusammengezählte Positionen; Sortierung der Transporte.
- **Repository-Test** (`@DataJpaTest`): die eigene JPQL-Abfrage für leere Plätze gegen H2.
- **Integrationstests** (`@SpringBootTest` + MockMvc): kompletter Ablauf von Wareneingang
  über Kommissionierung bis zum Transport, Klärplatz, und die HTTP-Codes bei
  ungültigen Eingaben (400), unbekannten Artikeln (404) und Konflikten (409).

## Was ich dabei gelernt habe

- Wie Spring Boot Schichten, Dependency Injection (per Konstruktor) und Transaktionen
  zusammenbringt – und dass Änderungen an geladenen Entities innerhalb einer Transaktion
  ohne `save()` gespeichert werden (Dirty Checking).
- Warum „erst prüfen, dann buchen“ einfacher ist als eine Buchung zurückzurollen.
- Den Unterschied zwischen Unit-Tests mit Mocks (schnell, testen die Logik) und
  Integrationstests mit echtem Kontext (langsamer, finden Fehler in Abfragen und Konfiguration).
- Dass ein fachlicher Sonderfall wie der Klärplatz kein technischer Fehler ist und anders
  behandelt werden sollte als eine ungültige Anfrage.

## Mögliche Erweiterungen

- Bestand erst bei Quittierung des Einlagerungs-Transports buchen (mit Platzreservierung)
- Anlieferungen auf mehrere Plätze verteilen
- Klärfall „erneut versuchen“, z. B. Kommissionierauftrag nach Nachschub neu prüfen
- Lagerplätze sperren (z. B. Regal beschädigt) und bei der Platzsuche auslassen
- Flyway-Migrationen statt `ddl-auto=update`, PostgreSQL statt H2
- Authentifizierung (Spring Security) und Rollen, z. B. Leitstand vs. Staplerfahrer
- GitHub Actions für `./mvnw verify` und den Docker-Build

## Hinweis

Dieses Projekt wurde mithilfe von Claude (Anthropic) erstellt.

## Lizenz

MIT – siehe [LICENSE](LICENSE).
