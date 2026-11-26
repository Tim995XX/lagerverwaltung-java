import { useEffect, type ReactNode } from "react";
import { RueckmeldungAnzeige } from "./Rueckmeldung";

/** Jede Seite: genau eine h1, passender Fenstertitel, darunter die Meldungen. */
export function Seite({ titel, einleitung, children }: { titel: string; einleitung?: ReactNode; children: ReactNode }) {
  useEffect(() => {
    document.title = `${titel} – Lagerverwaltung`;
  }, [titel]);

  return (
    <>
      {/* tabIndex -1: Nach einem Seitenwechsel springt der Fokus hierher (siehe Layout) */}
      <h1 id="seitentitel" tabIndex={-1}>
        {titel}
      </h1>
      {einleitung && <p className="einleitung">{einleitung}</p>}
      <RueckmeldungAnzeige />
      {children}
    </>
  );
}
