package com.example.vprofile.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

@Service
public class FFmpegService {

    /**
     * Compresses a video file and applies a watermark using an optimized FFmpeg command.
     *
     * @param inputFile  The source video file.
     * @param outputFile The destination for the compressed video file.
     * @throws IOException          if an I/O error occurs.
     * @throws InterruptedException if the process is interrupted.
     */
    public void compressVideo(File inputFile, File outputFile) throws IOException, InterruptedException {
        // Hardcoded paths for ffmpeg and the watermark image
       String ffmpegPath = "/usr/bin/ffmpeg";

        // Path to the watermark image
        String watermarkPath = "/home/wezume/htdocs/wezume.in/img/watermark.png";
        
        // --- IMPORTANT ---
        // For this to be fast, you must resize your 'circle.png' image to the
        // desired final dimensions (e.g., 300x150 pixels) *before* running this code.
        // This avoids forcing FFmpeg to scale the image for every video frame.

        // Scale to max 480p + cap 24fps — interview/talking-head quality (same as Zoom/Teams),
        // ~6-9x fewer pixels than 1080p 60fps iPhone input, making libx264 encode in ~3-5s.
        String[] command = {
            ffmpegPath,
            "-i", inputFile.getAbsolutePath(),
            "-i", watermarkPath,
            "-filter_complex", "[0:v]scale=854:480:force_original_aspect_ratio=decrease[v];[v][1:v]overlay=x=W-w-80:y=20[out]",
            "-map", "[out]",
            "-map", "0:a?",
            "-vcodec", "libx264",
            "-preset", "ultrafast",
            "-crf", "28",
            "-r", "24",
            "-movflags", "+faststart",
            "-f", "mp4",
            "-y",
            outputFile.getAbsolutePath()
        };

        System.out.println("Running FFmpeg command: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // We print line-by-line to see progress in real-time
                System.out.println("FFMPEG: " + line);
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("FFmpeg full output:\n" + output);
            throw new IOException("FFmpeg process failed with exit code " + exitCode);
        }
    }
}

