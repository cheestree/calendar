package e2e;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingFlowE2ETest extends E2ETestSupport {

    private static final String PASSWORD = "password";

    @Autowired
    private UserService userService;

    @Autowired
    private MeetingService meetingService;

    @Test
    void userCanRegister() {
        register("alice", "alice@example.com");

        inTransaction(() -> assertTrue(userRepository.existsByUsername("alice")));
    }

    @Test
    void signedInUserCanProposeMeeting() {
        createUser("alice", "alice@example.com");
        createUser("bob", "bob@example.com");

        signIn("alice");
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

        signIn("bob");
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

    private void register(String username, String email) {
        browser.get(url("/register"));
        browser.findElement(By.id("username")).sendKeys(username);
        browser.findElement(By.id("email")).sendKeys(email);
        browser.findElement(By.id("password")).sendKeys(PASSWORD);
        browser.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals("Login", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Account created"));
    }

    private void signIn(String username) {
        browser.get(url("/login"));
        browser.findElement(By.id("username")).sendKeys(username);
        browser.findElement(By.id("password")).sendKeys(PASSWORD);
        browser.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals("Calendar", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Signed in as"));
    }

    private User createUser(String username, String email) {
        return userService.register(username, email, PASSWORD);
    }

    private void setDateTime(String fieldId, String value) {
        WebElement field = browser.findElement(By.id(fieldId));
        ((JavascriptExecutor) browser).executeScript("arguments[0].value = arguments[1];", field, value);
    }
}
