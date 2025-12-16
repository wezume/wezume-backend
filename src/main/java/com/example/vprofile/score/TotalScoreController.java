package com.example.vprofile.score;

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
}