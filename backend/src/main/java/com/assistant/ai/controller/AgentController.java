package com.assistant.ai.controller;

import com.assistant.ai.dto.AgentRequest;
import com.assistant.ai.model.*;
import com.assistant.ai.repository.*;
import com.assistant.ai.service.AgentOrchestrationService;
import com.assistant.ai.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @Autowired
    private AgentOrchestrationService orchestrationService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private GeneratedSQLRepository sqlRepository;

    @Autowired
    private GeneratedDocRepository docRepository;

    @Autowired
    private GeneratedTestRepository testRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void verifyProjectOwnership(Long projectId) {
        User user = getAuthenticatedUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: You do not own this project");
        }
    }

    @PostMapping("/analyze-requirements")
    public ResponseEntity<String> analyzeRequirements(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        String result = orchestrationService.runRequirementAnalyzer(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-db-schema")
    public ResponseEntity<String> generateDbSchema(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        String result = orchestrationService.runDbSchemaGenerator(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-code")
    public ResponseEntity<String> generateCode(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        String result = orchestrationService.runCodeGenerator(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/review-code")
    public ResponseEntity<Review> reviewCode(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        Review review = orchestrationService.runCodeReviewer(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(review);
    }

    @PostMapping("/generate-sql")
    public ResponseEntity<GeneratedSQL> generateSql(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        GeneratedSQL sql = orchestrationService.runSqlGenerator(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(sql);
    }

    @PostMapping("/generate-docs")
    public ResponseEntity<GeneratedDoc> generateDocs(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        GeneratedDoc doc = orchestrationService.runApiDocGenerator(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/generate-tests")
    public ResponseEntity<GeneratedTest> generateTests(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        GeneratedTest test = orchestrationService.runUnitTestGenerator(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(test);
    }

    @PostMapping("/deployment-help")
    public ResponseEntity<String> deploymentHelp(@RequestBody AgentRequest request) {
        verifyProjectOwnership(request.getProjectId());
        String result = orchestrationService.runDeploymentAssistant(
                request.getProjectId(), request.getPrompt(),
                request.getProvider(), request.getModel(), request.getApiKey()
        );
        return ResponseEntity.ok(result);
    }

    // --- Output File Export and Download Endpoints ---

    @GetMapping("/reviews/{reviewId}/export-pdf")
    public ResponseEntity<byte[]> exportReviewPdf(@PathVariable Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        verifyProjectOwnership(review.getProject().getId());

        byte[] pdfBytes = exportService.generateReviewPdf(review);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"code_review_report_" + reviewId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/code/download")
    public ResponseEntity<byte[]> downloadCodeZip(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "app-project");
        String prompt = body.getOrDefault("prompt", "");
        byte[] zipBytes = exportService.generateCodeZip(name, prompt);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name.toLowerCase() + "_boilerplate.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBytes);
    }

    @GetMapping("/sql/{sqlId}/download")
    public ResponseEntity<byte[]> downloadSqlFile(@PathVariable Long sqlId) {
        GeneratedSQL sql = sqlRepository.findById(sqlId)
                .orElseThrow(() -> new RuntimeException("SQL not found"));

        verifyProjectOwnership(sql.getProject().getId());

        byte[] sqlBytes = sql.getSqlContent().getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"schema_" + sqlId + ".sql\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(sqlBytes);
    }

    @GetMapping("/docs/{docId}/download")
    public ResponseEntity<byte[]> downloadDocFile(@PathVariable Long docId) {
        GeneratedDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Doc not found"));

        verifyProjectOwnership(doc.getProject().getId());

        byte[] docBytes = doc.getContent().getBytes();

        // Sanitize name to prevent HTTP header injection
        String safeName = doc.getName().replaceAll("[\\r\\n\\t]", "_");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(docBytes);
    }

    // --- Dashboard, Reviews and Bugs Listings ---

    @GetMapping("/projects/{projectId}/reviews")
    public ResponseEntity<List<Review>> getProjectReviews(@PathVariable Long projectId) {
        verifyProjectOwnership(projectId);
        return ResponseEntity.ok(reviewRepository.findByProjectId(projectId));
    }

    @GetMapping("/reviews/{reviewId}/bugs")
    public ResponseEntity<List<BugReport>> getReviewBugs(@PathVariable Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        verifyProjectOwnership(review.getProject().getId());
        return ResponseEntity.ok(bugReportRepository.findByReviewId(reviewId));
    }

    @GetMapping("/projects/{projectId}/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics(@PathVariable Long projectId) {
        verifyProjectOwnership(projectId);
        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        List<GeneratedSQL> sqls = sqlRepository.findByProjectId(projectId);
        List<GeneratedTest> tests = testRepository.findByProjectId(projectId);
        List<GeneratedDoc> docs = docRepository.findByProjectId(projectId);

        int averageScore = 0;
        int totalBugs = 0;
        if (!reviews.isEmpty()) {
            int scoreSum = 0;
            for (Review r : reviews) {
                scoreSum += r.getScore();
                totalBugs += bugReportRepository.findByReviewId(r.getId()).size();
            }
            averageScore = scoreSum / reviews.size();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviews", reviews.size());
        stats.put("averageQualityScore", averageScore);
        stats.put("bugsFound", totalBugs);
        stats.put("generatedApis", docs.size());
        stats.put("generatedSqls", sqls.size());
        stats.put("generatedTests", tests.size());

        // Create a mock recent activity timeline
        List<Map<String, String>> activity = new ArrayList<>();
        if (!reviews.isEmpty()) {
            activity.add(Map.of("time", "1 hour ago", "action", "Code Review performed (Score: " + averageScore + ")"));
        }
        if (!sqls.isEmpty()) {
            activity.add(Map.of("time", "3 hours ago", "action", "Database schema generated"));
        }
        if (!docs.isEmpty()) {
            activity.add(Map.of("time", "Yesterday", "action", "API Swagger documentation created"));
        }
        if (activity.isEmpty()) {
            activity.add(Map.of("time", "Just now", "action", "Project workspace initialized"));
        }

        stats.put("activityTimeline", activity);

        return ResponseEntity.ok(stats);
    }
}
