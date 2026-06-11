package integration.rest;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    @WithMockUser(username = "alice")
    void calendarGetReturnsCalendarViewWithMeetingsInvitesAndIcalLinks() throws Exception {
        User alice = userRepository.save(new User("alice", "alice@example.com", "hash"));
        User bob = userRepository.save(new User("bob", "bob@example.com", "hash"));

        Meeting acceptedMeeting = new Meeting(
                "Planning",
                "Sprint planning",
                Instant.parse("2026-06-11T10:00:00Z"),
                Instant.parse("2026-06-11T11:00:00Z"),
                alice
        );
        acceptedMeeting.addParticipant(new MeetingParticipant(acceptedMeeting, alice, InviteStatus.ACCEPTED));
        Meeting pendingInvite = new Meeting(
                "Design Review",
                "Mockup review",
                Instant.parse("2026-06-12T10:00:00Z"),
                Instant.parse("2026-06-12T11:00:00Z"),
                bob
        );
        pendingInvite.addParticipant(new MeetingParticipant(pendingInvite, bob, InviteStatus.ACCEPTED));
        pendingInvite.addParticipant(new MeetingParticipant(pendingInvite, alice, InviteStatus.PENDING));
        meetingRepository.saveAll(List.of(acceptedMeeting, pendingInvite));

        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("calendar", result.getModelAndView().getViewName()))
                .andExpect(model().attribute("user", org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is(alice.getId()))))
                .andExpect(model().attribute("user", org.hamcrest.Matchers.hasProperty("username", org.hamcrest.Matchers.is("alice"))))
                .andExpect(model().attribute("icalHttpUrl", "http://test.local/ical/" + alice.getIcalToken() + ".ics"))
                .andExpect(model().attribute("icalWebcalUrl", "webcal://test.local/ical/" + alice.getIcalToken() + ".ics"))
                .andExpect(model().attribute("meetings", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(model().attribute("pendingInvites", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("pendingInvites", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.hasProperty("status", org.hamcrest.Matchers.is(InviteStatus.PENDING)))))
                .andExpect(model().attribute("meetings", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.hasProperty("title", org.hamcrest.Matchers.is("Planning")))))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Planning")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Design Review")));
    }
}
