package com.example.vprofile.culturfit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/culture-fit-scores")
public class CultureFitController {

    @Autowired
    private CultureFitService service;

    /**
     * GET /api/scores - Get all culture fit scores
     */
    @GetMapping
    public ResponseEntity<List<CultureFitScore>> getAllScores() {
        try {
            List<CultureFitScore> scores = service.getAllScores();
            return ResponseEntity.ok(scores);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/scores/{id} - Get a specific score by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CultureFitScore> getScoreById(@PathVariable Integer id) {
        try {
            Optional<CultureFitScore> score = service.getScore(id);
            return score.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/culture-fit-scores/candidate/{candidateId} - Get all evaluations for a candidate
     */
    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<CultureFitScore>> getScoresByCandidateId(@PathVariable Integer candidateId) {
        try {
            List<CultureFitScore> scores = service.getScoresByCandidateId(candidateId);
            return ResponseEntity.ok(scores);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/culture-fit-scores/profile/{candidateId} - Get consolidated profile (User + Automated Scores + Evaluations)
     */
    @GetMapping("/profile/{candidateId}")
    public ResponseEntity<?> getCandidateProfile(@PathVariable Integer candidateId) {
        try {
            Map<String, Object> profile = service.getCandidateCultureProfile(candidateId);
            if (profile.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(profile);
            }
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createScore(@RequestBody Map<String, Object> request) {
        try {
            Integer candidateId = (Integer) request.get("candidateId");
            String evaluatorName = (String) request.get("evaluatorName");
            Integer communicationScore = (Integer) request.get("communicationScore");
            Integer teamworkScore = (Integer) request.get("teamworkScore");
            Integer adaptabilityScore = (Integer) request.get("adaptabilityScore");
            Integer valuesAlignmentScore = (Integer) request.get("valuesAlignmentScore");
            String feedback = (String) request.get("feedback");

            CultureFitScore savedScore = service.submitEvaluation(
                    candidateId, evaluatorName, communicationScore, teamworkScore,
                    adaptabilityScore, valuesAlignmentScore, feedback);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedScore);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}
