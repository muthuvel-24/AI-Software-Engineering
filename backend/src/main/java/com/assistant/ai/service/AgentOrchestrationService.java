package com.assistant.ai.service;

import com.assistant.ai.model.*;
import com.assistant.ai.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentOrchestrationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        "You are the 'Code Review & Bug Detection Agent'. Analyze the provided source code for syntax errors, logical defects, security vulnerabilities (SQL Injection, XSS, CSRF, insecure auth, resource leaks), performance bottlenecks, and architectural cohesion.\n" +
        "Evaluate the code thoroughly. If the code is clean, high quality, and has NO bugs, explicitly state that no bugs were found and give appropriate high scores (90-100).\n" +
        "IMPORTANT: In addition to your markdown analysis, you MUST include a JSON summary block at the end of your response inside ```json and ``` with this exact structure:\n" +
        "{\n" +
        "  \"overallScore\": <0-100>,\n" +
        "  \"readability\": <0-100>,\n" +
        "  \"maintainability\": <0-100>,\n" +
        "  \"security\": <0-100>,\n" +
        "  \"performance\": <0-100>,\n" +
        "  \"architecture\": <0-100>,\n" +
        "  \"bugs\": [\n" +
        "    {\n" +
        "      \"title\": \"Short bug title\",\n" +
        "      \"severity\": \"HIGH|MEDIUM|LOW\",\n" +
        "      \"filePath\": \"File name or snippet identifier\",\n" +
        "      \"lineNumber\": <line number integer>,\n" +
        "      \"description\": \"Detailed explanation of the issue\",\n" +
        "      \"suggestedFix\": \"Code fix or refactoring advice\"\n" +
        "    }\n" +
        "  ]\n" +
        "}\n" +
        "If there are NO bugs, set \"bugs\": []. Do NOT invent fake bugs if the code is clean!";

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

        int score = 88;
        int readability = 90;
        int maintainability = 85;
        int security = 88;
        int performance = 85;
        int architecture = 85;
        boolean jsonParsed = false;
        List<BugReport> parsedBugs = new ArrayList<>();

        // Try extracting JSON block first
        String jsonBlock = extractCodeBlock(reviewReport, "json");
        if (jsonBlock != null && !jsonBlock.isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(jsonBlock);
                if (root.has("overallScore")) score = root.path("overallScore").asInt(score);
                if (root.has("readability")) readability = root.path("readability").asInt(readability);
                if (root.has("maintainability")) maintainability = root.path("maintainability").asInt(maintainability);
                if (root.has("security")) security = root.path("security").asInt(security);
                if (root.has("performance")) performance = root.path("performance").asInt(performance);
                if (root.has("architecture")) architecture = root.path("architecture").asInt(architecture);

                JsonNode bugsNode = root.path("bugs");
                if (bugsNode.isArray()) {
                    for (JsonNode b : bugsNode) {
                        String title = b.path("title").asText("Code Issue");
                        String severity = b.path("severity").asText("MEDIUM").toUpperCase();
                        String filePath = b.path("filePath").asText("SourceCode");
                        int lineNumber = b.path("lineNumber").asInt(1);
                        String description = b.path("description").asText("");
                        String suggestedFix = b.path("suggestedFix").asText("Refactor code");

                        parsedBugs.add(BugReport.builder()
                                .title(title)
                                .severity(severity)
                                .filePath(filePath)
                                .lineNumber(lineNumber)
                                .description(description)
                                .suggestedFix(suggestedFix)
                                .build());
                    }
                }
                jsonParsed = true;
            } catch (Exception e) {
                // Fallback to text parsing
            }
        }

        if (!jsonParsed) {
            score = parseMetric(reviewReport, "Quality", 85);
            readability = parseMetric(reviewReport, "Readability", 85);
            maintainability = parseMetric(reviewReport, "Maintainability", 85);
            security = parseMetric(reviewReport, "Security", 85);
            performance = parseMetric(reviewReport, "Performance", 85);
            architecture = parseMetric(reviewReport, "Architecture", 85);

            parsedBugs = parseBugsFromText(reviewReport);
        }

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

        // Associate and persist the parsed bugs (if code is clean, parsedBugs is empty - no fake bugs saved!)
        for (BugReport bug : parsedBugs) {
            bug.setReview(savedReview);
            bugReportRepository.save(bug);
        }

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
        if (text == null) return defaultValue;
        try {
            Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(metricName) + "[^0-9\\n]{0,25}(\\d{1,3})(?:/100|%)?");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                int val = Integer.parseInt(matcher.group(1));
                return Math.max(0, Math.min(100, val));
            }
        } catch (Exception e) {
            // Ignore parse failures
        }
        return defaultValue;
    }

    private List<BugReport> parseBugsFromText(String text) {
        List<BugReport> list = new ArrayList<>();
        if (text == null || text.isEmpty()) return list;

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("[BUG]")) {
                try {
                    String clean = line.substring(5).trim();
                    String[] parts = clean.split("\\|");
                    if (parts.length >= 5) {
                        list.add(BugReport.builder()
                                .title(parts[0].trim())
                                .severity(parts[1].trim().toUpperCase())
                                .filePath(parts[2].trim())
                                .lineNumber(Integer.parseInt(parts[3].trim().replaceAll("[^0-9]", "")))
                                .description(parts[4].trim())
                                .suggestedFix(parts.length > 5 ? parts[5].trim() : "Optimize layout structure")
                                .build());
                    }
                } catch (Exception e) {
                    // Ignore lines that fail parsing
                }
            }
        }
        return list;
    }

    private String extractCodeBlock(String text, String lang) {
        if (text == null) return null;
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
        return null;
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
