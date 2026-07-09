package com.assistant.ai.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VectorDBService {

    // Store in-memory document chunks keyed by project ID
    private final Map<Long, List<DocumentChunk>> projectChunks = new ConcurrentHashMap<>();

    public static class DocumentChunk {
        private final String sourceName;
        private final String content;

        public DocumentChunk(String sourceName, String content) {
            this.sourceName = sourceName;
            this.content = content;
        }

        public String getSourceName() {
            return sourceName;
        }

        public String getContent() {
            return content;
        }
    }

    public void ingestDocument(Long projectId, String fileName, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // Split text into chunks of roughly 500 characters, overlap by 100
        List<DocumentChunk> newChunks = new ArrayList<>();
        String[] paragraphs = content.split("(?:\r?\n){2,}"); // Split by double newlines

        for (String para : paragraphs) {
            if (para.trim().length() > 20) {
                newChunks.add(new DocumentChunk(fileName, para.trim()));
            }
        }

        projectChunks.computeIfAbsent(projectId, k -> Collections.synchronizedList(new ArrayList<>()))
                .addAll(newChunks);
    }

    public String retrieveContext(Long projectId, String query, int limit) {
        List<DocumentChunk> chunks = projectChunks.get(projectId);
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        // Simple scoring based on keyword overlap (simulating TF-IDF/embeddings)
        String[] queryTerms = query.toLowerCase().split("\\s+");

        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            double score = 0;
            String contentLower = chunk.getContent().toLowerCase();
            for (String term : queryTerms) {
                if (term.length() > 2 && contentLower.contains(term)) {
                    score += 1.0;
                }
            }
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }

        if (scored.isEmpty()) {
            // Return top chunks by default if no matching terms
            return chunks.stream()
                    .limit(limit)
                    .map(c -> "[Source: " + c.getSourceName() + "]\n" + c.getContent())
                    .collect(Collectors.joining("\n\n"));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        return scored.stream()
                .limit(limit)
                .map(sc -> "[Source: " + sc.chunk.getSourceName() + " (relevance: " + String.format("%.1f", sc.score) + ")]\n" + sc.chunk.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    public void clearProjectChunks(Long projectId) {
        projectChunks.remove(projectId);
    }

    private static class ScoredChunk {
        final DocumentChunk chunk;
        final double score;

        ScoredChunk(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
