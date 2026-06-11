package integration.rest;

import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

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

    @Test
    void registerGetReturnsForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("register", result.getModelAndView().getViewName()));
    }

    @Test
    void loginGetReturnsForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("login", result.getModelAndView().getViewName()));
    }

    @Test
    void rootGetRedirectsToCalendar() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }
}
