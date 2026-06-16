package e2e;

import com.example.meetings.MeetingsApplication;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
        classes = MeetingsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:e2e-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "app.base-url=http://test.local"
        })
abstract class E2ETestSupport {
    protected static final String PASSWORD = "password";

    @LocalServerPort
    private int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserService userService;

    @Autowired
    protected MeetingRepository meetingRepository;

    @Autowired
    protected MeetingParticipantRepository participantRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected WebDriver browser;

    @BeforeEach
    void setUpE2E() {
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();
        browser = new HtmlUnitDriver(true);
    }

    @AfterEach
    void tearDownE2E() {
        if (browser != null) {
            browser.quit();
        }
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected void inTransaction(Runnable assertions) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> assertions.run());
    }

    protected void register(String username, String email) {
        browser.get(url("/register"));
        browser.findElement(By.id("username")).sendKeys(username);
        browser.findElement(By.id("email")).sendKeys(email);
        browser.findElement(By.id("password")).sendKeys(PASSWORD);
        browser.findElement(By.cssSelector("button[type='submit']")).click();

        Assertions.assertEquals("Login", browser.getTitle());
        Assertions.assertTrue(browser.getPageSource().contains("Account created"));
    }

    protected void signIn(String username) {
        signIn(username, PASSWORD);
    }

    protected void signIn(String username, String password) {
        browser.get(url("/login"));
        browser.findElement(By.id("username")).sendKeys(username);
        browser.findElement(By.id("password")).sendKeys(password);
        browser.findElement(By.cssSelector("button[type='submit']")).click();
    }

    protected User createUser(String username, String email) {
        return userService.register(username, email, PASSWORD);
    }

    protected void setDateTime(String fieldId, String value) {
        WebElement field = browser.findElement(By.id(fieldId));
        ((JavascriptExecutor) browser).executeScript("arguments[0].value = arguments[1];", field, value);
    }
}
