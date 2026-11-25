-- Maße, Gewicht und Umschlag für die Platzsuche. Vorhandene Datensätze haben noch keine Maße (NULL)
-- und werden erst nach dem Nachtragen berücksichtigt.
ALTER TABLE artikel
    ADD COLUMN laenge_mm     INTEGER CHECK (laenge_mm > 0),
    ADD COLUMN breite_mm     INTEGER CHECK (breite_mm > 0),
    ADD COLUMN hoehe_mm      INTEGER CHECK (hoehe_mm > 0),
    ADD COLUMN gewicht_gramm INTEGER CHECK (gewicht_gramm > 0),
    ADD COLUMN umschlag      VARCHAR(255) NOT NULL DEFAULT 'MITTEL'
        CHECK (umschlag IN ('SCHNELL', 'MITTEL', 'LANGSAM'));

ALTER TABLE lagerplatz
    ADD COLUMN laenge_mm      INTEGER CHECK (laenge_mm > 0),
    ADD COLUMN breite_mm      INTEGER CHECK (breite_mm > 0),
    ADD COLUMN hoehe_mm       INTEGER CHECK (hoehe_mm > 0),
    ADD COLUMN max_gewicht_kg INTEGER CHECK (max_gewicht_kg > 0);

-- Unterwegs zum Platz, aber noch nicht eingescannt
ALTER TABLE bestand ADD COLUMN reserviert INTEGER NOT NULL DEFAULT 0 CHECK (reserviert >= 0);

-- Bisher wurde Bestand schon beim Wareneingang gebucht. Alte Aufträge bekommen deshalb TRUE,
-- damit ihr Scan den Bestand nicht ein zweites Mal bucht.
ALTER TABLE transportauftrag
    ADD COLUMN bestand_gebucht BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN version         BIGINT NOT NULL DEFAULT 0;
ALTER TABLE transportauftrag ALTER COLUMN bestand_gebucht DROP DEFAULT;

ALTER TABLE klaerfall DROP CONSTRAINT klaerfall_grund_check;
ALTER TABLE klaerfall ADD CONSTRAINT klaerfall_grund_check
    CHECK (grund IN ('KEIN_PASSENDER_PLATZ', 'MASSE_FEHLEN', 'BESTAND_NICHT_AUSREICHEND'));

CREATE SEQUENCE scan_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE scan (
    id                  BIGINT PRIMARY KEY,
    transportauftrag_id BIGINT NOT NULL REFERENCES transportauftrag (id),
    artikel_nummer      VARCHAR(255) NOT NULL,
    lagerplatz_code     VARCHAR(255) NOT NULL,
    erfolgreich         BOOLEAN NOT NULL,
    gescannt_am         TIMESTAMP(6) NOT NULL
);
CREATE INDEX idx_scan_transport ON scan (transportauftrag_id);
