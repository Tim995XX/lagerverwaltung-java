-- Anmeldung: Passwörter nur als Hash (Format von Springs DelegatingPasswordEncoder, z. B. "{bcrypt}$2a$...")
CREATE SEQUENCE benutzer_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE benutzer (
    id            BIGINT PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL UNIQUE,
    passwort_hash VARCHAR(100) NOT NULL,
    rolle         VARCHAR(20)  NOT NULL CHECK (rolle IN ('LEITSTAND', 'STAPLERFAHRER')),
    aktiv         BOOLEAN      NOT NULL DEFAULT TRUE
);
