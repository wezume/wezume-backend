package com.example.vprofile.score;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TotalScoreService {

    @Autowired
    private FacialScoringRepository facialScoringRepository;

    @Autowired
    private SpeechScoreRepository speechScoreRepository;

    @Autowired
    private TotalScoreRepository totalScoreRepository;

    public void computeTotalScoreIfReady(Long videoId) {
        // Skip if already scored
        if (totalScoreRepository.findByVideoId(videoId).isPresent()) {
            return;
        }

        Optional<FacialScoring> facialOpt = facialScoringRepository.findByVideoId(videoId);
        Optional<SpeechScore> audioOpt = speechScoreRepository.findByVideoId(videoId);

        if (facialOpt.isEmpty() || audioOpt.isEmpty())
            return;

        FacialScoring facial = facialOpt.get();
        SpeechScore audio = audioOpt.get();

        // Clarity Score — keep raw value, floor to 1 decimal
        double clarityRaw = audio.getFillerWordScore() + audio.getSpeechRateScore() +
                audio.getSentenceStructureScore() + audio.getArticulationScore();
        double clarity = Math.floor(clarityRaw * 10.0) / 10.0;

        // Confidence Score — fix duplicate usage
        double confidenceRaw = facial.getEyeContactScore() + facial.getStraightFaceScore() +
                audio.getPitchScore() + facial.getSmileScore();
        double confidence = Math.floor(confidenceRaw * 10.0) / 10.0;

        // Authenticity Score
        double authenticityRaw = facial.getSmileScore() + audio.getToneScore() +
                facial.getStraightFaceScore() + audio.getSpeechRateScore();
        double authenticity = Math.floor(authenticityRaw * 10.0) / 10.0;

        // Emotional Score
        double emotionalRaw = audio.getSpeechRateScore() + audio.getToneScore() +
                facial.getStraightFaceScore() + audio.getEnergyScore();
        double emotional = Math.floor(emotionalRaw * 10.0) / 10.0;

        // Total Score — sum of raw scores, floor to 1 decimal
        double totalRaw = clarityRaw + confidenceRaw + authenticityRaw + emotionalRaw;
        double total = Math.floor((totalRaw / 40.0) * 100.0) / 10.0;


        // Save TotalScore
        TotalScore score = new TotalScore();
        score.setVideoId(videoId);
        score.setClarityScore(clarity);
        score.setConfidenceScore(confidence);
        score.setAuthenticityScore(authenticity);
        score.setEmotionalScore(emotional);
        score.setTotalScore(total);

        totalScoreRepository.save(score);
    }
}
