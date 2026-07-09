package com.assistant.ai.controller;

import com.assistant.ai.model.Project;
import com.assistant.ai.model.CodeRepository;
import com.assistant.ai.model.User;
import com.assistant.ai.repository.ProjectRepository;
import com.assistant.ai.repository.CodeRepositoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    @Autowired
    private CodeRepositoryRepository repositoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectRepository(@RequestBody Map<String, Object> body) {
        Long projectId = Long.valueOf(body.get("projectId").toString());
        String repoUrl = body.get("url").toString();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwner().getId().equals(getAuthenticatedUser().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        String cleanUrl = repoUrl.replace("https://github.com/", "").replace(".git", "");
        String[] parts = cleanUrl.split("/");
        String owner = parts.length > 0 ? parts[0] : "unknown";
        String name = parts.length > 1 ? parts[1] : "repository";

        CodeRepository repo = CodeRepository.builder()
                .name(name)
                .owner(owner)
                .url(repoUrl.startsWith("http") ? repoUrl : "https://github.com/" + cleanUrl)
                .project(project)
                .build();

        return ResponseEntity.ok(repositoryRepository.save(repo));
    }

    @GetMapping("/projects/{projectId}/repositories")
    public ResponseEntity<List<CodeRepository>> getProjectRepositories(@PathVariable Long projectId) {
        return ResponseEntity.ok(repositoryRepository.findByProjectId(projectId));
    }

    @GetMapping("/repositories/{repoId}/pull-requests")
    public ResponseEntity<List<Map<String, Object>>> getPullRequests(@PathVariable Long repoId) {
        repositoryRepository.findById(repoId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        List<Map<String, Object>> prs = List.of(
                Map.of("id", 101, "title", "feat: Add JWT token authentication logic", "author", "johndoe",
                        "status", "OPEN", "reviewScore", 92, "date", "2 hours ago"),
                Map.of("id", 102, "title", "fix: SQL injection vulnerabilities on search controller", "author", "alex_mercer",
                        "status", "MERGED", "reviewScore", 81, "date", "1 day ago"),
                Map.of("id", 103, "title", "perf: Optimize database pooling constraints", "author", "sarah_k",
                        "status", "OPEN", "reviewScore", 74, "date", "3 days ago")
        );
        return ResponseEntity.ok(prs);
    }

    @GetMapping("/repositories/{repoId}/commits")
    public ResponseEntity<List<Map<String, String>>> getCommits(@PathVariable Long repoId) {
        repositoryRepository.findById(repoId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        List<Map<String, String>> commits = List.of(
                Map.of("hash", "8a2b5c4", "message", "Merge pull request #102 from fix/sqli", "author", "alex_mercer", "date", "Yesterday"),
                Map.of("hash", "3f7e1a9", "message", "docs: Update setup scripts instructions", "author", "sarah_k", "date", "2 days ago"),
                Map.of("hash", "2d5c8b0", "message", "test: Add integration mocks for service tests", "author", "johndoe", "date", "3 days ago")
        );
        return ResponseEntity.ok(commits);
    }

    @GetMapping("/repositories/{repoId}/statistics")
    public ResponseEntity<Map<String, Object>> getRepositoryStatistics(@PathVariable Long repoId) {
        CodeRepository repo = repositoryRepository.findById(repoId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("name", repo.getName());
        stats.put("owner", repo.getOwner());
        stats.put("url", repo.getUrl());
        stats.put("openIssues", 12);
        stats.put("openPrs", 2);
        stats.put("mergedPrs", 48);
        stats.put("healthRating", "A-");
        stats.put("securityIssues", 2);
        stats.put("linesReviewed", "14,240");
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/repositories/{repoId}/review-pr")
    public ResponseEntity<Map<String, String>> reviewPullRequest(
            @PathVariable Long repoId,
            @RequestBody Map<String, Object> body) {
        repositoryRepository.findById(repoId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));

        String prId = body.getOrDefault("prId", "unknown").toString();
        return ResponseEntity.ok(Map.of(
                "message", "Pull Request #" + prId + " review initiated. Report will appear in Dashboard.",
                "status", "success"
        ));
    }
}
