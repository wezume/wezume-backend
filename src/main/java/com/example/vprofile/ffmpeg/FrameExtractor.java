package com.example.vprofile.ffmpeg;

import java.io.File;
import java.nio.file.Files;

import org.springframework.stereotype.Component;

@Component
public class FrameExtractor {

    public String extractFrames(String videoPath) throws Exception {
        // Create a temporary directory for frames
        File tempDir = Files.createTempDirectory("video_frames").toFile();

        String frameOutputPattern = tempDir.getAbsolutePath() + "/frame_%04d.jpg";
        String command = String.format("ffmpeg -i %s -vf fps=1 %s", videoPath, frameOutputPattern);

        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();

        if (process.exitValue() == 0) {
            System.out.println("Frames extracted successfully to: " + tempDir.getAbsolutePath());
        } else {
            System.out.println("Error during frame extraction.");
            throw new Exception("Failed to extract frames using ffmpeg.");
        }

        return tempDir.getAbsolutePath();
    }
    
}

