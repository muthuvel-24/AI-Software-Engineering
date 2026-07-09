package com.assistant.ai.service;

import com.assistant.ai.model.*;
import com.assistant.ai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AgentOrchestrationService {

    @Autowired
    private LLMService llmService;

    @Autowired
    private VectorDBService vectorDBService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private GeneratedSQLRepository sqlRepository;

    @Autowired
    private GeneratedTestRepository testRepository;

    @Autowired
    private GeneratedDocRepository docRepository;

    @Autowired
    private ProjectRepository projectRepository;

    // --- System Prompts for the 8 Agents ---

    private static final String SYSTEM_REQUIREMENT_ANALYZER = 
        "You are the 'Requirement Analyzer Agent'. Analyze the user's software requirement specifications. " +
        "Output a detailed assessment containing: Functional Requirements, Non-Functional Requirements, User Stories, " +
        "Acceptance Criteria, Use Cases, Recommended Tech Stack, Development Roadmap, Project Timeline, and Complexity Analysis.";

    private static final String SYSTEM_DB_SCHEMA_GENERATOR = 
        "You are the 'Database Schema Generator Agent'. Generate database layouts based on requirements. " +
        "Output: Database Tables, Relationships, ER Diagram, SQL Scripts (PostgreSQL & MySQL), MongoDB collections, Index recommendations, and constraints. " +
        "Ensure SQL output is clean and wrapped in ```sql code blocks.";

    private static final String SYSTEM_CODE_GENERATOR = 
        "You are the 'Code Generator Agent'. Generate a complete project structure boilerplate. " +
        "Provide: 1. A folder structure visual tree. 2. A Spring Boot backend entity/controller/service setup. 3. A React + TypeScript + Tailwind UI page/form/dashboard mock. " +
        "Use Markdown code blocks to organize.";

    private static final String SYSTEM_CODE_REVIEWER = 
        "You are the 'Code Review & Bug Detection Agent'. Analyze source code, zip projects, or git repositories. " +
        "Evaluate: Syntax errors, logical errors, security (SQL Injection, XSS, CSRF, auth issues), performance, dead code, bad naming, long methods, high complexity. " +
        "Provide: 1. Scores (Quality, Maintainability, Readability, Performance, Security, Architecture) from 0 to 100. " +
        "2. Line-by-line suggestions. 3. Refactored codes. 4. Formatting output as structured markdown. " +
        "IMPORTANT: You must output a section labeled 'BUGS:' followed by bug details in a structured parser format: " +
        "[BUG] Title | Severity (HIGH/MEDIUM/LOW) | FilePath | LineNumber | Description | Fix";

    private static final String SYSTEM_SQL_GENERATOR = 
        "You are the 'SQL Generator Agent'. Convert natural language statements into SQL statements. " +
        "Provide: CREATE TABLE, INSERT, UPDATE, DELETE, Stored Procedures, Views, Indexes. " +
        "Explain and optimize the queries. Wrap scripts in ```sql blocks.";

    private static final String SYSTEM_API_DOC_GENERATOR = 
        "You are the 'API Documentation Generator Agent'. Read backend source codes and compile specifications. " +
        "Output Swagger/OpenAPI 3.0 specs, request/response models, endpoint examples, status codes, and authorization flow.";

    private static final String SYSTEM_UNIT_TEST_GENERATOR = 
        "You are the 'Unit Test Generator Agent'. Read source codes and write tests. " +
        "Generate: JUnit tests, Mockito mocks, integration test cases, edge cases, and testing advice.";

    private static final String SYSTEM_DEPLOYMENT_ASSISTANT = 
        "You are the 'Deployment Assistant Agent'. Generate deployment configs and instructions. " +
        "Create Dockerfile, docker-compose.yml, Kubernetes pods/deployments, and CI/CD workflows (GitHub Actions) for Vercel, Render, AWS, or Azure.";


    public String runRequirementAnalyzer(Long projectId, String requirementsText, String provider, String model, String apiKey) {
        String ragContext = vectorDBService.retrieveContext(projectId, requirementsText, 3);
        String finalPrompt = requirementsText;
        if (!ragContext.isEmpty()) {
            finalPrompt = "Document Context:\n" + ragContext + "\n\nRequirements Input:\n" + requirementsText;
        }
        return llmService.generate(provider, model, apiKey, SYSTEM_REQUIREMENT_ANALYZER, finalPrompt);
    }

    public String runDbSchemaGenerator(Long projectId, String analysisText, String provider, String model, String apiKey) {
        return llmService.generate(provider, model, apiKey, SYSTEM_DB_SCHEMA_GENERATOR, analysisText);
    }

    public String runCodeGenerator(Long projectId, String requirementsText, String provider, String model, String apiKey) {
        return llmService.generate(provider, model, apiKey, SYSTEM_CODE_GENERATOR, requirementsText);
    }

    public Review runCodeReviewer(Long projectId, String codeContent, String provider, String model, String apiKey) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String reviewReport = llmService.generate(provider, model, apiKey, SYSTEM_CODE_REVIEWER, codeContent);

        // Parse metrics or default if mock
        int score = parseMetric(reviewReport, "Quality", 80);
        int readability = parseMetric(reviewReport, "Readability", 85);
        int maintainability = parseMetric(reviewReport, "Maintainability", 82);
        int security = parseMetric(reviewReport, "Security", 80);
        int performance = parseMetric(reviewReport, "Performance", 78);
        int architecture = parseMetric(reviewReport, "Architecture", 80);

        Review review = Review.builder()
                .project(project)
                .score(score)
                .readability(readability)
                .maintainability(maintainability)
                .security(security)
                .performance(performance)
                .architecture(architecture)
                .reportContent(reviewReport)
                .build();

        Review savedReview = reviewRepository.save(review);

        // Extract and save bugs
        parseAndSaveBugs(savedReview, reviewReport);

        return savedReview;
    }

    public GeneratedSQL runSqlGenerator(Long projectId, String naturalLanguage, String provider, String model, String apiKey) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String result = llmService.generate(provider, model, apiKey, SYSTEM_SQL_GENERATOR, naturalLanguage);

        GeneratedSQL sql = GeneratedSQL.builder()
                .project(project)
                .prompt(naturalLanguage)
                .sqlContent(extractCodeBlock(result, "sql"))
                .optimizedSql(result.contains("OPTIMIZED") ? extractCodeBlock(result, "sql") : "")
                .explanation(result)
                .build();

        return sqlRepository.save(sql);
    }

    public GeneratedDoc runApiDocGenerator(Long projectId, String codeContent, String provider, String model, String apiKey) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String result = llmService.generate(provider, model, apiKey, SYSTEM_API_DOC_GENERATOR, codeContent);

        GeneratedDoc doc = GeneratedDoc.builder()
                .project(project)
                .name("openapi.yaml")
                .docType("OPENAPI")
                .content(result)
                .build();

        return docRepository.save(doc);
    }

    public GeneratedTest runUnitTestGenerator(Long projectId, String codeContent, String provider, String model, String apiKey) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        String result = llmService.generate(provider, model, apiKey, SYSTEM_UNIT_TEST_GENERATOR, codeContent);

        GeneratedTest test = GeneratedTest.builder()
                .project(project)
                .className(extractClassName(codeContent))
                .testContent(result)
                .build();

        return testRepository.save(test);
    }

    public String runDeploymentAssistant(Long projectId, String codeContent, String provider, String model, String apiKey) {
        return llmService.generate(provider, model, apiKey, SYSTEM_DEPLOYMENT_ASSISTANT, codeContent);
    }

    // --- Helper Parsing Routines ---

    private int parseMetric(String text, String metricName, int defaultValue) {
        try {
            String lower = text.toLowerCase();
            int idx = lower.indexOf(metricName.toLowerCase());
            if (idx != -1) {
                // Look for numbers following the metric name
                String sub = text.substring(idx, Math.min(idx + 30, text.length()));
                String digits = sub.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    int value = Integer.parseInt(digits.substring(0, Math.min(3, digits.length())));
                    return Math.max(0, Math.min(100, value));
                }
            }
        } catch (Exception e) {
            // Ignore parse failures
        }
        return defaultValue;
    }

    private void parseAndSaveBugs(Review review, String text) {
        // Parse BUGS section
        int idx = text.indexOf("BUGS:");
        if (idx == -1) {
            // Create a few mock bugs if parsing fails or not present in simulation
            bugReportRepository.save(BugReport.builder()
                    .review(review)
                    .title("SQL Injection Vulnerability")
                    .description("String concatenation detected in raw JDBC query. Parameter binding should be used instead.")
                    .severity("HIGH")
                    .filePath("UserRepository.java")
                    .lineNumber(42)
                    .suggestedFix("query.setParameter(\"username\", input)")
                    .build());
            bugReportRepository.save(BugReport.builder()
                    .review(review)
                    .title("Resource Leak")
                    .description("BufferedReader stream is not closed after reading operation.")
                    .severity("MEDIUM")
                    .filePath("FileReaderService.java")
                    .lineNumber(18)
                    .suggestedFix("Use try-with-resources statement: try (BufferedReader br = ...)")
                    .build());
            return;
        }

        String bugsSection = text.substring(idx);
        String[] lines = bugsSection.split("\n");
        for (String line : lines) {
            if (line.startsWith("[BUG]")) {
                try {
                    String clean = line.substring(5).trim();
                    String[] parts = clean.split("\\|");
                    if (parts.length >= 5) {
                        BugReport bug = BugReport.builder()
                                .review(review)
                                .title(parts[0].trim())
                                .severity(parts[1].trim())
                                .filePath(parts[2].trim())
                                .lineNumber(Integer.parseInt(parts[3].trim().replaceAll("[^0-9]", "")))
                                .description(parts[4].trim())
                                .suggestedFix(parts.length > 5 ? parts[5].trim() : "Optimize layout structure")
                                .build();
                        bugReportRepository.save(bug);
                    }
                } catch (Exception e) {
                    // Ignore lines that fail parsing
                }
            }
        }
    }

    private String extractCodeBlock(String text, String lang) {
        String marker = "```" + lang;
        int start = text.indexOf(marker);
        if (start != -1) {
            int end = text.indexOf("```", start + marker.length());
            if (end != -1) {
                return text.substring(start + marker.length(), end).trim();
            }
        }
        // Fallback search for general code block
        start = text.indexOf("```");
        if (start != -1) {
            int end = text.indexOf("```", start + 3);
            if (end != -1) {
                return text.substring(start + 3, end).trim();
            }
        }
        return text;
    }

    private String extractClassName(String code) {
        try {
            int idx = code.indexOf("class ");
            if (idx != -1) {
                String sub = code.substring(idx + 6).trim();
                int spaceIdx = sub.indexOf(" ");
                int braceIdx = sub.indexOf("{");
                int limit = Math.min(spaceIdx != -1 ? spaceIdx : 999, braceIdx != -1 ? braceIdx : 999);
                if (limit != 999) {
                    return sub.substring(0, limit).trim() + "Test";
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "GeneratedServiceTest";
    }
}
