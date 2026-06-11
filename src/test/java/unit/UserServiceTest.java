package unit;

import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerCreatesUser() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var user = userService.register("bob", "", "password");
        assertEquals("bob", user.getUsername());
        assertEquals("hashed", user.getPasswordHash());
    }

    @Test
    void registerThrowsWhenUsernameAlreadyTaken() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register("alice", "alice@example.com", "password")
        );

        assertEquals("Username already taken", error.getMessage());
        verify(passwordEncoder, never()).encode("password");
        verify(userRepository, never()).save(null);
    }

    @Test
    void requireByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(java.util.Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> userService.requireByUsername("missing")
        );

        assertEquals("Unknown user: missing", error.getMessage());
    }
}