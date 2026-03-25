package com.example.vprofile.voicesearch;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;

@Service
public class VoiceSearchService {

    private static final Logger log = LoggerFactory.getLogger(VoiceSearchService.class);

    private final SearchQueryRepository searchRepo;
    private final VideoRepository videoRepo;

    public VoiceSearchService(SearchQueryRepository searchRepo, VideoRepository videoRepo) {
        this.searchRepo = searchRepo;
        this.videoRepo = videoRepo;
    }
    public List<Video> search(Long userId, String query, String jobId) throws Exception {
        List<Video> videos;

        if (jobId != null && !jobId.isBlank()) {
            videos = videoRepo.findAllByJobId(jobId);
        } else {
            videos = videoRepo.findAll();
        }
        if (query == null || query.isBlank()) {
            videos.forEach(v -> v.setConfidence(100));
            return videos;
        }
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }
        searchRepo.save(new SearchQuery(userId, query, null));
        List<Scored> scored = new ArrayList<>();
        int minMatchPercentage = 20;
        for (Video v : videos) {
            if (v.getTranscription() == null || v.getTranscription().isBlank()) {
                continue;
            }

            int matchPercentage = calculateKeywordMatch(keywords, v.getTranscription());

            if (matchPercentage >= minMatchPercentage) {
                scored.add(new Scored(v, matchPercentage));
            }
        }

        if (scored.isEmpty()) {
            return List.of();
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.stream()
                .map(s -> {
                    s.video.setConfidence((int) s.score);
                    return s.video;
                })
                .toList();
    }

    /**
     * Extract keywords from query
     */
    private List<String> extractKeywords(String query) {
        java.util.Set<String> stopWords = java.util.Set.of(
                "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
                "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
                "to", "was", "will", "with", "i", "me", "my", "we", "you", "your");

        List<String> keywords = new ArrayList<>();
        String[] parts = query.toLowerCase().split("[,;|\\s]+");

        for (String word : parts) {
            word = word.replaceAll("[^a-z0-9+\\-]", "");

            if (!word.isEmpty() && word.length() > 1 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    /**
     * Liberal keyword matching with partial support
     */
    private int calculateKeywordMatch(List<String> keywords, String transcription) {
        String text = transcription.toLowerCase();
        int matched = 0;

        for (String keyword : keywords) {
            boolean found = false;

            // Exact word match
            String pattern = "\\b" + java.util.regex.Pattern.quote(keyword) + "\\b";
            if (text.matches(".*" + pattern + ".*")) {
                found = true;
            }

            // Partial match
            if (!found && text.contains(keyword)) {
                found = true;
            }

            if (found) {
                matched++;
            }
        }

        return (int) Math.round((matched * 100.0) / keywords.size());
    }

    /**
     * Internal score holder
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
