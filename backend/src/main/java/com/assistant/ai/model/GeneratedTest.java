package com.assistant.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generated_tests")
public class GeneratedTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "test_content", nullable = false, length = 100000)
    private String testContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GeneratedTest() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getClassName() { return className; }
    public String getTestContent() { return testContent; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setProject(Project project) { this.project = project; }
    public void setClassName(String className) { this.className = className; }
    public void setTestContent(String testContent) { this.testContent = testContent; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Project project;
        private String className, testContent;

        public Builder project(Project project) { this.project = project; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder testContent(String testContent) { this.testContent = testContent; return this; }

        public GeneratedTest build() {
            GeneratedTest t = new GeneratedTest();
            t.project = this.project; t.className = this.className;
            t.testContent = this.testContent;
            return t;
        }
    }
}
