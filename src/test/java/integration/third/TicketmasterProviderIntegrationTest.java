package integration.third;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.TicketmasterProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketmasterProviderIntegrationTest extends ThirdPartyProviderIntegrationTestSupport {

    /** Maps Ticketmaster's nested _embedded response, including event venues. */
    @Test
    void ticketmasterProviderMapsHttpResponseToDiscoveredEvents() throws Exception {
        LocalDate occurrence = LocalDate.now(ZoneOffset.UTC).plusYears(1);
        String body = """
                {
                  "_embedded": {
                    "events": [
                      {
                        "id": "tm-9",
                        "name": "Rock Lisboa",
                        "info": "Outdoor concert",
                        "url": "https://ticketmaster.example/events/tm-9",
                        "dates": {
                          "start": { "dateTime": "%sT19:00:00Z" }
                        },
                        "_embedded": {
                          "venues": [
                            { "name": "Campo Pequeno" }
                          ]
                        }
                      }
                    ]
                  }
                }
                """.formatted(occurrence);

        try (LocalJsonServer server = LocalJsonServer.respondingWith(body)) {
            TicketmasterProvider provider = new TicketmasterProvider("api-key-123", "PT");
            replaceHttp(provider, server.baseUrl());

            List<DiscoveredEvent> events = provider.search("rock");

            assertTrue(server.lastRawQuery().contains("keyword=rock"));
            assertTrue(server.lastRawQuery().contains("apikey=api-key-123"));
            assertTrue(server.lastRawQuery().contains("countryCode=PT"));
            assertEquals(1, events.size());
            DiscoveredEvent event = events.get(0);
            assertEquals("Ticketmaster", event.source());
            assertEquals("tm-9", event.externalId());
            assertEquals("Rock Lisboa", event.title());
            assertEquals("Outdoor concert", event.description());
            assertEquals(occurrence.atTime(19, 0).toInstant(ZoneOffset.UTC), event.start());
            assertEquals("https://ticketmaster.example/events/tm-9", event.url());
            assertEquals("Campo Pequeno", event.venue());
        }
    }
}
