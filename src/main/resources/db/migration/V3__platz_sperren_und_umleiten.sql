-- Gesperrte Plätze bekommen keine neue Ware
ALTER TABLE lagerplatz
    ADD COLUMN gesperrt   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sperrgrund VARCHAR(200);

ALTER TABLE transportauftrag DROP CONSTRAINT transportauftrag_status_check;
ALTER TABLE transportauftrag ADD CONSTRAINT transportauftrag_status_check
    CHECK (status IN ('OFFEN', 'IN_ARBEIT', 'ERLEDIGT', 'ABGEBROCHEN'));

ALTER TABLE klaerfall DROP CONSTRAINT klaerfall_grund_check;
ALTER TABLE klaerfall ADD CONSTRAINT klaerfall_grund_check
    CHECK (grund IN ('KEIN_PASSENDER_PLATZ', 'MASSE_FEHLEN', 'BESTAND_NICHT_AUSREICHEND', 'PLATZ_BLOCKIERT'));
