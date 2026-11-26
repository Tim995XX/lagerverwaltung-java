package de.hilpert.lager.sicherheit;

/**
 * LEITSTAND darf alles. STAPLERFAHRER darf lesen und seine eigenen Transportaufträge abarbeiten
 * (starten, scannen, abschließen, umleiten), aber keine Stammdaten, Prioritäten oder Klärfälle ändern.
 */
public enum Rolle {
    LEITSTAND,
    STAPLERFAHRER
}
