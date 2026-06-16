package e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthE2ETest extends E2ETestSupport {

    /**
     * Scenario: Alice registers. She fills in the form, and submits it.
     */
    @Test
    void userCanRegister() {
        register("alice", "alice@example.com");

        inTransaction(() -> assertTrue(userRepository.existsByUsername("alice")));
    }

    /**
     * Scenario: Alice registers twice with the same username. She fills in the form, and submits it.
     * It throws an error because the username is already taken.
     */
    @Test
    void duplicateRegistrationShowsErrorAndDoesNotCreateSecondUser() {
        createUser("alice", "alice@example.com");

        browser.get(url("/register"));
        browser.findElement(By.id("username")).sendKeys("alice");
        browser.findElement(By.id("email")).sendKeys("alice2@example.com");
        browser.findElement(By.id("password")).sendKeys(PASSWORD);
        browser.findElement(By.cssSelector("button[type='submit']")).click();

        assertEquals("Register", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Username already taken"));
        assertEquals("alice", browser.findElement(By.id("username")).getAttribute("value"));
        assertEquals("alice2@example.com", browser.findElement(By.id("email")).getAttribute("value"));
        inTransaction(() -> assertEquals(1, userRepository.findAll().size()));
    }

    /**
     * Scenario: Alice registers and tries to sign in with the wrong password. She fills in the form, and submits it.
     * It throws an error because the username or the password is incorrect.
     */
    @Test
    void invalidLoginShowsErrorAndDoesNotAuthenticateUser() {
        createUser("alice", "alice@example.com");

        signIn("alice", "wrong-password");

        assertEquals("Login", browser.getTitle());
        assertTrue(browser.getPageSource().contains("Invalid username or password"));
        assertFalse(browser.getPageSource().contains("Signed in as"));
    }

    /**
     * Scenario: Alice tries to access a protected page without signing in. She is redirected to the login page.
     */
    @Test
    void protectedPageRedirectsAnonymousUserToLogin() {
        browser.get(url("/meetings/new"));

        assertEquals("Login", browser.getTitle());
        assertTrue(browser.getCurrentUrl().contains("/login"));
    }
}
