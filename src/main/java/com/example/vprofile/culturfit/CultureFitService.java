package com.example.vprofile.culturfit;

import com.example.vprofile.logincredentials.User;
import com.example.vprofile.logincredentials.UserRepository;
import com.example.vprofile.score.FacialScoring;
import com.example.vprofile.score.FacialScoringRepository;
import com.example.vprofile.score.SpeechScore;
import com.example.vprofile.score.SpeechScoreRepository;
import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CultureFitService {

    @Autowired
    private CultureFitRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private SpeechScoreRepository speechScoreRepository;

    @Autowired
    private FacialScoringRepository facialScoringRepository;

    public List<CultureFitScore> getAllScores() {
        return repository.findAll();
    }

    public Optional<CultureFitScore> getScore(Integer id) {
        return repository.findById(id);
    }

    public List<CultureFitScore> getScoresByCandidateId(Integer candidateId) {
        return repository.findByCandidateId(candidateId);
    }

    /**
     * Consolidates Candidate data for Culture Fit dashboard
     */
    public Map<String, Object> getCandidateCultureProfile(Integer candidateId) {
        Map<String, Object> profile = new HashMap<>();
        Long userId = candidateId.longValue();

        // 1. Get User Details
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            profile.put("candidateId", candidateId);
            profile.put("name", user.getFirstName() + " " + user.getLastName());
            profile.put("email", user.getEmail());
            profile.put("jobRole", user.getCurrentRole());
        } else {
            profile.put("error", "Candidate not found in User table");
            return profile;
        }

        // 2. Get Video & Automated Scores
        Optional<Video> videoOpt = videoRepository.findByUserId(userId);
        if (videoOpt.isPresent()) {
            Long videoId = videoOpt.get().getId();
            Optional<SpeechScore> speechOpt = speechScoreRepository.findByVideoId(videoId);
            Optional<FacialScoring> facial = facialScoringRepository.findByVideoId(videoId);

            profile.put("automatedScores", Map.of(
                "speech", speechOpt.isPresent() ? speechOpt.get() : "No speech score found",
                "facial", facial.isPresent() ? facial.get() : "No facial score found",
                "videoId", videoId
            ));
        } else {
            profile.put("automatedScores", "No video found for candidate");
        }

        // 3. Get Recruiter Evaluations
        profile.put("evaluations", repository.findByCandidateId(candidateId));

        return profile;
    }

    /**
     * Computes culture fit scores for a list of videoIds.
     * Uses raw SpeechScore + FacialScoring sub-scores, mirrors the culture.jsx formula.
     * Returns map of videoId -> [teamwork, customer, integrity, innovation, excellence] (each 1-5).
     *
     * Normalization ranges from DB:
     *   Speech (emotion, pitch, energy, tone, speechRate, fillerWord, articulation): max = 2
     *   Facial smile: max = 1 | eyeContact, straightFace: max = 2.5
     */
    public Map<Long, double[]> computeBulkCultureScores(List<Long> videoIds) {
        List<SpeechScore> speechList = speechScoreRepository.findAllByVideoIdIn(videoIds);
        List<FacialScoring> facialList = facialScoringRepository.findAllByVideoIdIn(videoIds);

        Map<Long, SpeechScore> speechMap = new HashMap<>();
        for (SpeechScore s : speechList) speechMap.put(s.getVideoId(), s);

        Map<Long, FacialScoring> facialMap = new HashMap<>();
        for (FacialScoring f : facialList) facialMap.put(f.getVideoId(), f);

        Map<Long, double[]> result = new HashMap<>();
        for (Long videoId : videoIds) {
            SpeechScore s = speechMap.get(videoId);
            FacialScoring f = facialMap.get(videoId);
            if (s == null || f == null) continue;

            // Normalize to 0-1
            double emotion      = s.getEmotionScore()      / 2.0;
            double pitch        = s.getPitchScore()        / 2.0;
            double energy       = s.getEnergyScore()       / 2.0;
            double tone         = s.getToneScore()         / 2.0;
            double speechRate   = s.getSpeechRateScore()   / 2.0;
            double fillerWords  = s.getFillerWordScore()   / 2.0;
            double articulation = s.getArticulationScore() / 2.0;
            double smile        = f.getSmileScore()        / 1.0;
            double eyeContact   = f.getEyeContactScore()   / 2.5;
            double straightFace = f.getStraightFaceScore() / 2.5;

            // Culture dimensions (0-1 raw), then scaled to 1-5
            double teamwork   = 0.20*emotion + 0.20*smile + 0.10*eyeContact + 0.25*tone + 0.25*pitch;
            double customer   = 0.20*emotion + 0.20*smile + 0.20*tone + 0.15*speechRate + 0.10*eyeContact + 0.15*articulation;
            double integrity  = 0.20*emotion + 0.20*energy + 0.20*speechRate + 0.20*straightFace + 0.20*articulation;
            double innovation = 0.30*energy  + 0.30*pitch + 0.30*speechRate + 0.10*emotion;
            double excellence = 0.20*tone + 0.20*pitch + 0.20*articulation + 0.15*straightFace + 0.10*eyeContact + 0.10*fillerWords + 0.05*energy;

            result.put(videoId, new double[]{
                scale(teamwork), scale(customer), scale(integrity), scale(innovation), scale(excellence)
            });
        }
        return result;
    }

    private double scale(double raw) {
        return Math.max(1.0, Math.min(5.0, 1.0 + raw * 4.0));
    }

    public CultureFitScore submitEvaluation(Integer candidateId, String evaluatorName,
                                          Integer communicationScore, Integer teamworkScore,
                                          Integer adaptabilityScore, Integer valuesAlignmentScore, 
                                          String feedback) {
        
        String decision = "PENDING";
        double overall = (communicationScore + teamworkScore + adaptabilityScore + valuesAlignmentScore) / 4.0;
        if (overall >= 7.0) decision = "HIRE";
        else if (overall >= 5.0) decision = "MAYBE";
        else decision = "REJECT";

        CultureFitScore score = new CultureFitScore(
            candidateId, evaluatorName, communicationScore, teamworkScore,
            adaptabilityScore, valuesAlignmentScore, feedback, decision
        );
        
        return repository.save(score);
    }
}
