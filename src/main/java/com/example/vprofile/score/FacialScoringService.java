package com.example.vprofile.score;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy
public class FacialScoringService {
    private static final Logger logger = LoggerFactory.getLogger(FacialScoringService.class);

    @Value("${ffmpeg.path:/usr/bin/ffmpeg}")
    private String ffmpegPath;

    @Autowired
    private FacialScoringRepository facialScoringRepository;
    @Autowired
    private TotalScoreService totalScoreService;
    private CascadeClassifier faceCascade;
    private CascadeClassifier smileCascade;
    private CascadeClassifier eyeCascade;
    private boolean cascadesLoaded = false;

    public FacialScoringService() {
        logger.info("FacialScoringService initialized (cascades will be loaded on first use)");
    }

    private void ensureCascadesLoaded() throws IOException {
        if (cascadesLoaded) {
            return;
        }

        try {
            logger.info("Loading Haar cascades for face, smile, and eyes...");
            faceCascade = loadCascadeFromResource("haarcascades/haarcascade_frontalface_default.xml");
            smileCascade = loadCascadeFromResource("haarcascades/haarcascade_smile.xml");
            eyeCascade = loadCascadeFromResource("haarcascades/haarcascade_eye.xml");

            if (faceCascade == null || faceCascade.empty() || smileCascade == null || smileCascade.empty() || eyeCascade == null || eyeCascade.empty()) {
                throw new RuntimeException("Failed to load one or more Haar cascade classifiers.");
            }

            logger.info("Successfully loaded all Haar cascades.");
            cascadesLoaded = true;
        } catch (IOException e) {
            logger.error("Error loading Haar cascades", e);
            throw e;
        }
    }

