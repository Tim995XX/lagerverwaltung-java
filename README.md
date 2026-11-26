# lagerverwaltung-java

[![CI](https://github.com/Tim995XX/lagerverwaltung-java/actions/workflows/ci.yml/badge.svg)](https://github.com/Tim995XX/lagerverwaltung-java/actions/workflows/ci.yml)

Eine kleine Lagerverwaltung als REST-API mit **Java 21** und **Spring Boot**: Artikel und
Lagerplätze anlegen, Ware einlagern, Kommissionieraufträge auslagern, Transportaufträge nach
Priorität abarbeiten – und alles, was das System nicht automatisch buchen kann, landet auf dem
**Klärplatz**, wo ein Mensch es klärt.

## Motivation

Vor meiner Umschulung habe ich knapp anderthalb Jahre im Leitstand eines Lagers gearbeitet, mit einem
Lagerverwaltungssystem und SAP. Ein großer Teil der Arbeit waren die Fälle, in denen die
Automatik nicht weiterkam: Klärplatzmeldungen, Buchungsfehler, Transportaufträge, die
umpriorisiert werden mussten, weil ein Lkw wartet.

Dieses Projekt ist eine eigene, stark vereinfachte Nachbildung dieser Konzepte. Es enthält
keinen Code und keine Daten eines echten Systems. Mir ging es darum, die Abläufe, die ich als
Anwender kenne, einmal selbst als Software umzusetzen – und dabei Java und Spring Boot sauber
zu lernen.

## Fachlicher Ablauf

```
Wareneingang ──▶ passender Platz?  ── ja ──▶ Platz reservieren + Transportauftrag EINLAGERUNG
                       │                                     │
                       │                                     └──▶ Scan von Ware UND Platz ──▶ Bestand gebucht
                       │
                       └── nein ──▶ Klärfall KEIN_PASSENDER_PLATZ oder MASSE_FEHLEN

Kommissionierauftrag ──▶ Bestand reicht für alle Positionen?
                       │
                       ├── ja ──▶ ausbuchen (kleinste Bestände zuerst) + Transportaufträge AUSLAGERUNG
                       └── nein ──▶ nichts buchen, Auftrag IN_KLAERUNG + Klärfall BESTAND_NICHT_AUSREICHEND

Transportauftrag:  OFFEN ──start──▶ IN_ARBEIT ──abschluss (Auslagerung) / scan (Einlagerung)──▶ ERLEDIGT
                   IN_ARBEIT ──Platz blockiert, kein Ersatz──▶ ABGEBROCHEN (Ware auf Klärplatz)
                   Reihenfolge: Priorität (HOCH, NORMAL, NIEDRIG), dann ältester zuerst
```

## Features

- **Stammdaten:** Artikel (eindeutige Artikelnummer) und Lagerplätze mit Adresse
  Gang-Regal-Ebene (z. B. `01-03-2`) und Kapazität in Stück.
- **Platzsuche im Wareneingang:** zuerst ein Platz, auf dem der Artikel schon liegt und noch genug
  frei ist (Zulagerung), sonst der sinnvollste leere Platz. Muss-Regeln: Maße (Ware aufrecht,
  90° drehbar, stapelbar) und Traglast. Danach: schwere Ware (ab 15 kg) nach unten, Schnelldreher
  nah an die Tore, sonst der knappste passende Platz, Langsamdreher eher hinten. Plätze mit noch
  offener Auslagerung werden ausgelassen. Die Antwort nennt die Begründung.
- **Scan-Bestätigung:** Der Platz wird nur reserviert. Erst wenn Ware und Platz gescannt sind und
  zum Auftrag passen, entsteht Bestand. Ein falscher Scan ergibt `409`, bucht nichts und wird
  protokolliert. Ohne Scan lässt sich eine Einlagerung nicht abschließen.
- **Umleiten:** Meldet der Fahrer einen blockierten Platz, wird dieser gesperrt, die Reservierung
  gelöst und ein neuer Platz gesucht; der Leitstand bekommt einen Klärfall. Ohne Ersatzplatz geht
  die Ware auf den Klärplatz. Der Leitstand kann Plätze auch selbst sperren und wieder freigeben.
- **Kommissionierung:** mehrere Positionen pro Auftrag, Mengen desselben Artikels werden
  zusammengezählt. Gebucht wird nur, wenn **alle** Positionen bedient werden können.
  Ausgelagert wird von den kleinsten Beständen zuerst, damit angebrochene Plätze leer werden.
- **Transportaufträge:** Liste in Abarbeitungsreihenfolge, Statuswechsel mit Prüfung,
  Umpriorisieren durch den Leitstand.
- **Klärplatz:** offene Klärfälle mit Grund und Beschreibung, Abschluss mit Kommentar.
- **Anmeldung und Rollen** (Spring Security): Der **Leitstand** darf alles, der **Staplerfahrer**
  darf lesen und Transportaufträge abarbeiten (starten, scannen, abschließen, umleiten), aber
  keine Stammdaten, Prioritäten oder Klärfälle ändern. Ohne Anmeldung `401`, ohne Recht `403`.
- **Fehlerbehandlung:** zentral per `@RestControllerAdvice`, Antworten im Format
  „Problem Details“ (RFC 9457) mit passenden HTTP-Codes (400, 401, 403, 404, 409).
- **Bean Validation** auf allen Eingaben, **OpenAPI/Swagger-UI**, Health-Endpunkt.
- **Demodaten:** Beim ersten Start werden 3 Artikel, 12 Plätze, 3 Wareneingänge und je Rolle ein
  Benutzer angelegt.

## Architektur

Klassische Schichten: Controller → Service → Repository → Datenbank.

| Paket | Aufgabe |
|---|---|
| `web` | REST-Controller, `FehlerBehandlung` (Exceptions → HTTP) |
| `web.dto` | Request-/Response-Records mit Validierungs-Annotationen |
| `service` | Geschäftslogik und Transaktionen (`@Transactional`) |
| `repository` | Spring-Data-JPA-Interfaces, eine eigene JPQL-Abfrage für leere Plätze |
| `domain` | JPA-Entities und Enums; Regeln, die nur ein Objekt betreffen (z. B. Statuswechsel), stehen direkt in der Entity |
| `sicherheit` | Rechte je Rolle (`SicherheitsKonfiguration`), Benutzer aus der Datenbank, Demobenutzer |
| `fehler` | Fachliche Exceptions (`NichtGefundenException`, `KonfliktException`) |

Datenbank ist **PostgreSQL**. Das Schema liegt als SQL in `src/main/resources/db/migration` und
wird beim Start von **Flyway** angewendet. Hibernate prüft nur noch, ob Entities und Tabellen
zusammenpassen (`ddl-auto=validate`).

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
- **Migrationen statt `ddl-auto=update`:** Jede Schemaänderung ist eine nummerierte SQL-Datei, im
  Git nachvollziehbar und überall in derselben Reihenfolge ausgeführt.
- **Regeln auch in der Datenbank:** Check-, Unique- und Fremdschlüssel-Constraints lehnen z. B.
  negativen Bestand selbst dann ab, wenn der Java-Code einen Fehler hat.
- **Unique-Regel auf dem Lagerplatz:** Belegen zwei Anfragen gleichzeitig denselben leeren Platz,
  hilft `@Version` nicht, weil es noch keinen Datensatz gibt. Das fängt die Datenbank ab, die zweite
  Anfrage bekommt `409`. Andere Datenbankverstöße bleiben ein `500`, weil sie Programmfehler sind.
- **Reservierung und Bestand getrennt:** Reservierte Ware belegt Kapazität, kann aber nicht
  kommissioniert werden, solange sie nicht gescannt ist.
- **Falscher Scan ohne Rollback:** `noRollbackFor` sorgt dafür, dass der Protokolleintrag trotz
  Exception erhalten bleibt.
- **Platzsuche in Java statt SQL:** gut testbar. Bei sehr vielen Plätzen müsste mehr in die Datenbank.
- **Umleiten in einer Transaktion:** Sperre, Freigabe, neue Reservierung und neues Ziel gelten
  zusammen oder gar nicht.
- **Sperre nur für Einlagerungen:** Ware auf einem gesperrten Platz darf weiter abgeholt werden.
- **Grenze des Scans:** Er belegt, welche Codes gescannt wurden, nicht wo die Ware wirklich steht.
- **Rechte an einer Stelle:** Wer was darf, steht nach URL und HTTP-Methode in
  `SicherheitsKonfiguration`, nicht verteilt über die Controller. Unbekannte Pfade sind gesperrt
  (`denyAll`), damit ein neuer Endpunkt nicht versehentlich offen ist.
- **HTTP Basic ohne Session:** Jede Anfrage bringt Benutzername und Passwort mit, der Server merkt
  sich nichts. Das ist für eine API einfach und gut testbar, braucht aber zwingend HTTPS (in
  Produktion vor der Anwendung, z. B. im Reverse Proxy). Weil kein Cookie im Spiel ist, ist der
  CSRF-Schutz abgeschaltet; für eine Browser-Oberfläche mit Login-Cookie muss das neu entschieden werden.
- **Benutzer in PostgreSQL, Passwörter mit BCrypt:** In der Tabelle steht nur der Hash mit
  Verfahren als Präfix (`{bcrypt}...`), so lässt sich das Verfahren später wechseln. Benutzer
  werden deaktiviert statt gelöscht.
- **Demopasswörter:** `leitstand`/`leitstand` und `fahrer`/`fahrer` sind nur zum Ausprobieren
  gedacht und lassen sich vor dem ersten Start über `LAGER_PASSWORT_LEITSTAND` und
  `LAGER_PASSWORT_FAHRER` ersetzen. Eine Benutzerverwaltung per API gibt es noch nicht.
- **Vereinfachungen:** Ein Platz hält nur einen Artikel; eine Anlieferung wird nicht auf
  mehrere Plätze verteilt; Auslagerungen werden sofort abgebucht und ohne Scan
  abgeschlossen; die Entfernung ist Gang + Regal; keine Chargen, kein MHD.

## Starten

**Lokal** (JDK 21 und Docker; Maven muss nicht installiert sein, der Wrapper lädt es):

```bash
docker compose up -d db      # nur PostgreSQL
./mvnw spring-boot:run
```

**Alles mit Docker** (Anwendung und PostgreSQL):

```bash
docker compose up --build -d
docker compose down          # Daten bleiben im Volume "pgdaten"
```

Die Zugangsdaten sind Vorgaben für die lokale Entwicklung und lassen sich über `LAGER_DB_URL`,
`LAGER_DB_BENUTZER` und `LAGER_DB_PASSWORT` ersetzen (Compose: nur das Passwort, vor dem ersten
Start setzen).

Die API verlangt eine Anmeldung. Demobenutzer: `leitstand` / `leitstand` (darf alles) und
`fahrer` / `fahrer` (Staplerfahrer). In der Swagger-UI über den Knopf „Authorize“ anmelden.

Danach:

- API: <http://localhost:8080/api/...>
- Swagger-UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health> (ohne Anmeldung)

## Beispiele mit curl

```bash
L='-u leitstand:leitstand'   # Leitstand
F='-u fahrer:fahrer'         # Staplerfahrer

# Bestand je Lagerplatz (Demodaten)
curl -s $L localhost:8080/api/bestaende

# Wareneingang: 20 Stück Apfelsaft
curl -s $L -X POST localhost:8080/api/wareneingaenge \
  -H 'Content-Type: application/json' \
  -d '{"artikelNummer": "1002", "menge": 20}'
# → {"ergebnis":"PLATZ_RESERVIERT","lagerplatz":"01-01-2","transportauftragId":4,"klaerfallId":null,
#    "begruendung":"kleinster passender Platz, dann kurzer Weg"}

# Der Fahrer fährt, stellt ab und scannt: erst jetzt wird gebucht
curl -s $F -X POST localhost:8080/api/transportauftraege/4/start
curl -s $F -X POST localhost:8080/api/transportauftraege/4/scan \
  -H 'Content-Type: application/json' -d '{"artikelNummer": "1002", "lagerplatzCode": "01-01-2"}'

# Platz beschädigt? Vor dem Scan umleiten (gleicher Aufruf statt des Scans)
# curl -s $F -X POST localhost:8080/api/transportauftraege/4/umleiten \
#   -H 'Content-Type: application/json' -d '{"grund": "Regalboden beschädigt"}'

# Zu groß für jeden Platz → Klärplatz
curl -s $L -X POST localhost:8080/api/wareneingaenge \
  -H 'Content-Type: application/json' \
  -d '{"artikelNummer": "1001", "menge": 999}'
# → {"ergebnis":"KLAERPLATZ","lagerplatz":null,"transportauftragId":null,"klaerfallId":1,"begruendung":null}

# Kommissionierauftrag mit hoher Priorität
curl -s $L -X POST localhost:8080/api/kommissionierauftraege \
  -H 'Content-Type: application/json' \
  -d '{"positionen": [{"artikelNummer": "1001", "menge": 30}], "prioritaet": "HOCH"}'

# Offene Transportaufträge in Abarbeitungsreihenfolge (sieht auch der Fahrer)
curl -s $F 'localhost:8080/api/transportauftraege?status=OFFEN'

# Leitstand priorisiert um, der Fahrer startet und schließt ab
curl -s $L -X PUT localhost:8080/api/transportauftraege/5/prioritaet \
  -H 'Content-Type: application/json' -d '{"prioritaet": "HOCH"}'
curl -s $F -X POST localhost:8080/api/transportauftraege/5/start
curl -s $F -X POST localhost:8080/api/transportauftraege/5/abschluss

# Klärplatz ansehen und einen Fall abschließen
curl -s $L localhost:8080/api/klaerfaelle
curl -s $L -X POST localhost:8080/api/klaerfaelle/1/erledigen \
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
| `/api/transportauftraege/{id}/scan`, `/scans` | POST, GET | Einlagerung per Scan bestätigen, Protokoll |
| `/api/transportauftraege/{id}/umleiten` | POST | Platz sperren, neuen suchen |
| `/api/lagerplaetze/{code}/sperre` | PUT, DELETE | Platz sperren, freigeben |
| `/api/artikel/{nr}/masse`, `/api/lagerplaetze/{code}/masse` | PUT | Maße nachtragen |
| `/api/transportauftraege/{id}/prioritaet` | PUT | Umpriorisieren |
| `/api/klaerfaelle`, `/{id}/erledigen` | GET (`?erledigt=`), POST | Klärplatz |

| Rolle | darf |
|---|---|
| `LEITSTAND` | alles |
| `STAPLERFAHRER` | alle `GET`; `POST` auf `/api/transportauftraege/{id}/start`, `/scan`, `/abschluss`, `/umleiten` |
| ohne Anmeldung | nur Health, Swagger-UI und OpenAPI-Beschreibung |

## Tests

```bash
./mvnw verify
```

Docker muss laufen: **Testcontainers** startet für die Datenbanktests ein echtes PostgreSQL und
räumt es danach wieder weg.

56 Tests, alle grün:

- **Unit-Tests ohne Spring** (JUnit 5, Mockito, AssertJ): Statuswechsel und Kapazitätsregeln
  in den Entities; Platzwahl im Wareneingang; Kommissionierung über mehrere Plätze,
  Fehlmenge → Klärfall ohne Buchung, zusammengezählte Positionen; Sortierung der Transporte.
- **Repository-Test** (`@DataJpaTest`): die eigene JPQL-Abfrage für leere Plätze gegen PostgreSQL.
- **Schema-Test:** Flyway-Migration ist gelaufen; negativer Bestand und doppelt belegte Plätze
  werden von der Datenbank abgelehnt.
- **Platzsuche und Scan:** Drehen, Stapeln, Traglast, Reihenfolge; falscher und doppelter Scan;
  Umleiten mit und ohne Ersatzplatz, zwei Fahrer am selben gesperrten Platz.
- **Migrationstest:** V2 läuft auch auf einer Datenbank mit V1-Daten.
- **Test gleichzeitiger Buchungen:** Zwei Wareneingänge wollen denselben leeren Platz; die zweite
  Anfrage bekommt `409`, bucht nichts und gelingt beim nächsten Versuch.
- **Sicherheitstest** mit echten Benutzern aus PostgreSQL: ohne Anmeldung, falsches Passwort,
  unbekannter und deaktivierter Benutzer ergeben `401`; der Fahrer darf lesen und Transporte
  abarbeiten, bekommt aber `403` bei Stammdaten, Wareneingang, Priorität, Sperre und Klärfall;
  das Passwort steht nur als BCrypt-Hash in der Datenbank.
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
- Dass Spring Security als Filter **vor** den Controllern läuft: `401`/`403` entstehen dort, deshalb
  greift `@RestControllerAdvice` nicht und die Problem-Details-Antwort wird im Filter geschrieben.
- Den Unterschied zwischen `401` (wer bist du?) und `403` (du darfst das nicht).

## Mögliche Erweiterungen

- Auslagerungen ebenfalls per Scan bestätigen
- Anlieferungen auf mehrere Plätze verteilen
- Klärfall „erneut versuchen“, z. B. Kommissionierauftrag nach Nachschub neu prüfen
- Protokollieren, welcher Benutzer gescannt oder einen Klärfall erledigt hat
- Benutzerverwaltung per API (anlegen, deaktivieren, Passwort ändern)

## Hinweis

Dieses Projekt wurde mithilfe von Claude (Anthropic) erstellt.

## Lizenz

MIT – siehe [LICENSE](LICENSE).
