package de.hilpert.lager.service;

import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.TransportArt;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.fehler.NichtGefundenException;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportauftragServiceTest {

    @Mock TransportauftragRepository repository;

    @InjectMocks TransportauftragService service;

    private Transportauftrag auftrag(String artikel, Prioritaet prioritaet) {
        return new Transportauftrag(TransportArt.AUSLAGERUNG, artikel, 1, "01-01-1",
                Transportauftrag.WARENAUSGANG, prioritaet, null);
    }

    @Test
    void sortiertNachPrioritaetUndDannNachAlter() {
        // in dieser Reihenfolge angelegt, also a1 der älteste
        Transportauftrag a1 = auftrag("a1", Prioritaet.NIEDRIG);
        Transportauftrag a2 = auftrag("a2", Prioritaet.NORMAL);
        Transportauftrag a3 = auftrag("a3", Prioritaet.HOCH);
        Transportauftrag a4 = auftrag("a4", Prioritaet.NORMAL);
        when(repository.findAll()).thenReturn(List.of(a4, a1, a3, a2));

        List<Transportauftrag> liste = service.liste(null);

        assertThat(liste).extracting(Transportauftrag::getArtikelNummer)
                .containsExactly("a3", "a2", "a4", "a1");
    }

    @Test
    void unbekannteIdWirftNichtGefunden() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.starten(42L)).isInstanceOf(NichtGefundenException.class);
    }
}
