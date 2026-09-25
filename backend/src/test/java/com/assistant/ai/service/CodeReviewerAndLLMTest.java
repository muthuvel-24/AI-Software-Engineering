package com.assistant.ai.service;

import com.assistant.ai.model.BugReport;
import com.assistant.ai.model.Project;
import com.assistant.ai.model.Review;
import com.assistant.ai.model.User;
import com.assistant.ai.repository.BugReportRepository;
import com.assistant.ai.repository.ProjectRepository;
import com.assistant.ai.repository.ReviewRepository;
import com.assistant.ai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CodeReviewerAndLLMTest {

    @Autowired
    private LLMService llmService;

    @Autowired
    private AgentOrchestrationService orchestrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BugReportRepository bugReportRepository;

    private Project testProject;

    @BeforeEach
    public void setup() {
        User user = userRepository.findByEmail("test_reviewer@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("test_reviewer@example.com");
            u.setPassword("password123");
            u.setName("Test Reviewer");
            return userRepository.save(u);
        });

        testProject = projectRepository.save(Project.builder()
                .name("Code Review Test Project")
                .description("Project for testing clean vs buggy code reviewer")
                .owner(user)
                .build());
    }

    @Test
    public void testCleanCodeReview_HasZeroBugsAndHighScore() {
        String cleanCode = "public class Calculator {\n" +
                "    public int add(int a, int b) {\n" +
                "        return a + b;\n" +
                "    }\n" +
                "}";

        Review review = orchestrationService.runCodeReviewer(
                testProject.getId(), cleanCode, "openrouter", "openai/gpt-4o-mini", ""
        );

        assertNotNull(review);
        assertTrue(review.getScore() >= 90, "Clean code score should be >= 90 but was " + review.getScore());

        List<BugReport> bugs = bugReportRepository.findByReviewId(review.getId());
        assertEquals(0, bugs.size(), "Clean code should have exactly 0 bugs, but found: " + bugs.size());
    }

    @Test
    public void testVulnerableCodeReview_IdentifiesActualDefect() {
        String vulnerableCode = "public class OrderDao {\n" +
                "    public List<Order> findOrders(String customerId) {\n" +
                "        String query = \"SELECT * FROM orders WHERE customer_id = '\" + customerId + \"'\";\n" +
                "        return execute(query);\n" +
                "    }\n" +
                "}";

        Review review = orchestrationService.runCodeReviewer(
                testProject.getId(), vulnerableCode, "openrouter", "openai/gpt-4o-mini", ""
        );

        assertNotNull(review);
        List<BugReport> bugs = bugReportRepository.findByReviewId(review.getId());
        assertTrue(bugs.size() > 0, "Vulnerable code should have identified defects");
        
        boolean foundSqlInjection = bugs.stream().anyMatch(b -> b.getTitle().toLowerCase().contains("sql injection"));
        assertTrue(foundSqlInjection, "Should detect SQL Injection in string concatenation query");
    }

    @Test
    public void testLLMService_KeyAutoDetection() {
        // Test Gemini key prefix detection
        String resGemini = llmService.generate("openrouter", "openai/gpt-4o-mini", "AIzaSyDummyTestKey", "System", "Hello");
        assertNotNull(resGemini);
        // Because key is fake, it should report connection error with GEMINI API (not OPENAI!)
        assertTrue(resGemini.contains("GEMINI") || resGemini.contains("Simulation"), 
                "Should route AIzaSy key to Gemini API");

        // Test OpenAI key prefix detection
        String resOpenAi = llmService.generate("openrouter", "openai/gpt-4o-mini", "sk-proj-dummyKey12345", "System", "Hello");
        assertNotNull(resOpenAi);
        assertTrue(resOpenAi.contains("OPENAI") || resOpenAi.contains("Simulation"), 
                "Should route sk-proj- key to OpenAI API");
    }
}
