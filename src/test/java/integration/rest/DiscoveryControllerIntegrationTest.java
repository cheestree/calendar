package integration.rest;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.discover.EventProvider;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(DiscoveryControllerIntegrationTest.DiscoveryTestConfig.class)
class DiscoveryControllerIntegrationTest extends IntegrationTestSupport {

    /** Serves the discovery page with provider status and no results before a search is submitted. */
    @Test
    @WithMockUser(username = "alice")
    void discoverGetWithoutQueryReturnsPageWithProvidersAndEmptyResults() throws Exception {
        mockMvc.perform(get("/discover"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("discover", result.getModelAndView().getViewName()))
                .andExpect(model().attribute("q", ""))
                .andExpect(model().attribute("anyConfigured", true))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.empty()))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Fake Events")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("configured")));
    }

    /** Searches configured providers and renders the returned third-party events. */
    @Test
    @WithMockUser(username = "alice")
    void discoverGetWithQueryReturnsSearchResults() throws Exception {
        mockMvc.perform(get("/discover").param("q", "jazz"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("discover", result.getModelAndView().getViewName()))
                .andExpect(model().attribute("q", "jazz"))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(result -> {
                    @SuppressWarnings("unchecked")
                    List<DiscoveredEvent> results =
                            (List<DiscoveredEvent>) result.getModelAndView().getModel().get("results");
                    assertEquals("Lisbon Jazz Night", results.get(0).title());
                })
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Lisbon Jazz Night")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Hot Clube")));
    }

    /** Copies a discovered event into the authenticated user's calendar. */
    @Test
    @WithMockUser(username = "alice")
    void copyDiscoveredEventCreatesMeetingAndRedirectsToCalendar() throws Exception {
        User alice = userRepository.save(new User("alice", "alice@example.com", "hash"));

        mockMvc.perform(post("/discover/copy")
                        .with(csrf())
                        .param("source", "Fake Events")
                        .param("externalId", "fake-1")
                        .param("title", "Lisbon Jazz Night")
                        .param("description", "Late set")
                        .param("start", "2026-07-01T20:00:00Z")
                        .param("end", "2026-07-01T22:00:00Z")
                        .param("url", "https://events.example/fake-1")
                        .param("venue", "Hot Clube"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        Meeting meeting = meetingRepository.findAll().get(0);
        assertEquals("Lisbon Jazz Night", meeting.getTitle());
        assertEquals(alice.getId(), meeting.getOrganizer().getId());
        assertEquals(Instant.parse("2026-07-01T20:00:00Z"), meeting.getStartTime());
        assertEquals(Instant.parse("2026-07-01T22:00:00Z"), meeting.getEndTime());
        assertTrue(meeting.getDescription().contains("Late set"));
        assertTrue(meeting.getDescription().contains("Venue: Hot Clube"));
        assertTrue(meeting.getDescription().contains("Source: Fake Events"));
        assertTrue(participantRepository.findAll().stream()
                .anyMatch(participant ->
                        participant.getUser().getId().equals(alice.getId())
                                && participant.getStatus() == InviteStatus.ACCEPTED));
    }

    @TestConfiguration
    static class DiscoveryTestConfig {
        @Bean
        @Primary
        DiscoveryService discoveryService() {
            return new DiscoveryService(List.of(new FakeEventProvider()));
        }
    }

    private static class FakeEventProvider implements EventProvider {
        @Override
        public String name() {
            return "Fake Events";
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public List<DiscoveredEvent> search(String query) {
            return List.of(new DiscoveredEvent(
                    name(),
                    "fake-1",
                    "Lisbon Jazz Night",
                    "Late set",
                    Instant.parse("2026-07-01T20:00:00Z"),
                    Instant.parse("2026-07-01T22:00:00Z"),
                    "https://events.example/fake-1",
                    "Hot Clube"));
        }
    }
}
