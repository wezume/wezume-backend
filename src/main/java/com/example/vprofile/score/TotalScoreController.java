package com.example.vprofile.score;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/totalscore")
public class TotalScoreController {

    @Autowired
    private TotalScoreRepository totalScoreRepository;

    @GetMapping("/{videoId}")
    public ResponseEntity<?> getScoreByVideoId(@PathVariable Long videoId) {
        Optional<TotalScore> scoreOpt = totalScoreRepository.findByVideoId(videoId);

        if (scoreOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("❌ No total score found for videoId: " + videoId);
        }

        return ResponseEntity.ok(scoreOpt.get());
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<?> getScoreByVideoId1(@PathVariable Long videoId) {

        Optional<TotalScore> score = totalScoreRepository.findByVideoId(videoId);

        if (score.isPresent()) {
            return ResponseEntity.ok(score.get());
        } else {
            return ResponseEntity.status(404).body("Score not found for this video");
        }
    }

    @GetMapping("/video/{videoId}/percentile")
    public ResponseEntity<?> getPercentileByVideoId(@PathVariable Long videoId) {

        Optional<TotalScore> scoreOpt = totalScoreRepository.findByVideoId(videoId);

        if (scoreOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Score not found");
        }

        TotalScore score = scoreOpt.get();

        double totalScore = score.getTotalScore();
        double maxScore = 10.0;

        double percentile = Math.round((totalScore / maxScore) * 100);

        Map<String, Object> response = new HashMap<>();
        response.put("percentile", percentile);
        response.put("message", "Better than " + (int) percentile + "% of candidates");

        return ResponseEntity.ok(response);
    }
}
