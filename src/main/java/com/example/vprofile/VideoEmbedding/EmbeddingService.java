package com.example.vprofile.VideoEmbedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service responsible for generating and retrieving embedding vectors 
 * by interacting with the external BGE FastAPI service.
 * Implements dynamic query logic for combined searches.
 */
@Service
public class EmbeddingService {

    @Autowired
    private VideoRepository videoRepo;

    @Autowired
    private ObjectMapper mapper;

    // IMPORTANT: Update this URL to match your running FastAPI server
    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://app.wezume.in:8000") 
            .defaultHeader("token", "AYMEN_BGE_2025")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    // --- Core Video Embedding (Key Point Extraction) ---

    /**
     * Generates a highly focused embedding for a video using semantic key point extraction.
     */
    public void generateEmbeddingFor(Video video) throws Exception {
        
        // ⭐ Job-Focused Guide Query for Management/Leadership roles
        // This ensures the stored vector is highly relevant to management skills.
        String jobFocusedGuideQuery = 
            "What are the leadership skills, organizational experience, and communication strengths demonstrated by the speaker for a management job role?";

        Map<String, Object> body = Map.of(
                "transcription", video.getTranscription(),
                "guide_query", jobFocusedGuideQuery 
        );

        Map<String, Object> response = webClient.post()
                .uri("/keypoints") // Calling the dynamic key points endpoint in FastAPI
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("embedding")) {
            return;
        }

        List<Double> vector = (List<Double>) response.get("embedding");
        String jsonVector = mapper.writeValueAsString(vector);

        video.setEmbeddingVector(jsonVector);
        videoRepo.save(video);
    }
    
    // --- Core Search Query Embedding (Dynamic Logic) ---

    /**
     * Generates a 'query' mode embedding for text input. 
     * Handles combined searches ("finance and marketing") by averaging vectors.
     */
    public List<Double> getEmbeddingForText(String query) throws Exception {
        
        // Dynamic check for combination queries (e.g., "finance and marketing")
        if (query.toLowerCase().contains(" and ")) {
            String sanitizedQuery = query.toLowerCase()
                .replace("i am looking for the person who is good at", "")
                .replace("i am looking for the pearson who is good at", "")
                .trim();
                
            String[] parts = sanitizedQuery.split(" and ");
            
            if (parts.length >= 2) {
                String concept1 = parts[0].trim();
                String concept2 = parts[1].trim();
                
                // 1. Embed individual concepts
                // Use focused prompts for better concept vectors
                List<Double> vector1 = getRawVector("query: " + concept1 + " expertise and experience.");
                List<Double> vector2 = getRawVector("query: " + concept2 + " expertise and experience.");
                
                // 2. Combine/Average the vectors
                // 
                return combineVectors(vector1, vector2);
            }
        }

        // Default: Fallback to single query embedding (e.g., "finance" or a full sentence)
        return getRawVector("query: " + query);
    }

    /**
     * Private utility method to get a raw vector for a specific piece of text from the FastAPI /embed endpoint.
     */
    private List<Double> getRawVector(String text) throws Exception {
        Map<String, Object> body = Map.of(
            "input", text,
            "type", "query"
        );

        Map<String, Object> response = webClient.post()
            .uri("/embed")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Failed to retrieve embedding for query: " + text);
        }
        return (List<Double>) response.get("embedding");
    }

    /**
     * Utility method to calculate the average of two vectors, which serves as the combined vector.
     */
    private List<Double> combineVectors(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vector sizes must match for averaging.");
        }
        
        List<Double> combined = new ArrayList<>(vec1.size());
        for (int i = 0; i < vec1.size(); i++) {
            // Calculate the average (V1[i] + V2[i]) / 2.0
            combined.add((vec1.get(i) + vec2.get(i)) / 2.0);
        }
        return combined;
    }
}