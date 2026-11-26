import type { Prioritaet, TransportStatus } from "../api/typen";
import { TEXTE } from "../api/typen";

/** Status und Priorität immer als Text; die Farbe unterstützt nur (WCAG 1.4.1: nicht nur über Farbe). */
export function StatusMarke({ status }: { status: TransportStatus }) {
  return <span className={`marke marke-status-${status.toLowerCase()}`}>{TEXTE.status[status]}</span>;
}

export function PrioritaetMarke({ prioritaet }: { prioritaet: Prioritaet }) {
  return <span className={`marke marke-prio-${prioritaet.toLowerCase()}`}>{TEXTE.prioritaet[prioritaet]}</span>;
}
