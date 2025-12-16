package com.example.vprofile.voicesearch;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.vprofile.VideoEmbedding.EmbeddingService;
import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service responsible for performing semantic voice searches on video embeddings.
 * It uses dynamic thresholding for high accuracy, filtering out low-relevance matches.
 */
@Service
public class VoiceSearchService {

    // Manual SLF4J Logger Declaration
    private static final Logger log = LoggerFactory.getLogger(VoiceSearchService.class); 

    // Configuration for High Accuracy Search
    // Multiplier for dynamic threshold: Balances precision (for simple queries) and recall (for complex queries).
    private static final double ACCURACY_MULTIPLIER = 0.75; 
    // CRITICAL: Minimum floor threshold. Must be high (0.50+) to filter out low-relevance/gibberish matches.
    private static final double MIN_FLOOR_THRESHOLD = 0.60; 


    private final SearchQueryRepository searchRepo;
    private final VideoRepository videoRepo;
    private final EmbeddingService embeddingService;
    private final ObjectMapper mapper;

    public VoiceSearchService(SearchQueryRepository searchRepo,
            VideoRepository videoRepo,
            EmbeddingService embeddingService,
            ObjectMapper mapper) {
        this.searchRepo = searchRepo;
        this.videoRepo = videoRepo;
        this.embeddingService = embeddingService;
        this.mapper = mapper;
        log.info("VoiceSearchService initialized. Accuracy Multiplier: {} | Floor Threshold: {}", 
                 ACCURACY_MULTIPLIER, MIN_FLOOR_THRESHOLD);
    }

    /**
     * Searches videos for a user based on a natural language query using cosine similarity
     * and a dynamic threshold.
     */
    public List<Video> search(Long userId, String query) throws Exception {
        log.info("Starting semantic search for user {} (Recruiter) with query: '{}'", userId, query);

        // 1. Get the query vector
        List<Double> queryVector = embeddingService.getEmbeddingForText(query);
        String jsonVector = mapper.writeValueAsString(queryVector);
        searchRepo.save(new SearchQuery(userId, query, jsonVector));

        // 2. Fetch videos and score them
        // Corrected Logic: Recruiters search all available videos (findAll), not just their own (findAllByUserId).
        List<Video> videos = videoRepo.findAll(); 
        List<Scored> scored = new ArrayList<>();
        
        log.debug("Fetched {} videos for scoring.", videos.size()); 

        for (Video v : videos) {
            if (v.getEmbeddingVector() == null) {
                log.warn("Skipping video ID {} because it has a null embedding vector.", v.getId());
                continue;
            }

            try {
                // Deserialize the stored video vector
                List<Double> videoVector = mapper.readValue(
                        v.getEmbeddingVector(),
                        new TypeReference<List<Double>>() {}
                );

                double similarity = cosineSimilarity(queryVector, videoVector);
                scored.add(new Scored(v, similarity));
                log.trace("Video ID {} scored with similarity: {}", v.getId(), similarity);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing embedding vector for video ID {}", v.getId(), e);
                // Continue to the next video instead of failing the whole search
            }
        }

        if (scored.isEmpty()) {
            log.info("No videos with valid embeddings found for scoring. Returning empty list.");
            return List.of();
        }

        // 3. Dynamic Threshold Calculation
        double maxScore = scored.stream()
                .map(s -> s.score)
                .max(Double::compareTo)
                .orElse(0.0);
        
        log.debug("Query '{}' Max Score found: {}", query, maxScore);
        
        // Dynamic Threshold: Multiplier applied to the max score
        double DYNAMIC_THRESHOLD = maxScore * ACCURACY_MULTIPLIER;
        
        // Final Threshold: Use the higher (stricter) of the dynamic threshold or the minimum floor.
        double FINAL_THRESHOLD = 0.60 > DYNAMIC_THRESHOLD ? 0.60 : DYNAMIC_THRESHOLD;
        
        log.info("Thresholds: Max Score={}, Dynamic ({}x)={}, Floor={}, Final={}", 
            maxScore, ACCURACY_MULTIPLIER, DYNAMIC_THRESHOLD, MIN_FLOOR_THRESHOLD, FINAL_THRESHOLD);

        // 4. Filter, Sort, and Limit
        List<Scored> filtered = scored.stream()
                .filter(s -> s.score >= FINAL_THRESHOLD)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                // .limit(20)
                .toList();

        if (filtered.isEmpty()) {
            log.debug("No videos exceeded the FINAL_THRESHOLD of {}. Returning empty list.", FINAL_THRESHOLD);
            return List.of(); 
        }
        
        log.info("Successfully found {} relevant videos.", filtered.size());

        // 5. Calculate and Set Confidence Percentage
        return filtered.stream()
                .map(s -> {
                    double finalMaxScore = maxScore > 0 ? maxScore : 1.0;
                    // Calculate confidence based on the current score relative to the best score
                    int percent = (int) Math.round((s.score / finalMaxScore) * 100);
                    s.video.setConfidence(percent);
                    log.debug("Video ID {} confidence set to {}% (Score: {}).", s.video.getId(), percent, s.score);
                    return s.video;
                })
                .toList();
    }

    /**
     * Calculates the cosine similarity between two L2-normalized vectors.
     * $$ \text{similarity} = \cos(\theta) = \frac{\mathbf{A} \cdot \mathbf{B}}{\|\mathbf{A}\| \|\mathbf{B}\|} $$
     */
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        // Avoid division by zero, though L2-normalized vectors should have norm > 0
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Internal class to hold a video and its similarity score.
     */
    private static class Scored {

        Video video;
        double score;

        Scored(Video video, double score) {
            this.video = video;
            this.score = score;
        }
    }
}