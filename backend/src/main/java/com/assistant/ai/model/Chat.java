package com.assistant.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "chats")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Chat() {}

    public Chat(Long id, String title, Project project, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.project = project;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Project getProject() { return project; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setProject(Project project) { this.project = project; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String title;
        private Project project;

        public Builder title(String title) { this.title = title; return this; }
        public Builder project(Project project) { this.project = project; return this; }

        public Chat build() {
            Chat c = new Chat();
            c.title = this.title;
            c.project = this.project;
            return c;
        }
    }
}
