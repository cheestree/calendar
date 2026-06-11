package integration.rest;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MeetingControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    @WithMockUser(username = "alice")
    void proposeMeetingCreatesOrganizerAndInviteeParticipants() throws Exception {
        User alice = userRepository.save(new User("alice", "alice@example.com", "hash"));
        User bob = userRepository.save(new User("bob", "bob@example.com", "hash"));

        mockMvc.perform(post("/meetings/new")
                        .with(csrf())
                        .param("title", "Planning")
                        .param("description", "Sprint planning")
                        .param("start", "2026-06-11T10:00")
                        .param("end", "2026-06-11T11:00")
                        .param("invitees", "bob"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        Meeting meeting = meetingRepository.findAll().get(0);
        assertEquals("Planning", meeting.getTitle());
        assertEquals(alice.getId(), meeting.getOrganizer().getId());
        assertTrue(participantRepository.findAll().stream()
                .anyMatch(participant ->
                        participant.getUser().getId().equals(alice.getId())
                                && participant.getStatus() == InviteStatus.ACCEPTED));
        assertTrue(participantRepository.findAll().stream()
                .anyMatch(participant ->
                        participant.getUser().getId().equals(bob.getId())
                                && participant.getStatus() == InviteStatus.PENDING));
    }

    @Test
    @WithMockUser(username = "alice")
    void proposeFormGetReturnsForm() throws Exception {
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("propose", result.getModelAndView().getViewName()));
    }

    @Test
    @WithMockUser(username = "bob")
    void respondPostRedirectsToCalendar() throws Exception {
        User alice = userRepository.save(new User("alice", "alice@example.com", "hash"));
        User bob = userRepository.save(new User("bob", "bob@example.com", "hash"));
        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                Instant.parse("2026-06-11T10:00:00Z"),
                Instant.parse("2026-06-11T11:00:00Z"),
                alice
        );
        meeting.addParticipant(new MeetingParticipant(meeting, alice, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.PENDING));
        Meeting savedMeeting = meetingRepository.save(meeting);

        mockMvc.perform(post("/meetings/" + savedMeeting.getId() + "/respond")
                        .with(csrf())
                        .param("action", "accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        assertEquals(InviteStatus.ACCEPTED,
                participantRepository.findByMeetingIdAndUserId(savedMeeting.getId(), bob.getId())
                        .orElseThrow()
                        .getStatus());
    }

    @Test
    @WithMockUser(username = "alice")
    void proposeMeetingWithEndBeforeStartReturnsFormWithError() throws Exception {
        userRepository.save(new User("alice", "alice@example.com", "hash"));

        mockMvc.perform(post("/meetings/new")
                        .with(csrf())
                        .param("title", "Planning")
                        .param("description", "Sprint planning")
                        .param("start", "2026-06-11T11:00")
                        .param("end", "2026-06-11T10:00")
                        .param("invitees", ""))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("propose", result.getModelAndView().getViewName()))
                .andExpect(model().attribute("error", "End time must be after start time"));
    }
}
