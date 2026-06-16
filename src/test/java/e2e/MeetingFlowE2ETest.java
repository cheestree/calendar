package e2e;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingFlowE2ETest extends E2ETestSupport {


    @Autowired
    private MeetingService meetingService;

    /**
     * Scenario: Alice proposes a meeting to Bob. Alice signs in, proposes a meeting, fills in the form, and submits it.
     */
    @Test
    void signedInUserCanProposeMeeting() {
        createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");

        signIn("alice", PASSWORD);
        browser.findElement(By.linkText("Propose a meeting")).click();
        browser.findElement(By.id("title")).sendKeys("Planning");
        browser.findElement(By.id("description")).sendKeys("Sprint planning");
        setDateTime("start", "2026-07-01T10:00");
        setDateTime("end", "2026-07-01T11:00");
        browser.findElement(By.id("invitees")).sendKeys("bob");
        browser.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        assertEquals("Calendar", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Planning"));
        assertTrue(browser.getPageSource().contains("tentative"));

        inTransaction(() -> {
            Meeting meeting = meetingRepository.findAll().get(0);
            List<MeetingParticipant> participants = participantRepository.findAll();
            assertEquals("Planning", meeting.getTitle());
            assertTrue(participants.stream()
                    .anyMatch(participant -> participant.getUser().getUsername().equals("alice")
                            && participant.getStatus() == InviteStatus.ACCEPTED));
            assertTrue(participants.stream()
                    .anyMatch(participant -> participant.getUser().getUsername().equals("bob")
                            && participant.getStatus() == InviteStatus.PENDING));
        });
    }

    /**
     * Scenario: Alice proposes a meeting to missing-user. Alice signs in, proposes a meeting, fills in the form, and submits it.
     * It throws an error because missing-user is not a known user.
     */
    @Test
    void proposingMeetingWithUnknownInviteeShowsErrorAndDoesNotCreateMeeting() {
        createUser("alice", "alice@example.com");

        signIn("alice", PASSWORD);
        assertEquals("Calendar", browser.getTitle());
        browser.findElement(By.linkText("Propose a meeting")).click();
        browser.findElement(By.id("title")).sendKeys("Planning");
        browser.findElement(By.id("description")).sendKeys("Sprint planning");
        setDateTime("start", "2026-07-01T10:00");
        setDateTime("end", "2026-07-01T11:00");
        browser.findElement(By.id("invitees")).sendKeys("missing-user");
        browser.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        assertEquals("Propose a meeting", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Unknown invitee: missing-user"));
        assertEquals("Planning", browser.findElement(By.id("title")).getAttribute("value"));
        assertEquals("missing-user", browser.findElement(By.id("invitees")).getAttribute("value"));
        inTransaction(() -> assertEquals(0, meetingRepository.findAll().size()));
    }

    /**
     * Scenario: Alice proposes a meeting with an end time before the start time. Alice signs in, proposes a meeting, fills in the form, and submits it.
     * It throws an error because the end time is before the start time.
     */
    @Test
    void proposingMeetingWithEndBeforeStartShowsErrorAndDoesNotCreateMeeting() {
        createUser("alice", "alice@example.com");

        signIn("alice", PASSWORD);
        assertEquals("Calendar", browser.getTitle());
        browser.findElement(By.linkText("Propose a meeting")).click();
        browser.findElement(By.id("title")).sendKeys("Planning");
        setDateTime("start", "2026-07-01T11:00");
        setDateTime("end", "2026-07-01T10:00");
        browser.findElement(By.cssSelector("form[action='/meetings/new'] button[type='submit']")).click();

        assertEquals("Propose a meeting", browser.getTitle());
        assertTrue(browser.getPageSource().contains("End time must be after start time"));
        inTransaction(() -> assertEquals(0, meetingRepository.findAll().size()));
    }

    /**
     * Scenario: Bob is invited to a meeting proposed by Alice. Bob signs in, sees the meeting, and accepts it.
     */
    @Test
    void inviteeCanAcceptPendingInvite() {
        User alice = createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");
        meetingService.propose(
                alice,
                "Planning",
                "Sprint planning",
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T11:00:00Z"),
                List.of("bob"));

        signIn("bob", PASSWORD);
        assertTrue(browser.getPageSource().contains("Pending invites"));
        assertTrue(browser.getPageSource().contains("Planning"));
        browser.findElement(By.cssSelector("form[action$='/respond'] button[type='submit']")).click();

        assertEquals("Calendar", browser.getTitle());
        assertTrue(browser.getPageSource().contains("confirmed"));

        inTransaction(() -> {
            Meeting meeting = meetingRepository.findAll().get(0);
            List<MeetingParticipant> participants = participantRepository.findAll();
            assertEquals("Planning", meeting.getTitle());
            assertTrue(participants.stream()
                    .anyMatch(participant -> participant.getUser().getUsername().equals("bob")
                            && participant.getStatus() == InviteStatus.ACCEPTED));
            assertTrue(participants.stream()
                    .allMatch(participant -> participant.getStatus() == InviteStatus.ACCEPTED));
        });
    }

    /**
     * Scenario: Bob is invited to a meeting proposed by Alice. Bob signs in, sees the meeting, and declines it.
     */
    @Test
    void inviteeCanDeclinePendingInvite() {
        User alice = createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");
        meetingService.propose(
                alice,
                "Planning",
                "Sprint planning",
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T11:00:00Z"),
                List.of("bob"));

        signIn("bob", PASSWORD);
        assertEquals("Calendar", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Pending invites"));
        assertTrue(browser.getPageSource().contains("Planning"));
        browser.findElement(By.cssSelector("form[action$='/respond'] button.danger")).click();

        assertEquals("Calendar", browser.getTitle());
        assertFalse(browser.getPageSource().contains("Pending invites"));
        inTransaction(() -> {
            List<MeetingParticipant> participants = participantRepository.findAll();
            assertTrue(participants.stream()
                    .anyMatch(participant -> participant.getUser().getUsername().equals("bob")
                            && participant.getStatus() == InviteStatus.DECLINED));
        });
    }
}
