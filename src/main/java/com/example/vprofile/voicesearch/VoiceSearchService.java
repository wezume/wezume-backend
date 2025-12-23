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
 * Service responsible for performing semantic voice searches on video
 * embeddings.
 * It uses dynamic thresholding for high accuracy, filtering out low-relevance
 * matches.
 */
@Service
public class VoiceSearchService {

    // Manual SLF4J Logger Declaration
    private static final Logger log = LoggerFactory.getLogger(VoiceSearchService.class);

    // Configuration for High Accuracy Search
    // Multiplier for dynamic threshold: Balances precision (for simple queries) and
    // recall (for complex queries).
    private static final double ACCURACY_MULTIPLIER = 0.75;
    // CRITICAL: Minimum floor threshold. Must be high (0.70+) to filter out
    // low-relevance/gibberish matches.
    private static final double MIN_FLOOR_THRESHOLD = 0.70;

    private final SearchQueryRepository searchRepo;
    private final VideoRepository videoRepo;
    private final EmbeddingService embeddingService;
    private final ObjectMapper mapper;
    private final com.example.vprofile.logincredentials.UserRepository userRepo;

    public VoiceSearchService(SearchQueryRepository searchRepo,
            VideoRepository videoRepo,
            EmbeddingService embeddingService,
            ObjectMapper mapper,
            com.example.vprofile.logincredentials.UserRepository userRepo) {
        this.searchRepo = searchRepo;
        this.videoRepo = videoRepo;
        this.embeddingService = embeddingService;
        this.mapper = mapper;
        this.userRepo = userRepo;
        log.info("VoiceSearchService initialized. Accuracy Multiplier: {} | Floor Threshold: {}",
                ACCURACY_MULTIPLIER, MIN_FLOOR_THRESHOLD);
    }

    /**
     * Searches videos for a user based on a natural language query using cosine
     * similarity
     * and a dynamic threshold.
     */
    public List<Video> search(Long userId, String query) throws Exception {
        log.info("Starting semantic search for user {} (Recruiter) with query: '{}'", userId, query);

        // 1. Extract experience requirement from query
        Integer minExp = null;
        Integer maxExp = null;
        boolean isMinimumOnly = false; // For "above X years" or "X+ years"

        // Pattern 1: Range (e.g., "2-5 years", "3 to 6 years")
        java.util.regex.Pattern rangePattern = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:-|to)\\s*(\\d+)\\s*years?",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher rangeMatcher = rangePattern.matcher(query);

