package com.example.vprofile.score;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.vprofile.videofolder.VideoRepository;

@RestController
@RequestMapping("/api/facial-score")
public class FacialScoreController {
    private static final Logger logger = LoggerFactory.getLogger(FacialScoreController.class);

    @Autowired
    private final FacialScoringRepository facialScoringRepository;
    @Autowired
    private final FacialScoringService facialScoringService;
    private final VideoRepository videoRepository;

    public FacialScoreController(FacialScoringService facialScoringService,
            VideoRepository videoRepository,
            FacialScoringRepository facialScoringRepository) {
        this.facialScoringService = facialScoringService;
        this.videoRepository = videoRepository;
        this.facialScoringRepository = facialScoringRepository;
    }

    @GetMapping
    public ResponseEntity<Double> scoreVideo(
            @RequestParam("videoId") Long videoId,
            @RequestParam("url") String videoUrl) {

        // 1. Validate if video exists in DB
        if (!videoRepository.existsById(videoId)) {
            return ResponseEntity.badRequest().body(null);
        }

        // 2. Check if video already scored
        Optional<FacialScoring> existingScore = facialScoringRepository.findByVideoId(videoId);
        if (existingScore.isPresent()) {
            logger.info("ℹ️ Video {} already scored. Returning cached score: {}", videoId,
                    existingScore.get().getTotalScore());
            return ResponseEntity.ok(existingScore.get().getTotalScore());
        }

        // 3. Check if 'analyze' directory exists, create if not
        Path analyzeDir = Paths.get("analyze"); // relative to the project root
        try {
            if (!Files.exists(analyzeDir)) {
                Files.createDirectories(analyzeDir);
                System.out.println("📁 'analyze' directory created at: " + analyzeDir.toAbsolutePath());
            } else {
                System.out.println("✅ 'analyze' directory already exists at: " + analyzeDir.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to create 'analyze' directory: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

        // 4. Directly analyze video from the URL
        try {
            double score = facialScoringService.analyzeVideoAndScore(videoUrl, videoId);
            return ResponseEntity.ok(score);
        } catch (Exception e) {
            System.err.println("❌ Error analyzing video: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

}
