package com.example.vprofile.score;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FacialScoringService {
    @Autowired
    private FacialScoringRepository facialScoringRepository;
    @Autowired
    private TotalScoreService totalScoreService;
    private final CascadeClassifier faceCascade;
    private final CascadeClassifier smileCascade;
    private final CascadeClassifier eyeCascade;

    public FacialScoringService() {
        try {
            System.out.println("Loading Haar cascade for face...");
            faceCascade = loadCascadeFromResource("haarcascades/haarcascade_frontalface_default.xml");

            System.out.println("Loading Haar cascade for smile...");
            smileCascade = loadCascadeFromResource("haarcascades/haarcascade_smile.xml");

            System.out.println("Loading Haar cascade for eye...");
            eyeCascade = loadCascadeFromResource("haarcascades/haarcascade_eye.xml");

            if (faceCascade.empty() || smileCascade.empty() || eyeCascade.empty()) {
                throw new RuntimeException("Failed to load one or more Haar cascade classifiers.");
            }

            System.out.println("Successfully loaded all Haar cascades.");
        } catch (IOException e) {
            throw new RuntimeException("Error loading Haar cascades", e);
        }
    }

    private CascadeClassifier loadCascadeFromResource(String resourcePath) throws IOException {
        System.out.println("Loading resource: " + resourcePath);
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("Haar cascade file not found: " + resourcePath);
        }

        File tempFile = File.createTempFile("cascade-", ".xml");
        tempFile.deleteOnExit();
        Files.copy(stream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Cascade loaded and written to temp file: " + tempFile.getAbsolutePath());

        return new CascadeClassifier(tempFile.getAbsolutePath());
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public double analyzeVideoAndScore(String videoFilePath, Long videoId) {
        System.out.println("Extracting frames using FFmpeg...");

        File tempDir;
        try {
            tempDir = Files.createTempDirectory("frames").toFile();
            tempDir.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to create temp frame directory", e);
        }

        String outputPattern = new File(tempDir, "frame_%03d.png").getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(
                "/usr/bin/ffmpeg", "-i", videoFilePath,
                "-vf", "fps=1",
                outputPattern);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            process.waitFor();
            System.out.println("✅ Frames extracted to: " + tempDir.getAbsolutePath());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return -1;
        }

        // Analyze frames using OpenCV
        File[] frameFiles = tempDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (frameFiles == null || frameFiles.length == 0) {
            System.err.println("❌ No frames extracted by FFmpeg.");
            return -1;
        }

        int frameCount = 0;
        int smileCount = 0;
        int eyeContactCount = 0;
        int straightFaceCount = 0;
        Point previousCenter = null;

        for (File frameFile : frameFiles) {
            Mat frame = opencv_imgcodecs.imread(frameFile.getAbsolutePath());
            if (frame.empty())
                continue;

            frameCount++;
            System.out.println("📽️ Processing frame #" + frameCount + " - " + frameFile.getName());

            Mat gray = new Mat();
            opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.equalizeHist(gray, gray);

            RectVector faces = new RectVector();
            faceCascade.detectMultiScale(gray, faces);
            System.out.println("🧍 Faces detected: " + faces.size());

            for (int i = 0; i < faces.size(); i++) {
                Rect face = faces.get(i);
                Mat faceROI = new Mat(gray, face);

                RectVector smiles = new RectVector();
                smileCascade.detectMultiScale(faceROI, smiles);
                if (smiles.size() > 0)
                    smileCount++;

                RectVector eyes = new RectVector();
                eyeCascade.detectMultiScale(faceROI, eyes);
                if (eyes.size() >= 2)
                    eyeContactCount++;

                try (Point currentCenter = new Point(face.x() + face.width() / 2, face.y() + face.height() / 2)) {
                    if (previousCenter != null && !previousCenter.isNull() && !currentCenter.isNull()) {
                        double dx = Math.abs(currentCenter.x() - previousCenter.x());
                        double dy = Math.abs(currentCenter.y() - previousCenter.y());
                        if (dx < 15 && dy < 15)
                            straightFaceCount++;
                    }
                    previousCenter = currentCenter;
                }
            }
        }

        // --- Scoring logic stays the same ---
        double smileScore = 1;
        if (frameCount > 0) {
            double smilePercentage = (double) smileCount / frameCount;
            if (smilePercentage >= 0.20) {
                smileScore = 2.0;
            }
        }

        double eyeScore = 1;
        if (frameCount > 0) {
            double eyePercentage = (double) eyeContactCount / frameCount;
            int eyeBlocks = (int) (eyePercentage / 0.20);
            eyeScore += eyeBlocks * 0.4;
            eyeScore = Math.min(eyeScore, 2.5);
        }

        double straightFaceScore = 1;
        if (frameCount > 0) {
            double straightFacePercentage = (double) straightFaceCount / frameCount;
            int straightBlocks = (int) (straightFacePercentage / 0.20);
            straightFaceScore += straightBlocks * 0.25;
            straightFaceScore = Math.min(straightFaceScore, 2.5);
        }

        double finalScore = smileScore + eyeScore + straightFaceScore;
        System.out.println("✅ Final Total Facial Score: " + finalScore + " / 6.0");

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
