package com.assistant.ai.dto;

public class AuthResponse {
    private String token;
    private Long id;
    private String email;
    private String name;

    public AuthResponse() {}

    public AuthResponse(String token, Long id, String email, String name) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public String getToken() { return token; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }

    public void setToken(String token) { this.token = token; }
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String token, email, name;
        private Long id;

        public Builder token(String token) { this.token = token; return this; }
        public Builder id(Long id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder name(String name) { this.name = name; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, id, email, name);
        }
    }
}
