import { Navigate, Route, Routes } from "react-router";
import { AnmeldungProvider } from "./anmeldung/Anmeldung";
import { NurAngemeldet, NurRolle } from "./anmeldung/Zugriff";
import type { Ich } from "./api/typen";
import { Layout } from "./Layout";
import { Seite } from "./komponenten/Seite";
import { AnmeldeSeite } from "./seiten/AnmeldeSeite";
import { ArtikelSeite } from "./seiten/ArtikelSeite";
import { KlaerplatzSeite } from "./seiten/KlaerplatzSeite";
import { KommissionierSeite } from "./seiten/KommissionierSeite";
import { LagerSeite } from "./seiten/LagerSeite";
import { TransporteSeite } from "./seiten/TransporteSeite";
import { WareneingangSeite } from "./seiten/WareneingangSeite";

/** Die Pfade müssen zu OberflaecheController im Backend passen (Neuladen einer Unterseite). */
export function App({ startBenutzer }: { startBenutzer?: Ich | null }) {
  return (
    <AnmeldungProvider startBenutzer={startBenutzer}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/anmelden" element={<AnmeldeSeite />} />
          <Route element={<NurAngemeldet />}>
            <Route path="/" element={<Navigate to="/transporte" replace />} />
            <Route path="/transporte" element={<TransporteSeite />} />
            <Route path="/lager" element={<LagerSeite />} />
            <Route element={<NurRolle rolle="LEITSTAND" />}>
              <Route path="/wareneingang" element={<WareneingangSeite />} />
              <Route path="/kommissionierung" element={<KommissionierSeite />} />
              <Route path="/klaerplatz" element={<KlaerplatzSeite />} />
              <Route path="/artikel" element={<ArtikelSeite />} />
            </Route>
          </Route>
          <Route
            path="*"
            element={
              <Seite titel="Seite nicht gefunden">
                <p>Diese Adresse gibt es nicht. Bitte einen Eintrag aus dem Menü wählen.</p>
              </Seite>
            }
          />
        </Route>
      </Routes>
    </AnmeldungProvider>
  );
}
