package unit;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.AppUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    /**
     * Scenario: Username loads user details
     */
    @Test
    void loadUserByUsername() {
        User user = new User("bob", "bob@example.com", "hashed-password");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails userDetails = appUserDetailsService.loadUserByUsername("bob");

        assertEquals("bob", userDetails.getUsername());
        assertEquals("hashed-password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        verify(userRepository).findByUsername("bob");
    }

    /**
     * Scenario: Username throws when the user doesn't exist
     */
    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        UsernameNotFoundException error = assertThrows(
                UsernameNotFoundException.class,
                () -> appUserDetailsService.loadUserByUsername("missing")
        );

        assertEquals("Unknown user: missing", error.getMessage());
        verify(userRepository).findByUsername("missing");
    }
}
