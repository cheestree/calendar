package integration.third;

import com.example.meetings.discover.AgendaLxProvider;
import com.example.meetings.discover.DiscoveredEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaLxProviderIntegrationTest extends ThirdPartyProviderIntegrationTestSupport {
    private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

    /** Maps AgendaLx's public WordPress response, including HTML-stripped descriptions. */
    @Test
    void agendaLxProviderMapsHttpResponseToDiscoveredEvents() throws Exception {
        LocalDate occurrence = LocalDate.now(LISBON).plusYears(1);
        String body = """
                [
                  {
                    "id": 7,
                    "title": { "rendered": "Jazz Lisboa" },
                    "description": ["<p>Live jazz downtown</p>"],
                    "occurences": ["%s"],
                    "string_times": "sab: 21h30",
                    "link": "https://agendalx.example/events/7",
                    "venue": { "1": { "name": "Culturgest" } }
                  }
                ]
                """.formatted(occurrence);

        try (LocalJsonServer server = LocalJsonServer.respondingWith(body)) {
            AgendaLxProvider provider = new AgendaLxProvider();
            replaceHttp(provider, server.baseUrl());

            List<DiscoveredEvent> events = provider.search("jazz");

            assertTrue(server.lastRawQuery().contains("search=jazz"));
            assertEquals(1, events.size());
            DiscoveredEvent event = events.get(0);
            assertEquals("Agenda Cultural de Lisboa", event.source());
            assertEquals("7", event.externalId());
            assertEquals("Jazz Lisboa", event.title());
            assertEquals("Live jazz downtown", event.description());
            assertEquals(occurrence.atTime(21, 30).atZone(LISBON).toInstant(), event.start());
            assertEquals("https://agendalx.example/events/7", event.url());
            assertEquals("Culturgest", event.venue());
        }
    }
}
