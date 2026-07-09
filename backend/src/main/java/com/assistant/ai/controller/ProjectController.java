package com.assistant.ai.controller;

import com.assistant.ai.model.*;
import com.assistant.ai.repository.*;
import com.assistant.ai.service.LLMService;
import com.assistant.ai.service.VectorDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private LLMService llmService;

    @Autowired
    private VectorDBService vectorDBService;

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getProjects() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(projectRepository.findByOwnerId(user.getId()));
    }

    @PostMapping("/projects")
    public ResponseEntity<Project> createProject(@RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser();
        Project project = Project.builder()
                .name(body.getOrDefault("name", "New Project"))
                .description(body.get("description"))
                .owner(user)
                .build();
        return ResponseEntity.ok(projectRepository.save(project));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isPresent() && project.get().getOwner().getId().equals(getAuthenticatedUser().getId())) {
            return ResponseEntity.ok(project.get());
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isPresent() && project.get().getOwner().getId().equals(getAuthenticatedUser().getId())) {
            projectRepository.deleteById(id);
            vectorDBService.clearProjectChunks(id);
            return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/projects/{projectId}/chats")
    public ResponseEntity<?> getChats(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwner().getId().equals(getAuthenticatedUser().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }
        return ResponseEntity.ok(chatRepository.findByProjectId(projectId));
    }

    @PostMapping("/projects/{projectId}/chats")
    public ResponseEntity<?> createChat(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwner().getId().equals(getAuthenticatedUser().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }
        Chat chat = Chat.builder()
                .title(body.getOrDefault("title", "New Conversation"))
                .project(project)
                .build();
        return ResponseEntity.ok(chatRepository.save(chat));
    }

    @GetMapping("/chats/{chatId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        if (!chat.getProject().getOwner().getId().equals(getAuthenticatedUser().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }
        return ResponseEntity.ok(messageRepository.findByChatIdOrderByCreatedAtAsc(chatId));
    }

    @PostMapping("/chats/{chatId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> body) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        if (!chat.getProject().getOwner().getId().equals(getAuthenticatedUser().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        String userText = body.get("content");
        String provider = body.getOrDefault("provider", "gemini");
        String model = body.getOrDefault("model", "gemini-1.5-flash");
        String apiKey = body.get("apiKey");

        // Save User Message
        Message userMessage = Message.builder()
                .chat(chat)
                .role("user")
                .content(userText)
                .build();
        messageRepository.save(userMessage);

        // Fetch last 6 messages to build chat history context
        List<Message> history = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        List<Message> recentHistory = history.size() > 6 
                ? history.subList(history.size() - 6, history.size()) 
                : history;
        String historyContext = recentHistory.stream()
                .map(m -> m.getRole().toUpperCase() + ": " + m.getContent())
                .collect(Collectors.joining("\n\n"));

        // RAG Context from uploaded document vector store
        Long projectId = chat.getProject().getId();
        String retrievedRagText = vectorDBService.retrieveContext(projectId, userText, 2);

        String systemPrompt = "You are a helpful Software Engineering Assistant. Help the developer write code, review models, design databases, and configure servers.\n" +
                "Project Context: Name: " + chat.getProject().getName() + ", Description: " + chat.getProject().getDescription() + "\n";
        
        if (!retrievedRagText.isEmpty()) {
            systemPrompt += "\nRetrieved Relevant Document Chunks (RAG):\n" + retrievedRagText + "\n";
        }

        String finalPrompt = "Conversation history:\n" + historyContext + "\n\nUser Input:\n" + userText;

        // Generate response from LLM
        String aiResponse = llmService.generate(provider, model, apiKey, systemPrompt, finalPrompt);

        // Save AI Message
        Message assistantMessage = Message.builder()
                .chat(chat)
                .role("assistant")
                .content(aiResponse)
                .build();

        return ResponseEntity.ok(messageRepository.save(assistantMessage));
    }

    // Ingest uploaded files for Project RAG context
    @PostMapping("/projects/{projectId}/upload-document")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwner().getId().equals(getAuthenticatedUser().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        String fileName = body.getOrDefault("fileName", "document.txt");
        String fileContent = body.get("content");

        if (fileContent == null || fileContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Document content cannot be empty"));
        }

        vectorDBService.ingestDocument(projectId, fileName, fileContent);
        return ResponseEntity.ok(Map.of("message", "Document ingester processed: " + fileName));
    }
}
