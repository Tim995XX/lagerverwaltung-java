package de.hilpert.lager.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransportauftragTest {

    private Transportauftrag neuerAuftrag() {
        return new Transportauftrag(TransportArt.AUSLAGERUNG, "1001", 10,
                Transportauftrag.WARENEINGANG, "01-01-1", Prioritaet.NORMAL, null);
    }

    @Test
    void durchlaeuftOffenInArbeitErledigt() {
        Transportauftrag auftrag = neuerAuftrag();
        assertThat(auftrag.getStatus()).isEqualTo(TransportStatus.OFFEN);

        auftrag.starten();
        assertThat(auftrag.getStatus()).isEqualTo(TransportStatus.IN_ARBEIT);

        auftrag.abschliessen();
        assertThat(auftrag.getStatus()).isEqualTo(TransportStatus.ERLEDIGT);
    }

    @Test
    void abschliessenOhneStartIstNichtErlaubt() {
        Transportauftrag auftrag = neuerAuftrag();

        assertThatThrownBy(auftrag::abschliessen).isInstanceOf(UngueltigerStatusException.class);
        assertThat(auftrag.getStatus()).isEqualTo(TransportStatus.OFFEN);
    }

    @Test
    void zweimalStartenIstNichtErlaubt() {
        Transportauftrag auftrag = neuerAuftrag();
        auftrag.starten();

        assertThatThrownBy(auftrag::starten).isInstanceOf(UngueltigerStatusException.class);
    }

    @Test
    void erledigteAuftraegeKoennenNichtUmpriorisiertWerden() {
        Transportauftrag auftrag = neuerAuftrag();
        auftrag.aenderePrioritaet(Prioritaet.HOCH);
        assertThat(auftrag.getPrioritaet()).isEqualTo(Prioritaet.HOCH);

        auftrag.starten();
        auftrag.abschliessen();

        assertThatThrownBy(() -> auftrag.aenderePrioritaet(Prioritaet.NIEDRIG))
                .isInstanceOf(UngueltigerStatusException.class);
    }

    @Test
    void einlagerungKannNichtOhneScanAbgeschlossenWerden() {
        Transportauftrag auftrag = new Transportauftrag(TransportArt.EINLAGERUNG, "1001", 10,
                Transportauftrag.WARENEINGANG, "01-01-1", Prioritaet.NORMAL, null);
        auftrag.starten();

        assertThatThrownBy(auftrag::abschliessen).isInstanceOf(UngueltigerStatusException.class);
        auftrag.eingelagert();
        assertThat(auftrag.getStatus()).isEqualTo(TransportStatus.ERLEDIGT);
        assertThatThrownBy(auftrag::eingelagert).isInstanceOf(UngueltigerStatusException.class);
    }
}
