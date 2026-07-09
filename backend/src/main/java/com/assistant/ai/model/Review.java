package com.assistant.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private int score;
    private int readability;
    private int maintainability;
    private int security;
    private int performance;
    private int architecture;

    @Column(name = "report_content", nullable = false, length = 100000)
    private String reportContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Review() {}

    public Review(Long id, Project project, int score, int readability, int maintainability,
                  int security, int performance, int architecture, String reportContent, LocalDateTime createdAt) {
        this.id = id; this.project = project; this.score = score;
        this.readability = readability; this.maintainability = maintainability;
        this.security = security; this.performance = performance;
        this.architecture = architecture; this.reportContent = reportContent;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public int getScore() { return score; }
    public int getReadability() { return readability; }
    public int getMaintainability() { return maintainability; }
    public int getSecurity() { return security; }
    public int getPerformance() { return performance; }
    public int getArchitecture() { return architecture; }
    public String getReportContent() { return reportContent; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setProject(Project project) { this.project = project; }
    public void setScore(int score) { this.score = score; }
    public void setReadability(int readability) { this.readability = readability; }
    public void setMaintainability(int maintainability) { this.maintainability = maintainability; }
    public void setSecurity(int security) { this.security = security; }
    public void setPerformance(int performance) { this.performance = performance; }
    public void setArchitecture(int architecture) { this.architecture = architecture; }
    public void setReportContent(String reportContent) { this.reportContent = reportContent; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Project project;
        private int score, readability, maintainability, security, performance, architecture;
        private String reportContent;

        public Builder project(Project project) { this.project = project; return this; }
        public Builder score(int score) { this.score = score; return this; }
        public Builder readability(int readability) { this.readability = readability; return this; }
        public Builder maintainability(int maintainability) { this.maintainability = maintainability; return this; }
        public Builder security(int security) { this.security = security; return this; }
        public Builder performance(int performance) { this.performance = performance; return this; }
        public Builder architecture(int architecture) { this.architecture = architecture; return this; }
        public Builder reportContent(String reportContent) { this.reportContent = reportContent; return this; }

        public Review build() {
            Review r = new Review();
            r.project = this.project; r.score = this.score;
            r.readability = this.readability; r.maintainability = this.maintainability;
            r.security = this.security; r.performance = this.performance;
            r.architecture = this.architecture; r.reportContent = this.reportContent;
            return r;
        }
    }
}
