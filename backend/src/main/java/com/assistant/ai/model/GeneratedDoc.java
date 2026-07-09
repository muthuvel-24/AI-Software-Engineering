package com.assistant.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generated_docs")
public class GeneratedDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(name = "doc_type", nullable = false)
    private String docType; // "SWAGGER" or "OPENAPI"

    @Column(nullable = false, length = 100000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GeneratedDoc() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getName() { return name; }
    public String getDocType() { return docType; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setProject(Project project) { this.project = project; }
    public void setName(String name) { this.name = name; }
    public void setDocType(String docType) { this.docType = docType; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Project project;
        private String name, docType, content;

        public Builder project(Project project) { this.project = project; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder docType(String docType) { this.docType = docType; return this; }
        public Builder content(String content) { this.content = content; return this; }

        public GeneratedDoc build() {
            GeneratedDoc d = new GeneratedDoc();
            d.project = this.project; d.name = this.name;
            d.docType = this.docType; d.content = this.content;
            return d;
        }
    }
}
