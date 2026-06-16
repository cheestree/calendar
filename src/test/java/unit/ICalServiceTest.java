package unit;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.service.ICalService;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.Temporal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ICalServiceTest {

    private final ICalService icalService = new ICalService();

    private static final Instant START = Instant.parse("2026-06-11T10:00:00Z");
    private static final Instant END = Instant.parse("2026-06-11T11:00:00Z");

    /**
     * Scenario: User bob has a meeting with organizer alice.
     */
    @Test
    void renderIncludesCalendarAndMeetingFields() {
        User owner = new User("bob", "bob@example.com", "hash");
        User organizer = new User("alice", "alice@example.com", "hash");

        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                START,
                END,
                organizer
        );
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.PENDING));

        String ics = icalService.render(owner, List.of(meeting));

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.contains("VERSION:2.0\r\n"));
        assertTrue(ics.contains("X-WR-CALNAME:bob's meetings\r\n"));
        assertTrue(ics.contains("BEGIN:VEVENT\r\n"));
        assertTrue(ics.contains("DTSTART:20260611T100000Z\r\n"));
        assertTrue(ics.contains("DTEND:20260611T110000Z\r\n"));
        assertTrue(ics.contains("SUMMARY:Planning\r\n"));
        assertTrue(ics.contains("DESCRIPTION:Sprint planning\r\n"));
        assertTrue(ics.contains("ORGANIZER;CN=alice:mailto:alice@example.com\r\n"));
        assertTrue(ics.contains("ATTENDEE;CN=bob;PARTSTAT=NEEDS-ACTION:mailto:bob@example.com\r\n"));
        assertTrue(ics.contains("STATUS:TENTATIVE\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
    }

    /**
     * Scenario: User bob has a meeting with organizer alice. The meeting title and description contain special characters that need to be escaped in iCalendar format.
     */
    @Test
    void renderEscapesIcalTextValues() {
        User owner = new User("bo,b", "bob@example.com", "hash");
        Meeting meeting = new Meeting(
                "Plan; review, notes",
                "Line 1\nLine 2 \\ done",
                START,
                END,
                owner
        );
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String ics = icalService.render(owner, List.of(meeting));

        assertTrue(ics.contains("X-WR-CALNAME:bo\\,b's meetings\r\n"));
        assertTrue(ics.contains("SUMMARY:Plan\\; review\\, notes\r\n"));
        assertTrue(ics.contains("DESCRIPTION:Line 1\\nLine 2 \\\\ done\r\n"));
        assertTrue(ics.contains("STATUS:CONFIRMED\r\n"));
    }

    /**
     * Scenario: User bob has a meeting with organizer alice. The organizer and attendee have no email address.
     */
    @Test
    void renderEscapesNullEmailAsEmptyValue() {
        User owner = new User("bob", null, "hash");
        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                START,
                END,
                owner
        );
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String ics = icalService.render(owner, List.of(meeting));

        assertTrue(ics.contains("ORGANIZER;CN=bob:mailto:\r\n"));
        assertTrue(ics.contains("ATTENDEE;CN=bob;PARTSTAT=ACCEPTED:mailto:\r\n"));
    }

    /**
     * Scenario: User bob has a meeting with organizer alice. The organizer and attendee have no email address.
     */
    @Test
    void renderMapsDeclinedParticipantStatus() {
        User owner = new User("bob", "bob@example.com", "hash");
        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                START,
                END,
                owner
        );
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.DECLINED));

        String ics = icalService.render(owner, List.of(meeting));

        assertTrue(ics.contains("ATTENDEE;CN=bob;PARTSTAT=DECLINED:mailto:bob@example.com\r\n"));
    }

    /**
     * Scenario: User bob has a meeting with organizer alice. The meeting description is null or blank.
     */
    @Test
    void renderOmitsDescriptionWhenNullOrBlank() {
        User owner = new User("bob", "bob@example.com", "hash");
        Meeting nullDescription = new Meeting(
                "Planning without description",
                null,
                START,
                END,
                owner
        );
        Meeting blankDescription = new Meeting(
                "Planning with blank description",
                "   ",
                START.plus(Period.ofDays(1)),
                END.plus(Period.ofDays(1)),
                owner
        );

        String ics = icalService.render(owner, List.of(nullDescription, blankDescription));

        assertFalse(ics.contains("DESCRIPTION:"));
    }
}
