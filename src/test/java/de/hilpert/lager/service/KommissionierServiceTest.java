package de.hilpert.lager.service;

import de.hilpert.lager.domain.Artikel;
import de.hilpert.lager.domain.AuftragStatus;
import de.hilpert.lager.domain.Auftragsposition;
import de.hilpert.lager.domain.Bestand;
import de.hilpert.lager.domain.KlaerGrund;
import de.hilpert.lager.domain.Klaerfall;
import de.hilpert.lager.domain.Kommissionierauftrag;
import de.hilpert.lager.domain.Lagerplatz;
import de.hilpert.lager.domain.Prioritaet;
import de.hilpert.lager.domain.Transportauftrag;
import de.hilpert.lager.repository.BestandRepository;
import de.hilpert.lager.repository.KlaerfallRepository;
import de.hilpert.lager.repository.KommissionierauftragRepository;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KommissionierServiceTest {

    @Mock ArtikelService artikelService;
    @Mock KommissionierauftragRepository auftragRepository;
    @Mock BestandRepository bestandRepository;
    @Mock TransportauftragRepository transportauftragRepository;
    @Mock KlaerfallRepository klaerfallRepository;

    @InjectMocks KommissionierService service;

    private final Artikel wasser = new Artikel("1001", "Wasser");
    private Bestand kleinerBestand;
    private Bestand grosserBestand;

    @BeforeEach
    void setUp() {
        when(auftragRepository.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));
        when(artikelService.finde("1001")).thenReturn(wasser);
        kleinerBestand = new Bestand(wasser, new Lagerplatz(1, 1, 1, 40), 10);
        grosserBestand = new Bestand(wasser, new Lagerplatz(2, 1, 1, 120), 80);
        // Repository liefert bereits nach Menge aufsteigend sortiert
        when(bestandRepository.findByArtikelOrderByMengeAsc(wasser)).thenReturn(List.of(kleinerBestand, grosserBestand));
    }

    @Test
    void lagertUeberMehrerePlaetzeAusKleinsterBestandZuerst() {
        Kommissionierauftrag auftrag = service.anlegen(List.of(new Auftragsposition("1001", 25)), Prioritaet.HOCH);

        assertThat(auftrag.getStatus()).isEqualTo(AuftragStatus.FREIGEGEBEN);
        assertThat(kleinerBestand.getMenge()).isZero();
        assertThat(grosserBestand.getMenge()).isEqualTo(65);
        verify(bestandRepository).delete(kleinerBestand);

        ArgumentCaptor<Transportauftrag> transporte = ArgumentCaptor.forClass(Transportauftrag.class);
        verify(transportauftragRepository, times(2)).save(transporte.capture());
        assertThat(transporte.getAllValues())
                .extracting(Transportauftrag::getVonOrt, Transportauftrag::getMenge, Transportauftrag::getPrioritaet)
                .containsExactly(
                        tuple("01-01-1", 10, Prioritaet.HOCH),
                        tuple("02-01-1", 15, Prioritaet.HOCH));
    }

    @Test
    void zuWenigBestandFuehrtZuKlaerfallUndNichtsWirdGebucht() {
        Kommissionierauftrag auftrag = service.anlegen(List.of(new Auftragsposition("1001", 100)), Prioritaet.NORMAL);

        assertThat(auftrag.getStatus()).isEqualTo(AuftragStatus.IN_KLAERUNG);
        assertThat(kleinerBestand.getMenge()).isEqualTo(10);
        assertThat(grosserBestand.getMenge()).isEqualTo(80);
        verify(transportauftragRepository, never()).save(any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Klaerfall>> klaerfaelle = ArgumentCaptor.forClass(List.class);
        verify(klaerfallRepository).saveAll(klaerfaelle.capture());
        assertThat(klaerfaelle.getValue()).singleElement()
                .satisfies(k -> {
                    assertThat(k.getGrund()).isEqualTo(KlaerGrund.BESTAND_NICHT_AUSREICHEND);
                    assertThat(k.getBeschreibung()).contains("verfügbar 90");
                });
    }

    @Test
    void mengenDesselbenArtikelsWerdenZusammengezaehlt() {
        // je 50 würden einzeln passen, zusammen (100) aber nicht
        List<Auftragsposition> positionen = List.of(
                new Auftragsposition("1001", 50),
                new Auftragsposition("1001", 50));

        Kommissionierauftrag auftrag = service.anlegen(positionen, Prioritaet.NORMAL);

        assertThat(auftrag.getStatus()).isEqualTo(AuftragStatus.IN_KLAERUNG);
    }
}
