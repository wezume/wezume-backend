package com.example.vprofile.voicesearch;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;

/**
 * Service responsible for performing keyword-based video searches.
 * Uses adaptive matching threshold: 1+ keyword match for small queries (1-3
 * keywords),
 * 50%+ match for larger queries (4+ keywords).
 */
@Service
public class VoiceSearchService {

    private static final Logger log = LoggerFactory.getLogger(VoiceSearchService.class);

    private final SearchQueryRepository searchRepo;
    private final VideoRepository videoRepo;

    public VoiceSearchService(SearchQueryRepository searchRepo, VideoRepository videoRepo) {
        this.searchRepo = searchRepo;
        this.videoRepo = videoRepo;
        log.info("VoiceSearchService initialized with adaptive keyword matching");
    }

    /**
     * Searches videos for a user based on keyword matching against transcriptions.
     * Liberal matching: Returns videos with 20%+ keyword match.
     */
    public List<Video> search(Long userId, String query) throws Exception {
        log.info("Starting keyword-based search for user {} with query: '{}'", userId, query);

        // 1. Extract keywords from query
        List<String> keywords = extractKeywords(query);
        log.info("Extracted {} keywords: {}", keywords.size(), keywords);

        if (keywords.isEmpty()) {
            log.warn("No keywords extracted from query. Returning empty list.");
            return List.of();
        }

        // 2. Save search query (without embedding)
        searchRepo.save(new SearchQuery(userId, query, null));

        // 3. Fetch all videos
        List<Video> videos = videoRepo.findAll();
        List<Scored> scored = new ArrayList<>();

        log.debug("Fetched {} videos for keyword matching.", videos.size());

        // 4. LIBERAL MATCHING THRESHOLD
        // Accept videos if at least 20% of keywords match (or minimum 1 keyword)
        int minMatchPercentage = 20; // Reduced from 50% to 20% for liberal matching

        log.debug("Liberal matching: accepting {}%+ matches for {} keywords",
                minMatchPercentage, keywords.size());

        // 5. Score each video based on keyword matching
        for (Video v : videos) {
            if (v.getTranscription() == null || v.getTranscription().isBlank()) {
                log.warn("Skipping video ID {} because it has no transcription.", v.getId());
                continue;
            }

            // Calculate match percentage (now with partial matching support)
            int matchPercentage = calculateKeywordMatch(keywords, v.getTranscription());

            if (matchPercentage >= minMatchPercentage) {
                scored.add(new Scored(v, matchPercentage));
                log.debug("Video ID {} matched with {}%", v.getId(), matchPercentage);
            }
        }

        if (scored.isEmpty()) {
            log.info("No videos found matching the search criteria. Returning empty list.");
            return List.of();
        }

        // 5. Sort by match percentage (highest first)
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        log.info("Found {} matching videos. Top match: {}%",
                scored.size(),
                scored.isEmpty() ? 0 : (int) scored.get(0).score);

        // 6. Set confidence scores and return videos
        return scored.stream()
                .map(s -> {
                    s.video.setConfidence((int) s.score);
                    log.debug("Video ID {} confidence set to {}%", s.video.getId(), (int) s.score);
                    return s.video;
                })
                .toList();
    }

    /**
     * Extract keywords from query text
     */
    private List<String> extractKeywords(String query) {
        java.util.Set<String> stopWords = java.util.Set.of(
                "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
                "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
                "to", "was", "will", "with", "i", "me", "my", "we", "you", "your");

        List<String> keywords = new ArrayList<>();

        // Split by common delimiters and extract words
        String[] parts = query.toLowerCase().split("[,;|\\s]+");

        for (String word : parts) {
            // Clean word: remove special characters except hyphens and plus
            word = word.replaceAll("[^a-z0-9+\\-]", "");

            // Keep words that are not stop words and have length > 1
            if (!word.isEmpty() && word.length() > 1 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * Calculate keyword match percentage between query keywords and transcription.
     * Uses LIBERAL matching: accepts both exact word matches and partial matches.
     * Example: 10 total keywords, 9 matched = 90%, 10 matched = 100%
     */
    private int calculateKeywordMatch(List<String> keywords, String transcription) {
        String normalizedTranscription = transcription.toLowerCase();
        int matchedCount = 0;
        List<String> matchedKeywords = new ArrayList<>();
        List<String> unmatchedKeywords = new ArrayList<>();

        for (String keyword : keywords) {
            boolean matched = false;

            // Try 1: Exact word boundary match (highest priority)
            String pattern = "\\b" + java.util.regex.Pattern.quote(keyword) + "\\b";
            if (normalizedTranscription.matches(".*" + pattern + ".*")) {
                matched = true;
            }

            // Try 2: Partial/substring match (liberal matching)
            // E.g., "market" matches "marketing", "python" matches "pythonic"
            if (!matched && normalizedTranscription.contains(keyword)) {
                matched = true;
            }

            if (matched) {
                matchedCount++;
                matchedKeywords.add(keyword);
            } else {
                unmatchedKeywords.add(keyword);
            }
        }

        // Calculate percentage
        int percentage = keywords.isEmpty() ? 0 : (int) Math.round((matchedCount * 100.0) / keywords.size());

        log.debug("Keyword matching: {}/{} matched ({}%) | Matched: {} | Unmatched: {}",
                matchedCount, keywords.size(), percentage, matchedKeywords, unmatchedKeywords);

        return percentage;
    }

    /**
     * Internal class to hold a video and its match score.
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