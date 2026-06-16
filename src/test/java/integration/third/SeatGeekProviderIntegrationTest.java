package integration.third;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.SeatGeekProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeatGeekProviderIntegrationTest extends ThirdPartyProviderIntegrationTestSupport {

    /** Maps SeatGeek's public event response, including UTC datetimes without a zone designator. */
    @Test
    void seatGeekProviderMapsHttpResponseToDiscoveredEvents() throws Exception {
        LocalDate occurrence = LocalDate.now(ZoneOffset.UTC).plusYears(1);
        String body = """
                {
                  "events": [
                    {
                      "id": 42,
                      "title": "Benfica v Porto",
                      "short_title": "Benfica",
                      "description": "Liga match",
                      "datetime_utc": "%sT20:30:00",
                      "url": "https://seatgeek.example/events/42",
                      "venue": { "name": "Estadio da Luz" }
                    }
                  ]
                }
                """.formatted(occurrence);

        try (LocalJsonServer server = LocalJsonServer.respondingWith(body)) {
            SeatGeekProvider provider = new SeatGeekProvider("client-123");
            replaceHttp(provider, server.baseUrl());

            List<DiscoveredEvent> events = provider.search("benfica");

            assertTrue(server.lastRawQuery().contains("q=benfica"));
            assertTrue(server.lastRawQuery().contains("client_id=client-123"));
            assertEquals(1, events.size());
            DiscoveredEvent event = events.get(0);
            assertEquals("SeatGeek", event.source());
            assertEquals("42", event.externalId());
            assertEquals("Benfica v Porto", event.title());
            assertEquals("Liga match", event.description());
            assertEquals(occurrence.atTime(20, 30).toInstant(ZoneOffset.UTC), event.start());
            assertEquals("https://seatgeek.example/events/42", event.url());
            assertEquals("Estadio da Luz", event.venue());
        }
    }
}
