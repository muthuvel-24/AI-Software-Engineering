package com.assistant.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class LLMService {

    @Value("${openai.api.key:}")
    private String defaultOpenAiApiKey;

    @Value("${gemini.api.key:}")
    private String defaultGeminiApiKey;

    @Value("${openrouter.api.key:}")
    private String defaultOpenRouterApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public String generate(String provider, String model, String apiKey, String systemPrompt, String userPrompt) {
        // Resolve API Key: if client didn't supply one, fallback to properties/env configuration
        String resolvedKey = apiKey != null ? apiKey.trim() : "";
        String effectiveProvider = provider != null ? provider.trim().toLowerCase() : "openrouter";
        String effectiveModel = model != null ? model.trim() : "";

        // Smart Provider Auto-Detection based on API key prefix if provided
        if (!resolvedKey.isEmpty()) {
            if (resolvedKey.startsWith("AIzaSy")) {
                effectiveProvider = "gemini";
            } else if (resolvedKey.startsWith("sk-or-")) {
                effectiveProvider = "openrouter";
            } else if (resolvedKey.startsWith("sk-proj-") || (resolvedKey.startsWith("sk-") && !resolvedKey.startsWith("sk-ant-"))) {
                // If key is OpenAI format and provider isn't explicitly openrouter, route to OpenAI
                if (!"openrouter".equalsIgnoreCase(effectiveProvider)) {
                    effectiveProvider = "openai";
                }
            }
        }

        // If client didn't supply a key, fallback to backend environment variables for that provider
        if (resolvedKey.isEmpty()) {
            if ("gemini".equalsIgnoreCase(effectiveProvider)) {
                resolvedKey = defaultGeminiApiKey != null ? defaultGeminiApiKey.trim() : "";
            } else if ("openai".equalsIgnoreCase(effectiveProvider)) {
                resolvedKey = defaultOpenAiApiKey != null ? defaultOpenAiApiKey.trim() : "";
            } else {
                resolvedKey = defaultOpenRouterApiKey != null ? defaultOpenRouterApiKey.trim() : "";
            }

            // Cross-fallback: if specific provider key is missing, check OpenRouter default
            if (resolvedKey.isEmpty() && defaultOpenRouterApiKey != null && !defaultOpenRouterApiKey.trim().isEmpty()) {
                resolvedKey = defaultOpenRouterApiKey.trim();
                effectiveProvider = "openrouter";
            }
        }

        // If still no API key is resolved, fallback to Mock simulation response
        if (resolvedKey.isEmpty()) {
            return generateMockResponse(systemPrompt, userPrompt);
        }

        try {
            if ("gemini".equalsIgnoreCase(effectiveProvider)) {
                return callGemini(effectiveModel, resolvedKey, systemPrompt, userPrompt);
            } else if ("openai".equalsIgnoreCase(effectiveProvider)) {
                return callOpenAI(effectiveModel, resolvedKey, systemPrompt, userPrompt);
            } else {
                return callOpenRouter(effectiveModel, resolvedKey, systemPrompt, userPrompt);
            }
        } catch (Exception e) {
            String errorMsg = "Error communicating with " + effectiveProvider.toUpperCase() + " API: " + e.getMessage();
            return errorMsg + "\n\nFallback Simulation:\n" + generateMockResponse(systemPrompt, userPrompt);
        }
    }

    private String callGemini(String model, String apiKey, String systemPrompt, String userPrompt) throws Exception {
        String modelName = model != null ? model.trim() : "";
        if (modelName.contains("/")) {
            modelName = modelName.substring(modelName.lastIndexOf('/') + 1);
        }
        if (modelName.isEmpty() || modelName.startsWith("gpt") || modelName.startsWith("claude") || modelName.startsWith("llama")) {
            modelName = "gemini-1.5-flash";
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

        String combinedPrompt = systemPrompt + "\n\nUser Input:\n" + userPrompt;
        String requestBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .set("contents", objectMapper.createArrayNode().add(
                                objectMapper.createObjectNode()
                                        .set("parts", objectMapper.createArrayNode().add(
                                                objectMapper.createObjectNode().put("text", combinedPrompt)
                                        ))
                        ))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidate = root.path("candidates").path(0);
        if (candidate.isMissingNode()) {
            throw new RuntimeException("Gemini API returned no candidates. Response: " + response.body());
        }
        return candidate.path("content").path("parts").path(0).path("text").asText();
    }

    private String callOpenAI(String model, String apiKey, String systemPrompt, String userPrompt) throws Exception {
        String modelName = model != null ? model.trim() : "";
        if (modelName.startsWith("openai/")) {
            modelName = modelName.substring("openai/".length());
        }
        if (modelName.contains("/")) {
            modelName = modelName.substring(modelName.lastIndexOf('/') + 1);
        }
        if (modelName.isEmpty() || modelName.startsWith("gemini") || modelName.startsWith("claude") || modelName.startsWith("llama")) {
            modelName = "gpt-4o-mini";
        }
        String url = "https://api.openai.com/v1/chat/completions";

        String requestBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("model", modelName)
                        .set("messages", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
                                .add(objectMapper.createObjectNode().put("role", "user").put("content", userPrompt))
                        )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0);
        if (choice.isMissingNode()) {
            throw new RuntimeException("OpenAI API returned no choices. Response: " + response.body());
        }
        return choice.path("message").path("content").asText();
    }

    private String callOpenRouter(String model, String apiKey, String systemPrompt, String userPrompt) throws Exception {
        String url = "https://openrouter.ai/api/v1/chat/completions";

        // Map standard models to OpenRouter identifiers with vendor prefix
        String modelName = model != null ? model.trim() : "";
        if (modelName.isEmpty()) {
            modelName = "openai/gpt-4o-mini";
        } else if (!modelName.contains("/")) {
            if (modelName.startsWith("gpt-4") || modelName.startsWith("o1") || modelName.startsWith("o3")) {
                modelName = "openai/" + modelName;
            } else if (modelName.startsWith("gemini")) {
                modelName = "google/" + modelName;
            } else if (modelName.startsWith("claude")) {
                modelName = "anthropic/" + modelName;
            } else if (modelName.startsWith("llama")) {
                modelName = "meta-llama/" + modelName;
            } else {
                modelName = "openai/" + modelName;
            }
        }

        String requestBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("model", modelName)
                        .put("max_tokens", 4096)
                        .set("messages", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
                                .add(objectMapper.createObjectNode().put("role", "user").put("content", userPrompt))
                        )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "AI Software Engineering Assistant")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0);
        if (choice.isMissingNode()) {
            throw new RuntimeException("OpenRouter API returned no choices. Response: " + response.body());
        }
        return choice.path("message").path("content").asText();
    }

    private String generateMockResponse(String systemPrompt, String userPrompt) {
        String promptLower = userPrompt.toLowerCase();

        // 1. Requirement Analyzer Mock
        if (systemPrompt.contains("Analyzer") || systemPrompt.contains("requirement")) {
            String domain = detectDomain(promptLower);
            return "# Functional & Non-Functional Requirements: " + domain + "\n\n" +
                    "## 1. Functional Requirements\n" +
                    "- **FR-1:** Users must be able to create, read, update, and delete " + domain + " entries.\n" +
                    "- **FR-2:** Real-time search and filters by key fields.\n" +
                    "- **FR-3:** CSV/PDF export of summary metrics.\n" +
                    "- **FR-4:** JWT token authentication for administrative workflows.\n\n" +
                    "## 2. Non-Functional Requirements\n" +
                    "- **NFR-1 (Security):** All passwords encrypted using BCrypt.\n" +
                    "- **NFR-2 (Performance):** Page queries load in < 250ms under typical loads.\n" +
                    "- **NFR-3 (Scalability):** Stateless controllers to allow horizontal clustering.\n\n" +
                    "## 3. User Stories\n" +
                    "- *As a Manager*, I want to filter details so I can view performance reports.\n" +
                    "- *As an Engineer*, I want to submit logs so my team has real-time visibility.\n\n" +
                    "## 4. Tech Stack & Roadmap\n" +
                    "- **Tech Stack:** Spring Boot 3.x, React 19, TypeScript, PostgreSQL, Tailwind CSS.\n" +
                    "- **Roadmap:** Phase 1: Database Setup -> Phase 2: Backend APIs -> Phase 3: Frontend dashboard integration -> Phase 4: CI/CD Setup.";
        }

        // 2. Database Schema Generator Mock
        if (systemPrompt.contains("Schema") || systemPrompt.contains("ERD") || systemPrompt.contains("Database")) {
            String domain = detectDomain(promptLower);
            return "## Database Schema & Scripts for: " + domain + "\n\n" +
                    "### PostgreSQL Schema\n" +
                    "```sql\n" +
                    "CREATE TABLE users (\n" +
                    "    id SERIAL PRIMARY KEY,\n" +
                    "    email VARCHAR(255) UNIQUE NOT NULL,\n" +
                    "    password_hash VARCHAR(255) NOT NULL,\n" +
                    "    full_name VARCHAR(100),\n" +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                    ");\n\n" +
                    "CREATE TABLE " + domain + "_records (\n" +
                    "    id SERIAL PRIMARY KEY,\n" +
                    "    name VARCHAR(255) NOT NULL,\n" +
                    "    description TEXT,\n" +
                    "    status VARCHAR(50) DEFAULT 'ACTIVE',\n" +
                    "    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,\n" +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                    ");\n\n" +
                    "CREATE INDEX idx_" + domain + "_user ON " + domain + "_records(user_id);\n" +
                    "```\n\n" +
                    "### Relationships & ER Diagram\n" +
                    "- **Users (1) <----> (N) " + domain + "_records**\n" +
                    "  - Foreign Key: `" + domain + "_records.user_id` points to `users.id`.\n" +
                    "  - Constraint: Cascade delete matches relational cleanup rules.";
        }

        // 3. Code Review & Bug Detection Mock
        if (systemPrompt.contains("Code Review") || systemPrompt.contains("Bug Detection") || systemPrompt.contains("Review")) {
            return generateCodeReviewSimulation(userPrompt);
        }

        // 4. Code Generator Mock
        if (systemPrompt.contains("Code Generator") || systemPrompt.contains("boilerplate") || systemPrompt.contains("CRUD")) {
            String domain = detectDomain(promptLower);
            return "## Generated Project Boilerplate: " + domain + "\n\n" +
                    "### Project Folder Tree\n" +
                    "```\n" +
                    "├── src/main/java/com/app\n" +
                    "│   ├── controller/ModelController.java\n" +
                    "│   ├── model/ModelEntity.java\n" +
                    "│   ├── repository/ModelRepository.java\n" +
                    "│   └── service/ModelService.java\n" +
                    "└── src/components/Dashboard.tsx\n" +
                    "```\n\n" +
                    "### ModelEntity.java (Spring Boot JPA)\n" +
                    "```java\n" +
                    "package com.app.model;\n\n" +
                    "import jakarta.persistence.*;\n" +
                    "import lombok.Data;\n\n" +
                    "@Entity\n" +
                    "@Data\n" +
                    "public class ModelEntity {\n" +
                    "    @Id\n" +
                    "    @GeneratedValue(strategy = GenerationType.IDENTITY)\n" +
                    "    private Long id;\n" +
                    "    private String name;\n" +
                    "    private String description;\n" +
                    "}\n" +
                    "```";
        }

        // 5. SQL Generator Mock
        if (systemPrompt.contains("SQL Generator") || systemPrompt.contains("CREATE TABLE")) {
            return "## Generated SQL Queries\n\n" +
                    "```sql\n" +
                    "CREATE TABLE employee (\n" +
                    "    id SERIAL PRIMARY KEY,\n" +
                    "    first_name VARCHAR(100) NOT NULL,\n" +
                    "    last_name VARCHAR(100) NOT NULL,\n" +
                    "    email VARCHAR(255) UNIQUE NOT NULL,\n" +
                    "    department_id INTEGER NOT NULL,\n" +
                    "    hire_date DATE DEFAULT CURRENT_DATE,\n" +
                    "    salary DECIMAL(12, 2)\n" +
                    ");\n\n" +
                    "INSERT INTO employee (first_name, last_name, email, department_id, salary) \n" +
                    "VALUES ('John', 'Doe', 'john.doe@company.com', 5, 85000.00);\n" +
                    "```\n\n" +
                    "### Optimization & Explanation\n" +
                    "- The `email` column is marked `UNIQUE` to create an implicit index which accelerates lookups.\n" +
                    "- Added `department_id` to index queries: `CREATE INDEX idx_emp_dept ON employee(department_id);` to speed up join operations.";
        }

        // 6. API Documentation Mock
        if (systemPrompt.contains("Swagger") || systemPrompt.contains("API Documentation")) {
            return "openapi: 3.0.3\n" +
                    "info:\n" +
                    "  title: Developer Assistant REST API\n" +
                    "  version: 1.0.0\n" +
                    "paths:\n" +
                    "  /api/records:\n" +
                    "    get:\n" +
                    "      summary: Retrieve all active records\n" +
                    "      responses:\n" +
                    "        '200':\n" +
                    "          description: OK\n" +
                    "          content:\n" +
                    "            application/json:\n" +
                    "              schema:\n" +
                    "                type: array\n" +
                    "                items:\n" +
                    "                  type: object\n" +
                    "                  properties:\n" +
                    "                    id: {type: integer}\n" +
                    "                    name: {type: string}";
        }

        // 7. Unit Test Generator Mock
        if (systemPrompt.contains("Unit Test") || systemPrompt.contains("JUnit")) {
            return "## Generated JUnit Test Suite (Mockito)\n\n" +
                    "```java\n" +
                    "import static org.mockito.Mockito.*;\n" +
                    "import static org.junit.jupiter.api.Assertions.*;\n" +
                    "import org.junit.jupiter.api.Test;\n" +
                    "import org.junit.jupiter.api.extension.ExtendWith;\n" +
                    "import org.mockito.InjectMocks;\n" +
                    "import org.mockito.Mock;\n" +
                    "import org.mockito.junit.jupiter.MockitoExtension;\n\n" +
                    "@ExtendWith(MockitoExtension.class)\n" +
                    "public class ServiceTest {\n\n" +
                    "    @Mock\n" +
                    "    private Repository repository;\n\n" +
                    "    @InjectMocks\n" +
                    "    private ServiceImpl service;\n\n" +
                    "    @Test\n" +
                    "    public void testFindById_Success() {\n" +
                    "        Entity mockEntity = new Entity(1L, \"Test Item\");\n" +
                    "        when(repository.findById(1L)).thenReturn(Optional.of(mockEntity));\n" +
                    "        \n" +
                    "        Entity result = service.getById(1L);\n" +
                    "        \n" +
                    "        assertNotNull(result);\n" +
                    "        assertEquals(\"Test Item\", result.getName());\n" +
                    "    }\n" +
                    "}\n" +
                    "```";
        }

        // 8. Deployment Assistant Mock
        if (systemPrompt.contains("Deployment") || systemPrompt.contains("Docker")) {
            return "## Containerization & Deployment Guides\n\n" +
                    "### 1. Dockerfile\n" +
                    "```dockerfile\n" +
                    "FROM openjdk:21-slim\n" +
                    "WORKDIR /app\n" +
                    "COPY target/*.jar app.jar\n" +
                    "EXPOSE 8080\n" +
                    "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n" +
                    "```\n\n" +
                    "### 2. Docker Compose\n" +
                    "```yaml\n" +
                    "version: '3.8'\n" +
                    "services:\n" +
                    "  backend:\n" +
                    "    build: .\n" +
                    "    ports:\n" +
                    "      - \"8080:8080\"\n" +
                    "    environment:\n" +
                    "      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/db\n" +
                    "  db:\n" +
                    "    image: postgres:15-alpine\n" +
                    "    ports:\n" +
                    "      - \"5432:5432\"\n" +
                    "```";
        }

        return "### Chat Conversation Response\n\n" +
                "I am here to assist you with development tasks. Ask me anything about writing code, debugging, generating databases, deployment configurations, or document reviews.";
    }

    private String generateCodeReviewSimulation(String code) {
        String cleanCode = code != null ? code.trim() : "";
        String lower = cleanCode.toLowerCase();

        boolean hasSqlInjection = (lower.contains("select ") || lower.contains("where ") || lower.contains("insert into ")) 
                && (cleanCode.contains(" + ") || cleanCode.contains("+\"") || cleanCode.contains("${") || cleanCode.contains("%s"));
        boolean hasHardcodedSecret = lower.matches("(?s).*(password|secret|apikey|api_key|token)\\s*=\\s*[\"'][a-zA-Z0-9_!@#$%^&*]{5,}[\"'].*");
        boolean hasEval = lower.contains("eval(") || lower.contains("exec(");
        boolean hasResourceLeak = (lower.contains("bufferedreader") || lower.contains("fileinputstream") || lower.contains("connection ")) 
                && !lower.contains("try (") && !lower.contains("try(") && !lower.contains(".close()");

        List<String> bugJsonList = new ArrayList<>();
        StringBuilder markdownBugs = new StringBuilder();
        int bugCount = 0;

        if (hasSqlInjection) {
            bugCount++;
            int line = findLineNumber(cleanCode, "select", "where", "insert");
            bugJsonList.add("{\n" +
                    "      \"title\": \"SQL Injection Vulnerability\",\n" +
                    "      \"severity\": \"HIGH\",\n" +
                    "      \"filePath\": \"UserQueryService\",\n" +
                    "      \"lineNumber\": " + line + ",\n" +
                    "      \"description\": \"Dynamic string concatenation detected in SQL statement. Untrusted inputs can manipulate the query logic.\",\n" +
                    "      \"suggestedFix\": \"Use parameterized queries, PreparedStatement with ? placeholders, or ORM parameter binding.\"\n" +
                    "    }");
            markdownBugs.append("1. **SQL Injection Vulnerability** (HIGH, Line ").append(line).append(")\n")
                    .append("   - *Detail:* Dynamic string concatenation in SQL statement.\n")
                    .append("   - *Fix:* Use parameterized PreparedStatement or query placeholders.\n\n");
        }

        if (hasHardcodedSecret) {
            bugCount++;
            int line = findLineNumber(cleanCode, "password", "secret", "apikey", "token");
            bugJsonList.add("{\n" +
                    "      \"title\": \"Hardcoded Plaintext Credential\",\n" +
                    "      \"severity\": \"HIGH\",\n" +
                    "      \"filePath\": \"ApplicationConfig\",\n" +
                    "      \"lineNumber\": " + line + ",\n" +
                    "      \"description\": \"Sensitive credential or private key is directly hardcoded in the source code.\",\n" +
                    "      \"suggestedFix\": \"Extract secrets into external environment variables (System.getenv) or secret vault.\"\n" +
                    "    }");
            markdownBugs.append("2. **Hardcoded Plaintext Credential** (HIGH, Line ").append(line).append(")\n")
                    .append("   - *Detail:* Sensitive credential embedded directly in source code.\n")
                    .append("   - *Fix:* Move sensitive tokens into environment variables.\n\n");
        }

        if (hasEval) {
            bugCount++;
            int line = findLineNumber(cleanCode, "eval(", "exec(");
            bugJsonList.add("{\n" +
                    "      \"title\": \"Arbitrary Dynamic Code Execution (eval)\",\n" +
                    "      \"severity\": \"HIGH\",\n" +
                    "      \"filePath\": \"ScriptEngine\",\n" +
                    "      \"lineNumber\": " + line + ",\n" +
                    "      \"description\": \"Using eval() or exec() to evaluate untrusted strings leads to remote code execution (RCE).\",\n" +
                    "      \"suggestedFix\": \"Avoid dynamic code evaluation. Use secure JSON parsing or structured mapping instead.\"\n" +
                    "    }");
            markdownBugs.append("3. **Arbitrary Dynamic Code Execution** (HIGH, Line ").append(line).append(")\n")
                    .append("   - *Detail:* Insecure eval()/exec() invocation.\n")
                    .append("   - *Fix:* Replace dynamic evaluation with safe parsers.\n\n");
        }

        if (hasResourceLeak) {
            bugCount++;
            int line = findLineNumber(cleanCode, "bufferedreader", "fileinputstream", "connection");
            bugJsonList.add("{\n" +
                    "      \"title\": \"Unclosed I/O Resource Stream\",\n" +
                    "      \"severity\": \"MEDIUM\",\n" +
                    "      \"filePath\": \"FileStreamHandler\",\n" +
                    "      \"lineNumber\": " + line + ",\n" +
                    "      \"description\": \"I/O or database connection is opened without a try-with-resources statement, risking resource leaks.\",\n" +
                    "      \"suggestedFix\": \"Wrap stream initialization in a try-with-resources block: try (BufferedReader br = ...)\"\n" +
                    "    }");
            markdownBugs.append("4. **Unclosed I/O Resource Stream** (MEDIUM, Line ").append(line).append(")\n")
                    .append("   - *Detail:* Missing automatic resource management.\n")
                    .append("   - *Fix:* Implement try-with-resources statement.\n\n");
        }

        int overall = bugCount == 0 ? 95 : Math.max(50, 90 - (bugCount * 15));
        int security = bugCount == 0 ? 98 : Math.max(45, 88 - (bugCount * 18));
        int readability = bugCount == 0 ? 96 : 84;
        int maintainability = bugCount == 0 ? 94 : 80;
        int performance = 92;
        int architecture = 90;

        StringBuilder sb = new StringBuilder();
        sb.append("## Code Security & Quality Audit Report\n\n");
        sb.append("### Metrics Summary\n");
        sb.append("- **Overall Quality:** ").append(overall).append("/100\n");
        sb.append("- **Security Score:** ").append(security).append("/100\n");
        sb.append("- **Readability:** ").append(readability).append("/100\n");
        sb.append("- **Maintainability:** ").append(maintainability).append("/100\n");
        sb.append("- **Performance:** ").append(performance).append("/100\n");
        sb.append("- **Architecture:** ").append(architecture).append("/100\n\n");

        sb.append("### Diagnostic Findings\n");
        if (bugCount == 0) {
            sb.append("No security vulnerabilities or logical defects were identified in the submitted code.\n")
              .append("The implementation adheres to secure coding standards and modern software hygiene.\n\n");
        } else {
            sb.append(markdownBugs.toString());
        }

        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"overallScore\": ").append(overall).append(",\n");
        sb.append("  \"readability\": ").append(readability).append(",\n");
        sb.append("  \"maintainability\": ").append(maintainability).append(",\n");
        sb.append("  \"security\": ").append(security).append(",\n");
        sb.append("  \"performance\": ").append(performance).append(",\n");
        sb.append("  \"architecture\": ").append(architecture).append(",\n");
        sb.append("  \"bugs\": [\n");
        sb.append(String.join(",\n", bugJsonList));
        sb.append("\n  ]\n");
        sb.append("}\n");
        sb.append("```");

        return sb.toString();
    }

    private int findLineNumber(String code, String... keywords) {
        if (code == null) return 1;
        String[] lines = code.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String lineLower = lines[i].toLowerCase();
            for (String kw : keywords) {
                if (lineLower.contains(kw.toLowerCase())) {
                    return i + 1;
                }
            }
        }
        return 1;
    }

    private String detectDomain(String prompt) {
        if (prompt == null) return "SoftwareSystem";
        String lower = prompt.toLowerCase();
        if (lower.contains("library") || lower.contains("book")) return "Library";
        if (lower.contains("ecommerce") || lower.contains("shop") || lower.contains("store") || lower.contains("product")) return "ECommerce";
        if (lower.contains("employee") || lower.contains("department") || lower.contains("hr")) return "Employee";
        if (lower.contains("auth") || lower.contains("login") || lower.contains("user")) return "UserAuthentication";
        return "SoftwareSystem";
    }
}

