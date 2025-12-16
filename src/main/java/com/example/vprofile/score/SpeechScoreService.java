package com.example.vprofile.score;
import org.springframework.stereotype.Service;

@Service
public class SpeechScoreService {

    private final SpeechScoreRepository repository;

    public SpeechScoreService(SpeechScoreRepository repository) {
        this.repository = repository;
    }

    public SpeechScore saveScore(SpeechScore score) {
        // Calculate total score
        score.setTotalScore(
            score.getPitchScore() + 
            score.getEnergyScore() + 
            score.getToneScore() + 
            score.getEmotionScore()
        );
        return repository.save(score);
    }

    public SpeechScore getScoreByVideoId(Long videoId) {
        // Optional: throw error if not found
        return repository.findByVideoId(videoId).orElse(null);
    }

    public boolean isAnalysisAlreadyPerformed(Long videoId) {
        return repository.findByVideoId(videoId).isPresent(); // ✅ Correct usage
    }
}
