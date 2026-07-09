package com.assistant.ai.dto;

public class AgentRequest {
    private Long projectId;
    private String prompt;
    private String provider; // "gemini", "openai"
    private String model;
    private String apiKey;

    public AgentRequest() {}

    public Long getProjectId() { return projectId; }
    public String getPrompt() { return prompt; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getApiKey() { return apiKey; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setModel(String model) { this.model = model; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
