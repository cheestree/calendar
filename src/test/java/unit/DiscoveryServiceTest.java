package unit;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.discover.EventProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {
    @Mock
    private EventProvider ticketmaster;

    @Mock
    private EventProvider seatGeek;

    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new DiscoveryService(List.of(ticketmaster, seatGeek));
    }

    /**
     * Scenario: Discovery service is configured with two providers.
     */
    @Test
    void providersReturnsConfiguredProviderList() {
        List<EventProvider> providers = List.of(ticketmaster, seatGeek);
        discoveryService = new DiscoveryService(providers);

        assertSame(providers, discoveryService.providers());
    }

    /**
     * Scenario: Discovery service is not configured with any providers.
     */
    @Test
    void searchReturnsEmptyListForBlankQuery() {
        assertTrue(discoveryService.search(null).isEmpty());
        assertTrue(discoveryService.search("   ").isEmpty());
        verify(ticketmaster, never()).isConfigured();
        verify(seatGeek, never()).isConfigured();
        verify(ticketmaster, never()).search(anyString());
        verify(seatGeek, never()).search(anyString());
    }

    /**
     * Scenario: Discovery service is configured with two providers. One provider is configured and returns results, the other is not configured.
     */
    @Test
    void searchSkipsUnconfiguredProviders() {
        DiscoveredEvent event = new DiscoveredEvent(
                "Ticketmaster",
                "tm-1",
                "Concert",
                "",
                Instant.parse("2026-06-11T20:00:00Z"),
                null,
                "https://example.com/concert",
                ""
        );

        when(ticketmaster.isConfigured()).thenReturn(true);
        when(ticketmaster.search("music")).thenReturn(List.of(event));
        when(seatGeek.isConfigured()).thenReturn(false);

        List<DiscoveredEvent> results = discoveryService.search("music");

        assertEquals(List.of("Concert"), titles(results));
        verify(ticketmaster).search("music");
        verify(seatGeek, never()).search(anyString());
    }

    /**
     * Scenario: Discovery service is configured with two providers. Both providers return results. One event from each provider has the same URL, but different start times. The results are deduped by URL and sorted by start time.
     */
    @Test
    void searchDedupesByUrlAndSortsByStartTime() {
        DiscoveredEvent later = new DiscoveredEvent(
                "Ticketmaster",
                "tm-1",
                "Later",
                "",
                Instant.parse("2026-06-12T20:00:00Z"),
                null,
                "https://example.com/later",
                ""
        );
        DiscoveredEvent duplicateUrl = new DiscoveredEvent(
                "SeatGeek",
                "sg-duplicate",
                "Duplicate later",
                "",
                Instant.parse("2026-06-10T20:00:00Z"),
                null,
                "https://example.com/later",
                ""
        );
        DiscoveredEvent earlier = new DiscoveredEvent(
                "SeatGeek",
                "sg-1",
                "Earlier",
                "",
                Instant.parse("2026-06-11T20:00:00Z"),
                null,
                "https://example.com/earlier",
                ""
        );

        when(ticketmaster.isConfigured()).thenReturn(true);
        when(ticketmaster.search("music")).thenReturn(List.of(later));
        when(seatGeek.isConfigured()).thenReturn(true);
        when(seatGeek.search("music")).thenReturn(List.of(duplicateUrl, earlier));

        List<DiscoveredEvent> results = discoveryService.search("music");

        assertEquals(List.of("Earlier", "Later"), titles(results));
    }

    /**
     * Scenario: Discovery service is configured with two providers. Both providers return results. One event from each provider has the same URL, but different start times. The results are deduped by URL and sorted by start time.
     */
    @Test
    void searchFallsBackToSourceAndExternalIdWhenUrlIsMissing() {
        DiscoveredEvent first = new DiscoveredEvent(
                "Ticketmaster",
                "shared-id",
                "First",
                "",
                Instant.parse("2026-06-11T20:00:00Z"),
                null,
                null,
                ""
        );
        DiscoveredEvent duplicate = new DiscoveredEvent(
                "Ticketmaster",
                "shared-id",
                "Duplicate",
                "",
                Instant.parse("2026-06-12T20:00:00Z"),
                null,
                null,
                ""
        );
        DiscoveredEvent sameIdDifferentSource = new DiscoveredEvent(
                "SeatGeek",
                "shared-id",
                "Same id different source",
                "",
                Instant.parse("2026-06-13T20:00:00Z"),
                null,
                null,
                ""
        );

        when(ticketmaster.isConfigured()).thenReturn(true);
        when(ticketmaster.search("music")).thenReturn(List.of(first, duplicate));
        when(seatGeek.isConfigured()).thenReturn(true);
        when(seatGeek.search("music")).thenReturn(List.of(sameIdDifferentSource));

        List<DiscoveredEvent> results = discoveryService.search("music");

        assertEquals(List.of("First", "Same id different source"), titles(results));
    }

    private static List<String> titles(List<DiscoveredEvent> events) {
        return events.stream().map(DiscoveredEvent::title).toList();
    }
}