    private CascadeClassifier loadCascadeFromResource(String resourcePath) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("Haar cascade file not found: " + resourcePath);
        }

        File tempFile = File.createTempFile("cascade-", ".xml");
        tempFile.deleteOnExit();
        Files.copy(stream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.debug("Cascade resource {} written to temp file: {}", resourcePath, tempFile.getAbsolutePath());

        return new CascadeClassifier(tempFile.getAbsolutePath());
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public double analyzeVideoAndScore(String videoFilePath, Long videoId) {
        try {
            ensureCascadesLoaded();
        } catch (IOException e) {
            logger.error("Cannot analyze video - Haar cascade files not available", e);
            throw new RuntimeException("Facial analysis not available", e);
        }

        logger.info("🎥 Starting facial analysis for video ID: {} (Path: {})", videoId, videoFilePath);

        File tempDir;
        try {
            tempDir = Files.createTempDirectory("frames").toFile();
            tempDir.deleteOnExit();
        } catch (IOException e) {
            logger.error("❌ Failed to create temp frame directory", e);
            throw new RuntimeException("❌ Failed to create temp frame directory", e);
        }

        String outputPattern = new File(tempDir, "frame_%03d.png").getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath, "-i", videoFilePath,
                "-vf", "fps=1",
                outputPattern);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            process.waitFor();
            logger.info("✅ Frames extracted to: {}", tempDir.getAbsolutePath());
        } catch (IOException | InterruptedException e) {
            logger.error("❌ FFmpeg execution failed for video: {}", videoFilePath, e);
            return -1;
        }

        // Analyze frames using OpenCV
        File[] frameFiles = tempDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (frameFiles == null || frameFiles.length == 0) {
            logger.warn("❌ No frames extracted by FFmpeg (Path: {}). Analysis aborted.", videoFilePath);
            return -1;
        }

        // Sort frames numerically (frame_001.png, frame_002.png, etc.)
        Arrays.sort(frameFiles, Comparator.comparing(File::getName));
        logger.info("📂 Processing {} frames in chronological order...", frameFiles.length);

        int frameCount = 0;
        int smileCount = 0;
        int eyeContactCount = 0;
        int straightFaceCount = 0;
        double prevX = -1;
        double prevY = -1;

        // Reuse Size objects to save memory
        Size minFaceSize = new Size(100, 100);
        Size minSmileSize = new Size(30, 30);
        Size minEyeSize = new Size(20, 20);
        Size emptySize = new Size();

        for (File frameFile : frameFiles) {
            Mat frame = opencv_imgcodecs.imread(frameFile.getAbsolutePath());
            if (frame == null || frame.empty()) {
                if (frame != null)
                    frame.release();
                continue;
            }

            frameCount++;
            logger.debug("📽️ Frame #{} ({}): Processing...", frameCount, frameFile.getName());

            Mat gray = new Mat();
            Mat faceROI = null;
            RectVector faces = new RectVector();
            RectVector smiles = new RectVector();
            RectVector eyes = new RectVector();

            try {
                opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
                opencv_imgproc.equalizeHist(gray, gray);

                faceCascade.detectMultiScale(gray, faces, 1.1, 6, 0, minFaceSize, emptySize);
                logger.debug("🧍 Faces detected: {}", faces.size());

                if (faces.size() > 0) {
                    Rect bestFace = faces.get(0);

                    // Stability Fix: If we have a previous position, pick the face closest to it
                    if (prevX != -1) {
                        double minDistance = Double.MAX_VALUE;
                        for (int i = 0; i < faces.size(); i++) {
                            Rect current = faces.get(i);
                            double cx = current.x() + current.width() / 2.0;
                            double cy = current.y() + current.height() / 2.0;
                            double dist = Math.sqrt(Math.pow(cx - prevX, 2) + Math.pow(cy - prevY, 2));

                            // If this face is significantly closer than the current 'best', pick it
                            if (dist < minDistance) {
                                minDistance = dist;
                                bestFace = current;
                            }
                        }
                    } else {
                        // Otherwise pick the largest face
                        long maxArea = (long) bestFace.width() * bestFace.height();
                        for (int i = 1; i < faces.size(); i++) {
                            Rect current = faces.get(i);
                            long area = (long) current.width() * current.height();
                            if (area > maxArea) {
                                maxArea = area;
                                bestFace = current;
                            }
                        }
                    }

                    faceROI = new Mat(gray, bestFace);

                    // Analyze smile
                    smileCascade.detectMultiScale(faceROI, smiles, 1.7, 20, 0, minSmileSize, emptySize);
                    if (smiles.size() > 0) {
                        smileCount++;
                        logger.debug("😊 Smile detected");
                    }

                    // Analyze eye contact
                    eyeCascade.detectMultiScale(faceROI, eyes, 1.1, 10, 0, minEyeSize, emptySize);
                    if (eyes.size() >= 2) {
                        eyeContactCount++;
                        logger.debug("👀 Eye contact detected ({} eyes)", eyes.size());
                    }

                    // Analyze stillness
                    double currentX = bestFace.x() + bestFace.width() / 2.0;
                    double currentY = bestFace.y() + bestFace.height() / 2.0;

                    if (prevX != -1) {
                        double dx = Math.abs(currentX - prevX);
                        double dy = Math.abs(currentY - prevY);
                        // Stability threshold: 15 pixels
                        if (dx < 15 && dy < 15) {
                            straightFaceCount++;
                            logger.debug("✅ Stillness: dx={}/dy={}", String.format("%.2f", dx),
                                    String.format("%.2f", dy));
                        } else {
                            logger.debug("⚠️ Movement: dx={}/dy={}", String.format("%.2f", dx),
                                    String.format("%.2f", dy));
                        }
                    }
                    prevX = currentX;
                    prevY = currentY;
                } else {
                    logger.debug("❌ No face in frame - skipping stillness.");
                    prevX = -1;
                    prevY = -1;
                }
            } finally {
                // Manually release native OpenCV resources to prevent memory leaks
                if (frame != null)
                    frame.release();
                if (gray != null)
                    gray.release();
                if (faceROI != null)
                    faceROI.release();
                if (faces != null)
                    faces.close();
                if (smiles != null)
                    smiles.close();
                if (eyes != null)
                    eyes.close();
            }
        }

        // --- New Scoring Logic (Base 0) ---
        double smileScore = 0.5;
        if (frameCount > 0) {
            double smilePercentage = (double) smileCount / frameCount;
            logger.info("📊 Smile Stats: {}/{} ({}%)", smileCount, frameCount,
                    String.format("%.2f", smilePercentage * 100));
            if (smilePercentage >= 0.20) {
                smileScore = 1.0;
            } else if (smilePercentage > 0.05) {
                smileScore = 0.5;
            }
        }

        double eyeScore = 0.5;
        if (frameCount > 0) {
            double eyePercentage = (double) eyeContactCount / frameCount;
            logger.info("📊 Eye Stats: {}/{} ({}%)", eyeContactCount, frameCount,
                    String.format("%.2f", eyePercentage * 100));
            int eyeBlocks = (int) (eyePercentage / 0.16);
            eyeScore = Math.min(2.5, eyeBlocks * 0.5);
        }

        double straightFaceScore = 0.5;
        if (frameCount > 0) {
            double straightFacePercentage = (double) straightFaceCount / frameCount;
            logger.info("📊 Stillness Stats for ID {}: {}/{} ({}%)", videoId, straightFaceCount, frameCount,
                    String.format("%.2f", straightFacePercentage * 100));
            int straightBlocks = (int) (straightFacePercentage / 0.16);
            straightFaceScore = Math.min(2.5, straightBlocks * 0.5);
        }

        // Penalty if NO faces were detected in most frames (helpful for black screens)
        if (frameCount > 0 && (double) (smileCount + eyeContactCount + straightFaceCount) / frameCount < 0.05) {
            logger.warn("⚠️ Very low face activity - applying penalty.");
            smileScore *= 0.2;
            eyeScore *= 0.2;
            straightFaceScore *= 0.2;
        }

        double finalScore = smileScore + eyeScore + straightFaceScore;
        logger.info("🏁 Final Facial Score for ID {}: {}/6.0", videoId, String.format("%.2f", finalScore));

        FacialScoring facialScoring = new FacialScoring();
        facialScoring.setVideoId(videoId);
        facialScoring.setSmileScore(smileScore);
        facialScoring.setEyeContactScore(eyeScore);
        facialScoring.setStraightFaceScore(straightFaceScore);
        facialScoring.setTotalScore(finalScore);
        facialScoringRepository.save(facialScoring);

        totalScoreService.computeTotalScoreIfReady(videoId);

        return finalScore;
    }

}
