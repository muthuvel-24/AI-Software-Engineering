package com.assistant.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bug_reports")
public class BugReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false)
    private String severity; // "HIGH", "MEDIUM", "LOW"

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "suggested_fix", length = 5000)
    private String suggestedFix;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BugReport() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Review getReview() { return review; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public String getFilePath() { return filePath; }
    public Integer getLineNumber() { return lineNumber; }
    public String getSuggestedFix() { return suggestedFix; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setReview(Review review) { this.review = review; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Review review;
        private String title, description, severity, filePath, suggestedFix;
        private Integer lineNumber;

        public Builder review(Review review) { this.review = review; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder lineNumber(Integer lineNumber) { this.lineNumber = lineNumber; return this; }
        public Builder suggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; return this; }

        public BugReport build() {
            BugReport b = new BugReport();
            b.review = this.review; b.title = this.title;
            b.description = this.description; b.severity = this.severity;
            b.filePath = this.filePath; b.lineNumber = this.lineNumber;
            b.suggestedFix = this.suggestedFix;
            return b;
        }
    }
}
