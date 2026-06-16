package integration.rest;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ICalControllerIntegrationTest extends IntegrationTestSupport {

    private static final Instant START = Instant.parse("2026-06-11T10:00:00Z");
    private static final Instant END = Instant.parse("2026-06-11T11:00:00Z");

    /** Returns a text/calendar feed for a valid personal iCal token. */
    @Test
    void icalFeedReturnsCalendarForToken() throws Exception {
        User user = userRepository.save(new User("alice", "alice@example.com", "hash"));
        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                START,
                END,
                user
        );
        meeting.addParticipant(new MeetingParticipant(meeting, user, InviteStatus.ACCEPTED));
        meetingRepository.save(meeting);

        mockMvc.perform(get("/ical/" + user.getIcalToken() + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/calendar")))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"meetings.ics\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUMMARY:Planning")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("STATUS:CONFIRMED")));
    }

    /** Returns 404 when the iCal token does not belong to any user. */
    @Test
    void icalFeedReturnsNotFoundForUnknownToken() throws Exception {
        mockMvc.perform(get("/ical/missing.ics"))
                .andExpect(status().isNotFound());
    }
}
