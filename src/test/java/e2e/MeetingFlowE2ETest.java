package e2e;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingFlowE2ETest extends E2ETestSupport {

    /** Full browser flow: register users, propose a meeting, and accept the invite. */
    @Test
    void userCanRegisterProposeMeetingAndInviteeCanAccept() {
        register("alice", "alice@example.com");
        register("bob", "bob@example.com");

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
        signOut();

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
        browser.findElement(By.id("password")).sendKeys("password");
        browser.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals("Login", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Account created"));
    }

    private void signIn(String username) {
        browser.get(url("/login"));
        browser.findElement(By.id("username")).sendKeys(username);
        browser.findElement(By.id("password")).sendKeys("password");
        browser.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals("Calendar", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Signed in as"));
    }

    private void signOut() {
        browser.findElement(By.cssSelector("form[action='/logout'] button[type='submit']")).click();

        assertEquals("Login", browser.getTitle());
        assertTrue(browser.getPageSource().contains("You have been signed out"));
    }

    private void setDateTime(String fieldId, String value) {
        WebElement field = browser.findElement(By.id(fieldId));
        ((JavascriptExecutor) browser).executeScript("arguments[0].value = arguments[1];", field, value);
    }
}