        if (rangeMatcher.find()) {
            minExp = Integer.parseInt(rangeMatcher.group(1));
            maxExp = Integer.parseInt(rangeMatcher.group(2));
            log.info("Extracted experience range: {}-{} years", minExp, maxExp);
        } else {
            // Pattern 2: Minimum (e.g., "above 2 years", "2+ years", "2 plus years")
            java.util.regex.Pattern minPattern = java.util.regex.Pattern.compile(
                    "(?:above|more than|over|atleast|at least|minimum)\\s*(\\d+)\\s*years?|" +
                            "(\\d+)\\s*(?:\\+|plus)\\s*years?",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher minMatcher = minPattern.matcher(query);

            if (minMatcher.find()) {
                minExp = Integer.parseInt(minMatcher.group(1) != null ? minMatcher.group(1) : minMatcher.group(2));
                isMinimumOnly = true;
                log.info("Extracted minimum experience: {}+ years", minExp);
            } else {
                // Pattern 3: Exact (e.g., "2 years", "3 years experience")
                java.util.regex.Pattern exactPattern = java.util.regex.Pattern.compile(
                        "\\b(\\d+)\\s*years?(?:\\s+(?:of\\s+)?experience)?\\b",
                        java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher exactMatcher = exactPattern.matcher(query);

                if (exactMatcher.find()) {
                    minExp = Integer.parseInt(exactMatcher.group(1));
                    maxExp = minExp; // Exact match
                    log.info("Extracted exact experience: {} years", minExp);
                }
            }
        }

        // 2. Get the query vector
        List<Double> queryVector = embeddingService.getEmbeddingForText(query);
        String jsonVector = mapper.writeValueAsString(queryVector);
        searchRepo.save(new SearchQuery(userId, query, jsonVector));

        // 3. Fetch videos and score them
        // Corrected Logic: Recruiters search all available videos (findAll), not just
        // their own (findAllByUserId).
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
                        new TypeReference<List<Double>>() {
                        });

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
        double dynamicThreshold = maxScore * ACCURACY_MULTIPLIER;

        // Adaptive Floor Threshold:
        // - Use 0.60 for semantic-only queries (e.g., "internship", "marketing")
        // - Use 0.70 for experience-filtered queries (e.g., "2-5 years experience")
        double adaptiveFloor = (minExp != null) ? MIN_FLOOR_THRESHOLD : 0.60;

        // Final Threshold: Use the higher (stricter) of the dynamic threshold or the
        // adaptive floor.
        double finalThreshold = Math.max(adaptiveFloor, dynamicThreshold);

        log.info("Thresholds: Max Score={}, Dynamic ({}x)={}, Floor={}, Final={}, HasExpFilter={}",
                maxScore, ACCURACY_MULTIPLIER, dynamicThreshold, adaptiveFloor, finalThreshold, (minExp != null));

        // 4. Filter, Sort, and Limit
        List<Scored> filtered = scored.stream()
                .filter(s -> s.score >= finalThreshold)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .toList();

        if (filtered.isEmpty()) {
            log.warn("No videos exceeded the FINAL_THRESHOLD of {}. Returning empty list.", finalThreshold);
            log.warn("Top 5 scores were: {}",
                    scored.stream()
                            .sorted((a, b) -> Double.compare(b.score, a.score))
                            .limit(5)
                            .map(s -> String.format("%.3f", s.score))
                            .toList());
            return List.of();
        }

        log.info("Successfully found {} relevant videos out of {} total.", filtered.size(), scored.size());

        // 5. Filter by Experience (if specified in query)
        final Integer finalMinExp = minExp;
        final Integer finalMaxExp = maxExp;
        final boolean finalIsMinimumOnly = isMinimumOnly;

        if (minExp != null) {
            filtered = filtered.stream()
                    .filter(s -> {
                        try {
                            // Try to get experience from User table first
                            com.example.vprofile.logincredentials.User user = userRepo.findById(s.video.getUserId())
                                    .orElse(null);
                            String experienceSource = null;

                            if (user != null && user.getExperience() != null && !user.getExperience().isBlank()) {
                                experienceSource = user.getExperience();
                                log.debug("Using experience from User table for video ID {}: {}", s.video.getId(),
                                        experienceSource);
                            } else if (s.video.getTranscription() != null && !s.video.getTranscription().isBlank()) {
                                // Fallback to transcription
                                experienceSource = s.video.getTranscription();
                                log.debug("Using experience from transcription for video ID {}", s.video.getId());
                            }

                            if (experienceSource == null) {
                                log.debug("Skipping video ID {} - no experience data found", s.video.getId());
                                return false;
                            }

                            // Parse experience from source
                            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                                    "(\\d+)(?:\\s*-\\s*(\\d+))?\\s*(?:years?|yrs?)",
                                    java.util.regex.Pattern.CASE_INSENSITIVE);
                            java.util.regex.Matcher m = p.matcher(experienceSource);

                            if (m.find()) {
                                int userMinExp = Integer.parseInt(m.group(1));
                                int userMaxExp = m.group(2) != null ? Integer.parseInt(m.group(2)) : userMinExp;

                                boolean matches;

                                if (finalIsMinimumOnly) {
                                    // Mode 1: Minimum only (e.g., "above 2 years", "2+ years")
                                    // Accept if user has >= required minimum
                                    matches = (userMinExp >= finalMinExp);
                                    if (!matches) {
                                        log.debug("Filtering out video ID {} - user has {} years, required {}+ years",
                                                s.video.getId(), userMinExp, finalMinExp);
                                    }
                                } else if (finalMaxExp != null && finalMinExp.equals(finalMaxExp)) {
                                    // Mode 2: Exact match (e.g., "2 years")
                                    // Accept if user has exactly the required years
                                    matches = (userMinExp == finalMinExp || userMaxExp == finalMinExp ||
                                            (userMinExp <= finalMinExp && userMaxExp >= finalMinExp));
                                    if (!matches) {
                                        log.debug(
                                                "Filtering out video ID {} - user has {}-{} years, required exactly {} years",
                                                s.video.getId(), userMinExp, userMaxExp, finalMinExp);
                                    }
                                } else if (finalMaxExp != null) {
                                    // Mode 3: Range (e.g., "2-5 years")
                                    // Accept if user's experience overlaps with required range
                                    matches = (userMinExp <= finalMaxExp && userMaxExp >= finalMinExp);
                                    if (!matches) {
                                        log.debug(
                                                "Filtering out video ID {} - user has {}-{} years, required {}-{} years",
                                                s.video.getId(), userMinExp, userMaxExp, finalMinExp, finalMaxExp);
                                    }
                                } else {
                                    // Fallback: treat as minimum
                                    matches = (userMinExp >= finalMinExp);
                                }

                                return matches;
                            }

                            log.debug("Could not parse experience from source for video ID {}", s.video.getId());
                            return false;
                        } catch (Exception e) {
                            log.error("Error filtering by experience for video ID {}", s.video.getId(), e);
                            return false;
                        }
                    })
                    .toList();

            log.info("After experience filtering: {} videos remain", filtered.size());
        }

        // 6. Calculate and Set Confidence Percentage
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
     * $$ \text{similarity} = \cos(\theta) = \frac{\mathbf{A} \cdot
     * \mathbf{B}}{\|\mathbf{A}\| \|\mathbf{B}\|} $$
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