package com.example.vprofile.analysis;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.vprofile.score.FacialScoringService;
import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;

@Component
@Lazy
public class AnalysisScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisScheduler.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired(required = false)
    private FacialScoringService facialScoringService;

    @Autowired
    private AudioAnalysisService audioAnalysisService;

    // Scheduler for Facial Analysis - Runs every minute
    @Scheduled(cron = "0 * * * * *")
    public void scheduleFacialAnalysis() {
        if (facialScoringService == null) {
            logger.warn("⚠️ Facial Analysis Service not available (Haar cascade files missing)");
            return;
        }

        logger.info("🕒 Facial Analysis Scheduler Triggered...");
        Optional<Video> videoOpt = videoRepository.findFirstMissingFacialScore();

        if (videoOpt.isPresent()) {
            Video video = videoOpt.get();
            try {
                logger.info("🎥 Staring scheduled facial analysis for video ID: {}", video.getId());
                facialScoringService.analyzeVideoAndScore(video.getUrl(), video.getId());
            } catch (Exception e) {
                logger.error("❌ Error in scheduled facial analysis for video ID {}: {}", video.getId(), e.getMessage(),
                        e);
            }
        } else {
            logger.info("✅ No videos pending facial analysis.");
        }
    }

    // Scheduler for Audio Analysis - Runs every minute (offset by 30s to avoid
    // heavy overlap)
    @Scheduled(initialDelay = 30000, fixedRate = 60000)
    public void scheduleAudioAnalysis() {
        logger.info("🕒 Audio Analysis Scheduler Triggered...");
        Optional<Video> videoOpt = videoRepository.findFirstMissingSpeechScore();

        if (videoOpt.isPresent()) {
            Video video = videoOpt.get();
            try {
                logger.info("🎵 Starting scheduled audio analysis for video ID: {}", video.getId());
                audioAnalysisService.analyzeAudio(video.getUrl(), video.getId());
            } catch (Exception e) {
                logger.error("❌ Error in scheduled audio analysis for video ID {}: {}", video.getId(), e.getMessage(),
                        e);
            }
        } else {
            logger.info("✅ No videos pending audio analysis.");
        }
    }
}
