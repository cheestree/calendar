package integration.rest;

import com.example.meetings.MeetingsApplication;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = MeetingsApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:rest-api-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.base-url=http://test.local"
})
@AutoConfigureMockMvc
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MeetingRepository meetingRepository;

    @Autowired
    protected MeetingParticipantRepository participantRepository;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();
    }
}
