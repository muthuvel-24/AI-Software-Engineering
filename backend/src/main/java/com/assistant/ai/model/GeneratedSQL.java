package com.assistant.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generated_sql")
public class GeneratedSQL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Column(name = "sql_content", nullable = false, length = 20000)
    private String sqlContent;

    @Column(name = "optimized_sql", length = 20000)
    private String optimizedSql;

    @Column(length = 20000)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GeneratedSQL() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getPrompt() { return prompt; }
    public String getSqlContent() { return sqlContent; }
    public String getOptimizedSql() { return optimizedSql; }
    public String getExplanation() { return explanation; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setProject(Project project) { this.project = project; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setSqlContent(String sqlContent) { this.sqlContent = sqlContent; }
    public void setOptimizedSql(String optimizedSql) { this.optimizedSql = optimizedSql; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Project project;
        private String prompt, sqlContent, optimizedSql, explanation;

        public Builder project(Project project) { this.project = project; return this; }
        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder sqlContent(String sqlContent) { this.sqlContent = sqlContent; return this; }
        public Builder optimizedSql(String optimizedSql) { this.optimizedSql = optimizedSql; return this; }
        public Builder explanation(String explanation) { this.explanation = explanation; return this; }

        public GeneratedSQL build() {
            GeneratedSQL s = new GeneratedSQL();
            s.project = this.project; s.prompt = this.prompt;
            s.sqlContent = this.sqlContent; s.optimizedSql = this.optimizedSql;
            s.explanation = this.explanation;
            return s;
        }
    }
}
