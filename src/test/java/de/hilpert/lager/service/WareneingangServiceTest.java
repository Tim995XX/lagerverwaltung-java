package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.KlaerGrund;
import de.hilpert.lager.domain.Klaerfall;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.domain.TransportArt;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.KlaerfallRepository;
import de.hilpert.lager.repository.LagerplatzRepository;
import de.hilpert.lager.repository.TransportauftragRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reine Unit-Tests ohne Spring und Datenbank: Repositories sind Mocks. */
@ExtendWith(MockitoExtension.class)
class WareneingangServiceTest {

    @Mock ArtikelService artikelService;
    @Mock BestandRepository bestandRepository;
    @Mock LagerplatzRepository lagerplatzRepository;
    @Mock TransportauftragRepository transportauftragRepository;
    @Mock KlaerfallRepository klaerfallRepository;

    @InjectMocks WareneingangService service;

    private final Artikel wasser = new Artikel("1001", "Wasser");

    @BeforeEach
    void setUp() {
        when(artikelService.finde("1001")).thenReturn(wasser);
        // save(...) gibt das übergebene Objekt zurück, wie es JPA auch tun würde
        lenient().when(bestandRepository.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));
        lenient().when(transportauftragRepository.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));
        lenient().when(klaerfallRepository.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));
    }

    @Test
    void lagertAufVorhandenenPlatzZuWennNochPlatzIst() {
        Bestand vorhanden = new Bestand(wasser, new Lagerplatz(1, 1, 1, 50), 30);
        when(bestandRepository.findByArtikelOrderByMengeAsc(wasser)).thenReturn(List.of(vorhanden));

        WareneingangErgebnis ergebnis = service.einlagern("1001", 20);

        assertThat(ergebnis.ergebnis()).isEqualTo(WareneingangErgebnis.Ergebnis.EINGELAGERT);
        assertThat(ergebnis.lagerplatz()).isEqualTo("01-01-1");
        assertThat(vorhanden.getMenge()).isEqualTo(50);
        verify(lagerplatzRepository, never()).findeLeerePlaetzeFuer(any(Integer.class));
    }

    @Test
    void nimmtLeerenPlatzWennZulagerungNichtPasst() {
        Bestand fastVoll = new Bestand(wasser, new Lagerplatz(1, 1, 1, 50), 45);
        Lagerplatz leer = new Lagerplatz(2, 1, 1, 120);
        when(bestandRepository.findByArtikelOrderByMengeAsc(wasser)).thenReturn(List.of(fastVoll));
        when(lagerplatzRepository.findeLeerePlaetzeFuer(20)).thenReturn(List.of(leer));

        WareneingangErgebnis ergebnis = service.einlagern("1001", 20);

        assertThat(ergebnis.lagerplatz()).isEqualTo("02-01-1");
        assertThat(fastVoll.getMenge()).isEqualTo(45);

        ArgumentCaptor<Transportauftrag> transport = ArgumentCaptor.forClass(Transportauftrag.class);
        verify(transportauftragRepository).save(transport.capture());
        assertThat(transport.getValue().getArt()).isEqualTo(TransportArt.EINLAGERUNG);
        assertThat(transport.getValue().getVonOrt()).isEqualTo(Transportauftrag.WARENEINGANG);
        assertThat(transport.getValue().getNachOrt()).isEqualTo("02-01-1");
    }

    @Test
    void kommtAufDenKlaerplatzWennKeinPlatzPasst() {
        when(bestandRepository.findByArtikelOrderByMengeAsc(wasser)).thenReturn(List.of());
        when(lagerplatzRepository.findeLeerePlaetzeFuer(500)).thenReturn(List.of());

        WareneingangErgebnis ergebnis = service.einlagern("1001", 500);

        assertThat(ergebnis.ergebnis()).isEqualTo(WareneingangErgebnis.Ergebnis.KLAERPLATZ);
        ArgumentCaptor<Klaerfall> klaerfall = ArgumentCaptor.forClass(Klaerfall.class);
        verify(klaerfallRepository).save(klaerfall.capture());
        assertThat(klaerfall.getValue().getGrund()).isEqualTo(KlaerGrund.KEIN_PASSENDER_PLATZ);
        assertThat(klaerfall.getValue().getMenge()).isEqualTo(500);
        verify(bestandRepository, never()).save(any());
        verify(transportauftragRepository, never()).save(any());
    }
}
