package unit;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {
    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeetingService meetingService;

    /**
     * Scenario: Organizer proposes meeting with invitees
     */
    @Test
    void proposeCreatesMeetingWithAcceptedOrganizerAndPendingInvitees() {
        User organizer = new User("alice", "alice@example.com", "hash");
        User bob = new User("bob", "bob@example.com", "hash");
        User clara = new User("clara", "clara@example.com", "hash");

        Instant start = Instant.parse("2026-06-11T10:00:00Z");
        Instant end = Instant.parse("2026-06-11T11:00:00Z");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("clara")).thenReturn(Optional.of(clara));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.propose(
                organizer,
                "Planning",
                "Sprint planning",
                start,
                end,
                List.of("bob", "clara", "bob", "alice", " ")
        );

        assertEquals("Planning", meeting.getTitle());
        assertEquals("Sprint planning", meeting.getDescription());
        assertEquals(start, meeting.getStartTime());
        assertEquals(end, meeting.getEndTime());
        assertEquals(organizer, meeting.getOrganizer());

        assertEquals(3, meeting.getParticipants().size());

        assertParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        assertParticipant(meeting, bob, InviteStatus.PENDING);
        assertParticipant(meeting, clara, InviteStatus.PENDING);

        assertFalse(meeting.isConfirmed());
    }

    /**
     * Scenario: Organizer proposes meeting with invalid time
     */
    @Test
    void proposeThrowsWhenEndIsNotAfterStart() {
        User organizer = new User("alice", "alice@example.com", "hash");
        Instant start = Instant.parse("2026-06-11T10:00:00Z");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> meetingService.propose(
                        organizer,
                        "Bad meeting",
                        "Invalid time",
                        start,
                        start,
                        List.of()
                )
        );

        assertEquals("End time must be after start time", error.getMessage());
    }

    /**
     * Scenario: Organizer proposes meeting with invitee that doesn't exist
     */
    @Test
    void proposeThrowsWhenInviteeDoesNotExist() {
        User organizer = new User("alice", "alice@example.com", "hash");
        Instant start = Instant.parse("2026-06-11T10:00:00Z");
        Instant end = Instant.parse("2026-06-11T11:00:00Z");

        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> meetingService.propose(
                        organizer,
                        "Planning",
                        "Sprint planning",
                        start,
                        end,
                        List.of("missing")
                )
        );

        assertEquals("Unknown invitee: missing", error.getMessage());
    }

    private static void assertParticipant(Meeting meeting, User user, InviteStatus status) {
        assertTrue(meeting.getParticipants().stream()
                .anyMatch(participant ->
                        participant.getUser() == user &&
                                participant.getStatus() == status));
    }

    /**
     * Scenario: Invitee responds to meeting invite
     */
    @Test
    void respondAcceptsInvite() {
        User user = new User("bob", "bob@example.com", "hash");
        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                Instant.parse("2026-06-11T10:00:00Z"),
                Instant.parse("2026-06-11T11:00:00Z"),
                new User("alice", "alice@example.com", "hash")
        );
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);

        when(participantRepository.findByMeetingIdAndUserId(eq(1L), nullable(Long.class)))
                .thenReturn(Optional.of(participant));

        meetingService.respond(1L, user, InviteStatus.ACCEPTED);

        assertEquals(InviteStatus.ACCEPTED, participant.getStatus());
    }

    /**
     * Scenario: Invitee responds to meeting invite with invalid status
     */
    @Test
    void respondSaysPending() {
        User user = new User("bob", "bob@example.com", "hash");
        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                Instant.parse("2026-06-11T10:00:00Z"),
                Instant.parse("2026-06-11T11:00:00Z"),
                new User("alice", "alice@example.com", "hash")
        );
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> meetingService.respond(1L, user, InviteStatus.PENDING)
        );

        assertEquals("Response must be ACCEPTED or DECLINED", error.getMessage());
    }

    /**
     * Scenario: User requests calendar for themselves
     */
    @Test
    void calendarForUserSucceeds() {
        User user = new User("bob", "bob@example.com", "hash");
        Meeting first = new Meeting(
                "Planning",
                "Sprint planning",
                Instant.parse("2026-06-11T10:00:00Z"),
                Instant.parse("2026-06-11T11:00:00Z"),
                new User("alice", "alice@example.com", "hash")
        );
        Meeting second = new Meeting(
                "Retrospective",
                "Sprint retrospective",
                Instant.parse("2026-06-12T10:00:00Z"),
                Instant.parse("2026-06-12T11:00:00Z"),
                user
        );
        List<Meeting> calendar = List.of(first, second);

        when(meetingRepository.findCalendarMeetings(user)).thenReturn(calendar);

        List<Meeting> result = meetingService.calendarFor(user);

        assertSame(calendar, result);
        verify(meetingRepository).findCalendarMeetings(user);
    }

    /**
     * Scenario: User requests pending invites for themselves
     */
    @Test
    void pendingInvitesForUserSucceeds() {
        User user = new User("bob", "bob@example.com", "hash");
        Meeting meeting = new Meeting(
                "Planning",
                "Sprint planning",
                Instant.parse("2026-06-11T10:00:00Z"),
                Instant.parse("2026-06-11T11:00:00Z"),
                new User("alice", "alice@example.com", "hash")
        );
        MeetingParticipant pendingInvite = new MeetingParticipant(meeting, user, InviteStatus.PENDING);
        List<MeetingParticipant> pendingInvites = List.of(pendingInvite);

        when(participantRepository.findByUserAndStatus(user, InviteStatus.PENDING)).thenReturn(pendingInvites);

        List<MeetingParticipant> result = meetingService.pendingInvitesFor(user);

        assertSame(pendingInvites, result);
        verify(participantRepository).findByUserAndStatus(user, InviteStatus.PENDING);
    }

    /**
     * Scenario: User discovers an event and creates a meeting from it
     */
    @Test
    void copyFromDiscoveredCreatesAcceptedUserMeeting() {
        User user = new User("bob", "bob@example.com", "hash");
        Instant start = Instant.parse("2026-06-11T20:00:00Z");
        Instant end = Instant.parse("2026-06-11T22:30:00Z");
        DiscoveredEvent event = new DiscoveredEvent(
                "Ticketmaster",
                "tm-123",
                "Jazz Night",
                "Live trio",
                start,
                end,
                "https://example.com/events/tm-123",
                "Blue Room"
        );

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(user, event);

        assertEquals("Jazz Night", meeting.getTitle());
        assertEquals("Live trio\n\nVenue: Blue Room\nSource: Ticketmaster (https://example.com/events/tm-123)", meeting.getDescription());
        assertEquals(start, meeting.getStartTime());
        assertEquals(end, meeting.getEndTime());
        assertEquals(user, meeting.getOrganizer());
        assertEquals(1, meeting.getParticipants().size());
        assertParticipant(meeting, user, InviteStatus.ACCEPTED);
        assertTrue(meeting.isConfirmed());
        verify(meetingRepository).save(meeting);
    }

    /**
     * Scenario: User discovers an event with missing end time and creates meeting from it
     */
    @Test
    void copyFromDiscoveredDefaultsEndTimeWhenMissing() {
        User user = new User("bob", "bob@example.com", "hash");
        Instant start = Instant.parse("2026-06-11T20:00:00Z");
        DiscoveredEvent event = new DiscoveredEvent(
                "SeatGeek",
                "sg-123",
                "Basketball",
                null,
                start,
                null,
                null,
                ""
        );

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(user, event);

        assertEquals(start.plusSeconds(7200), meeting.getEndTime());
        assertEquals("Source: SeatGeek", meeting.getDescription());
        assertParticipant(meeting, user, InviteStatus.ACCEPTED);
        verify(meetingRepository).save(meeting);
    }
}
