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
        String resolvedKey = apiKey;
        if (resolvedKey == null || resolvedKey.trim().isEmpty()) {
            if ("gemini".equalsIgnoreCase(provider)) {
                resolvedKey = defaultGeminiApiKey;
            } else {
                resolvedKey = defaultOpenAiApiKey;
            }
            if (resolvedKey == null || resolvedKey.trim().isEmpty()) {
                resolvedKey = defaultOpenRouterApiKey;
            }
        }

        // If no API key is resolved, fallback to Mock simulation response
        if (resolvedKey == null || resolvedKey.trim().isEmpty()) {
            return generateMockResponse(systemPrompt, userPrompt);
        }

        try {
            if (resolvedKey.startsWith("sk-or-")) {
                return callOpenRouter(model, resolvedKey, systemPrompt, userPrompt);
            } else if ("gemini".equalsIgnoreCase(provider)) {
                return callGemini(model, resolvedKey, systemPrompt, userPrompt);
            } else {
                return callOpenAI(model, resolvedKey, systemPrompt, userPrompt);
            }
        } catch (Exception e) {
            return "Error calling AI provider: " + e.getMessage() + "\n\nFallback Simulation:\n" + generateMockResponse(systemPrompt, userPrompt);
        }
    }

    private String callGemini(String model, String apiKey, String systemPrompt, String userPrompt) throws Exception {
        String modelName = model != null ? model : "gemini-1.5-flash";
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
        String modelName = model != null ? model : "gpt-4o-mini";
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

        // Map standard models to OpenRouter identifiers
        String modelName = model;
        if (modelName == null || modelName.isEmpty()) {
            modelName = "openai/gpt-4o-mini";
        } else if (!modelName.contains("/")) {
            if (modelName.startsWith("gpt-4")) {
                modelName = "openai/" + modelName;
            } else if (modelName.startsWith("gemini")) {
                if (modelName.contains("flash")) {
                    modelName = "google/gemini-2.5-flash";
                } else {
                    modelName = "google/gemini-2.5-pro";
                }
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

        // 3. Code Generator Mock
        if (systemPrompt.contains("Code") || systemPrompt.contains("boilerplate") || systemPrompt.contains("CRUD")) {
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

        // 4. Code Review Mock
        if (systemPrompt.contains("Review") || systemPrompt.contains("Bug")) {
            return "## Code Review Analysis & Diagnostics\n\n" +
                    "### Metrics Summary\n" +
                    "- **Overall Quality:** 82/100\n" +
                    "- **Readability:** 85/100\n" +
                    "- **Security:** 78/100\n" +
                    "- **Performance:** 80/100\n\n" +
                    "### Identified Bugs & Vulnerabilities\n" +
                    "1. **SQL Injection Vulnerability**\n" +
                    "   - *Location:* `UserRepository.java:42`\n" +
                    "   - *Explanation:* Direct string concatenation in queries: `\"SELECT * FROM users WHERE name = '\" + input + \"'\"` makes the query vulnerable to SQL Injection.\n" +
                    "   - *Fix:* Use parameter binding or JPA finder methods: `findByUsername(String username)`.\n\n" +
                    "2. **Resource Leak**\n" +
                    "   - *Location:* `FileReaderService.java:18`\n" +
                    "   - *Explanation:* Unclosed `BufferedReader` stream in standard reading operations.\n" +
                    "   - *Fix:* Wrap execution in a try-with-resources statement.";
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

    private String detectDomain(String prompt) {
        if (prompt.contains("library") || prompt.contains("book")) return "Library";
        if (prompt.contains("ecommerce") || prompt.contains("shop") || prompt.contains("store") || prompt.contains("product")) return "ECommerce";
        if (prompt.contains("employee") || prompt.contains("department") || prompt.contains("hr")) return "Employee";
        if (prompt.contains("auth") || prompt.contains("login") || prompt.contains("user")) return "UserAuthentication";
        return "SoftwareSystem";
    }
}
