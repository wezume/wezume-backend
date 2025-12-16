package com.example.vprofile.ffmpeg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VideoProcessingService {

    @Autowired
    private FrameExtractor frameExtractor;

    private static final double SKIN_TONE_THRESHOLD = 60.0; // Threshold for skin tone percentage violation

    private static final Map<String, int[]> SKIN_TONE_RANGES = Map.of(
        "Asian", new int[]{100, 255, 80, 210, 50, 160},
        "European", new int[]{150, 255, 100, 210, 70, 180},
        "American", new int[]{110, 255, 90, 220, 60, 170},
        "African", new int[]{80, 180, 50, 150, 30, 100}
    );

    public boolean checkForVisualProfanity(String videoPath) throws Exception {
        String frameDirectory = frameExtractor.extractFrames(videoPath);

        File frameFolder = new File(frameDirectory);
        File[] frames = frameFolder.listFiles((dir, name) -> name.endsWith(".jpg"));

        if (frames == null || frames.length == 0) {
            throw new Exception("No frames extracted for analysis.");
        }

        for (File frame : frames) {
            double skinTonePercentage = calculateSkinTonePercentage(frame);
            System.out.println("Frame: " + frame.getName() + " Skin Tone: " + skinTonePercentage + "%");
            if (skinTonePercentage > SKIN_TONE_THRESHOLD) {
                return true; // Profane content detected
            }
        }

        return false; // No profane content found
    }

    private double calculateSkinTonePercentage(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                throw new IOException("Invalid image file: " + imageFile.getName());
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int totalPixels = width * height;
            int skinToneCount = 0;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int rgb = image.getRGB(x, y);
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    if (isSkinTone(red, green, blue)) {
                        skinToneCount++;
                    }
                }
            }

            return (double) skinToneCount / totalPixels * 100;
        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
        }

        return 0.0;
    }

    private boolean isSkinTone(int red, int green, int blue) {
        for (int[] range : SKIN_TONE_RANGES.values()) {
            if (red >= range[0] && red <= range[1] &&
                green >= range[2] && green <= range[3] &&
                blue >= range[4] && blue <= range[5]) {
                return true;
            }
        }
        return false;
    }
}
