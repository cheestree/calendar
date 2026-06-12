package integration.third;

import com.example.meetings.discover.AgendaLxProvider;
import com.example.meetings.discover.DiscoveredEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgendaLxProviderIntegrationTest extends ThirdPartyProviderIntegrationTestSupport {

    /** Maps AgendaLx's public WordPress response, including HTML-stripped descriptions. */
    @Test
    void agendaLxProviderMapsHttpResponseToDiscoveredEvents() throws Exception {
        String body = """
                [
                  {
                    "id": 7,
                    "title": { "rendered": "Jazz Lisboa" },
                    "description": ["<p>Live jazz downtown</p>"],
                    "occurences": ["2026-06-13"],
                    "string_times": "sab: 21h30",
                    "link": "https://agendalx.example/events/7",
                    "venue": { "1": { "name": "Culturgest" } }
                  }
                ]
                """;

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
            assertEquals(Instant.parse("2026-06-13T20:30:00Z"), event.start());
            assertEquals("https://agendalx.example/events/7", event.url());
            assertEquals("Culturgest", event.venue());
        }
    }
}
