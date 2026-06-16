package integration.rest;

import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Registers a new user, redirects to login, and stores a hashed password. */
    @Test
    void registerCreatesUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("email", "alice@example.com")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User user = userRepository.findByUsername("alice").orElseThrow();
        assertEquals("alice@example.com", user.getEmail());
        assertNotEquals("password", user.getPasswordHash());
    }

    /** Rejects duplicate usernames and returns the form with the submitted values preserved. */
    @Test
    void registerDuplicateUsernameReturnsFormWithError() throws Exception {
        userRepository.save(new User("alice", "alice@example.com", "hash"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "alice")
                        .param("email", "new-alice@example.com")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("register", result.getModelAndView().getViewName()))
                .andExpect(model().attribute("error", "Username already taken"))
                .andExpect(model().attribute("username", "alice"))
                .andExpect(model().attribute("email", "new-alice@example.com"));
    }

    /** Serves the registration form. */
    @Test
    void registerGetReturnsForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("register", result.getModelAndView().getViewName()));
    }

    /** Serves the login form. */
    @Test
    void loginGetReturnsForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("login", result.getModelAndView().getViewName()));
    }

    /** Authenticates a registered user and redirects to the calendar. */
    @Test
    void loginPostWithValidCredentialsRedirectsToCalendar() throws Exception {
        userRepository.save(new User("alice", "alice@example.com", passwordEncoder.encode("password")));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    /** Rejects invalid login credentials and redirects back to the login page with an error. */
    @Test
    void loginPostWithInvalidCredentialsRedirectsToLoginError() throws Exception {
        userRepository.save(new User("alice", "alice@example.com", passwordEncoder.encode("password")));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    /** Rejects registration posts without a CSRF token. */
    @Test
    void registerPostWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "alice")
                        .param("email", "alice@example.com")
                        .param("password", "password"))
                .andExpect(status().isForbidden());

        assertEquals(0, userRepository.count());
    }

    /** Rejects login posts without a CSRF token. */
    @Test
    void loginPostWithoutCsrfIsForbidden() throws Exception {
        userRepository.save(new User("alice", "alice@example.com", passwordEncoder.encode("password")));

        mockMvc.perform(post("/login")
                        .param("username", "alice")
                        .param("password", "password"))
                .andExpect(status().isForbidden());
    }

    /** Redirects the root route to the authenticated calendar entry point. */
    @Test
    void rootGetRedirectsToCalendar() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }
}
