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
