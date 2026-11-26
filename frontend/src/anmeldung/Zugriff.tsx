import { Navigate, Outlet, useLocation } from "react-router";
import type { Rolle } from "../api/typen";
import { Seite } from "../komponenten/Seite";
import { useAnmeldung } from "./Anmeldung";

/** Nicht angemeldet: zur Anmeldung und danach zurück zur gewünschten Seite. */
export function NurAngemeldet() {
  const { benutzer } = useAnmeldung();
  const ort = useLocation();
  if (!benutzer) {
    return <Navigate to="/anmelden" replace state={{ zurueck: ort.pathname }} />;
  }
  return <Outlet />;
}

/**
 * Blendet Seiten für andere Rollen aus. Das ist nur Bedienkomfort:
 * Die eigentliche Rechteprüfung macht der Server bei jeder Anfrage.
 */
export function NurRolle({ rolle }: { rolle: Rolle }) {
  const { benutzer } = useAnmeldung();
  if (benutzer?.rolle !== rolle) {
    return (
      <Seite titel="Keine Berechtigung">
        <p>Diese Seite ist nur für den Leitstand freigegeben.</p>
      </Seite>
    );
  }
  return <Outlet />;
}
