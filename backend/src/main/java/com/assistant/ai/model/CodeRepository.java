package com.assistant.ai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "repositories")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CodeRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    public CodeRepository() {}

    @PrePersist
    protected void onCreate() { connectedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public String getUrl() { return url; }
    public Project getProject() { return project; }
    public LocalDateTime getConnectedAt() { return connectedAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setUrl(String url) { this.url = url; }
    public void setProject(Project project) { this.project = project; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name, owner, url;
        private Project project;

        public Builder name(String name) { this.name = name; return this; }
        public Builder owner(String owner) { this.owner = owner; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder project(Project project) { this.project = project; return this; }

        public CodeRepository build() {
            CodeRepository r = new CodeRepository();
            r.name = this.name; r.owner = this.owner;
            r.url = this.url; r.project = this.project;
            return r;
        }
    }
}
