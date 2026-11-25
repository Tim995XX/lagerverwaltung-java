-- Ausgangsschema. Diese Datei nach dem Veröffentlichen nicht mehr ändern,
-- sondern für Änderungen eine neue Migration V2__... anlegen.

-- Hibernate holt IDs in 50er-Blöcken, deshalb INCREMENT BY 50
CREATE SEQUENCE artikel_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE lagerplatz_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE bestand_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE kommissionierauftrag_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE transportauftrag_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE klaerfall_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE artikel (
    id             BIGINT PRIMARY KEY,
    artikel_nummer VARCHAR(255) NOT NULL UNIQUE,
    bezeichnung    VARCHAR(255) NOT NULL
);

CREATE TABLE lagerplatz (
    id         BIGINT PRIMARY KEY,
    code       VARCHAR(255) NOT NULL UNIQUE,
    gang       INTEGER NOT NULL,
    regal      INTEGER NOT NULL,
    ebene      INTEGER NOT NULL,
    kapazitaet INTEGER NOT NULL CHECK (kapazitaet > 0)
);

CREATE TABLE bestand (
    id            BIGINT PRIMARY KEY,
    artikel_id    BIGINT NOT NULL REFERENCES artikel (id),
    lagerplatz_id BIGINT NOT NULL UNIQUE REFERENCES lagerplatz (id),
    menge         INTEGER NOT NULL CHECK (menge >= 0),
    version       BIGINT NOT NULL
);

CREATE TABLE kommissionierauftrag (
    id          BIGINT PRIMARY KEY,
    prioritaet  VARCHAR(255) NOT NULL CHECK (prioritaet IN ('HOCH', 'NORMAL', 'NIEDRIG')),
    status      VARCHAR(255) NOT NULL CHECK (status IN ('FREIGEGEBEN', 'IN_KLAERUNG')),
    erstellt_am TIMESTAMP(6) NOT NULL
);

CREATE TABLE kommissionierauftrag_positionen (
    kommissionierauftrag_id BIGINT NOT NULL REFERENCES kommissionierauftrag (id),
    artikel_nummer          VARCHAR(255) NOT NULL,
    menge                   INTEGER NOT NULL CHECK (menge > 0)
);

CREATE TABLE transportauftrag (
    id                      BIGINT PRIMARY KEY,
    art                     VARCHAR(255) NOT NULL CHECK (art IN ('EINLAGERUNG', 'AUSLAGERUNG')),
    artikel_nummer          VARCHAR(255) NOT NULL,
    menge                   INTEGER NOT NULL CHECK (menge > 0),
    von_ort                 VARCHAR(255) NOT NULL,
    nach_ort                VARCHAR(255) NOT NULL,
    prioritaet              VARCHAR(255) NOT NULL CHECK (prioritaet IN ('HOCH', 'NORMAL', 'NIEDRIG')),
    status                  VARCHAR(255) NOT NULL CHECK (status IN ('OFFEN', 'IN_ARBEIT', 'ERLEDIGT')),
    kommissionierauftrag_id BIGINT REFERENCES kommissionierauftrag (id),
    erstellt_am             TIMESTAMP(6) NOT NULL
);

CREATE TABLE klaerfall (
    id                      BIGINT PRIMARY KEY,
    grund                   VARCHAR(255) NOT NULL
                            CHECK (grund IN ('KEIN_PASSENDER_PLATZ', 'BESTAND_NICHT_AUSREICHEND')),
    artikel_nummer          VARCHAR(255) NOT NULL,
    menge                   INTEGER NOT NULL CHECK (menge > 0),
    beschreibung            VARCHAR(500) NOT NULL,
    kommissionierauftrag_id BIGINT REFERENCES kommissionierauftrag (id),
    erstellt_am             TIMESTAMP(6) NOT NULL,
    erledigt                BOOLEAN NOT NULL,
    kommentar               VARCHAR(500)
);

-- PostgreSQL legt für Fremdschlüssel nicht automatisch einen Index an
CREATE INDEX idx_bestand_artikel ON bestand (artikel_id);
CREATE INDEX idx_positionen_auftrag ON kommissionierauftrag_positionen (kommissionierauftrag_id);
CREATE INDEX idx_transport_auftrag ON transportauftrag (kommissionierauftrag_id);
CREATE INDEX idx_klaerfall_auftrag ON klaerfall (kommissionierauftrag_id);
