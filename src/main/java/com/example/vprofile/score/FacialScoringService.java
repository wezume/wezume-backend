package com.example.vprofile.score;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy
public class FacialScoringService {
    private static final Logger logger = LoggerFactory.getLogger(FacialScoringService.class);

    @Autowired(required = false)
    private FacialScoringRepository facialScoringRepository;

    @Autowired(required = false)
    private TotalScoreService totalScoreService;

    public FacialScoringService() {
        logger.warn("⚠️  FacialScoringService - OpenCV dependencies not available (facial analysis disabled)");
    }

    public double analyzeVideoAndScore(String videoFilePath, Long videoId) {
        logger.error("❌ Facial analysis not available - OpenCV dependencies disabled");
        throw new UnsupportedOperationException("Facial analysis service is not available - dependencies not configured");
    }
}
